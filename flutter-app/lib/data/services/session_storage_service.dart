import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Servicio para almacenamiento seguro y persistencia local de la sesión.
/// Los tokens de autenticación se almacenan estrictamente en el almacenamiento seguro
/// del sistema operativo (Keystore en Android / Keychain en iOS) vía FlutterSecureStorage.
class SessionStorageService {
  static const String _keyAccessToken = 'asociacion_access_token';
  static const String _keyRefreshToken = 'asociacion_refresh_token';
  static const String _keyThemeMode = 'asociacion_theme_mode';

  final SharedPreferences _prefs;
  final FlutterSecureStorage _secureStorage;

  String? _cachedAccessToken;
  String? _cachedRefreshToken;

  SessionStorageService._(
    this._prefs,
    this._secureStorage, {
    String? accessToken,
    String? refreshToken,
  })  : _cachedAccessToken = accessToken,
        _cachedRefreshToken = refreshToken;

  /// Inicializa el servicio cargando los tokens seguros en memoria.
  static Future<SessionStorageService> init({
    FlutterSecureStorage? secureStorage,
    SharedPreferences? prefs,
  }) async {
    final sharedPrefs = prefs ?? await SharedPreferences.getInstance();
    final secStorage = secureStorage ?? const FlutterSecureStorage();

    // Migración de seguridad: eliminar residuos en texto plano de SharedPreferences
    await sharedPrefs.remove(_keyAccessToken);
    await sharedPrefs.remove(_keyRefreshToken);
    await sharedPrefs.remove('asociacion_custom_base_url');

    final accessToken = await secStorage.read(key: _keyAccessToken);
    final refreshToken = await secStorage.read(key: _keyRefreshToken);

    return SessionStorageService._(
      sharedPrefs,
      secStorage,
      accessToken: accessToken,
      refreshToken: refreshToken,
    );
  }

  /// Token de acceso para solicitudes autenticadas (almacenado de forma segura).
  String? get accessToken => _cachedAccessToken;

  /// Token de refresco para renovar la sesión (almacenado de forma segura).
  String? get refreshToken => _cachedRefreshToken;

  /// Modo de tema de la aplicación (preferencia no sensible).
  String? get themeMode => _prefs.getString(_keyThemeMode);

  /// Determina si existe una sesión activa y con tokens válidos.
  bool get hasSession =>
      _cachedAccessToken != null &&
      _cachedAccessToken!.isNotEmpty &&
      _cachedRefreshToken != null &&
      _cachedRefreshToken!.isNotEmpty;

  /// Guarda los tokens de sesión de forma segura en el almacenamiento del sistema operativo.
  Future<void> saveTokens({
    required String accessToken,
    required String refreshToken,
  }) async {
    _cachedAccessToken = accessToken;
    _cachedRefreshToken = refreshToken;
    await _secureStorage.write(key: _keyAccessToken, value: accessToken);
    await _secureStorage.write(key: _keyRefreshToken, value: refreshToken);
  }

  /// Elimina los tokens seguros y limpia la sesión en el dispositivo.
  Future<void> clearSession() async {
    _cachedAccessToken = null;
    _cachedRefreshToken = null;
    await _secureStorage.delete(key: _keyAccessToken);
    await _secureStorage.delete(key: _keyRefreshToken);
  }

  /// Persiste la preferencia de tema (Claro / Oscuro / Sistema).
  Future<void> setThemeMode(String mode) async {
    await _prefs.setString(_keyThemeMode, mode);
  }
}
