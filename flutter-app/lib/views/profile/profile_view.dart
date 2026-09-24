import 'package:flutter/material.dart';
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/bouncy_tap.dart';
import '../../core/widgets/fade_slide_entrance.dart';
import '../../core/widgets/glass_container.dart';
import '../../core/widgets/neo_glass_container.dart';
import '../../core/widgets/status_badge.dart';
import '../../data/services/session_storage_service.dart';
import '../../viewmodels/auth_viewmodel.dart';
import '../../viewmodels/housing_viewmodel.dart';
import 'housing_detail_view.dart';
import '../auth/change_password_view.dart';

/// Pantalla de Perfil y Configuración del Residente.
class ProfileView extends StatelessWidget {
  final AuthViewModel authViewModel;
  final HousingViewModel housingViewModel;
  final SessionStorageService storage;
  final VoidCallback onToggleTheme;

  const ProfileView({
    super.key,
    required this.authViewModel,
    required this.housingViewModel,
    required this.storage,
    required this.onToggleTheme,
  });

  void _confirmLogout(BuildContext context) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Cerrar Sesión'),
        content: const Text('¿Estás seguro de que deseas salir del portal móvil?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancelar'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.errorRed,
              foregroundColor: Colors.white,
            ),
            onPressed: () {
              Navigator.pop(ctx);
              authViewModel.logout();
            },
            child: const Text('Cerrar Sesión'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final profile = authViewModel.profile;
    final member = profile?.member;
    final user = profile?.user;
    final fullName = member?.fullName.isNotEmpty == true
        ? member!.fullName
        : (user?.username ?? 'Residente');

    return AmbientBackground(
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(20, 16, 20, 110),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            FadeSlideEntrance(
              delay: Duration.zero,
              child: Text(
                'Mi Cuenta',
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                  color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                ),
              ),
            ),
            const SizedBox(height: 18),

            // Tarjeta Principal de Perfil Neo-Glass con FadeSlideEntrance y BouncyTap
            FadeSlideEntrance(
              delay: const Duration(milliseconds: 90),
              child: BouncyTap(
                scaleDown: 0.98,
                child: NeoGlassContainer(
                  padding: const EdgeInsets.all(20),
                  borderRadius: BorderRadius.circular(24),
                  child: Row(
                    children: [
                      NeoGlassContainer.circle(
                        size: 60,
                        accentColor: isDark ? null : AppColors.brandBlue.withValues(alpha: 0.10),
                        child: Center(
                          child: Text(
                            Formatters.initials(fullName),
                            style: TextStyle(
                              color: isDark ? Colors.white : AppColors.brandBlue,
                              fontSize: 20,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(width: 16),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              fullName,
                              style: TextStyle(
                                fontSize: 17,
                                fontWeight: FontWeight.bold,
                                color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              '@${user?.username ?? ""} • Rol: ${user?.role ?? "MIEMBRO"}',
                              style: TextStyle(
                                fontSize: 12,
                                color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                              ),
                            ),
                            const SizedBox(height: 6),
                            StatusBadge.fromStatus(member?.status ?? 'ACTIVO'),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(height: 24),

            // Opciones de Configuración con FadeSlideEntrance
            FadeSlideEntrance(
              delay: const Duration(milliseconds: 180),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Preferencias',
                    style: TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.bold,
                      color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                    ),
                  ),
                  const SizedBox(height: 10),
                  NeoGlassContainer(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                    borderRadius: BorderRadius.circular(22),
                    child: Column(
                      children: [
                        _buildOptionTile(
                          icon: FluentIcons.dark_theme_24_regular,
                          title: 'Tema Oscuro',
                          trailing: Switch.adaptive(
                            value: isDark,
                            activeTrackColor: AppColors.brandSky,
                            onChanged: (_) => onToggleTheme(),
                          ),
                          isDark: isDark,
                        ),
                        Divider(height: 1, color: isDark ? Colors.white10 : Colors.black12),
                        _buildOptionTile(
                          icon: FluentIcons.home_24_regular,
                          title: 'Mi Vivienda',
                          subtitle: 'Consultar asignación comunal',
                          trailing: Icon(
                            FluentIcons.chevron_right_16_regular,
                            size: 16,
                            color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                          ),
                          onTap: () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (_) => HousingDetailView(
                                  viewModel: housingViewModel,
                                  idMiembro: member?.id ?? 0,
                                ),
                              ),
                            );
                          },
                          isDark: isDark,
                        ),
                        Divider(height: 1, color: isDark ? Colors.white10 : Colors.black12),
                        _buildOptionTile(
                          icon: FluentIcons.key_reset_24_regular,
                          title: 'Cambiar Contraseña',
                          subtitle: 'Actualizar clave de acceso a la cuenta',
                          trailing: Icon(
                            FluentIcons.chevron_right_16_regular,
                            size: 16,
                            color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                          ),
                          onTap: () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (_) => ChangePasswordView(viewModel: authViewModel),
                              ),
                            );
                          },
                          isDark: isDark,
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 28),

            // Botón de Cierre de Sesión con FadeSlideEntrance y BouncyTap
            FadeSlideEntrance(
              delay: const Duration(milliseconds: 270),
              child: BouncyTap(
                scaleDown: 0.96,
                onTap: () => _confirmLogout(context),
                child: NeoGlassContainer(
                  padding: const EdgeInsets.all(16),
                  borderRadius: BorderRadius.circular(18),
                  child: const Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(FluentIcons.sign_out_24_regular, color: AppColors.errorRed, size: 20),
                      SizedBox(width: 8),
                      Text(
                        'Cerrar Sesión',
                        style: TextStyle(
                          color: AppColors.errorRed,
                          fontSize: 15,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(height: 24),

            // Versión de la Aplicación con FadeSlideEntrance
            FadeSlideEntrance(
              delay: const Duration(milliseconds: 360),
              child: Center(
                child: Text(
                  'Asociación Comunal Mobile • Flutter v0.1.0',
                  style: TextStyle(
                    fontSize: 11,
                    color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildOptionTile({
    required IconData icon,
    required String title,
    String? subtitle,
    Widget? trailing,
    VoidCallback? onTap,
    required bool isDark,
  }) {
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(vertical: 2),
      leading: NeoGlassContainer.circle(
        size: 38,
        child: Icon(
          icon,
          color: isDark ? Colors.white : AppColors.brandBlue,
          size: 18,
        ),
      ),
      title: Text(
        title,
        style: TextStyle(
          fontSize: 14,
          fontWeight: FontWeight.w600,
          color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
        ),
      ),
      subtitle: subtitle != null
          ? Text(
              subtitle,
              style: TextStyle(
                fontSize: 11.5,
                color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
              ),
            )
          : null,
      trailing: trailing,
      onTap: onTap,
    );
  }
}
