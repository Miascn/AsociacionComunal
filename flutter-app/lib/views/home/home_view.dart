import 'package:flutter/material.dart';
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/bouncy_tap.dart';
import '../../core/widgets/fade_slide_entrance.dart';
import '../../core/widgets/glass_container.dart';
import '../../core/widgets/mini_trend_chart.dart';
import '../../core/widgets/neo_glass_container.dart';
import '../../core/widgets/pulsing_badge.dart';
import '../../core/widgets/status_badge.dart';
import '../../data/models/auth_models.dart';
import '../../data/models/meeting_model.dart';
import '../../data/models/voting_model.dart';
import '../../data/repositories/notifications_repository.dart';
import '../../viewmodels/home_viewmodel.dart';
import 'notifications_sheet.dart';

/// Vista principal (Home) inspirada en diseño Fintech moderno con Glassmorphism.
class HomeView extends StatefulWidget {
  final HomeViewModel viewModel;
  final MeResponse profile;
  final VoidCallback onNavigateToPayments;
  final VoidCallback onNavigateToCommunity;
  final VoidCallback onNavigateToVoting;

  const HomeView({
    super.key,
    required this.viewModel,
    required this.profile,
    required this.onNavigateToPayments,
    required this.onNavigateToCommunity,
    required this.onNavigateToVoting,
  });

  @override
  State<HomeView> createState() => _HomeViewState();
}

