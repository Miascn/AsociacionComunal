import 'package:flutter_test/flutter_test.dart';
import 'package:asociacion_comunal_app/core/theme/app_colors.dart';
import 'package:asociacion_comunal_app/core/theme/app_theme.dart';

void main() {
  test('Stitch Design System Tokens & Theme Smoke Test', () {
    // Verificar que los tokens Stitch existan y tengan valores válidos
    expect(AppColors.stitchSapphire, isNotNull);
    expect(AppColors.stitchNavy, isNotNull);
    expect(AppColors.stitchEmerald, isNotNull);
    expect(AppColors.stitchTeal, isNotNull);
    expect(AppColors.stitchCanvasLight, isNotNull);

    // Verificar tema claro y oscuro
    final lightTheme = AppTheme.lightTheme;
    final darkTheme = AppTheme.darkTheme;

    expect(lightTheme.scaffoldBackgroundColor, isNotNull);
    expect(darkTheme.scaffoldBackgroundColor, isNotNull);
  });
}
