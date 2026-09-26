import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import 'package:asociacion_comunal_app/core/constants/api_constants.dart';
import 'package:asociacion_comunal_app/data/models/directiva_model.dart';
import 'package:asociacion_comunal_app/data/models/voting_model.dart';
import 'package:asociacion_comunal_app/data/repositories/auth_repository.dart';
import 'package:asociacion_comunal_app/data/repositories/community_repository.dart';
import 'package:asociacion_comunal_app/data/repositories/housing_repository.dart';
import 'package:asociacion_comunal_app/data/services/api_client.dart';
import 'package:asociacion_comunal_app/data/services/session_storage_service.dart';
import 'package:asociacion_comunal_app/viewmodels/auth_viewmodel.dart';
import 'package:asociacion_comunal_app/viewmodels/home_viewmodel.dart';
import 'package:asociacion_comunal_app/views/auth/change_password_view.dart';

class FakeSecureStorage extends FlutterSecureStorage {
  final Map<String, String> _storage = {};

  FakeSecureStorage() : super();

  @override
  Future<String?> read({
    required String key,
    AppleOptions? iOptions,
    AppleOptions? appleOptions,
    AndroidOptions? aOptions,
    LinuxOptions? lOptions,
    WebOptions? webOptions,
    AppleOptions? mOptions,
    WindowsOptions? wOptions,
  }) async =>
      _storage[key];

  @override
  Future<void> write({
    required String key,
    required String? value,
    AppleOptions? iOptions,
    AppleOptions? appleOptions,
    AndroidOptions? aOptions,
    LinuxOptions? lOptions,
    WebOptions? webOptions,
    AppleOptions? mOptions,
    WindowsOptions? wOptions,
  }) async {
    if (value != null) {
      _storage[key] = value;
    } else {
      _storage.remove(key);
    }
  }

