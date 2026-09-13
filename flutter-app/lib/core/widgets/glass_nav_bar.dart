import 'dart:ui';
import 'package:flutter/material.dart';
import '../theme/app_colors.dart';
import 'bouncy_tap.dart';

/// Barra de navegación flotante con efecto Glassmorfismo (desenfoque de fondo y borde traslúcido).
class GlassNavBar extends StatelessWidget {
  final int currentIndex;
  final ValueChanged<int> onTap;
  final List<GlassNavItem> items;

  const GlassNavBar({
    super.key,
    required this.currentIndex,
    required this.onTap,
    required this.items,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Padding(
      padding: const EdgeInsets.only(left: 20, right: 20, bottom: 24),
      child: Container(
        height: 72,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(36),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: isDark ? 0.65 : 0.14),
              blurRadius: 28,
              offset: const Offset(0, 10),
            ),
            BoxShadow(
              color: (isDark ? Colors.white : Colors.white).withValues(alpha: isDark ? 0.08 : 0.8),
              blurRadius: 8,
              offset: const Offset(0, -2),
            ),
          ],
        ),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(36),
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 24, sigmaY: 24),
            child: Container(
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(36),
                color: isDark
                    ? const Color(0xE60E1624)
                    : const Color(0xE8FFFFFF),
                border: Border.all(
                  color: isDark
                      ? AppColors.neoGlassBorderDark
                      : AppColors.neoGlassBorderLight,
                  width: 1.2,
                ),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                children: List.generate(items.length, (index) {
                  final item = items[index];
                  final isSelected = currentIndex == index;

                  return BouncyTap(
                    scaleDown: 0.88,
                    onTap: () => onTap(index),
                    child: AnimatedContainer(
                      duration: const Duration(milliseconds: 280),
                      curve: Curves.easeOutBack,
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(24),
                        color: isSelected
                            ? (isDark
                                ? AppColors.neoEmerald.withValues(alpha: 0.16)
                                : AppColors.brandBlue.withValues(alpha: 0.10))
                            : Colors.transparent,
                      ),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          AnimatedScale(
                            scale: isSelected ? 1.14 : 1.0,
                            duration: const Duration(milliseconds: 250),
                            curve: Curves.easeOutBack,
                            child: Icon(
                              isSelected ? item.activeIcon : item.icon,
                              size: 24,
                              color: isSelected
                                  ? (isDark ? AppColors.neoEmerald : AppColors.brandBlue)
                                  : (isDark
                                      ? AppColors.textMutedDark
                                      : AppColors.textSecondaryLight),
                            ),
                          ),
                          const SizedBox(height: 3),
                          Text(
                            item.label,
                            style: TextStyle(
                              fontSize: 10.5,
                              fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
                              color: isSelected
                                  ? (isDark ? AppColors.neoEmerald : AppColors.brandBlue)
                                  : (isDark
                                      ? AppColors.textMutedDark
                                      : AppColors.textSecondaryLight),
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                }),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class GlassNavItem {
  final IconData icon;
  final IconData activeIcon;
  final String label;

  const GlassNavItem({
    required this.icon,
    required this.activeIcon,
    required this.label,
  });
}
