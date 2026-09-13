import 'dart:ui';
import 'package:flutter/material.dart';
import '../theme/app_colors.dart';

/// Contenedor reutilizable con efecto de Glassmorfismo (desenfoque de fondo y borde traslúcido).
class GlassContainer extends StatelessWidget {
  final Widget child;
  final double? width;
  final double? height;
  final double blur;
  final BorderRadiusGeometry? borderRadius;
  final EdgeInsetsGeometry? padding;
  final EdgeInsetsGeometry? margin;
  final Gradient? gradient;
  final Color? color;
  final Color? borderColor;
  final double borderWidth;
  final VoidCallback? onTap;
  final List<BoxShadow>? shadows;

  const GlassContainer({
    super.key,
    required this.child,
    this.width,
    this.height,
    this.blur = 16.0,
    this.borderRadius,
    this.padding,
    this.margin,
    this.gradient,
    this.color,
    this.borderColor,
    this.borderWidth = 1.2,
    this.onTap,
    this.shadows,
  });

  /// Variante para tarjetas oscuras de alto contraste (similar al card de Balance de la referencia)
  factory GlassContainer.heroDark({
    Key? key,
    required Widget child,
    double? width,
    double? height,
    EdgeInsetsGeometry? padding,
    EdgeInsetsGeometry? margin,
    VoidCallback? onTap,
    BorderRadiusGeometry? borderRadius,
  }) {
    return GlassContainer(
      key: key,
      width: width,
      height: height,
      blur: 24.0,
      borderRadius: borderRadius ?? BorderRadius.circular(24),
      padding: padding ?? const EdgeInsets.all(22),
      margin: margin,
      onTap: onTap,
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: [
          Color(0xE60B1222), // 90% opacity dark navy
          Color(0xCC1E293B), // 80% slate
        ],
      ),
      borderColor: const Color(0x3338B6FF), // Cyan accent border glow
      shadows: [
        BoxShadow(
          color: Colors.black.withValues(alpha: 0.55),
          blurRadius: 24,
          offset: const Offset(6, 8),
        ),
        BoxShadow(
          color: Colors.white.withValues(alpha: 0.08),
          blurRadius: 10,
          offset: const Offset(-3, -3),
        ),
      ],
      child: child,
    );
  }

  /// Variante para tarjetas traslúcidas claras con relieve
  factory GlassContainer.frosted({
    Key? key,
    required Widget child,
    double? width,
    double? height,
    EdgeInsetsGeometry? padding,
    EdgeInsetsGeometry? margin,
    VoidCallback? onTap,
    BorderRadiusGeometry? borderRadius,
    Color? accentColor,
  }) {
    return GlassContainer(
      key: key,
      width: width,
      height: height,
      blur: 16.0,
      borderRadius: borderRadius ?? BorderRadius.circular(20),
      padding: padding ?? const EdgeInsets.all(18),
      margin: margin,
      onTap: onTap,
      gradient: LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: [
          (accentColor ?? Colors.white).withValues(alpha: 0.22),
          (accentColor ?? Colors.white).withValues(alpha: 0.08),
        ],
      ),
      borderColor: (accentColor ?? Colors.white).withValues(alpha: 0.30),
      child: child,
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final effectiveRadius = borderRadius ?? BorderRadius.circular(20);

    final defaultShadows = isDark
        ? const [
            BoxShadow(
              color: AppColors.neoShadowDark,
              blurRadius: 18,
              offset: Offset(5, 7),
            ),
            BoxShadow(
              color: AppColors.neoHighlightDark,
              blurRadius: 10,
              offset: Offset(-3, -3),
            ),
          ]
        : const <BoxShadow>[];

    Widget container = Container(
      width: width,
      height: height,
      margin: margin,
      decoration: BoxDecoration(
        borderRadius: effectiveRadius,
        boxShadow: shadows ?? defaultShadows,
      ),
      child: ClipRRect(
        borderRadius: effectiveRadius,
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: blur, sigmaY: blur),
          child: Container(
            padding: padding ?? const EdgeInsets.all(16),
            decoration: BoxDecoration(
              borderRadius: effectiveRadius,
              color: color ?? (gradient == null ? AppColors.glassWhite : null),
              gradient: gradient ?? AppColors.glassLightGradient,
              border: Border.all(
                color: borderColor ?? AppColors.glassBorderLight,
                width: borderWidth,
              ),
            ),
            child: child,
          ),
        ),
      ),
    );

    if (onTap != null) {
      return GestureDetector(
        onTap: onTap,
        behavior: HitTestBehavior.opaque,
        child: container,
      );
    }

    return container;
  }
}

/// Fondo ambiental con orbes de color que permiten que el efecto glassmórfico resalte.
class AmbientBackground extends StatelessWidget {
  final Widget child;

  const AmbientBackground({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      color: isDark ? AppColors.neoBackgroundDark : AppColors.backgroundLight,
      child: SafeArea(child: child),
    );
  }
}
