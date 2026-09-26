import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/bouncy_tap.dart';
import '../../core/widgets/community_top_header.dart';
import '../../core/widgets/fade_slide_entrance.dart';
import '../../data/models/auth_models.dart';
import '../../data/models/meeting_model.dart';
import '../../data/models/project_model.dart';
import '../../data/models/voting_model.dart';
import '../../data/repositories/notifications_repository.dart';
import '../../viewmodels/home_viewmodel.dart';
import '../../viewmodels/payments_viewmodel.dart';
import '../payments/simulated_payment_sheet.dart';
import 'notifications_sheet.dart';
import 'visitor_qr_dialog.dart';

/// Pantalla Principal (Inicio / Dashboard del Residente)
/// Replicando con máxima fidelidad la interfaz Stitch (Colonia Conecta).
class HomeView extends StatefulWidget {
  final HomeViewModel viewModel;
  final PaymentsViewModel paymentsViewModel;
  final MeResponse profile;
  final VoidCallback onNavigateToPayments;
  final VoidCallback onNavigateToCommunity;
  final VoidCallback onNavigateToVoting;

  const HomeView({
    super.key,
    required this.viewModel,
    required this.paymentsViewModel,
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

  int get _unreadCount {
    final notifs = _notificationsRepo.buildNotifications(
      payments: widget.viewModel.recentPayments,
      polls: widget.viewModel.pendingVoting != null ? [widget.viewModel.pendingVoting!] : [],
      meetings: widget.viewModel.upcomingMeetings,
      projects: widget.viewModel.activeProjects,
      currentMemberId: widget.profile.member?.id,
    );
    return notifs.where((n) => !n.isRead).length;
  }

  void _openNotifications() {
    final notifs = _notificationsRepo.buildNotifications(
      payments: widget.viewModel.recentPayments,
      polls: widget.viewModel.pendingVoting != null ? [widget.viewModel.pendingVoting!] : [],
      meetings: widget.viewModel.upcomingMeetings,
      projects: widget.viewModel.activeProjects,
      currentMemberId: widget.profile.member?.id,
    );

    NotificationsSheet.show(
      context,
      notifications: notifs,
      onNavigateToPayments: widget.onNavigateToPayments,
      onNavigateToCommunity: widget.onNavigateToCommunity,
      onNavigateToVoting: widget.onNavigateToVoting,
    );
  }

  void _openVisitorPass() {
    final member = widget.profile.member;
    final name = member?.fullName ?? widget.profile.user.username;
    const unit = 'Casa #42-B';
    VisitorQrDialog.show(
      context,
      residentName: name.isNotEmpty ? name : 'Carlos Mendoza',
      residentUnit: unit,
    );
  }

  void _payMonthlyDues() {
    final pending = widget.viewModel.pendingBalance;
    final isPaid = pending <= 0;
    if (isPaid) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            '¡Tu cuota de mantenimiento de este período ya está saldada! Podrás realizar el siguiente pago cuando inicie el próximo período mensual.',
          ),
          backgroundColor: AppColors.stitchEmerald,
          duration: Duration(seconds: 4),
        ),
      );
      return;
    }

    final member = widget.profile.member;
    final now = DateTime.now();
    final nextPeriod = '${now.year}-${now.month.toString().padLeft(2, '0')}';

    SimulatedPaymentSheet.show(
      context,
      viewModel: widget.paymentsViewModel,
      idMiembro: member?.id ?? 1,
      title: 'Cuota de Mantenimiento',
      description: 'Período $nextPeriod · Seguridad y Áreas Verdes',
      defaultAmount: 15.00,
      periodoMes: nextPeriod,
      onPaymentSuccess: (payment) {
        widget.viewModel.loadDashboardData(idMiembro: member?.id);
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final member = widget.profile.member;
    final firstName = member?.firstName ?? widget.profile.user.username;
    const unitCode = 'Casa #42-B';

    return Scaffold(
      backgroundColor: isDark ? const Color(0xFF0F172A) : AppColors.stitchCanvasLight,
      body: SafeArea(
        bottom: false,
        child: RefreshIndicator(
          onRefresh: () => widget.viewModel.loadDashboardData(idMiembro: member?.id),
          color: AppColors.stitchSapphire,
          child: SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 110),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 1. Barra Superior Estandarizada
                FadeSlideEntrance(
                  delay: Duration.zero,
                  child: CommunityTopHeader(
                    title: 'Inicio',
                    subtitle: 'Residencial Las Flores',
                    unreadNotifications: _unreadCount,
                    residentName: firstName,
                    onNotificationTap: _openNotifications,
                  ),
                ),
                const SizedBox(height: 12),

                // 2. Saludo y Contexto Superior
                FadeSlideEntrance(
                  delay: const Duration(milliseconds: 40),
                  child: _buildGreetingHeader(isDark, firstName, unitCode),
                ),
                const SizedBox(height: 16),

                // 3. Tarjeta Protagonista de Cuota Mensual
                FadeSlideEntrance(
                  delay: const Duration(milliseconds: 80),
                  child: _buildHeroCuotaCard(isDark),
                ),
                const SizedBox(height: 20),

                // 4. Acciones Frecuentes (Grid 4 Columnas)
                FadeSlideEntrance(
                  delay: const Duration(milliseconds: 120),
                  child: _buildFrequentActionsGrid(isDark),
                ),
                const SizedBox(height: 20),

                // 5. Banner Informativo Comunal
                FadeSlideEntrance(
                  delay: const Duration(milliseconds: 160),
                  child: _buildCommunityAnnouncementBanner(isDark),
                ),
                const SizedBox(height: 24),

                // 6. Sección "Decisiones & Actividad"
                FadeSlideEntrance(
                  delay: const Duration(milliseconds: 200),
                  child: _buildDecisionsAndActivitySection(isDark, unitCode),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  /// 2. Saludo y Contexto Superior
  Widget _buildGreetingHeader(bool isDark, String firstName, String unitCode) {
    final now = DateTime.now();
    final monthName = Formatters.date(now);
    final hasPendingDues = widget.viewModel.pendingBalance > 0;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Flexible(
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: hasPendingDues
                      ? const Color(0xFFFEF3C7)
                      : AppColors.stitchEmeraldContainer,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      width: 7,
                      height: 7,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: hasPendingDues
                            ? const Color(0xFFD97706)
                            : AppColors.stitchEmerald,
                      ),
                    ),
                    const SizedBox(width: 6),
                    Flexible(
                      child: Text(
                        hasPendingDues ? '1 Cuota Pendiente' : 'Al día con la colonia',
                        style: TextStyle(
                          fontSize: 11.5,
                          fontWeight: FontWeight.w700,
                          color: hasPendingDues
                              ? const Color(0xFF92400E)
                              : const Color(0xFF065F46),
                        ),
                        overflow: TextOverflow.ellipsis,
                        maxLines: 1,
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(width: 8),
            Text(
              monthName,
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w500,
                color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
        Text(
          '¡${HomeViewModel.getGreeting()}, $firstName!',
          style: TextStyle(
            fontSize: 26,
            fontWeight: FontWeight.w800,
            letterSpacing: -0.6,
            color: isDark ? Colors.white : AppColors.stitchTextPrimary,
          ),
        ),
        const SizedBox(height: 5),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
          decoration: BoxDecoration(
            color: isDark ? const Color(0xFF1E293B) : const Color(0xFFF1F5F9),
            borderRadius: BorderRadius.circular(16),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(
                Icons.location_on_rounded,
                size: 14,
                color: AppColors.stitchSapphire,
              ),
              const SizedBox(width: 5),
              Text(
                'Residencial Las Flores · $unitCode',
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w500,
                  color: isDark ? Colors.white70 : AppColors.stitchTextSecondary,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  /// 3. Tarjeta Protagonista de Cuota Mensual
  Widget _buildHeroCuotaCard(bool isDark) {
    final pending = widget.viewModel.pendingBalance;
    final isPaid = pending <= 0;
    final amount = isPaid ? 15.0 : pending;

    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(28),
        gradient: AppColors.stitchHeroGradient,
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF0A3832).withValues(alpha: 0.35),
            blurRadius: 22,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Stack(
        children: [
          Positioned(
            top: -20,
            right: -20,
            child: Container(
              width: 140,
              height: 140,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: AppColors.stitchEmeraldMint.withValues(alpha: 0.16),
              ),
            ),
          ),
          Positioned(
            bottom: -30,
            left: -20,
            child: Container(
              width: 120,
              height: 120,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: AppColors.stitchSapphire.withValues(alpha: 0.25),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(22),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'MANTENIMIENTO & SEGURIDAD',
                      style: TextStyle(
                        fontSize: 10.5,
                        fontWeight: FontWeight.w800,
                        letterSpacing: 0.8,
                        color: Color(0xFF6EE7B7),
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3.5),
                      decoration: BoxDecoration(
                        color: Colors.white.withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(14),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Container(
                            width: 6,
                            height: 6,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              color: isPaid ? const Color(0xFF34D399) : const Color(0xFFFBBF24),
                            ),
                          ),
                          const SizedBox(width: 5),
                          Text(
                            isPaid ? 'Al día' : 'Pendiente',
                            style: TextStyle(
                              fontSize: 11,
                              fontWeight: FontWeight.bold,
                              color: isPaid ? const Color(0xFF6EE7B7) : const Color(0xFFFDE68A),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 14),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.baseline,
                  textBaseline: TextBaseline.alphabetic,
                  children: [
                    Text(
                      Formatters.currency(amount),
                      style: const TextStyle(
                        fontSize: 40,
                        fontWeight: FontWeight.w800,
                        letterSpacing: -1.0,
                        color: Colors.white,
                        height: 1.0,
                      ),
                    ),
                    const SizedBox(width: 6),
                    const Text(
                      '/ mes',
                      style: TextStyle(
                        fontSize: 14,
                        color: Color(0xFF99F6E4),
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                Text(
                  isPaid
                      ? 'Cuota del mes saldada · Próxima liquidación en 30 días'
                      : 'Noviembre 2026 · Vence en 5 días (30 Nov)',
                  style: TextStyle(
                    fontSize: 12,
                    color: Colors.white.withValues(alpha: 0.85),
                  ),
                ),
                const SizedBox(height: 18),
                Row(
                  children: [
                    Expanded(
                      flex: 6,
                      child: BouncyTap(
                        onTap: _payMonthlyDues,
                        child: Container(
                          height: 48,
                          padding: const EdgeInsets.symmetric(horizontal: 8),
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(24),
                            gradient: const LinearGradient(
                              colors: [Color(0xFF34D399), Color(0xFF10B981)],
                            ),
                            boxShadow: [
                              BoxShadow(
                                color: const Color(0xFF10B981).withValues(alpha: 0.4),
                                blurRadius: 10,
                                offset: const Offset(0, 4),
                              ),
                            ],
                          ),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(
                                isPaid ? Icons.check_circle_rounded : Icons.credit_card_rounded,
                                color: const Color(0xFF064E3B),
                                size: 17,
                              ),
                              const SizedBox(width: 4),
                              Flexible(
                                child: Text(
                                  isPaid ? 'Cuota ya saldada' : 'Pagar cuota ahora',
                                  style: const TextStyle(
                                    color: Color(0xFF064E3B),
                                    fontSize: 13,
                                    fontWeight: FontWeight.bold,
                                  ),
                                  overflow: TextOverflow.ellipsis,
                                  maxLines: 1,
                                ),
                              ),
                              if (!isPaid) ...[
                                const SizedBox(width: 4),
                                const Icon(Icons.arrow_forward_rounded, color: Color(0xFF064E3B), size: 15),
                              ],
                            ],
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      flex: 4,
                      child: BouncyTap(
                        onTap: widget.onNavigateToPayments,
                        child: Container(
                          height: 48,
                          padding: const EdgeInsets.symmetric(horizontal: 8),
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(24),
                            color: Colors.white.withValues(alpha: 0.12),
                            border: Border.all(
                              color: Colors.white.withValues(alpha: 0.25),
                            ),
                          ),
                          child: const Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(Icons.history_rounded, color: Colors.white, size: 16),
                              SizedBox(width: 5),
                              Flexible(
                                child: Text(
                                  'Ver historial',
                                  style: TextStyle(
                                    color: Colors.white,
                                    fontSize: 12,
                                    fontWeight: FontWeight.w600,
                                  ),
                                  overflow: TextOverflow.ellipsis,
                                  maxLines: 1,
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
          ),
        ],
      ),
    );
  }

  /// 4. Acciones Frecuentes (Grid 4 Columnas)
  Widget _buildFrequentActionsGrid(bool isDark) {
    final pendingVoting = widget.viewModel.pendingVoting;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              'Acciones Frecuentes',
              style: TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.bold,
                color: isDark ? Colors.white : AppColors.stitchTextPrimary,
              ),
            ),
            const Text(
              'Personalizar',
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w600,
                color: AppColors.stitchEmerald,
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            // 1. Pagar
            _buildActionItem(
              isDark: isDark,
              icon: Icons.payments_rounded,
              label: 'Pagar',
              circleColor: AppColors.stitchSapphire,
              iconColor: Colors.white,
              onTap: widget.onNavigateToPayments,
            ),
            // 2. Votar con badge
            _buildActionItem(
              isDark: isDark,
              icon: Icons.how_to_vote_rounded,
              label: 'Votar',
              circleColor: isDark ? const Color(0xFF1E293B) : const Color(0xFFEEF2FF),
              iconColor: AppColors.stitchSapphire,
              badgeCount: pendingVoting != null ? 1 : null,
              onTap: widget.onNavigateToVoting,
            ),
            // 3. Pase Visita
            _buildActionItem(
              isDark: isDark,
              icon: Icons.qr_code_2_rounded,
              label: 'Pase Visita',
              circleColor: const Color(0xFF0F766E),
              iconColor: Colors.white,
              onTap: _openVisitorPass,
            ),
            // 4. Reportar / Comunidad
            _buildActionItem(
              isDark: isDark,
              icon: Icons.build_circle_rounded,
              label: 'Reportar',
              circleColor: isDark ? const Color(0xFF1E293B) : const Color(0xFFF1F5F9),
              iconColor: isDark ? Colors.white70 : AppColors.stitchTextSecondary,
              onTap: widget.onNavigateToCommunity,
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildActionItem({
    required bool isDark,
    required IconData icon,
    required String label,
    required Color circleColor,
    required Color iconColor,
    required VoidCallback onTap,
    int? badgeCount,
  }) {
    return BouncyTap(
      onTap: onTap,
      child: Container(
        width: 76,
        padding: const EdgeInsets.symmetric(vertical: 12),
        decoration: BoxDecoration(
          color: isDark ? const Color(0xFF1E293B) : Colors.white,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: isDark ? const Color(0xFF334155) : const Color(0xFFE2E8F0),
          ),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: isDark ? 0.2 : 0.04),
              blurRadius: 8,
              offset: const Offset(0, 3),
            ),
          ],
        ),
        child: Column(
          children: [
            Stack(
              clipBehavior: Clip.none,
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: circleColor,
                  ),
                  child: Center(
                    child: Icon(icon, color: iconColor, size: 22),
                  ),
                ),
                if (badgeCount != null && badgeCount > 0)
                  Positioned(
                    top: -2,
                    right: -2,
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1.5),
                      decoration: BoxDecoration(
                        color: const Color(0xFFEF4444),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: Text(
                        '$badgeCount',
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 9,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              label,
              style: TextStyle(
                fontSize: 11.5,
                fontWeight: FontWeight.w600,
                color: isDark ? Colors.white : AppColors.stitchTextPrimary,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  }

  /// 5. Banner Informativo Comunal
  Widget _buildCommunityAnnouncementBanner(bool isDark) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: isDark ? const Color(0xFF1E293B) : const Color(0xFFEEF2FF),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(
          color: isDark ? const Color(0xFF334155) : const Color(0xFFC7D2FE),
        ),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 38,
            height: 38,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(10),
              color: isDark ? const Color(0xFF312E81) : Colors.white,
            ),
            child: const Icon(
              Icons.water_drop_rounded,
              color: AppColors.stitchSapphire,
              size: 22,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'Corte Programado de Agua',
                      style: TextStyle(
                        fontSize: 13.5,
                        fontWeight: FontWeight.bold,
                        color: isDark ? Colors.white : AppColors.stitchTextPrimary,
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                      decoration: BoxDecoration(
                        color: AppColors.stitchEmeraldContainer,
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: const Text(
                        'Mañana',
                        style: TextStyle(
                          fontSize: 10,
                          fontWeight: FontWeight.w700,
                          color: Color(0xFF065F46),
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 3),
                Text(
                  'De 8:00 AM a 12:00 PM por limpieza integral y mantenimiento de cisterna general.',
                  style: TextStyle(
                    fontSize: 11.5,
                    color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                    height: 1.35,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  /// 6. Sección "Decisiones & Actividad"
  Widget _buildDecisionsAndActivitySection(bool isDark, String unitCode) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Row(
              children: [
                Text(
                  'Decisiones & Actividad',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: isDark ? Colors.white : AppColors.stitchTextPrimary,
                  ),
                ),
                const SizedBox(width: 6),
                Container(
                  width: 6,
                  height: 6,
                  decoration: const BoxDecoration(
                    shape: BoxShape.circle,
                    color: AppColors.stitchSapphire,
                  ),
                ),
              ],
            ),
            BouncyTap(
              onTap: widget.onNavigateToCommunity,
              child: const Text(
                'Ver todo',
                style: TextStyle(
                  fontSize: 12.5,
                  fontWeight: FontWeight.w600,
                  color: AppColors.stitchSapphire,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 14),

        // A. Tarjeta de Votación Activa (Si existe)
        ListenableBuilder(
          listenable: widget.viewModel,
          builder: (context, _) {
            final poll = widget.viewModel.pendingVoting;
            if (poll != null) {
              return Padding(
                padding: const EdgeInsets.only(bottom: 16),
                child: _buildActiveVotingCard(isDark, poll),
              );
            }
            return const SizedBox.shrink();
          },
        ),

        // B. Tarjeta de Proyecto en Recaudación
        ListenableBuilder(
          listenable: widget.viewModel,
          builder: (context, _) {
            final projects = widget.viewModel.activeProjects;
            final project = projects.isNotEmpty ? projects.first : null;
            if (project != null) {
              return Padding(
                padding: const EdgeInsets.only(bottom: 16),
                child: _buildProjectProgressCard(isDark, project, unitCode),
              );
            }
            return const SizedBox.shrink();
          },
        ),

        // C. Tarjeta de Próxima Asamblea
        ListenableBuilder(
          listenable: widget.viewModel,
          builder: (context, _) {
            final meeting = widget.viewModel.nextMeeting;
            if (meeting != null) {
              return Padding(
                padding: const EdgeInsets.only(bottom: 16),
                child: _buildNextMeetingCard(isDark, meeting),
              );
            }
            return const SizedBox.shrink();
          },
        ),

        // D. Caseta de Vigilancia (Tarjeta de contacto rápido)
        _buildSecurityGuardCard(isDark),
      ],
    );
  }

  /// Tarjeta de Votación Activa
  Widget _buildActiveVotingCard(bool isDark, VotingModel poll) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: isDark ? const Color(0xFF1E293B) : Colors.white,
        borderRadius: BorderRadius.circular(22),
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
          Row(
            children: [
              Container(
                width: 32,
                height: 32,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(8),
                  color: const Color(0xFFDBEAFE),
                ),
                child: const Icon(Icons.how_to_vote_rounded, color: AppColors.stitchSapphire, size: 18),
              ),
              const SizedBox(width: 8),
              const Icon(Icons.schedule_rounded, size: 14, color: Color(0xFFEA580C)),
              const SizedBox(width: 4),
              const Text(
                'Finaliza en 2 días',
                style: TextStyle(
                  fontSize: 11.5,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFFEA580C),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            poll.titulo,
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
              color: isDark ? Colors.white : AppColors.stitchTextPrimary,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            poll.descripcion.isNotEmpty
                ? poll.descripcion
                : 'Tu voto aún no ha sido registrado. La decisión comunitaria definirá el inicio de obras el próximo mes.',
            style: TextStyle(
              fontSize: 12,
              color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
              height: 1.35,
            ),
          ),
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
            decoration: BoxDecoration(
              color: isDark ? const Color(0xFF0F172A) : const Color(0xFFF8FAFC),
              borderRadius: BorderRadius.circular(14),
            ),
            child: const Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Presupuesto total', style: TextStyle(fontSize: 10.5, color: AppColors.stitchTextSecondary)),
                      SizedBox(height: 2),
                      Text('\$1,850.00', style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: AppColors.stitchSapphire)),
                    ],
                  ),
                ),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Aporte est./vivienda', style: TextStyle(fontSize: 10.5, color: AppColors.stitchTextSecondary)),
                      SizedBox(height: 2),
                      Text('\$25.00', style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: AppColors.stitchEmerald)),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: BouncyTap(
                  onTap: widget.onNavigateToVoting,
                  child: Container(
                    height: 44,
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(22),
                      color: AppColors.stitchSapphire,
                    ),
                    child: const Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.how_to_vote_rounded, color: Colors.white, size: 16),
                        SizedBox(width: 8),
                        Text(
                          'Emitir mi voto',
                          style: TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.bold),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Container(
                width: 44,
                height: 44,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(22),
                  color: isDark ? const Color(0xFF334155) : const Color(0xFFEEF2FF),
                ),
                child: const Icon(Icons.info_outline_rounded, color: AppColors.stitchSapphire, size: 20),
              ),
            ],
          ),
        ],
      ),
    );
  }

  /// Tarjeta de Proyecto en Recaudación
  Widget _buildProjectProgressCard(bool isDark, ProjectModel project, String unitCode) {
    final progreso = project.progresoPorcentaje;
    final recaudado = project.montoRecaudado;
    final total = project.presupuesto;

    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: isDark ? const Color(0xFF1E293B) : Colors.white,
        borderRadius: BorderRadius.circular(22),
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
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 3.5),
                decoration: BoxDecoration(
                  color: const Color(0xFFCCFBF1),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Text(
                  'EN RECAUDACIÓN',
                  style: TextStyle(fontSize: 10, fontWeight: FontWeight.w800, color: Color(0xFF0F766E)),
                ),
              ),
              Text(
                'Meta: 15 Dic',
                style: TextStyle(
                  fontSize: 11,
                  fontWeight: FontWeight.w500,
                  color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            project.nombre,
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
              color: isDark ? Colors.white : AppColors.stitchTextPrimary,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            project.descripcion,
            style: TextStyle(
              fontSize: 12,
              color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
              height: 1.35,
            ),
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
          ),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '${progreso.toStringAsFixed(0)}%',
                style: TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.w800,
                  color: isDark ? Colors.white : AppColors.stitchTextPrimary,
                ),
              ),
              Text(
                '${Formatters.currency(recaudado)} de ${Formatters.currency(total)}',
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w500,
                  color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          ClipRRect(
            borderRadius: BorderRadius.circular(6),
            child: LinearProgressIndicator(
              value: (progreso / 100).clamp(0.0, 1.0),
              minHeight: 8,
              backgroundColor: isDark ? const Color(0xFF334155) : const Color(0xFFE2E8F0),
              valueColor: const AlwaysStoppedAnimation<Color>(AppColors.stitchEmerald),
            ),
          ),
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            decoration: BoxDecoration(
              color: const Color(0xFFFFFBEB),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Row(
              children: [
                const Icon(Icons.home_outlined, color: Color(0xFFB45309), size: 18),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    'Aporte $unitCode: Pendiente (${Formatters.currency(project.aportePorMiembro)})',
                    style: const TextStyle(fontSize: 11.5, fontWeight: FontWeight.bold, color: Color(0xFF92400E)),
                  ),
                ),
                Container(
                  width: 8,
                  height: 8,
                  decoration: const BoxDecoration(
                    shape: BoxShape.circle,
                    color: Color(0xFFF59E0B),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          BouncyTap(
            onTap: widget.onNavigateToCommunity,
            child: const Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                Text(
                  'Ver detalles y aportar',
                  style: TextStyle(
                    fontSize: 12.5,
                    fontWeight: FontWeight.bold,
                    color: AppColors.stitchSapphire,
                  ),
                ),
                SizedBox(width: 4),
                Icon(Icons.arrow_forward_rounded, size: 16, color: AppColors.stitchSapphire),
              ],
            ),
          ),
        ],
      ),
    );
  }

  /// Tarjeta de Próxima Asamblea
  Widget _buildNextMeetingCard(bool isDark, MeetingModel meeting) {
    final date = DateTime.tryParse(meeting.fechaHora) ?? DateTime.now();
    final dayStr = date.day.toString().padLeft(2, '0');
    final monthStr = Formatters.date(date).split(' ')[1].toUpperCase();
    final timeStr = Formatters.time(meeting.fechaHora);

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: isDark ? const Color(0xFF1E293B) : Colors.white,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: isDark ? const Color(0xFF334155) : const Color(0xFFE2E8F0),
        ),
      ),
      child: Row(
        children: [
          Container(
            width: 54,
            height: 58,
            decoration: BoxDecoration(
              color: const Color(0xFFEEF2FF),
              borderRadius: BorderRadius.circular(14),
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  monthStr.length > 3 ? monthStr.substring(0, 3) : monthStr,
                  style: const TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                    color: AppColors.stitchSapphire,
                  ),
                ),
                Text(
                  dayStr,
                  style: const TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.w800,
                    color: AppColors.stitchSapphire,
                    height: 1.1,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  meeting.titulo,
                  style: TextStyle(
                    fontSize: 14.5,
                    fontWeight: FontWeight.bold,
                    color: isDark ? Colors.white : AppColors.stitchTextPrimary,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 3),
                Row(
                  children: [
                    const Icon(Icons.schedule_rounded, size: 13, color: AppColors.stitchTextSecondary),
                    const SizedBox(width: 4),
                    Text(
                      '$timeStr · ${meeting.lugar}',
                      style: TextStyle(
                        fontSize: 11.5,
                        color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
                  decoration: BoxDecoration(
                    color: const Color(0xFFEEF2FF),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: const Text(
                    'Agendar / Asistiré',
                    style: TextStyle(
                      fontSize: 10.5,
                      fontWeight: FontWeight.bold,
                      color: AppColors.stitchSapphire,
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

  /// Tarjeta de Caseta de Vigilancia
  Widget _buildSecurityGuardCard(bool isDark) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: isDark ? const Color(0xFF1E293B) : const Color(0xFFEEF2FF),
        borderRadius: BorderRadius.circular(18),
      ),
      child: Row(
        children: [
          Container(
            width: 38,
            height: 38,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: isDark ? const Color(0xFF0F172A) : Colors.white,
            ),
            child: const Icon(Icons.shield_outlined, color: AppColors.stitchSapphire, size: 20),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Caseta de Vigilancia',
                  style: TextStyle(
                    fontSize: 13.5,
                    fontWeight: FontWeight.bold,
                    color: isDark ? Colors.white : AppColors.stitchTextPrimary,
                  ),
                ),
                Text(
                  'Guardia de turno: Mario Rivera',
                  style: TextStyle(
                    fontSize: 11.5,
                    color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                  ),
                ),
              ],
            ),
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(16),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.05),
                  blurRadius: 4,
                  offset: const Offset(0, 2),
                ),
              ],
            ),
            child: const Row(
              children: [
                Icon(Icons.phone_rounded, color: AppColors.stitchEmerald, size: 14),
                SizedBox(width: 4),
                Text(
                  'Llamar',
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                    color: AppColors.stitchTextPrimary,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
