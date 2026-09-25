import 'package:flutter/material.dart';

/// Paleta de colores del sistema Asociación Comunal con soporte para Glassmorphism.
class AppColors {
  AppColors._();

  // Colores de Marca Primarios
  static const Color brandBlue = Color(0xFF0759C7);
  static const Color deepNavy = Color(0xFF002D72);
  static const Color brandSky = Color(0xFF38B6FF);
  static const Color brandCyan = Color(0xFF00D2D3);

  // Colores de Estado
  static const Color successGreen = Color(0xFF21A453);
  static const Color successGreenLight = Color(0xFFE8F8EE);
  static const Color warningAmber = Color(0xFFFFB020);
  static const Color warningAmberLight = Color(0xFFFFF7E6);
  static const Color errorRed = Color(0xFFE53935);
  static const Color errorRedLight = Color(0xFFFFEBEE);
  static const Color infoIndigo = Color(0xFF4F46E5);
  static const Color electricIndigo = Color(0xFF4F46E5);
  static const Color charcoalGrey = Color(0xFF1E293B);

  // Tarjetas de Acciones Rápidas (Inspiradas en diseño Fintech / Glassmorphism)
  static const Color actionCoral = Color(0xFFFF7B60);
  static const Color actionCoralLight = Color(0xFFFFECE8);
  static const Color actionMint = Color(0xFF48CF88);
  static const Color actionMintLight = Color(0xFFE6F9F0);
  static const Color actionPurple = Color(0xFFA77DFF);
  static const Color actionPurpleLight = Color(0xFFF3EDFF);

  // Superficies y Fondos
  static const Color backgroundLight = Color(0xFFF1F5F9);
  static const Color backgroundDark = Color(0xFF090D14);
  static const Color surfaceLight = Color(0xFFFFFFFF);
  static const Color surfaceDark = Color(0xFF131B27);

  // Textos
  static const Color textPrimaryLight = Color(0xFF0F172A);
  static const Color textSecondaryLight = Color(0xFF64748B);
  static const Color textMutedLight = Color(0xFF94A3B8);

  static const Color textPrimaryDark = Color(0xFFF8FAFC);
  static const Color textSecondaryDark = Color(0xFF8E9CAE);
  static const Color textMutedDark = Color(0xFF5B6B82);

  // Colores Específicos para Neo-Glassmorphism (Neumorphism + Glassmorphism)
  static const Color neoEmerald = Color(0xFF00D287);
  static const Color neoCyan = Color(0xFF0EA5E9);
  static const Color neoPurple = Color(0xFF8B5CF6);
  static const Color neoBackgroundDark = Color(0xFF090D14);
  static const Color neoSurfaceDark = Color(0xFF131B27);
  static const Color neoGlassFillDark = Color(0x45162232);
  static const Color neoGlassFillLight = Color(0xEAFFFFFF);
  static const Color neoGlassBorderDark = Color(0x25FFFFFF);
  static const Color neoGlassBorderLight = Color(0x180F172A);
  static const Color neoHighlightDark = Color(0x18FFFFFF);
  static const Color neoHighlightLight = Color(0xB3FFFFFF);
  static const Color neoShadowDark = Color(0x60000000);
  static const Color neoShadowLight = Color(0x120F172A);

  // Colores Específicos para Glassmorphism Clásico
  static const Color glassWhite = Color(0x35FFFFFF);
  static const Color glassWhiteHover = Color(0x45FFFFFF);
  static const Color glassDark = Color(0x40141D33);
  static const Color glassBorderLight = Color(0x40FFFFFF);
  static const Color glassBorderDark = Color(0x20FFFFFF);
  static const Color glassShadow = Color(0x10000000);

  // Gradientes Institucionales
  static const LinearGradient brandGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [deepNavy, brandBlue],
  );

  static const LinearGradient heroCardGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [
      Color(0xFF0F172A),
      Color(0xFF1E293B),
      Color(0xFF021B4D),
    ],
  );

  static const LinearGradient glassLightGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [
      Color(0x40FFFFFF),
      Color(0x15FFFFFF),
    ],
  );

  static const LinearGradient glassDarkGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [
      Color(0x601E293B),
      Color(0x300F172A),
    ],
  );

  // Gradiente de fondo con esferas sutiles para efecto de desenfoque
  static const RadialGradient ambientOrb1 = RadialGradient(
    center: Alignment(-0.8, -0.7),
    radius: 1.2,
    colors: [
      Color(0x330759C7),
      Color(0x000759C7),
    ],
  );

  static const RadialGradient ambientOrb2 = RadialGradient(
    center: Alignment(0.8, -0.2),
    radius: 1.0,
    colors: [
      Color(0x2638B6FF),
      Color(0x0038B6FF),
    ],
  );

  // ==========================================
  // STITCH DESIGN SYSTEM (Colonia Conecta)
  // ==========================================
  static const Color stitchSapphire = Color(0xFF1E3A8A);
  static const Color stitchNavy = Color(0xFF0B224E);
  static const Color stitchNavyContainer = Color(0xFF0D2B68);
  static const Color stitchBlueLight = Color(0xFF2563EB);
  static const Color stitchEmerald = Color(0xFF10B981);
  static const Color stitchEmeraldMint = Color(0xFF34D399);
  static const Color stitchEmeraldContainer = Color(0xFFD1FAE5);
  static const Color stitchTeal = Color(0xFF0D9488);
  static const Color stitchTealContainer = Color(0xFFCCFBF1);
  static const Color stitchCanvasLight = Color(0xFFF8FAFC);
  static const Color stitchSurfaceContainer = Color(0xFFEEF2FF);
  static const Color stitchSurfaceLow = Color(0xFFF1F5F9);
  static const Color stitchSurfaceBorder = Color(0xFFE2E8F0);
  static const Color stitchTextPrimary = Color(0xFF0F172A);
  static const Color stitchTextSecondary = Color(0xFF475569);
  static const Color stitchTextMuted = Color(0xFF94A3B8);

  static const LinearGradient stitchHeroGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [
      Color(0xFF0B224E),
      Color(0xFF0F3E3A),
      Color(0xFF0A3832),
    ],
  );

  static const LinearGradient stitchPaymentGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [
      Color(0xFF0D2B68),
      Color(0xFF1E3A8A),
      Color(0xFF10357E),
    ],
  );
}
