import 'package:flutter/material.dart';
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/bouncy_tap.dart';
import '../../core/widgets/empty_state.dart';
import '../../core/widgets/fade_slide_entrance.dart';
import '../../core/widgets/glass_container.dart';
import '../../core/widgets/neo_glass_container.dart';
import '../../core/widgets/status_badge.dart';
import '../../data/models/meeting_model.dart';
import '../../data/models/project_model.dart';
import '../../viewmodels/community_viewmodel.dart';
import '../../viewmodels/payments_viewmodel.dart';
import '../../viewmodels/voting_viewmodel.dart';
import '../payments/simulated_payment_sheet.dart';
import 'voting_view.dart';

/// Pantalla de Vida Comunitaria: Asambleas, Proyectos y Desglose Financiero.
class CommunityView extends StatefulWidget {
  final CommunityViewModel viewModel;
  final PaymentsViewModel? paymentsViewModel;
  final VotingViewModel? votingViewModel;
  final int? idMiembro;

  const CommunityView({
    super.key,
    required this.viewModel,
    this.paymentsViewModel,
    this.votingViewModel,
    this.idMiembro,
  });

  @override
  State<CommunityView> createState() => _CommunityViewState();
}

class _CommunityViewState extends State<CommunityView> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      widget.viewModel.loadCommunityData();
    });
  }

  void _showMeetingDetail(MeetingModel meeting, bool isDark) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (ctx) => GlassContainer(
        borderRadius: const BorderRadius.vertical(top: Radius.circular(30)),
        padding: const EdgeInsets.fromLTRB(22, 16, 22, 32),
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            (isDark ? const Color(0xF2131D33) : const Color(0xF5FFFFFF)),
            (isDark ? const Color(0xF20B1120) : const Color(0xFAF4F7FC)),
          ],
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Center(
              child: Container(
                width: 44,
                height: 4.5,
                decoration: BoxDecoration(
                  color: Colors.grey.withValues(alpha: 0.35),
                  borderRadius: BorderRadius.circular(3),
                ),
              ),
            ),
            const SizedBox(height: 18),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                StatusBadge.fromStatus(meeting.estado),
                Text(
                  meeting.tipo,
                  style: const TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                    color: AppColors.brandBlue,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Text(
              meeting.titulo,
              style: TextStyle(
                fontSize: 19,
                fontWeight: FontWeight.bold,
                color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
              ),
            ),
            if (meeting.descripcion != null && meeting.descripcion!.isNotEmpty) ...[
              const SizedBox(height: 8),
              Text(
                meeting.descripcion!,
                style: TextStyle(
                  fontSize: 13,
                  color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                ),
              ),
            ],
            const SizedBox(height: 16),
            if (meeting.hasLinkedProject) ...[
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.brandBlue.withValues(alpha: 0.10),
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: AppColors.brandBlue.withValues(alpha: 0.25)),
                ),
                child: Row(
                  children: [
                    const Icon(FluentIcons.building_retail_toolbox_24_filled, color: AppColors.brandBlue, size: 20),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'PROYECTO TRATADO EN ESTA ASAMBLEA',
                            style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: AppColors.brandBlue),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            meeting.nombreProyecto!,
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.bold,
                              color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
            ],
            Row(
              children: [
                Icon(FluentIcons.calendar_clock_24_regular, size: 18, color: isDark ? Colors.white70 : AppColors.brandBlue),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    Formatters.dateTime(meeting.fechaHora),
                    style: TextStyle(fontSize: 13, color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                Icon(FluentIcons.location_24_regular, size: 18, color: isDark ? Colors.white70 : AppColors.brandBlue),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    meeting.lugar,
                    style: TextStyle(fontSize: 13, color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 14),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Convocados: ${meeting.totalConvocados}',
                  style: TextStyle(fontSize: 12, color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight),
                ),
                Text(
                  'Asistentes: ${meeting.totalAsistentes} (${meeting.porcentajeAsistencia.toStringAsFixed(1)}%)',
                  style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.neoEmerald),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  void _openProjectDonationSheet(ProjectModel project) {
    if (widget.paymentsViewModel == null || widget.idMiembro == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Para realizar un aporte voluntario debes tener un miembro asignado.'),
        ),
      );
      return;
    }

    final suggestedAmount = project.aportePorMiembro > 0 ? project.aportePorMiembro : 20.00;
    final now = DateTime.now();
    final currentMonth = '${now.year}-${now.month.toString().padLeft(2, '0')}';

    SimulatedPaymentSheet.show(
      context,
      viewModel: widget.paymentsViewModel!,
      idMiembro: widget.idMiembro!,
      title: 'Aporte a Proyecto Comunal',
      description: 'Aporte para la obra "${project.nombre}".',
      defaultAmount: suggestedAmount,
      isAmountEditable: true,
      periodoMes: currentMonth,
      idProyecto: project.id,
      nombreProyecto: project.nombre,
      onPaymentSuccess: (payment) {
        widget.viewModel.loadCommunityData();
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('¡Aporte de \$${payment.monto.toStringAsFixed(2)} registrado para ${project.nombre}!'),
            backgroundColor: AppColors.successGreen,
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return AmbientBackground(
      child: RefreshIndicator(
        onRefresh: () => widget.viewModel.loadCommunityData(),
        color: AppColors.brandBlue,
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 110),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              FadeSlideEntrance(
                delay: Duration.zero,
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Comunidad',
                            style: TextStyle(
                              fontSize: 24,
                              fontWeight: FontWeight.bold,
                              color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            'Asambleas, participación y obras comunales',
                            style: TextStyle(
                              fontSize: 13,
                              color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                            ),
                          ),
                        ],
                      ),
                    ),
                    if (widget.votingViewModel != null)
                      BouncyTap(
                        scaleDown: 0.88,
                        onTap: () {
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (_) => VotingView(
                                viewModel: widget.votingViewModel!,
                                idMiembro: widget.idMiembro,
                              ),
                            ),
                          );
                        },
                        child: Container(
                          padding: const EdgeInsets.all(8),
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: AppColors.brandBlue.withValues(alpha: 0.12),
                          ),
                          child: const Icon(
                            FluentIcons.vote_24_filled,
                            color: AppColors.brandBlue,
                            size: 22,
                          ),
                        ),
                      ),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // Banner destacado para Votaciones Comunitarias
              if (widget.votingViewModel != null)
                Padding(
                  padding: const EdgeInsets.only(bottom: 16),
                  child: FadeSlideEntrance(
                    delay: const Duration(milliseconds: 90),
                    child: BouncyTap(
                      scaleDown: 0.96,
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => VotingView(
                              viewModel: widget.votingViewModel!,
                              idMiembro: widget.idMiembro,
                            ),
                          ),
                        );
                      },
                      child: NeoGlassContainer(
                        padding: const EdgeInsets.all(16),
                        child: Row(
                          children: [
                            NeoGlassContainer.circle(
                              size: 44,
                              child: Icon(
                                FluentIcons.poll_24_regular,
                                color: isDark ? Colors.white : AppColors.brandBlue,
                                size: 22,
                              ),
                            ),
                            const SizedBox(width: 14),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    'Procesos de Votación y Consulta',
                                    style: TextStyle(
                                      fontWeight: FontWeight.bold,
                                      fontSize: 14,
                                      color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                                    ),
                                  ),
                                  const SizedBox(height: 2),
                                  Text(
                                    'Participa en las decisiones y proyectos vecinales',
                                    style: TextStyle(
                                      fontSize: 11.5,
                                      color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            Icon(
                              FluentIcons.chevron_right_16_regular,
                              size: 16,
                              color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              const SizedBox(height: 8),

              // Selector de Pestañas estilo Neo-Glass
              ListenableBuilder(
                listenable: widget.viewModel,
                builder: (context, _) {
                  final selected = widget.viewModel.selectedTab;
                  return NeoGlassContainer(
                    padding: const EdgeInsets.all(5),
                    borderRadius: BorderRadius.circular(20),
                    child: Row(
                      children: [
                        Expanded(
                          child: BouncyTap(
                            scaleDown: 0.96,
                            onTap: () => widget.viewModel.setSelectedTab(0),
                            child: AnimatedContainer(
                              duration: const Duration(milliseconds: 200),
                              padding: const EdgeInsets.symmetric(vertical: 10),
                              decoration: BoxDecoration(
                                borderRadius: BorderRadius.circular(16),
                                color: selected == 0
                                    ? (isDark ? const Color(0xFF1E293B) : AppColors.brandBlue)
                                    : Colors.transparent,
                              ),
                              child: Center(
                                child: Text(
                                  'Asambleas (${widget.viewModel.meetings.length})',
                                  style: TextStyle(
                                    fontSize: 13,
                                    fontWeight: selected == 0 ? FontWeight.bold : FontWeight.w500,
                                    color: selected == 0 ? Colors.white : (isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight),
                                  ),
                                ),
                              ),
                            ),
                          ),
                        ),
                        Expanded(
                          child: BouncyTap(
                            scaleDown: 0.96,
                            onTap: () => widget.viewModel.setSelectedTab(1),
                            child: AnimatedContainer(
                              duration: const Duration(milliseconds: 200),
                              padding: const EdgeInsets.symmetric(vertical: 10),
                              decoration: BoxDecoration(
                                borderRadius: BorderRadius.circular(16),
                                color: selected == 1
                                    ? (isDark ? const Color(0xFF1E293B) : AppColors.brandBlue)
                                    : Colors.transparent,
                              ),
                              child: Center(
                                child: Text(
                                  'Proyectos (${widget.viewModel.projects.length})',
                                  style: TextStyle(
                                    fontSize: 13,
                                    fontWeight: selected == 1 ? FontWeight.bold : FontWeight.w500,
                                    color: selected == 1 ? Colors.white : (isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight),
                                  ),
                                ),
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  );
                },
              ),
              const SizedBox(height: 20),

              // Contenido según la pestaña seleccionada
              ListenableBuilder(
                listenable: widget.viewModel,
                builder: (context, _) {
                  if (widget.viewModel.isLoading) {
                    return const Center(
                      child: Padding(
                        padding: EdgeInsets.symmetric(vertical: 40),
                        child: CircularProgressIndicator(),
                      ),
                    );
                  }

                  if (widget.viewModel.selectedTab == 0) {
                    return _buildMeetingsList(isDark);
                  } else {
                    return _buildProjectsList(isDark);
                  }
                },
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildMeetingsList(bool isDark) {
    final meetings = widget.viewModel.meetings;
    if (meetings.isEmpty) {
      return EmptyStateWidget(
        icon: FluentIcons.calendar_cancel_24_regular,
        title: 'No hay asambleas programadas',
        message: 'Las nuevas convocatorias de reunión aparecerán aquí.',
        actionLabel: 'Comprobar',
        onAction: () => widget.viewModel.loadCommunityData(),
      );
    }

    return ListView.separated(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: meetings.length,
      separatorBuilder: (_, __) => const SizedBox(height: 12),
      itemBuilder: (context, index) {
        final meeting = meetings[index];
        return BouncyTap(
          scaleDown: 0.98,
          onTap: () => _showMeetingDetail(meeting, isDark),
          child: NeoGlassContainer(
            padding: const EdgeInsets.all(18),
            borderRadius: BorderRadius.circular(22),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    StatusBadge.fromStatus(meeting.estado),
                    Text(
                      meeting.tipo,
                      style: TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.bold,
                        color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Text(
                  meeting.titulo,
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                  ),
                ),
                if (meeting.descripcion != null && meeting.descripcion!.isNotEmpty) ...[
                  const SizedBox(height: 4),
                  Text(
                    meeting.descripcion!,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      fontSize: 12.5,
                      color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                    ),
                  ),
                ],
                if (meeting.hasLinkedProject) ...[
                  const SizedBox(height: 8),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: AppColors.brandBlue.withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(color: AppColors.brandBlue.withValues(alpha: 0.25)),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(FluentIcons.building_retail_toolbox_24_filled, size: 13, color: AppColors.brandBlue),
                        const SizedBox(width: 5),
                        Flexible(
                          child: Text(
                            'Proyecto: ${meeting.nombreProyecto}',
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontSize: 11,
                              fontWeight: FontWeight.bold,
                              color: AppColors.brandBlue,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
                const SizedBox(height: 10),
                Row(
                  children: [
                    Icon(
                      FluentIcons.calendar_clock_24_regular,
                      size: 16,
                      color: isDark ? Colors.white70 : AppColors.brandBlue,
                    ),
                    const SizedBox(width: 6),
                    Text(
                      Formatters.date(meeting.fechaHora),
                      style: TextStyle(fontSize: 12.5, color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight),
                    ),
                    const SizedBox(width: 14),
                    Icon(
                      FluentIcons.location_24_regular,
                      size: 16,
                      color: isDark ? Colors.white70 : AppColors.brandBlue,
                    ),
                    const SizedBox(width: 6),
                    Expanded(
                      child: Text(
                        meeting.lugar,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(fontSize: 12.5, color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Icon(
                      FluentIcons.chevron_right_12_regular,
                      size: 12,
                      color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                    ),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildProjectsList(bool isDark) {
    final projects = widget.viewModel.projects;
    if (projects.isEmpty) {
      return EmptyStateWidget(
        icon: FluentIcons.building_retail_toolbox_24_regular,
        title: 'No hay proyectos en curso',
        message: 'Los proyectos de infraestructura y mejoras comunales se publicarán en este módulo.',
        actionLabel: 'Actualizar',
        onAction: () => widget.viewModel.loadCommunityData(),
      );
    }

    return ListView.separated(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: projects.length,
      separatorBuilder: (_, __) => const SizedBox(height: 16),
      itemBuilder: (context, index) {
        final project = projects[index];
        final progressRatio = (project.progresoPorcentaje / 100).clamp(0.0, 1.0);

        return NeoGlassContainer(
          padding: const EdgeInsets.all(18),
          borderRadius: BorderRadius.circular(22),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Encabezado con estado y presupuesto
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  StatusBadge.fromStatus(project.estado),
                  Text(
                    'Presupuesto: ${Formatters.currency(project.presupuesto)}',
                    style: const TextStyle(
                      fontSize: 12.5,
                      fontWeight: FontWeight.bold,
                      color: AppColors.neoEmerald,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),

              // Título y Descripción
              Text(
                project.nombre,
                style: TextStyle(
                  fontSize: 17,
                  fontWeight: FontWeight.bold,
                  color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                project.descripcion,
                style: TextStyle(
                  fontSize: 13,
                  color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                ),
              ),

              // Badge si fue aprobado en votación
              if (project.hasVoting) ...[
                const SizedBox(height: 10),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                  decoration: BoxDecoration(
                    color: AppColors.actionPurple.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(color: AppColors.actionPurple.withValues(alpha: 0.3)),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(FluentIcons.vote_20_regular, size: 14, color: AppColors.actionPurple),
                      const SizedBox(width: 6),
                      Flexible(
                        child: Text(
                          'Aprobado en Consulta: ${project.tituloVotacion}',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: AppColors.actionPurple),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
              const SizedBox(height: 14),

              // Barra de Progreso Financiero
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        'Recaudación Financiera',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                          color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                        ),
                      ),
                      Text(
                        '${project.progresoPorcentaje.toStringAsFixed(1)}% financiado',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                          color: progressRatio >= 1.0 ? AppColors.neoEmerald : AppColors.brandBlue,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  ClipRRect(
                    borderRadius: BorderRadius.circular(6),
                    child: LinearProgressIndicator(
                      value: progressRatio,
                      minHeight: 8,
                      backgroundColor: isDark ? const Color(0xFF1E293B) : const Color(0xFFE2E8F0),
                      valueColor: AlwaysStoppedAnimation<Color>(
                        progressRatio >= 1.0 ? AppColors.neoEmerald : AppColors.brandBlue,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 14),

              // Grid de Desglose Financiero
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: isDark ? const Color(0x301E293B) : const Color(0x0C0F172A),
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(
                    color: isDark ? const Color(0x18FFFFFF) : const Color(0x100F172A),
                  ),
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Recaudado',
                            style: TextStyle(fontSize: 10.5, color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            Formatters.currency(project.montoRecaudado),
                            style: const TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.bold,
                              color: AppColors.neoEmerald,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Container(width: 1, height: 30, color: isDark ? Colors.white12 : Colors.black12),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Pendiente',
                            style: TextStyle(fontSize: 10.5, color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            Formatters.currency(project.montoPendiente),
                            style: TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.bold,
                              color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Container(width: 1, height: 30, color: isDark ? Colors.white12 : Colors.black12),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Cuota sugerida',
                            style: TextStyle(fontSize: 10.5, color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            Formatters.currency(project.aportePorMiembro),
                            style: const TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.bold,
                              color: AppColors.brandSky,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),

              // Botón de Aporte Voluntario
              if (project.canReceiveContributions) ...[
                const SizedBox(height: 14),
                BouncyTap(
                  scaleDown: 0.96,
                  onTap: () => _openProjectDonationSheet(project),
                  child: Container(
                    width: double.infinity,
                    padding: const EdgeInsets.symmetric(vertical: 12),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(14),
                      gradient: const LinearGradient(
                        colors: [AppColors.neoEmerald, Color(0xFF059669)],
                      ),
                      boxShadow: [
                        BoxShadow(
                          color: AppColors.neoEmerald.withValues(alpha: 0.3),
                          blurRadius: 8,
                          offset: const Offset(0, 3),
                        ),
                      ],
                    ),
                    child: const Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(FluentIcons.payment_20_filled, color: Colors.white, size: 18),
                        SizedBox(width: 8),
                        Text(
                          'Aportar a este Proyecto',
                          style: TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ],
          ),
        );
      },
    );
  }
}
