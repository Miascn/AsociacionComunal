import 'package:flutter/material.dart';
import '../theme/app_colors.dart';

/// Gráfica de línea ondulada con resplandor y degradado fluido,
/// inspirada en la interfaz de referencia Neo-Glassmorphism.
class MiniTrendChart extends StatelessWidget {
  final List<double> dataPoints;
  final List<String> labels;
  final Color primaryColor;
  final Color secondaryColor;
  final double height;

  const MiniTrendChart({
    super.key,
    this.dataPoints = const [0.35, 0.45, 0.30, 0.65, 0.50, 0.85, 0.70, 0.90],
    this.labels = const ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago'],
    this.primaryColor = AppColors.neoEmerald,
    this.secondaryColor = AppColors.neoCyan,
    this.height = 70,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        SizedBox(
          height: height,
          width: double.infinity,
          child: CustomPaint(
            painter: _TrendChartPainter(
              dataPoints: dataPoints,
              primaryColor: primaryColor,
              secondaryColor: secondaryColor,
              isDark: isDark,
            ),
          ),
        ),
        const SizedBox(height: 8),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: labels.map((label) {
            return Text(
              label,
              style: TextStyle(
                fontSize: 10,
                fontWeight: FontWeight.w500,
                color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
              ),
            );
          }).toList(),
        ),
      ],
    );
  }
}

class _TrendChartPainter extends CustomPainter {
  final List<double> dataPoints;
  final Color primaryColor;
  final Color secondaryColor;
  final bool isDark;

  _TrendChartPainter({
    required this.dataPoints,
    required this.primaryColor,
    required this.secondaryColor,
    required this.isDark,
  });

  @override
  void paint(Canvas canvas, Size size) {
    if (dataPoints.isEmpty) return;

    final width = size.width;
    final height = size.height;
    final stepX = width / (dataPoints.length - 1);

    // Calcular puntos en pantalla
    final points = <Offset>[];
    for (int i = 0; i < dataPoints.length; i++) {
      final x = i * stepX;
      // Invertir Y (0 arriba, height abajo) y dejar margen superior e inferior
      final y = height - (dataPoints[i] * (height - 14)) - 7;
      points.add(Offset(x, y));
    }

    // Construir Path suave con curvas Bézier cúbicas
    final path = Path();
    path.moveTo(points.first.dx, points.first.dy);

    for (int i = 0; i < points.length - 1; i++) {
      final p0 = points[i];
      final p1 = points[i + 1];
      final controlPoint1 = Offset(p0.dx + (p1.dx - p0.dx) / 2, p0.dy);
      final controlPoint2 = Offset(p0.dx + (p1.dx - p0.dx) / 2, p1.dy);
      path.cubicTo(
        controlPoint1.dx,
        controlPoint1.dy,
        controlPoint2.dx,
        controlPoint2.dy,
        p1.dx,
        p1.dy,
      );
    }

    // 1. Relleno con degradado traslúcido bajo la curva
    final fillPath = Path.from(path)
      ..lineTo(width, height)
      ..lineTo(0, height)
      ..close();

    final fillGradient = LinearGradient(
      begin: Alignment.topCenter,
      end: Alignment.bottomCenter,
      colors: [
        primaryColor.withValues(alpha: isDark ? 0.35 : 0.20),
        primaryColor.withValues(alpha: 0.0),
      ],
    );

    final fillPaint = Paint()
      ..shader = fillGradient.createShader(Rect.fromLTWH(0, 0, width, height))
      ..style = PaintingStyle.fill;

    canvas.drawPath(fillPath, fillPaint);

    // 2. Resplandor exterior (Glow) de la línea
    final glowPaint = Paint()
      ..color = primaryColor.withValues(alpha: isDark ? 0.45 : 0.25)
      ..strokeWidth = 6.0
      ..strokeCap = StrokeCap.round
      ..style = PaintingStyle.stroke
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 6);

    canvas.drawPath(path, glowPaint);

    // 3. Línea principal con degradado Esmeralda a Cian
    final lineGradient = LinearGradient(
      colors: [primaryColor, secondaryColor],
    );

    final strokePaint = Paint()
      ..shader = lineGradient.createShader(Rect.fromLTWH(0, 0, width, height))
      ..strokeWidth = 2.8
      ..strokeCap = StrokeCap.round
      ..style = PaintingStyle.stroke;

    canvas.drawPath(path, strokePaint);

    // 4. Punto luminoso en el último dato
    final lastPoint = points.last;
    final dotGlow = Paint()
      ..color = secondaryColor.withValues(alpha: 0.6)
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 4);
    canvas.drawCircle(lastPoint, 5, dotGlow);

    final dotPaint = Paint()..color = Colors.white;
    canvas.drawCircle(lastPoint, 2.5, dotPaint);
  }

  @override
  bool shouldRepaint(covariant _TrendChartPainter oldDelegate) {
    return oldDelegate.dataPoints != dataPoints ||
        oldDelegate.primaryColor != primaryColor ||
        oldDelegate.secondaryColor != secondaryColor ||
        oldDelegate.isDark != isDark;
  }
}
