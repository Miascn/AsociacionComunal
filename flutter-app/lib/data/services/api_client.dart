import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import '../../core/constants/api_constants.dart';
import 'session_storage_service.dart';

/// Excepción personalizada de la API con código y mensaje amigable.
class ApiException implements Exception {
  final int statusCode;
  final String message;
  final String? errorCode;

  const ApiException({
    required this.statusCode,
    required this.message,
    this.errorCode,
  });

  @override
  String toString() => message;
}

/// Cliente HTTP principal para la comunicación con la API central de la Asociación Comunal.
class ApiClient {
  final http.Client _client;
  final SessionStorageService _storage;
  void Function()? onSessionExpired;

  Future<bool>? _refreshFuture;

  ApiClient({
    http.Client? client,
    required SessionStorageService storage,
    this.onSessionExpired,
  })  : _client = client ?? http.Client(),
        _storage = storage;

  String get _activeBaseUrl => ApiConstants.baseUrl.endsWith('/')
      ? ApiConstants.baseUrl.substring(0, ApiConstants.baseUrl.length - 1)
      : ApiConstants.baseUrl;

  Map<String, String> _buildHeaders({String? token, bool requiresAuth = true}) {
    final headers = Map<String, String>.from(ApiConstants.standardHeaders);
    final authToken = token ?? (requiresAuth ? _storage.accessToken : null);

    if (authToken != null && authToken.isNotEmpty) {
      headers['Authorization'] = 'Bearer $authToken';
    }
    return headers;
  }

  Future<dynamic> get(
    String endpoint, {
    Map<String, String>? queryParams,
    bool requiresAuth = true,
  }) async {
    return _sendWithAutoRefresh(
      (token) {
        final uri = Uri.parse('$_activeBaseUrl$endpoint')
            .replace(queryParameters: queryParams);
        return _client.get(
          uri,
          headers: _buildHeaders(token: token, requiresAuth: requiresAuth),
        );
      },
      requiresAuth: requiresAuth,
    );
  }

  Future<dynamic> post(
    String endpoint, {
    Map<String, dynamic>? body,
    String? token,
    bool requiresAuth = true,
  }) async {
    return _sendWithAutoRefresh(
      (authToken) {
        final uri = Uri.parse('$_activeBaseUrl$endpoint');
        return _client.post(
          uri,
          headers: _buildHeaders(
            token: token ?? authToken,
            requiresAuth: requiresAuth,
          ),
          body: body != null ? jsonEncode(body) : null,
        );
      },
      requiresAuth: requiresAuth,
    );
  }

  Future<dynamic> put(
    String endpoint, {
    Map<String, dynamic>? body,
    bool requiresAuth = true,
  }) async {
    return _sendWithAutoRefresh(
      (token) {
        final uri = Uri.parse('$_activeBaseUrl$endpoint');
        return _client.put(
          uri,
          headers: _buildHeaders(token: token, requiresAuth: requiresAuth),
          body: body != null ? jsonEncode(body) : null,
        );
      },
      requiresAuth: requiresAuth,
    );
  }

  String get activeBaseUrl => _activeBaseUrl;

  /// Verifica si el servidor está respondiendo adecuadamente en el endpoint /health.
  Future<Map<String, dynamic>> checkHealth() async {
    try {
      final uri = Uri.parse('$_activeBaseUrl/health');
      final response = await _client.get(
        uri,
        headers: ApiConstants.standardHeaders,
      ).timeout(const Duration(seconds: 6));

      if (response.statusCode >= 200 && response.statusCode < 300) {
        return {'ok': true, 'message': 'Conexión exitosa con la API de la Asociación'};
      }
      return {
        'ok': false,
        'message': 'El servidor respondió con código HTTP ${response.statusCode}',
      };
    } on SocketException {
      return {
        'ok': false,
        'message': 'No se pudo conectar con el servidor. Verifica tu conexión a internet.',
      };
    } on TimeoutException {
      return {
        'ok': false,
        'message': 'Tiempo de espera agotado al conectar con el servidor.',
      };
    } catch (e) {
      return {
        'ok': false,
        'message': 'Error al conectar con la API: $e',
      };
    }
  }

