import '../../core/constants/api_constants.dart';
import '../models/auth_models.dart';
import '../services/api_client.dart';
import '../services/session_storage_service.dart';

/// Repositorio encargado del ciclo de vida de la autenticación del residente.
class AuthRepository {
  final ApiClient _api;
  final SessionStorageService _storage;

  AuthRepository({
    required ApiClient api,
    required SessionStorageService storage,
  })  : _api = api,
        _storage = storage;

  /// Prueba la conectividad con el servidor API.
  Future<Map<String, dynamic>> checkConnection([String? url]) async {
    try {
      final res = await _api.get(ApiConstants.healthEndpoint, requiresAuth: false);
      if (res is Map<String, dynamic>) return res;
      return {'status': 'UP'};
    } catch (e) {
      return {'status': 'DOWN', 'error': e.toString()};
    }
  }

  /// Contraseña temporal activa guardada para cambio obligatorio.
  String? get temporaryPassword => _storage.temporaryPassword;

  Future<void> saveTemporaryPassword(String? tempPassword) =>
      _storage.saveTemporaryPassword(tempPassword);

  /// Inicia sesión con usuario y contraseña, guarda los tokens y obtiene el perfil de miembro.
  Future<MeResponse> login(String username, String password) async {
    final response = await _api.post(
      ApiConstants.loginEndpoint,
      body: {
        'username': username.trim(),
        'password': password,
      },
      requiresAuth: false,
    );

    final loginData = LoginResponse.fromJson(response as Map<String, dynamic>);

    await _storage.saveTokens(
      accessToken: loginData.accessToken,
      refreshToken: loginData.refreshToken,
    );

    final profile = await getProfile();
    if (profile.user.passwordChangeRequired) {
      await _storage.saveTemporaryPassword(password);
    } else {
      await _storage.saveTemporaryPassword(null);
    }
    return profile;
  }

  /// Restaura la sesión guardada localmente y renueva el token rotativo si es necesario.
  Future<MeResponse?> restoreSession() async {
    final refreshToken = _storage.refreshToken;
    final accessToken = _storage.accessToken;

    if (refreshToken == null || accessToken == null) {
      return null;
    }

    try {
      // Intenta obtener perfil con el accessToken existente
      return await getProfile();
    } catch (_) {
      // Si falló (ej. token de 15m expirado), intenta rotar usando el refreshToken
      try {
        final refreshResponse = await _api.post(
          ApiConstants.refreshEndpoint,
          body: {'refreshToken': refreshToken},
          requiresAuth: false,
        );

        final tokenData = TokenResponse.fromJson(refreshResponse as Map<String, dynamic>);
        await _storage.saveTokens(
          accessToken: tokenData.accessToken,
          refreshToken: tokenData.refreshToken,
        );

        return await getProfile();
      } catch (_) {
        await _storage.clearSession();
        return null;
      }
    }
  }

  /// Obtiene los datos del usuario actual y su afiliación comunal.
  Future<MeResponse> getProfile() async {
    final response = await _api.get(ApiConstants.meEndpoint);
    return MeResponse.fromJson(response as Map<String, dynamic>);
  }

  /// Cierra la sesión en el servidor y limpia las credenciales locales.
  Future<void> logout() async {
    final refreshToken = _storage.refreshToken;
    if (refreshToken != null) {
      try {
        await _api.post(
          ApiConstants.logoutEndpoint,
          body: {'refreshToken': refreshToken},
          requiresAuth: false,
        );
      } catch (_) {
        // Ignorar fallos de red durante el logout para asegurar la limpieza local
      }
    }
    await _storage.clearSession();
  }

  /// Cambia la contraseña obligatoria (útil para usuarios creados con clave temporal por DUI).
  Future<MeResponse> changePassword(String currentPassword, String newPassword) async {
    final response = await _api.post(
      ApiConstants.changePasswordEndpoint,
      body: {
        'currentPassword': currentPassword,
        'newPassword': newPassword,
      },
      requiresAuth: true,
    );

    final loginData = LoginResponse.fromJson(response as Map<String, dynamic>);
    await _storage.saveTokens(
      accessToken: loginData.accessToken,
      refreshToken: loginData.refreshToken,
    );
    await _storage.saveTemporaryPassword(null);

    return getProfile();
  }
}
