import 'package:flutter/material.dart';
import '../theme/app_colors.dart';
import 'bouncy_tap.dart';

/// Barra superior estandarizada según el diseño Stitch (Colonia Conecta).
class CommunityTopHeader extends StatelessWidget {
  final String title;
  final String? subtitle;
  final int unreadNotifications;
  final VoidCallback? onNotificationTap;
  final VoidCallback? onBack;
  final VoidCallback? onProfileTap;
  final String residentName;
  final String? avatarUrl;

  const CommunityTopHeader({
    super.key,
    required this.title,
    this.subtitle,
    this.unreadNotifications = 0,
    this.onNotificationTap,
    this.onBack,
    this.onProfileTap,
    this.residentName = 'Vecino',
    this.avatarUrl,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final initials = residentName.isNotEmpty
        ? residentName.trim().split(' ').map((e) => e.isNotEmpty ? e[0] : '').take(2).join().toUpperCase()
        : 'V';

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      color: Colors.transparent,
      child: Row(
        children: [
          if (onBack != null) ...[
            BouncyTap(
              onTap: onBack!,
              child: Container(
                width: 38,
                height: 38,
                margin: const EdgeInsets.only(right: 8),
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: isDark ? const Color(0xFF1E293B) : const Color(0xFFF1F5F9),
                ),
                child: Icon(
                  Icons.arrow_back,
                  size: 20,
                  color: isDark ? Colors.white : AppColors.stitchTextPrimary,
                ),
              ),
            ),
          ],
          // Logo e Ícono de Comunidad
          Container(
            width: 38,
            height: 38,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(10),
              color: const Color(0xFF007A8A),
              boxShadow: [
                BoxShadow(
                  color: const Color(0xFF007A8A).withValues(alpha: 0.25),
                  blurRadius: 8,
                  offset: const Offset(0, 3),
                ),
              ],
            ),
            child: const Center(
              child: Icon(
                Icons.home_work_rounded,
                color: Colors.white,
                size: 22,
              ),
            ),
          ),
          const SizedBox(width: 10),
          // Subtítulo Colonia + Título de la Pantalla
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  (subtitle ?? 'RESIDENCIAL LAS FLORES').toUpperCase(),
                  style: const TextStyle(
                    fontSize: 9.5,
                    fontWeight: FontWeight.w700,
                    letterSpacing: 0.6,
                    color: Color(0xFF0D9488),
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 1),
                Text(
                  title,
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: isDark ? Colors.white : AppColors.stitchTextPrimary,
                    letterSpacing: -0.3,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
          // Botón Notificaciones con campana y badge
          if (onNotificationTap != null)
            BouncyTap(
              onTap: onNotificationTap!,
              child: Stack(
                clipBehavior: Clip.none,
                children: [
                  Container(
                    width: 40,
                    height: 40,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: isDark ? const Color(0xFF1E293B) : const Color(0xFFF1F5F9),
                    ),
                    child: Icon(
                      Icons.notifications_none_rounded,
                      size: 22,
                      color: isDark ? Colors.white70 : AppColors.stitchTextPrimary,
                    ),
                  ),
                  if (unreadNotifications > 0)
                    Positioned(
                      top: 4,
                      right: 4,
                      child: Container(
                        padding: const EdgeInsets.all(3),
                        decoration: const BoxDecoration(
                          color: Color(0xFFEF4444),
                          shape: BoxShape.circle,
                        ),
                        constraints: const BoxConstraints(minWidth: 10, minHeight: 10),
                        child: Center(
                          child: Text(
                            unreadNotifications > 9 ? '9+' : '$unreadNotifications',
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 8,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                      ),
                    ),
                ],
              ),
            ),
          const SizedBox(width: 10),
          // Avatar del Residente
          BouncyTap(
            onTap: onProfileTap ?? () {},
            child: Container(
              width: 38,
              height: 38,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: const LinearGradient(
                  colors: [AppColors.stitchSapphire, AppColors.stitchBlueLight],
                ),
                border: Border.all(
                  color: Colors.white,
                  width: 1.5,
                ),
                boxShadow: [
                  BoxShadow(
                    color: AppColors.stitchSapphire.withValues(alpha: 0.2),
                    blurRadius: 6,
                    offset: const Offset(0, 2),
                  ),
                ],
              ),
              child: Center(
                child: Text(
                  initials,
                  style: const TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.bold,
                    fontSize: 13,
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
