import 'package:flutter/material.dart';
import '../data/models/auth_models.dart';
import '../data/repositories/auth_repository.dart';

enum AuthStatus {
  initial,
  loading,
  authenticated,
  unauthenticated,
  passwordChangeRequired,
  error,
}

/// ViewModel para gestionar el estado de sesión y autenticación del residente.
class AuthViewModel extends ChangeNotifier {
  final AuthRepository _authRepository;

  AuthStatus _status = AuthStatus.initial;
  MeResponse? _profile;
  String? _errorMessage;
  String? _temporaryPassword;

  AuthViewModel({required AuthRepository authRepository})
      : _authRepository = authRepository;

  AuthStatus get status => _status;
  MeResponse? get profile => _profile;
  String? get errorMessage => _errorMessage;
  String? get temporaryPassword =>
      _temporaryPassword ?? _authRepository.temporaryPassword;
  bool get isLoading => _status == AuthStatus.loading;
  bool get isAuthenticated => _status == AuthStatus.authenticated;

  /// Restaura la sesión guardada en frío al iniciar la aplicación.
  Future<void> restoreSession() async {
    _status = AuthStatus.loading;
    notifyListeners();

    try {
      final restored = await _authRepository.restoreSession();
      if (restored != null) {
        _profile = restored;
        if (restored.user.passwordChangeRequired) {
          _temporaryPassword = _authRepository.temporaryPassword;
          _status = AuthStatus.passwordChangeRequired;
        } else {
          _status = AuthStatus.authenticated;
        }
      } else {
        _status = AuthStatus.unauthenticated;
      }
    } catch (e) {
      _status = AuthStatus.unauthenticated;
    }
    notifyListeners();
  }

  /// Inicia sesión con credenciales de usuario.
  Future<bool> login(String username, String password) async {
    _status = AuthStatus.loading;
    _errorMessage = null;
    _temporaryPassword = password;
    notifyListeners();

    try {
      final profile = await _authRepository.login(username, password);
      _profile = profile;

      if (profile.user.passwordChangeRequired) {
        _status = AuthStatus.passwordChangeRequired;
      } else {
        _status = AuthStatus.authenticated;
      }
      notifyListeners();
      return true;
    } catch (e) {
      _status = AuthStatus.error;
      _errorMessage = e.toString();
      notifyListeners();
      return false;
    }
  }

  /// Cambia la contraseña (tanto obligatoria como voluntaria).
  Future<bool> changePassword(String currentPassword, String newPassword) async {
    final previousStatus = _status;
    _status = AuthStatus.loading;
    _errorMessage = null;
    notifyListeners();

    try {
      final updatedProfile = await _authRepository.changePassword(
        currentPassword,
        newPassword,
      );
      _profile = updatedProfile;
      _temporaryPassword = null;
      _status = AuthStatus.authenticated;
      notifyListeners();
      return true;
    } catch (e) {
      _status = previousStatus == AuthStatus.passwordChangeRequired
          ? AuthStatus.passwordChangeRequired
          : AuthStatus.authenticated;
      _errorMessage = e.toString();
      notifyListeners();
      return false;
    }
  }

  /// Cierra la sesión activa y regresa a la pantalla de login.
  Future<void> logout() async {
    _status = AuthStatus.loading;
    notifyListeners();

    await _authRepository.logout();
    _profile = null;
    _temporaryPassword = null;
    _status = AuthStatus.unauthenticated;
    notifyListeners();
  }

  void clearError() {
    _errorMessage = null;
    if (_status == AuthStatus.error) {
      _status = AuthStatus.unauthenticated;
    }
    notifyListeners();
  }

  /// Verifica la conectividad con el servidor API.
  Future<Map<String, dynamic>> checkConnection([String? url]) {
    return _authRepository.checkConnection(url);
  }
}
