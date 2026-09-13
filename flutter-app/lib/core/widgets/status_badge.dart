import 'package:flutter/material.dart';
import '../theme/app_colors.dart';

/// Insignia de estado elegante para miembros, aportaciones y proyectos.
class StatusBadge extends StatelessWidget {
  final String label;
  final Color? color;
  final Color? backgroundColor;
  final IconData? icon;

  const StatusBadge({
    super.key,
    required this.label,
    this.color,
    this.backgroundColor,
    this.icon,
  });

  factory StatusBadge.fromStatus(String status) {
    final s = status.toUpperCase();
    if (s == 'ACTIVO' || s == 'PAGADO' || s == 'COMPLETADO' || s == 'APROBADO') {
      return StatusBadge(
        label: status,
        color: AppColors.successGreen,
        backgroundColor: AppColors.successGreenLight,
        icon: Icons.check_circle_outline,
      );
    } else if (s == 'PENDIENTE' || s == 'EN_PROCESO' || s == 'EN_CURSO') {
      return StatusBadge(
        label: status,
        color: AppColors.warningAmber,
        backgroundColor: AppColors.warningAmberLight,
        icon: Icons.access_time,
      );
    } else if (s == 'INACTIVO' || s == 'CANCELADO' || s == 'ANULADO' || s == 'RECHAZADO') {
      return StatusBadge(
        label: status,
        color: AppColors.errorRed,
        backgroundColor: AppColors.errorRedLight,
        icon: Icons.cancel_outlined,
      );
    }
    return StatusBadge(
      label: status,
      color: AppColors.brandBlue,
      backgroundColor: const Color(0xFFE8F1FC),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final fg = color ?? AppColors.brandBlue;
    final bg = isDark
        ? fg.withValues(alpha: 0.18)
        : (backgroundColor ?? fg.withValues(alpha: 0.12));

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: fg.withValues(alpha: 0.3), width: 0.8),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, size: 12, color: fg),
            const SizedBox(width: 4),
          ],
          Text(
            label.toUpperCase(),
            style: TextStyle(
              color: fg,
              fontSize: 10.5,
              fontWeight: FontWeight.bold,
              letterSpacing: 0.5,
            ),
          ),
        ],
      ),
    );
  }
}
