import 'package:flutter/foundation.dart';

/// Constantes y configuración de red para la API de Asociación Comunal.
class ApiConstants {
  ApiConstants._();

  /// URL base por defecto según la plataforma en tiempo de ejecución.
  static String get defaultBaseUrl {
    if (kIsWeb) return 'http://localhost:8080';
    switch (defaultTargetPlatform) {
      case TargetPlatform.android:
        // 10.0.2.2 es el alias del host en el emulador estándar de Android
        return 'http://10.0.2.2:8080';
      case TargetPlatform.iOS:
      case TargetPlatform.macOS:
      case TargetPlatform.windows:
      case TargetPlatform.linux:
      default:
        return 'http://localhost:8080';
    }
  }

  /// URL activa (permite ser reconfigurada en tiempo de ejecución por túnel ngrok/cloudflare).
  static String baseUrl = defaultBaseUrl;

  // Cabeceras personalizadas
  static const Map<String, String> standardHeaders = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'ngrok-skip-browser-warning': 'asociacion-flutter',
  };

  // Rutas de Autenticación
  static const String loginEndpoint = '/api/auth/login';
  static const String refreshEndpoint = '/api/auth/refresh';
  static const String logoutEndpoint = '/api/auth/logout';
  static const String changePasswordEndpoint = '/api/auth/change-password';
  static const String meEndpoint = '/api/me';

  // Rutas de Negocio para el Residente
  static const String aportacionesEndpoint = '/api/aportaciones';
  static const String proyectosEndpoint = '/api/proyectos';
  static const String reunionesEndpoint = '/api/reuniones';
  static const String votacionesEndpoint = '/api/votaciones';

  // Actualizaciones
  static const String updateManifestEndpoint = '/api/mobile/updates/android/manifest';

  // Timeouts
  static const Duration connectionTimeout = Duration(seconds: 15);
  static const Duration receiveTimeout = Duration(seconds: 20);
}
