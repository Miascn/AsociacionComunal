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
import '../../viewmodels/community_viewmodel.dart';
import '../../viewmodels/voting_viewmodel.dart';
import 'voting_view.dart';

/// Pantalla de Vida Comunitaria: Asambleas y Proyectos Comunitarios.
class CommunityView extends StatefulWidget {
  final CommunityViewModel viewModel;
  final VotingViewModel? votingViewModel;
  final int? idMiembro;

  const CommunityView({
    super.key,
    required this.viewModel,
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

              // Banner destacado para Votaciones Comunitarias con BouncyTap y FadeSlideEntrance
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
                                  'Asambleas',
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
                                  'Proyectos',
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
      separatorBuilder: (_, __) => const SizedBox(height: 12),
      itemBuilder: (context, index) {
        final project = projects[index];
        return BouncyTap(
          scaleDown: 0.98,
          child: NeoGlassContainer(
            padding: const EdgeInsets.all(18),
            borderRadius: BorderRadius.circular(22),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    StatusBadge.fromStatus(project.estado),
                    Text(
                      'Presupuesto: ${Formatters.currency(project.presupuesto)}',
                      style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.neoEmerald),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                Text(
                  project.nombre,
                  style: TextStyle(
                    fontSize: 16,
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
              ],
            ),
          ),
        );
      },
    );
  }
}
