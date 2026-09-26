import 'dart:async';
import 'package:flutter/services.dart';

class DeepLinkCredentials {
  final String username;
  final String password;

  const DeepLinkCredentials({
    required this.username,
    required this.password,
  });

  @override
  String toString() => 'DeepLinkCredentials(username: $username)';
}

class DeepLinkService {
  static const MethodChannel _channel = MethodChannel('sv.asociacion.comunal/deeplink');
  static final StreamController<DeepLinkCredentials> _controller =
      StreamController<DeepLinkCredentials>.broadcast();
  static DeepLinkCredentials? _latestCredentials;

  static Stream<DeepLinkCredentials> get credentialsStream => _controller.stream;
  static DeepLinkCredentials? get latestCredentials => _latestCredentials;

  static void initialize() {
    _channel.setMethodCallHandler((call) async {
      if (call.method == 'onDeepLink') {
        final link = call.arguments as String?;
        if (link != null) {
          _processLink(link);
        }
      }
    });

    // Revisar initial link (cold start desde cámara/scanner)
    _channel.invokeMethod<String>('getInitialLink').then((link) {
      if (link != null && link.isNotEmpty) {
        _processLink(link);
      }
    }).catchError((_) {});
  }

  static void _processLink(String link) {
    try {
      final uri = Uri.parse(link);
      String? username = uri.queryParameters['username'] ?? uri.queryParameters['u'];
      String? password = uri.queryParameters['password'] ?? uri.queryParameters['p'];

      if (username != null && username.isNotEmpty) {
        final creds = DeepLinkCredentials(
          username: username.trim(),
          password: password ?? '',
        );
        _latestCredentials = creds;
        _controller.add(creds);
      }
    } catch (_) {}
  }
}
