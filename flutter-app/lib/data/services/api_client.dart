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

/// Cliente HTTP principal para la comunicación con el backend Javalin.
class ApiClient {
  final http.Client _client;
  final SessionStorageService _storage;

  ApiClient({
    http.Client? client,
    required SessionStorageService storage,
  })  : _client = client ?? http.Client(),
        _storage = storage;

  String get _activeBaseUrl {
    final custom = _storage.customBaseUrl;
    if (custom != null && custom.isNotEmpty) {
      return custom.endsWith('/') ? custom.substring(0, custom.length - 1) : custom;
    }
    return ApiConstants.baseUrl.endsWith('/')
        ? ApiConstants.baseUrl.substring(0, ApiConstants.baseUrl.length - 1)
        : ApiConstants.baseUrl;
  }

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
    final uri = Uri.parse('$_activeBaseUrl$endpoint')
        .replace(queryParameters: queryParams);
    return _sendRequest(() => _client.get(
          uri,
          headers: _buildHeaders(requiresAuth: requiresAuth),
        ));
  }

  Future<dynamic> post(
    String endpoint, {
    Map<String, dynamic>? body,
    String? token,
    bool requiresAuth = true,
  }) async {
    final uri = Uri.parse('$_activeBaseUrl$endpoint');
    return _sendRequest(() => _client.post(
          uri,
          headers: _buildHeaders(token: token, requiresAuth: requiresAuth),
          body: body != null ? jsonEncode(body) : null,
        ));
  }

  Future<dynamic> put(
    String endpoint, {
    Map<String, dynamic>? body,
    bool requiresAuth = true,
  }) async {
    final uri = Uri.parse('$_activeBaseUrl$endpoint');
    return _sendRequest(() => _client.put(
          uri,
          headers: _buildHeaders(requiresAuth: requiresAuth),
          body: body != null ? jsonEncode(body) : null,
        ));
  }

  String get activeBaseUrl => _activeBaseUrl;

  /// Verifica si el servidor está respondiendo adecuadamente en el endpoint /health.
  Future<Map<String, dynamic>> checkHealth([String? testUrl]) async {
    final rawUrl = (testUrl != null && testUrl.trim().isNotEmpty)
        ? testUrl.trim()
        : _activeBaseUrl;
    final cleanUrl = rawUrl.endsWith('/') ? rawUrl.substring(0, rawUrl.length - 1) : rawUrl;

    try {
      final uri = Uri.parse('$cleanUrl/health');
      final response = await _client.get(uri).timeout(const Duration(seconds: 4));
      if (response.statusCode >= 200 && response.statusCode < 300) {
        return {'ok': true, 'message': 'Conexión exitosa con la API'};
      }
      return {
        'ok': false,
        'message': 'El servidor respondió con código HTTP ${response.statusCode}',
      };
    } on SocketException {
      if (cleanUrl.contains('10.0.2.2')) {
        return {
          'ok': false,
          'message': '10.0.2.2 solo funciona en emulador. En tu celular real usa la IP de tu PC o adb reverse.',
        };
      }
      return {
        'ok': false,
        'message': 'No se pudo conectar a $cleanUrl. Verifica que el servidor esté iniciado.',
      };
    } on TimeoutException {
      return {
        'ok': false,
        'message': 'Tiempo de espera agotado al conectar a $cleanUrl.',
      };
    } catch (e) {
      return {
        'ok': false,
        'message': 'Error al conectar: $e',
      };
    }
  }

  Future<dynamic> _sendRequest(Future<http.Response> Function() requestFn) async {
    try {
      final response = await requestFn().timeout(ApiConstants.connectionTimeout);
      return _handleResponse(response);
    } on SocketException {
      final url = _activeBaseUrl;
      String hint;
      if (url.contains('10.0.2.2')) {
        hint = 'No se pudo conectar con 10.0.2.2:8080.\n'
            'En un celular físico real debes configurar la IP de tu PC (ej: http://192.168.0.2:8080) '
            'o usar http://localhost:8080 con "adb reverse". Toca abajo en "Servidor" para configurarlo.';
      } else {
        hint = 'No se pudo conectar con el servidor ($url).\n'
            'Verifica que la API esté encendida en la PC y ambos dispositivos estén en la misma red.';
      }
      throw ApiException(
        statusCode: 0,
        message: hint,
      );
    } on TimeoutException {
      throw const ApiException(
        statusCode: 408,
        message: 'El servidor tardó demasiado en responder.',
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
      } else if (decoded.containsKey('error')) {
        errorMessage = decoded['error'].toString();
      }
      errorCode = decoded['code']?.toString() ?? decoded['error']?.toString();
    }

    if (statusCode == 401) {
      errorMessage = 'Sesión expirada o credenciales incorrectas.';
    } else if (statusCode == 403) {
      errorMessage = 'No tienes permiso para realizar esta acción.';
    } else if (statusCode == 404) {
      errorMessage = 'El recurso solicitado no fue encontrado.';
    }

    throw ApiException(
      statusCode: statusCode,
      message: errorMessage,
      errorCode: errorCode,
    );
  }
}
