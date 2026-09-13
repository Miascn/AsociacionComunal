import 'dart:ui';
import 'package:flutter/material.dart';
import '../theme/app_colors.dart';

/// Contenedor que fusiona Neumorfismo (relieve y sombras físicas duales)
/// con Glassmorfismo (desenfoque gaussiano y transparencia vidriada).
class NeoGlassContainer extends StatelessWidget {
  final Widget child;
  final double? width;
  final double? height;
  final double blur;
  final BorderRadiusGeometry? borderRadius;
  final EdgeInsetsGeometry? padding;
  final EdgeInsetsGeometry? margin;
  final Color? accentColor;
  final VoidCallback? onTap;
  final bool isInset;
  final bool showGlow;
  final double borderWidth;

  const NeoGlassContainer({
    super.key,
    required this.child,
    this.width,
    this.height,
    this.blur = 16.0,
    this.borderRadius,
    this.padding,
    this.margin,
    this.accentColor,
    this.onTap,
    this.isInset = false,
    this.showGlow = false,
    this.borderWidth = 1.0,
  });

  /// Variante para la Tarjeta Principal de Balance (Hero Card)
  factory NeoGlassContainer.hero({
    Key? key,
    required Widget child,
    double? width,
    double? height,
    EdgeInsetsGeometry? padding,
    EdgeInsetsGeometry? margin,
    VoidCallback? onTap,
    BorderRadiusGeometry? borderRadius,
  }) {
    return NeoGlassContainer(
      key: key,
      width: width,
      height: height,
      blur: 20.0,
      borderRadius: borderRadius ?? BorderRadius.circular(26),
      padding: padding ?? const EdgeInsets.all(22),
      margin: margin,
      onTap: onTap,
      borderWidth: 1.2,
      child: child,
    );
  }

  /// Variante circular para botones e iconos estilo Neumórfico
  factory NeoGlassContainer.circle({
    Key? key,
    required Widget child,
    double size = 48,
    Color? accentColor,
    VoidCallback? onTap,
    bool isInset = false,
  }) {
    return NeoGlassContainer(
      key: key,
      width: size,
      height: size,
      blur: 14.0,
      borderRadius: BorderRadius.circular(size / 2),
      padding: EdgeInsets.zero,
      accentColor: accentColor,
      onTap: onTap,
      isInset: isInset,
      borderWidth: 1.0,
      child: Center(child: child),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final effectiveRadius = borderRadius ?? BorderRadius.circular(22);

    // Sombras: Neumórficas en modo oscuro, limpias (sin manchas ni sombras grises) en modo claro
    final shadows = isDark
        ? const <BoxShadow>[
            // Sombra inferior derecha (profundidad física)
            BoxShadow(
              color: AppColors.neoShadowDark,
              offset: Offset(6, 8),
              blurRadius: 18,
              spreadRadius: 0,
            ),
            // Resplandor de luz superior izquierda (relieve especular)
            BoxShadow(
              color: AppColors.neoHighlightDark,
              offset: Offset(-3, -3),
              blurRadius: 10,
              spreadRadius: 0,
            ),
          ]
        : const <BoxShadow>[];

    Widget content = Container(
      width: width,
      height: height,
      margin: margin,
      decoration: BoxDecoration(
        borderRadius: effectiveRadius,
        boxShadow: isInset ? null : shadows,
      ),
      child: ClipRRect(
        borderRadius: effectiveRadius,
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: blur, sigmaY: blur),
          child: Container(
            padding: padding ?? const EdgeInsets.all(16),
            decoration: BoxDecoration(
              borderRadius: effectiveRadius,
              gradient: LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: isDark
                    ? const [
                        Color(0x35182234),
                        Color(0x180D1522),
                      ]
                    : const [
                        Color(0xF5FFFFFF),
                        Color(0xD8F8FAFC),
                      ],
              ),
              border: Border.all(
                color: isDark
                    ? AppColors.neoGlassBorderDark
                    : AppColors.neoGlassBorderLight,
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
        child: content,
      );
    }

    return content;
  }
}
