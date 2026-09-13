import 'package:shared_preferences/shared_preferences.dart';

/// Servicio para almacenamiento seguro y persistencia local de la sesión.
class SessionStorageService {
  static const String _keyAccessToken = 'asociacion_access_token';
  static const String _keyRefreshToken = 'asociacion_refresh_token';
  static const String _keyCustomBaseUrl = 'asociacion_custom_base_url';
  static const String _keyThemeMode = 'asociacion_theme_mode';

  final SharedPreferences _prefs;

  SessionStorageService(this._prefs);

  static Future<SessionStorageService> init() async {
    final prefs = await SharedPreferences.getInstance();
    return SessionStorageService(prefs);
  }

  String? get accessToken => _prefs.getString(_keyAccessToken);
  String? get refreshToken => _prefs.getString(_keyRefreshToken);
  String? get customBaseUrl => _prefs.getString(_keyCustomBaseUrl);
  String? get themeMode => _prefs.getString(_keyThemeMode);

  bool get hasSession => accessToken != null && refreshToken != null;

  Future<void> saveTokens({
    required String accessToken,
    required String refreshToken,
  }) async {
    await _prefs.setString(_keyAccessToken, accessToken);
    await _prefs.setString(_keyRefreshToken, refreshToken);
  }

  Future<void> clearSession() async {
    await _prefs.remove(_keyAccessToken);
    await _prefs.remove(_keyRefreshToken);
  }

  Future<void> setCustomBaseUrl(String? url) async {
    if (url == null || url.trim().isEmpty) {
      await _prefs.remove(_keyCustomBaseUrl);
    } else {
      await _prefs.setString(_keyCustomBaseUrl, url.trim());
    }
  }

  Future<void> setThemeMode(String mode) async {
    await _prefs.setString(_keyThemeMode, mode);
  }
}
