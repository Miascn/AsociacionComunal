/// Constantes y configuración de red para la API de Asociación Comunal.
class ApiConstants {
  ApiConstants._();

  /// URL base oficial de producción para la Asociación Comunal.
  static const String defaultBaseUrl = 'https://asociacion-comunal.miascn.org';

  /// URL activa de la API.
  static String baseUrl = defaultBaseUrl;

  /// Cabeceras estándar para todas las peticiones HTTPS.
  static const Map<String, String> standardHeaders = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'User-Agent': 'AsociacionComunalApp/1.0 (Android/Flutter)',
    'ngrok-skip-browser-warning': 'asociacion-flutter',
  };

  // Rutas de Verificación
  static const String healthEndpoint = '/health';

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
  static const String votosEndpoint = '/api/votos';
  static const String directivaActualEndpoint = '/api/directiva/actual';
  static const String viviendasEndpoint = '/api/viviendas';

  // Actualizaciones
  static const String updateManifestEndpoint = '/api/mobile/updates/android/manifest';

  // Timeouts
  static const Duration connectionTimeout = Duration(seconds: 15);
  static const Duration receiveTimeout = Duration(seconds: 20);
}
