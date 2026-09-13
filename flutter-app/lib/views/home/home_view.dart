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
              // 1. Header con Avatar, Saludo y Campana de Notificaciones
              FadeSlideEntrance(
                delay: Duration.zero,
                child: _buildHeader(isDark, displayName),
              ),
              const SizedBox(height: 22),

              // 2. Tarjeta Hero de Balance / Aportaciones
              FadeSlideEntrance(
                delay: const Duration(milliseconds: 90),
                child: _buildHeroBalanceCard(isDark),
              ),
              const SizedBox(height: 24),

              // 3. Botones de Acción Rápida (Coral, Menta, Púrpura)
              FadeSlideEntrance(
                delay: const Duration(milliseconds: 180),
                child: _buildActionTiles(context),
              ),
              const SizedBox(height: 28),

              // 4. Directorio Rápido de la Comunidad (Vecinos / Contactos)
              FadeSlideEntrance(
                delay: const Duration(milliseconds: 270),
                child: _buildCommunityContacts(isDark),
              ),
              const SizedBox(height: 28),

              // 5. Historial de Aportaciones Recientes
              FadeSlideEntrance(
                delay: const Duration(milliseconds: 360),
                child: _buildRecentTransactionsSection(isDark),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeader(bool isDark, String displayName) {
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
                'Hola, $displayName',
                style: TextStyle(
                  fontSize: 13.5,
                  fontWeight: FontWeight.w500,
                  color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                '¡Bienvenido de vuelta!',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                ),
              ),
            ],
          ),
        ),
        // Botón de Notificaciones Neo-Glass con PulsingBadge
        BouncyTap(
          scaleDown: 0.88,
          onTap: () => NotificationsSheet.show(context),
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
        ),
      ],
    );
  }

  Widget _buildHeroBalanceCard(bool isDark) {
    return ListenableBuilder(
      listenable: widget.viewModel,
      builder: (context, _) {
        final totalPaid = widget.viewModel.totalContributed;
        final pending = widget.viewModel.pendingBalance;

        return BouncyTap(
          scaleDown: 0.98,
          onTap: widget.onNavigateToPayments,
          child: NeoGlassContainer.hero(
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
                      'Actividad Reciente',
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
              ],
            ),
          ),
        );
      },
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
            // Botón 1: Aportar
            Expanded(
              child: _buildActionTile(
                label: 'Aportar',
                subtitle: 'Cuota comunal',
                icon: FluentIcons.payment_24_regular,
                onTap: widget.onNavigateToPayments,
              ),
            ),
            const SizedBox(width: 12),
            // Botón 2: Asambleas
            Expanded(
              child: _buildActionTile(
                label: 'Asambleas',
                subtitle: 'Reuniones',
                icon: FluentIcons.people_audience_24_regular,
                onTap: widget.onNavigateToCommunity,
              ),
            ),
            const SizedBox(width: 12),
            // Botón 3: Votaciones
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
            // Contenedor Circular Neumórfico con Icono limpio
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

  Widget _buildCommunityContacts(bool isDark) {
    final contacts = [
      {
        'name': 'Directiva General',
        'role': 'Presidencia Comunal',
        'contact': 'directiva@asociacion.sv',
      },
      {
        'name': 'Tesorería y Cobros',
        'role': 'Finanzas Vecinales',
        'contact': 'tesoreria@asociacion.sv',
      },
      {
        'name': 'Vigilancia y Obras',
        'role': 'Seguridad 24/7',
        'contact': 'seguridad@asociacion.sv',
      },
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Directorio Comunal',
          style: TextStyle(
            fontSize: 17,
            fontWeight: FontWeight.bold,
            color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
          ),
        ),
        const SizedBox(height: 14),
        ListView.separated(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          itemCount: contacts.length,
          separatorBuilder: (_, __) => const SizedBox(height: 10),
          itemBuilder: (context, index) {
            final contact = contacts[index];
            return BouncyTap(
              scaleDown: 0.98,
              onTap: () {},
              child: NeoGlassContainer(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                borderRadius: BorderRadius.circular(18),
                child: Row(
                  children: [
                    // Avatar Neo limpio
                    NeoGlassContainer.circle(
                      size: 40,
                      child: Text(
                        contact['name']!.substring(0, 1),
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
                            contact['name']!,
                            style: TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.bold,
                              color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                            ),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            '${contact['role']!} • ${contact['contact']!}',
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
                    // Botón de Teléfono Neo
                    BouncyTap(
                      scaleDown: 0.88,
                      onTap: () {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text('Llamando a ${contact['name']}...')),
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
                    const SizedBox(width: 8),
                    // Botón de Mensaje Neo
                    BouncyTap(
                      scaleDown: 0.88,
                      onTap: () {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text('Mensaje a ${contact['name']}...')),
                        );
                      },
                      child: NeoGlassContainer.circle(
                        size: 36,
                        child: Icon(
                          FluentIcons.chat_24_regular,
                          size: 17,
                          color: isDark ? Colors.white70 : AppColors.brandBlue,
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
                          // Icono de aportación Neo
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