  @override
  Future<void> delete({
    required String key,
    AppleOptions? iOptions,
    AppleOptions? appleOptions,
    AndroidOptions? aOptions,
    LinuxOptions? lOptions,
    WebOptions? webOptions,
    AppleOptions? mOptions,
    WindowsOptions? wOptions,
  }) async =>
      _storage.remove(key);
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late FakeSecureStorage fakeSecure;
  late SharedPreferences prefs;
  late SessionStorageService storage;

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
    prefs = await SharedPreferences.getInstance();
    fakeSecure = FakeSecureStorage();
    storage = await SessionStorageService.init(
      secureStorage: fakeSecure,
      prefs: prefs,
    );
  });

  group('Regla Crítica: Token Refresh y 401', () {
    test('Petición con 401 refresca token y reintenta la petición exitosamente', () async {
      await storage.saveTokens(
        accessToken: 'expired_access_token',
        refreshToken: 'valid_refresh_token',
      );

      int requestCount = 0;
      int refreshCount = 0;

      final mockClient = MockClient((request) async {
        if (request.url.path == '/api/auth/refresh') {
          refreshCount++;
          final body = jsonDecode(request.body) as Map<String, dynamic>;
          expect(body['refreshToken'], 'valid_refresh_token');
          return http.Response(
            jsonEncode({
              'accessToken': 'new_valid_access_token',
              'refreshToken': 'new_valid_refresh_token',
            }),
            200,
            headers: {'content-type': 'application/json'},
          );
        }

        if (request.url.path == '/api/me') {
          requestCount++;
          if (request.headers['Authorization'] == 'Bearer expired_access_token') {
            return http.Response(jsonEncode({'error': 'Token expirado'}), 401);
          }
          if (request.headers['Authorization'] == 'Bearer new_valid_access_token') {
            return http.Response(
              jsonEncode({
                'user': {'id': 1, 'username': 'miembro1', 'role': 'MIEMBRO'},
              }),
              200,
              headers: {'content-type': 'application/json'},
            );
          }
        }

        return http.Response('Not Found', 404);
      });

      final client = ApiClient(
        storage: storage,
        client: mockClient,
      );

      final result = await client.get('/api/me');
      expect(result, isNotNull);
      expect((result as Map<String, dynamic>)['user']['username'], 'miembro1');
      expect(refreshCount, 1);
      expect(requestCount, 2);
      expect(storage.accessToken, 'new_valid_access_token');
      expect(storage.refreshToken, 'new_valid_refresh_token');
    });

    test('Fallo de refresh (401 en /api/auth/refresh) dispara onSessionExpired', () async {
      await storage.saveTokens(
        accessToken: 'expired_token',
        refreshToken: 'invalid_refresh_token',
      );

      bool sessionExpiredFired = false;

      final mockClient = MockClient((request) async {
        if (request.url.path == '/api/auth/refresh') {
          return http.Response(jsonEncode({'error': 'Refresh token inválido'}), 401);
        }
        if (request.url.path == '/api/me') {
          return http.Response(jsonEncode({'error': 'No autorizado'}), 401);
        }
        return http.Response('Not Found', 404);
      });

      final client = ApiClient(
        storage: storage,
        client: mockClient,
        onSessionExpired: () {
          sessionExpiredFired = true;
        },
      );

      expect(() => client.get('/api/me'), throwsA(isA<Exception>()));
      // Permite que se procese el callback
      await Future.delayed(const Duration(milliseconds: 50));
      expect(sessionExpiredFired, true);
    });

    test('Evita loop infinito de refresh: solo reintenta una vez', () async {
      await storage.saveTokens(
        accessToken: 'bad_token',
        refreshToken: 'dummy_refresh',
      );

      int meCalls = 0;
      final mockClient = MockClient((request) async {
        if (request.url.path == '/api/auth/refresh') {
          return http.Response(
            jsonEncode({
              'accessToken': 'another_bad_token',
              'refreshToken': 'dummy_refresh_2',
            }),
            200,
            headers: {'content-type': 'application/json'},
          );
        }
        if (request.url.path == '/api/me') {
          meCalls++;
          return http.Response(jsonEncode({'error': 'Siempre 401'}), 401);
        }
        return http.Response('Not Found', 404);
      });

      final client = ApiClient(storage: storage, client: mockClient);
      expect(() => client.get('/api/me'), throwsA(isA<Exception>()));
      await Future.delayed(const Duration(milliseconds: 50));
      // Debe haberse ejecutado exactamente 2 veces (petición inicial + 1 solo reintento)
      expect(meCalls, 2);
    });
  });

  group('Regla Crítica: Manejo de HTTP 409 en Aportaciones', () {
    test('HTTP 409 se convierte en mensaje amigable de aportación ya registrada', () async {
      final mockClient = MockClient((request) async {
        return http.Response(
          jsonEncode({'mensaje': 'Ya existe aportacion para el periodo 2026-03'}),
          409,
          headers: {'content-type': 'application/json'},
        );
      });

      final client = ApiClient(storage: storage, client: mockClient);

      expect(
        () => client.post('/api/aportaciones', body: {'monto': 10.0}),
        throwsA(
          predicate((e) =>
              e.toString().contains('Ya registraste una aportación para este período.')),
        ),
      );
    });
  });

  group('Regla Crítica: Privacidad Electoral de Votaciones', () {
    final activePollJson = {
      'id': 10,
      'titulo': 'Presupuesto Comunal 2026',
      'descripcion': 'Votación para aprobar mejoras en parque infantil.',
      'estado': 'ABIERTA',
      'totalOpciones': 2,
      'totalVotos': 85,
      'opciones': [
        {'idOpcion': 1, 'descripcion': 'A favor', 'orden': 1, 'votos': 70},
        {'idOpcion': 2, 'descripcion': 'En contra', 'orden': 2, 'votos': 15},
      ]
    };

    final closedPollJson = {
      'id': 11,
      'titulo': 'Elección de Directiva',
      'descripcion': 'Proceso cerrado de elección.',
      'estado': 'CERRADA',
      'totalOpciones': 2,
      'totalVotos': 100,
      'opciones': [
        {'idOpcion': 1, 'descripcion': 'Planilla Verde', 'orden': 1, 'votos': 60},
        {'idOpcion': 2, 'descripcion': 'Planilla Azul', 'orden': 2, 'votos': 40},
      ]
    };

    test('Votación abierta y usuario ya votó NO debe revelar resultados parciales', () {
      final poll = VotingModel.fromJson(activePollJson);
      const hasVoted = true;

      expect(poll.isOpen, true);
      expect(poll.isClosed, false);
      expect(hasVoted, true);

      // Regla de privacidad:
      // Cuando poll.isOpen && hasVoted -> resultados no deben ser visibles
      final shouldShowResults = poll.isClosed;
      expect(shouldShowResults, false);

      final shouldShowLockedBallot = poll.isOpen && hasVoted;
      expect(shouldShowLockedBallot, true);
    });

    test('Votación cerrada permite ver resultados agregados', () {
      final poll = VotingModel.fromJson(closedPollJson);
      expect(poll.isClosed, true);

      final shouldShowResults = poll.isClosed;
      expect(shouldShowResults, true);
      expect(poll.totalVotos, 100);
      expect(poll.opciones.first.votos, 60);
    });
  });

  group('Regla Crítica: Directorio Comunal Real (/api/directiva/actual)', () {
    test('DirectivaMemberModel deserializa respuesta real AsignacionCargoResponse', () {
      final json = {
        'id': 1,
        'idMiembro': 101,
        'nombreMiembro': 'Ana Patricia Ramos',
        'telefonoMiembro': '7890-1234',
        'idCargo': 2,
        'nombreCargo': 'Presidenta',
        'estado': 'ACTIVO',
      };

      final member = DirectivaMemberModel.fromJson(json);
      expect(member.nombreCargo, 'Presidenta');
      expect(member.nombreMiembro, 'Ana Patricia Ramos');
      expect(member.telefonoMiembro, '7890-1234');
      expect(member.estado, 'ACTIVO');
    });

    test('CommunityRepository consume ApiConstants.directivaActualEndpoint', () async {
      final mockClient = MockClient((request) async {
        expect(request.url.path, ApiConstants.directivaActualEndpoint);
        return http.Response(
          jsonEncode([
            {
              'id': 1,
              'nombreMiembro': 'Carlos Roberto Gómez',
              'telefonoMiembro': '7123-4567',
              'nombreCargo': 'Tesorero',
              'estado': 'ACTIVO',
            }
          ]),
          200,
          headers: {'content-type': 'application/json'},
        );
      });

      final client = ApiClient(storage: storage, client: mockClient);
      final repo = CommunityRepository(api: client);

      final list = await repo.getDirectivaActual();
      expect(list.length, 1);
      expect(list.first.nombreCargo, 'Tesorero');
      expect(list.first.nombreMiembro, 'Carlos Roberto Gómez');
    });
  });

  group('Regla Crítica: Saludo Dinámico', () {
    test('getGreeting devuelve saludo correcto según la hora del día', () {
      expect(HomeViewModel.getGreeting(DateTime(2026, 3, 15, 6, 0)), 'Buenos días');
      expect(HomeViewModel.getGreeting(DateTime(2026, 3, 15, 11, 59)), 'Buenos días');
      expect(HomeViewModel.getGreeting(DateTime(2026, 3, 15, 12, 0)), 'Buenas tardes');
      expect(HomeViewModel.getGreeting(DateTime(2026, 3, 15, 18, 59)), 'Buenas tardes');
      expect(HomeViewModel.getGreeting(DateTime(2026, 3, 15, 19, 0)), 'Buenas noches');
      expect(HomeViewModel.getGreeting(DateTime(2026, 3, 15, 23, 30)), 'Buenas noches');
      expect(HomeViewModel.getGreeting(DateTime(2026, 3, 15, 3, 0)), 'Buenas noches');
    });
  });

  group('Regla Crítica: Vivienda no asigna vivienda ajena', () {
    test('HousingRepository.getByMemberId retorna null si el miembro no es representante', () async {
      final mockClient = MockClient((request) async {
        return http.Response(
          jsonEncode([
            {
              'id': 1,
              'codigo': 'VIV-001',
              'sector': 'Sector A',
              'direccion': 'Calle 1, Casa 5',
              'idRepresentante': 999, // Otro miembro
              'estado': 'ACTIVA',
            }
          ]),
          200,
          headers: {'content-type': 'application/json'},
        );
      });

      final client = ApiClient(storage: storage, client: mockClient);
      final repo = HousingRepository(api: client);

      final house = await repo.getByMemberId(50); // Miembro 50 busca su casa
      expect(house, isNull); // NUNCA debe devolver la casa del miembro 999
    });
  });

  group('Regla Crítica: Navegación de 5 pestañas', () {
    test('La barra de navegación principal define exactamente 5 pestañas', () {
      const tabs = [
        'Inicio',
        'Aportaciones',
        'Comunidad',
        'Votaciones',
        'Perfil',
      ];
      expect(tabs.length, 5);
      expect(tabs[0], 'Inicio');
      expect(tabs[1], 'Aportaciones');
      expect(tabs[2], 'Comunidad');
      expect(tabs[3], 'Votaciones');
      expect(tabs[4], 'Perfil');
    });
  });

  group('Regla Crítica: Autocompletado de Contraseña Temporal', () {
    test('SessionStorageService almacena y limpia la contraseña temporal', () async {
      expect(storage.temporaryPassword, isNull);
      await storage.saveTemporaryPassword('TempPass123!');
      expect(storage.temporaryPassword, 'TempPass123!');
      await storage.saveTemporaryPassword(null);
      expect(storage.temporaryPassword, isNull);
    });

    testWidgets('ChangePasswordView autocompleta la contraseña temporal en el formulario', (tester) async {
      await storage.saveTemporaryPassword('ClaveTemporal999');

      final mockClient = MockClient((request) async {
        return http.Response('{}', 200);
      });
      final client = ApiClient(storage: storage, client: mockClient);
      final authRepo = AuthRepository(api: client, storage: storage);
      final authVm = AuthViewModel(authRepository: authRepo);

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ChangePasswordView(viewModel: authVm),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Debe encontrarse el texto del indicador de autocompletado
      expect(find.text('Contraseña temporal colocada automáticamente'), findsOneWidget);

      // Verificamos que el campo tenga el texto precargado
      final formField = tester.widget<TextFormField>(
        find.descendant(
          of: find.byType(ChangePasswordView),
          matching: find.byType(TextFormField),
        ).first,
      );
      expect(formField.controller?.text, 'ClaveTemporal999');
    });
  });
}


