import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/bouncy_tap.dart';
import '../../core/widgets/community_top_header.dart';
import '../../core/widgets/empty_state.dart';
import '../../core/widgets/fade_slide_entrance.dart';
import '../../data/models/meeting_model.dart';
import '../../data/models/project_model.dart';
import '../../viewmodels/community_viewmodel.dart';
import '../../viewmodels/payments_viewmodel.dart';
import '../../viewmodels/voting_viewmodel.dart';
import '../payments/simulated_payment_sheet.dart';

/// Pantalla de Proyectos y Vida Comunitaria
/// Replicando con máxima fidelidad la interfaz Stitch (Colonia Conecta).
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
  int _selectedProjectFilter = 0; // 0: Todos, 1: En Recaudación, 2: En Ejecución, 3: Completados

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      widget.viewModel.loadCommunityData();
      widget.paymentsViewModel?.loadPayments(idMiembro: widget.idMiembro);
    });
  }

  void _showMeetingDetail(MeetingModel meeting, bool isDark) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (ctx) => Container(
        padding: const EdgeInsets.fromLTRB(22, 16, 22, 32),
        decoration: BoxDecoration(
          color: isDark ? const Color(0xFF1E293B) : Colors.white,
          borderRadius: const BorderRadius.vertical(top: Radius.circular(28)),
          boxShadow: const [
            BoxShadow(
              color: Color(0x33000000),
              blurRadius: 28,
              offset: Offset(0, -8),
            ),
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
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: AppColors.stitchEmeraldContainer,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    meeting.estado.toUpperCase(),
                    style: const TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.bold,
                      color: Color(0xFF065F46),
                    ),
                  ),
                ),
                Text(
                  meeting.tipo,
                  style: const TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                    color: AppColors.stitchSapphire,
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
                color: isDark ? Colors.white : AppColors.stitchTextPrimary,
              ),
            ),
            if (meeting.hasLinkedProject) ...[
              const SizedBox(height: 8),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: const Color(0xFFCCFBF1),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.architecture_rounded, size: 14, color: Color(0xFF0F766E)),
                    const SizedBox(width: 6),
                    Text(
                      'Proyecto vinculado: ${meeting.nombreProyecto}',
                      style: const TextStyle(
                        fontSize: 11.5,
                        fontWeight: FontWeight.w700,
                        color: Color(0xFF0F766E),
                      ),
                    ),
                  ],
                ),
              ),
            ],
            const SizedBox(height: 14),
            Row(
              children: [
                const Icon(Icons.event_rounded, size: 16, color: AppColors.stitchSapphire),
                const SizedBox(width: 8),
                Text(
                  '${Formatters.date(meeting.fechaHora)} · ${Formatters.time(meeting.fechaHora)}',
                  style: TextStyle(
                    fontSize: 13,
                    color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 6),
            Row(
              children: [
                const Icon(Icons.location_on_outlined, size: 16, color: AppColors.stitchSapphire),
                const SizedBox(width: 8),
                Text(
                  meeting.lugar,
                  style: TextStyle(
                    fontSize: 13,
                    color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                  ),
                ),
              ],
            ),
            if (meeting.descripcion != null && meeting.descripcion!.isNotEmpty) ...[
              const SizedBox(height: 14),
              Text(
                meeting.descripcion!,
                style: TextStyle(
                  fontSize: 12.5,
                  color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                  height: 1.4,
                ),
              ),
            ],
            const SizedBox(height: 22),
            BouncyTap(
              onTap: () => Navigator.of(context).pop(),
              child: Container(
                height: 48,
                width: double.infinity,
                decoration: BoxDecoration(
                  color: AppColors.stitchSapphire,
                  borderRadius: BorderRadius.circular(24),
                ),
                child: const Center(
                  child: Text(
                    'Entendido',
                    style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 14),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  List<PaymentModel> _getUserContributionsForProject(ProjectModel project) {
    if (widget.paymentsViewModel == null) return const [];
    return widget.paymentsViewModel!.payments.where((p) {
      final matchesId = p.idProyecto != null && p.idProyecto == project.id;
      final matchesName = p.nombreProyecto != null &&
          p.nombreProyecto!.trim().toLowerCase() == project.nombre.trim().toLowerCase();
      return (matchesId || matchesName) && p.isPaid;
    }).toList();
  }

  void _showContributeSheet(ProjectModel project) {
    if (widget.paymentsViewModel == null) return;
    final now = DateTime.now();
    final periodo = '${now.year}-${now.month.toString().padLeft(2, '0')}';

    SimulatedPaymentSheet.show(
      context,
      viewModel: widget.paymentsViewModel!,
      idMiembro: widget.idMiembro ?? 1,
      title: 'Aporte a ${project.nombre}',
      description: 'Contribución solidaria para el financiamiento de la obra comunal.',
      defaultAmount: project.aportePorMiembro > 0 ? project.aportePorMiembro : 25.00,
      isAmountEditable: true,
      periodoMes: periodo,
      idProyecto: project.id,
      nombreProyecto: project.nombre,
      onPaymentSuccess: (p) async {
        await widget.viewModel.loadCommunityData();
        if (widget.paymentsViewModel != null) {
          await widget.paymentsViewModel!.loadPayments(idMiembro: widget.idMiembro);
        }
        if (mounted) setState(() {});
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      backgroundColor: isDark ? const Color(0xFF0F172A) : AppColors.stitchCanvasLight,
      body: SafeArea(
        bottom: false,
        child: RefreshIndicator(
          onRefresh: () async {
            await Future.wait([
              widget.viewModel.loadCommunityData(),
              if (widget.paymentsViewModel != null)
                widget.paymentsViewModel!.loadPayments(idMiembro: widget.idMiembro),
            ]);
          },
          color: AppColors.stitchSapphire,
          child: SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 110),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 1. Barra Superior Estandarizada
                const FadeSlideEntrance(
                  delay: Duration.zero,
                  child: CommunityTopHeader(
                    title: 'Proyectos',
                    subtitle: 'Residencial Las Flores',
                    residentName: 'Carlos Mendoza',
                  ),
                ),
                const SizedBox(height: 12),

                // 2. Encabezado de Proyectos
                FadeSlideEntrance(
                  delay: const Duration(milliseconds: 40),
                  child: _buildProjectsHeader(isDark),
                ),
                const SizedBox(height: 16),

                // 3. Tarjeta de Transparencia Ciudadana & Ciclo de Vida
                FadeSlideEntrance(
                  delay: const Duration(milliseconds: 80),
                  child: _buildTransparencyInfographicCard(isDark),
                ),
                const SizedBox(height: 18),

                // 4. Selector de Píldoras de Filtro
                FadeSlideEntrance(
                  delay: const Duration(milliseconds: 120),
                  child: _buildFilterPills(isDark),
                ),
                const SizedBox(height: 16),

                // 5. Lista de Proyectos de Obras y Mejoras
                FadeSlideEntrance(
                  delay: const Duration(milliseconds: 160),
                  child: _buildProjectsList(isDark),
                ),
                const SizedBox(height: 24),

                // 6. Asambleas y Reuniones Comunitarias
                FadeSlideEntrance(
                  delay: const Duration(milliseconds: 200),
                  child: _buildMeetingsSection(isDark),
                ),
                const SizedBox(height: 24),

                // 7. Banner "¿Tienes una idea para la colonia?"
                FadeSlideEntrance(
                  delay: const Duration(milliseconds: 240),
                  child: _buildProposeProjectBanner(isDark),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  /// 2. Encabezado de Proyectos
  Widget _buildProjectsHeader(bool isDark) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
          decoration: BoxDecoration(
            color: const Color(0xFFD1FAE5),
            borderRadius: BorderRadius.circular(20),
          ),
          child: const Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.how_to_vote_rounded, size: 14, color: Color(0xFF065F46)),
              SizedBox(width: 5),
              Text(
                'Fondo Vecinal 2026',
                style: TextStyle(
                  fontSize: 10.5,
                  fontWeight: FontWeight.w700,
                  color: Color(0xFF065F46),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 8),
        Text(
          'Proyectos de la Colonia',
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.w800,
            letterSpacing: -0.5,
            color: isDark ? Colors.white : AppColors.stitchTextPrimary,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          'Conoce, vota y sigue las mejoras colectivas para Residencial Las Flores.',
          style: TextStyle(
            fontSize: 13,
            color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
            height: 1.35,
          ),
        ),
      ],
    );
  }

  /// 3. Tarjeta de Transparencia Ciudadana & Ciclo de Vida
  Widget _buildTransparencyInfographicCard(bool isDark) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: isDark ? const Color(0xFF1E293B) : Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(
          color: isDark ? const Color(0xFF334155) : const Color(0xFFE2E8F0),
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: isDark ? 0.2 : 0.04),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Expanded(
                child: Row(
                  children: [
                    Icon(Icons.account_balance_rounded, size: 18, color: AppColors.stitchSapphire),
                    SizedBox(width: 6),
                    Expanded(
                      child: Text(
                        'TRANSPARENCIA CIUDADANA',
                        style: TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.w800,
                          letterSpacing: 0.6,
                          color: AppColors.stitchSapphire,
                        ),
                        overflow: TextOverflow.ellipsis,
                        maxLines: 1,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: isDark ? const Color(0xFF0F172A) : const Color(0xFFEEF2FF),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Text(
                  'Actualizado hoy',
                  style: TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                    color: AppColors.stitchSapphire,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Text(
            'Total recaudado comunal en obras',
            style: TextStyle(
              fontSize: 12,
              color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
            ),
          ),
          const SizedBox(height: 4),
          Row(
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              Text(
                '\$4,280',
                style: TextStyle(
                  fontSize: 34,
                  fontWeight: FontWeight.w800,
                  color: isDark ? Colors.white : AppColors.stitchSapphire,
                  letterSpacing: -0.8,
                ),
              ),
              const SizedBox(width: 8),
              const Text(
                'USD este año',
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.bold,
                  color: AppColors.stitchEmerald,
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          // 3 Metric Badges
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: isDark ? const Color(0xFF0F172A) : const Color(0xFFF8FAFC),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Row(
              children: [
                _buildMetricBox('2', 'En Recaudación', AppColors.stitchSapphire, isDark),
                const SizedBox(width: 8),
                _buildMetricBox('1', 'En Obra', AppColors.stitchTeal, isDark),
                const SizedBox(width: 8),
                _buildMetricBox('4', 'Completados', AppColors.stitchEmerald, isDark),
              ],
            ),
          ),
          const SizedBox(height: 16),
          // 4-step Stepper
          Row(
            children: [
              _buildStepItem('1. Votación', Icons.how_to_vote_rounded, false, isDark),
              _buildStepLine(isDark),
              _buildStepItem('2. Colecta', Icons.savings_rounded, true, isDark),
              _buildStepLine(isDark),
              _buildStepItem('3. Ejecución', Icons.engineering_rounded, false, isDark),
              _buildStepLine(isDark),
              _buildStepItem('4. Entrega', Icons.task_alt_rounded, false, isDark),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildMetricBox(String count, String label, Color color, bool isDark) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 10),
        decoration: BoxDecoration(
          color: isDark ? const Color(0xFF1E293B) : Colors.white,
          borderRadius: BorderRadius.circular(12),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: isDark ? 0.15 : 0.03),
              blurRadius: 4,
              offset: const Offset(0, 1),
            ),
          ],
        ),
        child: Column(
          children: [
            Text(
              count,
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: color,
              ),
            ),
            const SizedBox(height: 2),
            Text(
              label,
              style: TextStyle(
                fontSize: 9.5,
                color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStepItem(String title, IconData icon, bool isActive, bool isDark) {
    return Column(
      children: [
        Container(
          width: 26,
          height: 26,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: isActive
                ? AppColors.stitchSapphire
                : (isDark ? const Color(0xFF1E293B) : const Color(0xFFEEF2FF)),
          ),
          child: Icon(
            icon,
            size: 14,
            color: isActive ? Colors.white : AppColors.stitchSapphire,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          title,
          style: TextStyle(
            fontSize: 8.5,
            fontWeight: isActive ? FontWeight.bold : FontWeight.w500,
            color: isActive ? AppColors.stitchSapphire : AppColors.stitchTextSecondary,
          ),
        ),
      ],
    );
  }

  Widget _buildStepLine(bool isDark) {
    return Expanded(
      child: Container(
        height: 1.5,
        margin: const EdgeInsets.only(bottom: 14),
        color: isDark ? const Color(0xFF334155) : const Color(0xFFE2E8F0),
      ),
    );
  }

  /// 4. Selector de Píldoras de Filtro
  Widget _buildFilterPills(bool isDark) {
    final all = widget.viewModel.projects;
    final activeCount = all.where((p) => p.isActive).length;
    final recaudacionCount = all.where((p) => p.isEnRecaudacion && p.isActive).length;
    final ejecucionCount = all.where((p) => p.isEnEjecucion && p.isActive).length;
    final finalizadosCount = all.where((p) => p.isFinalizado).length;

    final filters = [
      'Obras Activas ($activeCount)',
      'En Recaudación ($recaudacionCount)',
      'En Ejecución ($ejecucionCount)',
      if (finalizadosCount > 0) 'Concluidos ($finalizadosCount)',
    ];

    return SizedBox(
      height: 36,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: filters.length,
        separatorBuilder: (_, __) => const SizedBox(width: 8),
        itemBuilder: (context, index) {
          final isSelected = _selectedProjectFilter == index;
          return BouncyTap(
            onTap: () => setState(() => _selectedProjectFilter = index),
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 200),
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(20),
                color: isSelected
                    ? AppColors.stitchSapphire
                    : (isDark ? const Color(0xFF1E293B) : const Color(0xFFF1F5F9)),
                boxShadow: isSelected
                    ? [
                        BoxShadow(
                          color: AppColors.stitchSapphire.withValues(alpha: 0.25),
                          blurRadius: 6,
                          offset: const Offset(0, 2),
                        ),
                      ]
                    : null,
              ),
              child: Center(
                child: Text(
                  filters[index],
                  style: TextStyle(
                    fontSize: 11.5,
                    fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
                    color: isSelected
                        ? Colors.white
                        : (isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary),
                  ),
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  /// 5. Lista de Proyectos de Obras y Mejoras
  Widget _buildProjectsList(bool isDark) {
    return ListenableBuilder(
      listenable: Listenable.merge([
        widget.viewModel,
        if (widget.paymentsViewModel != null) widget.paymentsViewModel!,
      ]),
      builder: (context, _) {
        final all = widget.viewModel.projects;
        final List<ProjectModel> projects;

        switch (_selectedProjectFilter) {
          case 1:
            projects = all.where((p) => p.isEnRecaudacion && p.isActive).toList();
            break;
          case 2:
            projects = all.where((p) => p.isEnEjecucion && p.isActive).toList();
            break;
          case 3:
            projects = all.where((p) => p.isFinalizado).toList();
            break;
          case 0:
          default:
            // Por defecto solo muestra los proyectos activos; los finalizados desaparecen de la vista principal
            projects = all.where((p) => p.isActive).toList();
            break;
        }

        if (projects.isEmpty) {
          final emptyMsg = _selectedProjectFilter == 3
              ? 'No hay proyectos finalizados registrados.'
              : 'No hay proyectos activos en esta categoría en este momento.';
          return EmptyStateWidget(
            icon: Icons.architecture_rounded,
            title: 'Sin proyectos',
            message: emptyMsg,
          );
        }

        return ListView.separated(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          itemCount: projects.length,
          separatorBuilder: (_, __) => const SizedBox(height: 16),
          itemBuilder: (context, index) {
            return _buildProjectCard(isDark, projects[index], index);
          },
        );
      },
    );
  }

  Widget _buildProjectCard(bool isDark, ProjectModel project, int index) {
    final progreso = project.progresoPorcentaje;
    final recaudado = project.montoRecaudado;
    final meta = project.presupuesto;
    final faltante = project.montoPendiente;

    // Colores e imágenes según estado real del proyecto
    final isFinalizado = project.isFinalizado;
    final isEjecucion = project.isEnEjecucion;
    final isRecaudacion = project.isEnRecaudacion;

    final String statusLabel = isFinalizado
        ? 'Finalizado'
        : (isEjecucion ? 'En Ejecución' : 'En Recaudación');
    final Color statusColor = isFinalizado
        ? AppColors.stitchEmerald
        : (isEjecucion ? AppColors.stitchTeal : AppColors.stitchSapphire);

    return Container(
      decoration: BoxDecoration(
        color: isDark ? const Color(0xFF1E293B) : Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(
          color: isDark ? const Color(0xFF334155) : const Color(0xFFE2E8F0),
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: isDark ? 0.2 : 0.04),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Imagen / Banner Superior
          ClipRRect(
            borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
            child: Container(
              height: 130,
              width: double.infinity,
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: [
                    isFinalizado
                        ? const Color(0xFF064E3B)
                        : (isEjecucion ? const Color(0xFF0D9488) : const Color(0xFF0F172A)),
                    isFinalizado
                        ? const Color(0xFF047857)
                        : (isEjecucion ? const Color(0xFF115E59) : const Color(0xFF1E293B)),
                  ],
                ),
              ),
              child: Stack(
                children: [
                  Positioned(
                    top: 12,
                    left: 14,
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(16),
                      ),
                      child: Row(
                        children: [
                          Container(
                            width: 6,
                            height: 6,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              color: statusColor,
                            ),
                          ),
                          const SizedBox(width: 5),
                          Text(
                            statusLabel,
                            style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: AppColors.stitchTextPrimary),
                          ),
                        ],
                      ),
                    ),
                  ),
                  if (project.hasVoting || isRecaudacion)
                    Positioned(
                      top: 12,
                      right: 14,
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: const Color(0xFF065F46),
                          borderRadius: BorderRadius.circular(16),
                        ),
                        child: const Row(
                          children: [
                            Text('👍 ', style: TextStyle(fontSize: 11)),
                            Text(
                              '84% a favor',
                              style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: Colors.white),
                            ),
                          ],
                        ),
                      ),
                    ),
                  Positioned(
                    bottom: 12,
                    left: 14,
                    right: 14,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'PRIORIDAD COMUNAL #1',
                          style: TextStyle(
                            fontSize: 9.5,
                            fontWeight: FontWeight.bold,
                            letterSpacing: 0.8,
                            color: Color(0xFF93C5FD),
                          ),
                        ),
                        Text(
                          project.nombre,
                          style: const TextStyle(
                            fontSize: 17,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
          // Contenido de la Tarjeta
          Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  project.descripcion,
                  style: TextStyle(
                    fontSize: 12.5,
                    color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                    height: 1.4,
                  ),
                ),
                const SizedBox(height: 14),
                // Progreso Financiado
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Flexible(
                      child: Text(
                        '${progreso.toStringAsFixed(0)}% financiado',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.bold,
                          color: isDark ? Colors.white : AppColors.stitchSapphire,
                        ),
                        overflow: TextOverflow.ellipsis,
                        maxLines: 1,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Flexible(
                      child: Text(
                        'Meta: ${Formatters.currency(meta)}',
                        style: TextStyle(
                          fontSize: 12,
                          color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                        ),
                        overflow: TextOverflow.ellipsis,
                        maxLines: 1,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 6),
                ClipRRect(
                  borderRadius: BorderRadius.circular(6),
                  child: LinearProgressIndicator(
                    value: (progreso / 100).clamp(0.0, 1.0),
                    minHeight: 8,
                    backgroundColor: isDark ? const Color(0xFF334155) : const Color(0xFFE2E8F0),
                    valueColor: const AlwaysStoppedAnimation<Color>(AppColors.stitchEmerald),
                  ),
                ),
                const SizedBox(height: 6),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Flexible(
                      child: Text(
                        '${Formatters.currency(recaudado)} recaudados',
                        style: const TextStyle(fontSize: 11.5, fontWeight: FontWeight.bold, color: AppColors.stitchEmerald),
                        overflow: TextOverflow.ellipsis,
                        maxLines: 1,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Flexible(
                      child: Text(
                        'Faltan ${Formatters.currency(faltante)}',
                        style: const TextStyle(fontSize: 11.5, fontWeight: FontWeight.w600, color: Color(0xFFDC2626)),
                        overflow: TextOverflow.ellipsis,
                        maxLines: 1,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 14),
                // Caja Tu Aporte Voluntario (dinámica según si ya aportó o no)
                () {
                  final userProjectPayments = _getUserContributionsForProject(project);
                  final totalUserContributed =
                      userProjectPayments.fold(0.0, (sum, p) => sum + p.monto);
                  final hasUserContributed = userProjectPayments.isNotEmpty;

                  return Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: hasUserContributed
                          ? (isDark
                              ? const Color(0xFF064E3B).withValues(alpha: 0.3)
                              : const Color(0xFFECFDF5))
                          : const Color(0xFFFEF2F2),
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(
                        color: hasUserContributed
                            ? (isDark
                                ? const Color(0xFF059669).withValues(alpha: 0.4)
                                : const Color(0xFFA7F3D0))
                            : const Color(0xFFFECACA),
                      ),
                    ),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Icon(
                          Icons.volunteer_activism_rounded,
                          color: hasUserContributed
                              ? const Color(0xFF059669)
                              : const Color(0xFFDC2626),
                          size: 20,
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                children: [
                                  Text(
                                    'Tu Aporte Voluntario',
                                    style: TextStyle(
                                      fontSize: 12,
                                      fontWeight: FontWeight.bold,
                                      color: hasUserContributed
                                          ? (isDark ? const Color(0xFF6EE7B7) : const Color(0xFF065F46))
                                          : const Color(0xFF991B1B),
                                    ),
                                  ),
                                  Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
                                    decoration: BoxDecoration(
                                      color: hasUserContributed
                                          ? const Color(0xFF10B981).withValues(alpha: 0.15)
                                          : const Color(0xFFDC2626).withValues(alpha: 0.1),
                                      borderRadius: BorderRadius.circular(10),
                                    ),
                                    child: Text(
                                      hasUserContributed
                                          ? 'Aportado: ${Formatters.currency(totalUserContributed)}'
                                          : 'Pendiente',
                                      style: TextStyle(
                                        fontSize: 10,
                                        fontWeight: FontWeight.bold,
                                        color: hasUserContributed
                                            ? const Color(0xFF059669)
                                            : const Color(0xFFDC2626),
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 3),
                              Text(
                                project.isFinalizado
                                    ? (hasUserContributed
                                        ? 'Registraste ${userProjectPayments.length} aporte${userProjectPayments.length > 1 ? "s" : ""} por un total de ${Formatters.currency(totalUserContributed)}. ¡Muchas gracias por tu contribución comunal!'
                                        : 'Esta obra comunal ya fue concluida con éxito y cumplió su objetivo presupuestario.')
                                    : (hasUserContributed
                                        ? 'Registras ${userProjectPayments.length} aporte${userProjectPayments.length > 1 ? "s" : ""} acumulado${userProjectPayments.length > 1 ? "s" : ""}. ¡Puedes continuar aportando voluntariamente cuantas veces desees!'
                                        : 'Aporte sugerido: ${Formatters.currency(project.aportePorMiembro)} por vivienda para cubrir presupuesto en fecha.'),
                                style: TextStyle(
                                  fontSize: 11,
                                  color: hasUserContributed
                                      ? (isDark ? AppColors.stitchTextMuted : const Color(0xFF047857))
                                      : const Color(0xFF7F1D1D),
                                  height: 1.3,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  );
                }(),
                const SizedBox(height: 16),
                // Botón CTA Aportar
                () {
                  if (project.isFinalizado) {
                    return Container(
                      height: 48,
                      width: double.infinity,
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(24),
                        color: isDark ? const Color(0xFF064E3B).withValues(alpha: 0.3) : const Color(0xFFECFDF5),
                        border: Border.all(color: const Color(0xFF059669).withValues(alpha: 0.4)),
                      ),
                      child: const Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.check_circle_rounded, color: Color(0xFF059669), size: 18),
                          SizedBox(width: 8),
                          Text(
                            'Proyecto Concluido con Éxito',
                            style: TextStyle(color: Color(0xFF059669), fontSize: 13, fontWeight: FontWeight.bold),
                          ),
                        ],
                      ),
                    );
                  }

                  final userProjectPayments = _getUserContributionsForProject(project);
                  final hasUserContributed = userProjectPayments.isNotEmpty;

                  return BouncyTap(
                    onTap: () => _showContributeSheet(project),
                    child: Container(
                      height: 50,
                      width: double.infinity,
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(25),
                        color: AppColors.stitchSapphire,
                        boxShadow: [
                          BoxShadow(
                            color: AppColors.stitchSapphire.withValues(alpha: 0.3),
                            blurRadius: 10,
                            offset: const Offset(0, 4),
                          ),
                        ],
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(
                            hasUserContributed
                                ? Icons.volunteer_activism_rounded
                                : Icons.volunteer_activism_outlined,
                            color: Colors.white,
                            size: 18,
                          ),
                          const SizedBox(width: 8),
                          Text(
                            hasUserContributed
                                ? 'Aportar de nuevo al proyecto'
                                : 'Ver detalles y aportar',
                            style: const TextStyle(color: Colors.white, fontSize: 13.5, fontWeight: FontWeight.bold),
                          ),
                        ],
                      ),
                    ),
                  );
                }(),
                const SizedBox(height: 6),
                Center(
                  child: Text(
                    'Recibo fiscal digital emitido al instante',
                    style: TextStyle(
                      fontSize: 11,
                      color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  /// 6. Asambleas y Reuniones Comunitarias
  Widget _buildMeetingsSection(bool isDark) {
    return ListenableBuilder(
      listenable: widget.viewModel,
      builder: (context, _) {
        final meetings = widget.viewModel.meetings;
        if (meetings.isEmpty) return const SizedBox.shrink();

        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Asambleas y Reuniones',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: isDark ? Colors.white : AppColors.stitchTextPrimary,
                  ),
                ),
                const Text(
                  'Historial',
                  style: TextStyle(
                    fontSize: 12.5,
                    fontWeight: FontWeight.bold,
                    color: AppColors.stitchSapphire,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            ListView.separated(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: meetings.length,
              separatorBuilder: (_, __) => const SizedBox(height: 10),
              itemBuilder: (context, index) {
                final meeting = meetings[index];
                return BouncyTap(
                  onTap: () => _showMeetingDetail(meeting, isDark),
                  child: Container(
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      color: isDark ? const Color(0xFF1E293B) : Colors.white,
                      borderRadius: BorderRadius.circular(18),
                      border: Border.all(
                        color: isDark ? const Color(0xFF334155) : const Color(0xFFE2E8F0),
                      ),
                    ),
                    child: Row(
                      children: [
                        Container(
                          width: 40,
                          height: 40,
                          decoration: BoxDecoration(
                            color: const Color(0xFFEEF2FF),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: const Icon(Icons.groups_rounded, color: AppColors.stitchSapphire, size: 22),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                meeting.titulo,
                                style: TextStyle(
                                  fontSize: 13.5,
                                  fontWeight: FontWeight.bold,
                                  color: isDark ? Colors.white : AppColors.stitchTextPrimary,
                                ),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                '${Formatters.date(meeting.fechaHora)} · ${meeting.lugar}',
                                style: TextStyle(
                                  fontSize: 11.5,
                                  color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                                ),
                              ),
                            ],
                          ),
                        ),
                        const Icon(Icons.chevron_right_rounded, color: AppColors.stitchTextSecondary),
                      ],
                    ),
                  ),
                );
              },
            ),
          ],
        );
      },
    );
  }

  /// 7. Banner "¿Tienes una idea para la colonia?"
  Widget _buildProposeProjectBanner(bool isDark) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        color: AppColors.stitchNavy,
        boxShadow: [
          BoxShadow(
            color: AppColors.stitchNavy.withValues(alpha: 0.3),
            blurRadius: 16,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: const BoxDecoration(
                  shape: BoxShape.circle,
                  color: Color(0xFF1E3A8A),
                ),
                child: const Icon(Icons.lightbulb_rounded, color: Color(0xFF67E8F9), size: 20),
              ),
              const SizedBox(width: 10),
              const Expanded(
                child: Text(
                  '¿Tienes una idea para la colonia?',
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.bold,
                    color: Colors.white,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          const Text(
            'Las iniciativas con más de 15 firmas pasan directamente a la mesa de votación comunitaria mensual.',
            style: TextStyle(
              fontSize: 12,
              color: Color(0xFFCBD5E1),
              height: 1.4,
            ),
          ),
          const SizedBox(height: 16),
          BouncyTap(
            onTap: () {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text('Formulario de propuesta de proyecto abierto para firmas comunitarias.'),
                  backgroundColor: AppColors.stitchSapphire,
                ),
              );
            },
            child: Container(
              height: 46,
              width: double.infinity,
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(23),
              ),
              child: const Center(
                child: Text(
                  'Proponer nuevo proyecto',
                  style: TextStyle(
                    fontSize: 13.5,
                    fontWeight: FontWeight.bold,
                    color: AppColors.stitchNavy,
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