class _HomeViewState extends State<HomeView> {
  final NotificationsRepository _notificationsRepo = NotificationsRepository();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      widget.viewModel.loadDashboardData(idMiembro: widget.profile.member?.id);
    });
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final member = widget.profile.member;
    final displayName = member?.firstName ?? widget.profile.user.username;

    return AmbientBackground(
      child: RefreshIndicator(
        onRefresh: () => widget.viewModel.loadDashboardData(idMiembro: member?.id),
        color: AppColors.brandBlue,
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 110),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 1. Header con Avatar, Saludo Dinámico y Campana de Notificaciones
              FadeSlideEntrance(
                delay: Duration.zero,
                child: _buildHeader(isDark, displayName),
              ),
              const SizedBox(height: 20),

              // 2. Banner Condicional de Votación Pendiente (solo si existe y no ha participado)
              ListenableBuilder(
                listenable: widget.viewModel,
                builder: (context, _) {
                  final pending = widget.viewModel.pendingVoting;
                  if (pending == null) return const SizedBox.shrink();
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 20),
                    child: FadeSlideEntrance(
                      delay: const Duration(milliseconds: 60),
                      child: _buildPendingVotingBanner(isDark, pending),
                    ),
                  );
                },
              ),

              // 3. Tarjeta Hero de Balance / Aportaciones con Acciones
              FadeSlideEntrance(
                delay: const Duration(milliseconds: 90),
                child: _buildHeroBalanceCard(isDark),
              ),
              const SizedBox(height: 22),

              // 4. Tarjeta Condicional de Próxima Reunión Relevante
              ListenableBuilder(
                listenable: widget.viewModel,
                builder: (context, _) {
                  final meeting = widget.viewModel.nextMeeting;
                  if (meeting == null) return const SizedBox.shrink();
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 22),
                    child: FadeSlideEntrance(
                      delay: const Duration(milliseconds: 140),
                      child: _buildNextMeetingCard(isDark, meeting),
                    ),
                  );
                },
              ),

              // 5. Botones de Acción Rápida (Aportar, Asambleas, Votaciones)
              FadeSlideEntrance(
                delay: const Duration(milliseconds: 180),
                child: _buildActionTiles(context),
              ),
              const SizedBox(height: 26),

              // 6. Resumen de Proyectos de la Comunidad
              FadeSlideEntrance(
                delay: const Duration(milliseconds: 220),
                child: _buildProjectsSummary(isDark),
              ),
              const SizedBox(height: 26),

              // 7. Directorio Comunal Real (GET /api/directiva/actual)
              FadeSlideEntrance(
                delay: const Duration(milliseconds: 270),
                child: _buildCommunityContacts(isDark),
              ),
              const SizedBox(height: 28),

              // 8. Historial de Aportaciones Recientes
              FadeSlideEntrance(
                delay: const Duration(milliseconds: 320),
                child: _buildRecentTransactionsSection(isDark),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeader(bool isDark, String displayName) {
    final greeting = '${HomeViewModel.getGreeting()}, $displayName';

    return Row(
      children: [
        // Avatar circular con relieve Neo-Glass
        NeoGlassContainer.circle(
          size: 48,
          child: Container(
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: isDark ? const Color(0xFF1E293B) : const Color(0xFFE2E8F0),
            ),
            child: Center(
              child: Text(
                Formatters.initials(displayName),
                style: TextStyle(
                  color: isDark ? Colors.white : AppColors.brandBlue,
                  fontWeight: FontWeight.bold,
                  fontSize: 17,
                ),
              ),
            ),
          ),
        ),
        const SizedBox(width: 14),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                greeting,
                style: TextStyle(
                  fontSize: 17,
                  fontWeight: FontWeight.bold,
                  color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                'Portal de Autoservicio del Miembro',
                style: TextStyle(
                  fontSize: 12.5,
                  fontWeight: FontWeight.w500,
                  color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                ),
              ),
            ],
          ),
        ),
        // Botón de Notificaciones Neo-Glass con PulsingBadge dinámico
        ListenableBuilder(
          listenable: widget.viewModel,
          builder: (context, _) {
            final dynamicNotifs = _notificationsRepo.buildNotifications(
              payments: widget.viewModel.recentPayments,
              polls: widget.viewModel.pendingVoting != null ? [widget.viewModel.pendingVoting!] : [],
              meetings: widget.viewModel.nextMeeting != null ? [widget.viewModel.nextMeeting!] : [],
              projects: widget.viewModel.activeProjects,
              currentMemberId: widget.profile.member?.id,
            );
            final unreadCount = _notificationsRepo.countUnread(dynamicNotifs);

            return BouncyTap(
              scaleDown: 0.88,
              onTap: () {
                NotificationsSheet.show(
                  context,
                  notifications: dynamicNotifs,
                  onNavigateToPayments: widget.onNavigateToPayments,
                  onNavigateToCommunity: widget.onNavigateToCommunity,
                  onNavigateToVoting: widget.onNavigateToVoting,
                );
              },
              child: NeoGlassContainer.circle(
                size: 44,
                child: Stack(
                  clipBehavior: Clip.none,
                  children: [
                    Icon(
                      FluentIcons.alert_24_regular,
                      size: 22,
                      color: isDark ? Colors.white : AppColors.textPrimaryLight,
                    ),
                    if (unreadCount > 0)
                      const Positioned(
                        right: 0,
                        top: 0,
                        child: PulsingBadge(
                          minScale: 0.8,
                          maxScale: 1.3,
                          child: SizedBox(
                            width: 8,
                            height: 8,
                            child: DecoratedBox(
                              decoration: BoxDecoration(
                                color: AppColors.errorRed,
                                shape: BoxShape.circle,
                              ),
                            ),
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            );
          },
        ),
      ],
    );
  }

  Widget _buildPendingVotingBanner(bool isDark, VotingModel poll) {
    return BouncyTap(
      scaleDown: 0.98,
      onTap: widget.onNavigateToVoting,
      child: NeoGlassContainer(
        padding: const EdgeInsets.all(16),
        borderRadius: BorderRadius.circular(20),
        accentColor: AppColors.warningAmber.withValues(alpha: 0.12),
        child: Row(
          children: [
            NeoGlassContainer.circle(
              size: 42,
              accentColor: AppColors.warningAmber.withValues(alpha: 0.20),
              child: const Icon(
                FluentIcons.vote_24_filled,
                color: AppColors.warningAmber,
                size: 20,
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Text(
                        'Hay una votación pendiente',
                        style: TextStyle(
                          fontSize: 13.5,
                          fontWeight: FontWeight.bold,
                          color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                        ),
                      ),
                      const SizedBox(width: 6),
                      const PulsingBadge(
                        minScale: 0.8,
                        maxScale: 1.3,
                        child: SizedBox(
                          width: 7,
                          height: 7,
                          child: DecoratedBox(
                            decoration: BoxDecoration(
                              color: AppColors.warningAmber,
                              shape: BoxShape.circle,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 3),
                  Text(
                    poll.titulo,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      fontSize: 12,
                      color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 10),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(12),
                gradient: const LinearGradient(
                  colors: [AppColors.warningAmber, Color(0xFFD97706)],
                ),
                boxShadow: [
                  BoxShadow(
                    color: AppColors.warningAmber.withValues(alpha: 0.35),
                    blurRadius: 8,
                    offset: const Offset(0, 3),
                  ),
                ],
              ),
              child: const Text(
                'Votar ahora',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 11.5,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeroBalanceCard(bool isDark) {
    return ListenableBuilder(
      listenable: widget.viewModel,
      builder: (context, _) {
        final totalPaid = widget.viewModel.totalContributed;
        final pending = widget.viewModel.pendingBalance;

        return NeoGlassContainer.hero(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Fila Superior: Título + Monto Grande a la izquierda, Badge Neo a la derecha
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Total Aportado',
                        style: TextStyle(
                          color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                          fontSize: 13,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        Formatters.currency(totalPaid),
                        style: TextStyle(
                          color: isDark ? Colors.white : AppColors.textPrimaryLight,
                          fontSize: 28,
                          fontWeight: FontWeight.w800,
                          letterSpacing: -0.5,
                        ),
                      ),
                    ],
                  ),
                  // Icono de Comunidad Neo en la esquina superior derecha
                  NeoGlassContainer.circle(
                    size: 42,
                    child: Icon(
                      FluentIcons.people_community_24_regular,
                      color: isDark ? Colors.white70 : AppColors.brandBlue,
                      size: 20,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),

              // Gráfica de Onda Fluida con Meses
              const MiniTrendChart(
                primaryColor: AppColors.neoEmerald,
                secondaryColor: AppColors.neoEmerald,
                height: 62,
              ),
              const SizedBox(height: 16),

              // Separador sutil
              Container(
                height: 1,
                color: isDark
                    ? Colors.white.withValues(alpha: 0.08)
                    : const Color(0x140F172A),
              ),
              const SizedBox(height: 12),

              // Fila Inferior: Actividad Reciente + Estado
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Estado de cuenta',
                    style: TextStyle(
                      fontSize: 12,
                      color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  Row(
                    children: [
                      Text(
                        pending > 0 ? 'PENDIENTE' : 'SOLVENTE',
                        style: TextStyle(
                          color: pending > 0 ? AppColors.warningAmber : AppColors.neoEmerald,
                          fontSize: 10.5,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(width: 4),
                      Icon(
                        FluentIcons.chevron_right_12_regular,
                        size: 12,
                        color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                      ),
                    ],
                  ),
                ],
              ),
              const SizedBox(height: 14),

              // Acciones: "Ver estado de cuenta" y "Pagar cuota"
              Row(
                children: [
                  Expanded(
                    child: BouncyTap(
                      scaleDown: 0.95,
                      onTap: widget.onNavigateToPayments,
                      child: Container(
                        padding: const EdgeInsets.symmetric(vertical: 10),
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(14),
                          color: isDark ? const Color(0x301E293B) : const Color(0x100F172A),
                          border: Border.all(
                            color: isDark ? const Color(0x30FFFFFF) : const Color(0x1A0F172A),
                          ),
                        ),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(
                              FluentIcons.document_bullet_list_20_regular,
                              size: 16,
                              color: isDark ? Colors.white70 : AppColors.brandBlue,
                            ),
                            const SizedBox(width: 6),
                            Text(
                              'Ver estado de cuenta',
                              style: TextStyle(
                                fontSize: 11.5,
                                fontWeight: FontWeight.w600,
                                color: isDark ? Colors.white : AppColors.textPrimaryLight,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: BouncyTap(
                      scaleDown: 0.95,
                      onTap: widget.onNavigateToPayments,
                      child: Container(
                        padding: const EdgeInsets.symmetric(vertical: 10),
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(14),
                          gradient: AppColors.brandGradient,
                          boxShadow: [
                            BoxShadow(
                              color: AppColors.brandBlue.withValues(alpha: 0.3),
                              blurRadius: 8,
                              offset: const Offset(0, 3),
                            ),
                          ],
                        ),
                        child: const Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(
                              FluentIcons.payment_20_filled,
                              size: 16,
                              color: Colors.white,
                            ),
                            SizedBox(width: 6),
                            Text(
                              'Pagar cuota',
                              style: TextStyle(
                                fontSize: 11.5,
                                fontWeight: FontWeight.bold,
                                color: Colors.white,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildNextMeetingCard(bool isDark, MeetingModel meeting) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              'Próxima Reunión',
              style: TextStyle(
                fontSize: 17,
                fontWeight: FontWeight.bold,
                color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
              ),
            ),
            TextButton(
              onPressed: widget.onNavigateToCommunity,
              child: const Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text('Ver reunión', style: TextStyle(fontSize: 12.5)),
                  SizedBox(width: 4),
                  Icon(FluentIcons.chevron_right_12_regular, size: 12),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
        BouncyTap(
          scaleDown: 0.98,
          onTap: widget.onNavigateToCommunity,
          child: NeoGlassContainer(
            padding: const EdgeInsets.all(16),
            borderRadius: BorderRadius.circular(20),
            accentColor: AppColors.brandBlue.withValues(alpha: 0.08),
            child: Row(
              children: [
                NeoGlassContainer.circle(
                  size: 44,
                  child: const Icon(
                    FluentIcons.calendar_clock_24_regular,
                    color: AppColors.brandBlue,
                    size: 22,
                  ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        meeting.titulo,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.bold,
                          color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Row(
                        children: [
                          Icon(
                            FluentIcons.clock_16_regular,
                            size: 13,
                            color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                          ),
                          const SizedBox(width: 4),
                          Expanded(
                            child: Text(
                              Formatters.dateTime(meeting.fechaHora),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: TextStyle(
                                fontSize: 11.5,
                                color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                              ),
                            ),
                          ),
                        ],
                      ),
                      if (meeting.lugar.isNotEmpty) ...[
                        const SizedBox(height: 2),
                        Row(
                          children: [
                            Icon(
                              FluentIcons.location_16_regular,
                              size: 13,
                              color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                            ),
                            const SizedBox(width: 4),
                            Expanded(
                              child: Text(
                                meeting.lugar,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(
                                  fontSize: 11.5,
                                  color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ],
                  ),
                ),
                const SizedBox(width: 10),
                BouncyTap(
                  scaleDown: 0.92,
                  onTap: widget.onNavigateToCommunity,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(12),
                      color: isDark ? const Color(0xFF1E293B) : AppColors.brandBlue,
                    ),
                    child: const Text(
                      'Ver reunión',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 11.5,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildActionTiles(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Acciones Rápidas',
          style: TextStyle(
            fontSize: 17,
            fontWeight: FontWeight.bold,
            color: Theme.of(context).brightness == Brightness.dark
                ? AppColors.textPrimaryDark
                : AppColors.textPrimaryLight,
          ),
        ),
        const SizedBox(height: 14),
        Row(
          children: [
            Expanded(
              child: _buildActionTile(
                label: 'Aportar',
                subtitle: 'Cuota comunal',
                icon: FluentIcons.payment_24_regular,
                onTap: widget.onNavigateToPayments,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _buildActionTile(
                label: 'Asambleas',
                subtitle: 'Reuniones',
                icon: FluentIcons.people_audience_24_regular,
                onTap: widget.onNavigateToCommunity,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _buildActionTile(
                label: 'Votaciones',
                subtitle: 'Consultas',
                icon: FluentIcons.vote_24_regular,
                onTap: widget.onNavigateToVoting,
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildActionTile({
    required String label,
    required String subtitle,
    required IconData icon,
    required VoidCallback onTap,
  }) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return BouncyTap(
      scaleDown: 0.94,
      onTap: onTap,
      child: NeoGlassContainer(
        padding: const EdgeInsets.symmetric(vertical: 18, horizontal: 10),
        borderRadius: BorderRadius.circular(22),
        child: Column(
          children: [
            NeoGlassContainer.circle(
              size: 44,
              child: Icon(
                icon,
                color: isDark ? Colors.white : AppColors.brandBlue,
                size: 22,
              ),
            ),
            const SizedBox(height: 12),
            Text(
              label,
              style: TextStyle(
                color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                fontWeight: FontWeight.bold,
                fontSize: 13.5,
              ),
            ),
            const SizedBox(height: 3),
            Text(
              subtitle,
              textAlign: TextAlign.center,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                fontSize: 10.5,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildProjectsSummary(bool isDark) {
    return ListenableBuilder(
      listenable: widget.viewModel,
      builder: (context, _) {
        final activeList = widget.viewModel.activeProjects;
        final total = activeList.length;

        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Proyectos de la Comunidad',
                  style: TextStyle(
                    fontSize: 17,
                    fontWeight: FontWeight.bold,
                    color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                  ),
                ),
                TextButton(
                  onPressed: widget.onNavigateToCommunity,
                  child: const Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text('Ver todos', style: TextStyle(fontSize: 12.5)),
                      SizedBox(width: 4),
                      Icon(FluentIcons.chevron_right_12_regular, size: 12),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            if (activeList.isEmpty)
              NeoGlassContainer(
                padding: const EdgeInsets.all(16),
                borderRadius: BorderRadius.circular(18),
                child: Row(
                  children: [
                    const Icon(FluentIcons.building_retail_toolbox_24_regular, color: Colors.grey, size: 24),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        total > 0
                            ? 'Sin proyectos en ejecución activa actualmente.'
                            : 'No hay proyectos registrados.',
                        style: TextStyle(
                          fontSize: 12.5,
                          color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                        ),
                      ),
                    ),
                  ],
                ),
              )
            else
              Column(
                children: activeList.take(2).map((proj) {
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 10),
                    child: BouncyTap(
                      scaleDown: 0.98,
                      onTap: widget.onNavigateToCommunity,
                      child: NeoGlassContainer(
                        padding: const EdgeInsets.all(14),
                        borderRadius: BorderRadius.circular(18),
                        child: Row(
                          children: [
                            NeoGlassContainer.circle(
                              size: 40,
                              child: const Icon(
                                FluentIcons.building_retail_toolbox_24_regular,
                                color: AppColors.neoEmerald,
                                size: 20,
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    proj.nombre,
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: TextStyle(
                                      fontWeight: FontWeight.bold,
                                      fontSize: 13.5,
                                      color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                                    ),
                                  ),
                                  const SizedBox(height: 2),
                                  Text(
                                    proj.descripcion,
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: TextStyle(
                                      fontSize: 11.5,
                                      color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            const SizedBox(width: 8),
                            StatusBadge.fromStatus(proj.estado),
                          ],
                        ),
                      ),
                    ),
                  );
                }).toList(),
              ),
          ],
        );
      },
    );
  }

  Widget _buildCommunityContacts(bool isDark) {
    return ListenableBuilder(
      listenable: widget.viewModel,
      builder: (context, _) {
        final directiva = widget.viewModel.directiva;
        final isLoading = widget.viewModel.isLoading;

        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Directorio Comunal',
                  style: TextStyle(
                    fontSize: 17,
                    fontWeight: FontWeight.bold,
                    color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(
                    color: AppColors.brandBlue.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Text(
                    'JUNTA DIRECTIVA',
                    style: TextStyle(
                      fontSize: 9.5,
                      fontWeight: FontWeight.bold,
                      color: isDark ? Colors.white70 : AppColors.brandBlue,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            if (isLoading && directiva.isEmpty)
              const Center(
                child: Padding(
                  padding: EdgeInsets.symmetric(vertical: 20),
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
              )
            else if (directiva.isEmpty)
              NeoGlassContainer(
                padding: const EdgeInsets.all(16),
                borderRadius: BorderRadius.circular(18),
                child: Center(
                  child: Text(
                    'No hay miembros de directiva registrados.',
                    style: TextStyle(
                      fontSize: 12.5,
                      color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                    ),
                  ),
                ),
              )
            else
              ListView.separated(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: directiva.length,
                separatorBuilder: (_, __) => const SizedBox(height: 10),
                itemBuilder: (context, index) {
                  final member = directiva[index];
                  final initial = member.nombreMiembro.isNotEmpty
                      ? member.nombreMiembro.substring(0, 1).toUpperCase()
                      : 'D';
                  final phone = member.telefonoMiembro;

                  return BouncyTap(
                    scaleDown: 0.98,
                    child: NeoGlassContainer(
                      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                      borderRadius: BorderRadius.circular(18),
                      child: Row(
                        children: [
                          NeoGlassContainer.circle(
                            size: 40,
                            child: Text(
                              initial,
                              style: TextStyle(
                                fontWeight: FontWeight.bold,
                                fontSize: 15,
                                color: isDark ? Colors.white : AppColors.brandBlue,
                              ),
                            ),
                          ),
                          const SizedBox(width: 14),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  member.nombreMiembro,
                                  style: TextStyle(
                                    fontSize: 14,
                                    fontWeight: FontWeight.bold,
                                    color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                                  ),
                                ),
                                const SizedBox(height: 2),
                                Text(
                                  phone != null && phone.isNotEmpty
                                      ? '${member.nombreCargo} • $phone'
                                      : member.nombreCargo,
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: TextStyle(
                                    fontSize: 11.5,
                                    color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          if (phone != null && phone.isNotEmpty) ...[
                            const SizedBox(width: 8),
                            BouncyTap(
                              scaleDown: 0.88,
                              onTap: () {
                                ScaffoldMessenger.of(context).showSnackBar(
                                  SnackBar(content: Text('Contactando a ${member.nombreMiembro} ($phone)...')),
                                );
                              },
                              child: NeoGlassContainer.circle(
                                size: 36,
                                child: Icon(
                                  FluentIcons.call_24_regular,
                                  size: 17,
                                  color: isDark ? Colors.white70 : AppColors.brandBlue,
                                ),
                              ),
                            ),
                          ],
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

  Widget _buildRecentTransactionsSection(bool isDark) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              'Aportaciones Recientes',
              style: TextStyle(
                fontSize: 17,
                fontWeight: FontWeight.bold,
                color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
              ),
            ),
            TextButton(
              onPressed: widget.onNavigateToPayments,
              child: const Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text('Ver todas', style: TextStyle(fontSize: 13)),
                  SizedBox(width: 4),
                  Icon(FluentIcons.chevron_right_12_regular, size: 12),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(height: 10),
        ListenableBuilder(
          listenable: widget.viewModel,
          builder: (context, _) {
            final payments = widget.viewModel.recentPayments;

            if (payments.isEmpty) {
              return NeoGlassContainer(
                padding: const EdgeInsets.symmetric(vertical: 24, horizontal: 16),
                child: Center(
                  child: Column(
                    children: [
                      Icon(
                        FluentIcons.receipt_24_regular,
                        size: 38,
                        color: isDark ? Colors.white38 : AppColors.textSecondaryLight,
                      ),
                      const SizedBox(height: 10),
                      Text(
                        'No hay aportaciones registradas aún',
                        style: TextStyle(
                          fontSize: 13,
                          color: isDark
                              ? AppColors.textSecondaryDark
                              : AppColors.textSecondaryLight,
                        ),
                      ),
                    ],
                  ),
                ),
              );
            }

            return Column(
              children: payments.take(4).map((payment) {
                return Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: BouncyTap(
                    scaleDown: 0.97,
                    onTap: widget.onNavigateToPayments,
                    child: NeoGlassContainer(
                      padding: const EdgeInsets.all(14),
                      borderRadius: BorderRadius.circular(18),
                      child: Row(
                        children: [
                          NeoGlassContainer.circle(
                            size: 42,
                            child: Icon(
                              FluentIcons.receipt_24_regular,
                              color: isDark ? Colors.white70 : AppColors.brandBlue,
                              size: 20,
                            ),
                          ),
                          const SizedBox(width: 14),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  payment.nombreProyecto ?? 'Cuota Mensual Comunal',
                                  style: TextStyle(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 14,
                                    color: isDark
                                        ? AppColors.textPrimaryDark
                                        : AppColors.textPrimaryLight,
                                  ),
                                ),
                                const SizedBox(height: 3),
                                Text(
                                  '${payment.periodoMes} • ${Formatters.date(payment.fechaPago)}',
                                  style: TextStyle(
                                    fontSize: 12,
                                    color: isDark
                                        ? AppColors.textMutedDark
                                        : AppColors.textSecondaryLight,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.end,
                            children: [
                              Text(
                                Formatters.currency(payment.monto),
                                style: TextStyle(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 15,
                                  color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                                ),
                              ),
                              const SizedBox(height: 4),
                              StatusBadge.fromStatus(payment.estado),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              }).toList(),
            );
          },
        ),
      ],
    );
  }
}