  Future<dynamic> _sendWithAutoRefresh(
    Future<http.Response> Function(String? currentToken) requestFn, {
    required bool requiresAuth,
  }) async {
    try {
      final response = await requestFn(_storage.accessToken)
          .timeout(ApiConstants.connectionTimeout);

      // Si recibimos 401 y la solicitud requería autenticación, intentamos refrescar el token
      if (response.statusCode == 401 && requiresAuth && _storage.refreshToken != null) {
        final refreshSuccess = await _tryRefreshToken();
        if (refreshSuccess) {
          // Reintentamos UNA sola vez la solicitud con el nuevo token
          final retryResponse = await requestFn(_storage.accessToken)
              .timeout(ApiConstants.connectionTimeout);
          return _handleResponse(retryResponse);
        } else {
          await _storage.clearSession();
          onSessionExpired?.call();
          throw const ApiException(
            statusCode: 401,
            message: 'Tu sesión ha expirado. Por favor ingresa nuevamente.',
          );
        }
      }

      return _handleResponse(response);
    } on SocketException {
      throw const ApiException(
        statusCode: 0,
        message: 'No fue posible conectar con el servidor. Verifica tu conexión a internet.',
      );
    } on TimeoutException {
      throw const ApiException(
        statusCode: 408,
        message: 'El servidor tardó demasiado en responder. Por favor reintenta.',
      );
    } on ApiException {
      rethrow;
    } catch (e) {
      throw ApiException(
        statusCode: 500,
        message: 'Ocurrió un error inesperado: ${e.toString()}',
      );
    }
  }

  /// Ejecuta el refresco de token de forma sincronizada para evitar peticiones concurrentes duplicadas.
  Future<bool> _tryRefreshToken() async {
    if (_refreshFuture != null) {
      return _refreshFuture!;
    }

    _refreshFuture = _performTokenRefresh();
    try {
      final result = await _refreshFuture!;
      return result;
    } finally {
      _refreshFuture = null;
    }
  }

  Future<bool> _performTokenRefresh() async {
    final currentRefreshToken = _storage.refreshToken;
    if (currentRefreshToken == null || currentRefreshToken.isEmpty) {
      return false;
    }

    try {
      final uri = Uri.parse('$_activeBaseUrl${ApiConstants.refreshEndpoint}');
      final response = await _client.post(
        uri,
        headers: ApiConstants.standardHeaders,
        body: jsonEncode({'refreshToken': currentRefreshToken}),
      ).timeout(ApiConstants.connectionTimeout);

      if (response.statusCode >= 200 && response.statusCode < 300) {
        final decoded = jsonDecode(response.body);
        if (decoded is Map<String, dynamic>) {
          final newAccess = decoded['accessToken'] as String?;
          final newRefresh = decoded['refreshToken'] as String?;
          if (newAccess != null && newRefresh != null) {
            await _storage.saveTokens(
              accessToken: newAccess,
              refreshToken: newRefresh,
            );
            return true;
          }
        }
      }
      return false;
    } catch (_) {
      return false;
    }
  }

  dynamic _handleResponse(http.Response response) {
    final statusCode = response.statusCode;

    // 204 No Content
    if (statusCode == 204) return null;

    dynamic decoded;
    if (response.body.isNotEmpty) {
      try {
        decoded = jsonDecode(response.body);
      } catch (_) {
        decoded = response.body;
      }
    }

    if (statusCode >= 200 && statusCode < 300) {
      return decoded;
    }

    String errorMessage = 'Error en el servidor ($statusCode)';
    String? errorCode;

    if (decoded is Map<String, dynamic>) {
      if (decoded.containsKey('message')) {
        errorMessage = decoded['message'].toString();
      } else if (decoded.containsKey('mensaje')) {
        errorMessage = decoded['mensaje'].toString();
      } else if (decoded.containsKey('error')) {
        errorMessage = decoded['error'].toString();
      } else if (decoded.containsKey('detail')) {
        errorMessage = decoded['detail'].toString();
      }
      errorCode = decoded['code']?.toString() ?? decoded['error']?.toString();
    }

    if (statusCode == 401) {
      errorMessage = 'Sesión expirada o credenciales incorrectas.';
    } else if (statusCode == 403) {
      errorMessage = 'No tienes permiso para realizar esta acción.';
    } else if (statusCode == 404) {
      errorMessage = 'El recurso solicitado no fue encontrado.';
    } else if (statusCode == 409) {
      final lower = errorMessage.toLowerCase();
      if (lower.contains('aport') ||
          lower.contains('period') ||
          lower.contains('períod') ||
          (response.request?.url.path.contains('aportaciones') ?? false)) {
        errorMessage = 'Ya registraste una aportación para este período.';
      } else if (lower.contains('voto') ||
          (response.request?.url.path.contains('voto') ?? false)) {
        errorMessage = 'Ya has participado en este proceso de votación.';
      } else {
        errorMessage = 'El registro ya existe o entra en conflicto con un registro previo.';
      }
    }

    throw ApiException(
      statusCode: statusCode,
      message: errorMessage,
      errorCode: errorCode,
    );
  }
}
