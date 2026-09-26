import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:asociacion_comunal_app/data/models/meeting_model.dart';
import 'package:asociacion_comunal_app/data/models/payment_model.dart';
import 'package:asociacion_comunal_app/data/models/project_model.dart';
import 'package:asociacion_comunal_app/data/models/voting_model.dart';
import 'package:asociacion_comunal_app/data/repositories/notifications_repository.dart';
import 'package:asociacion_comunal_app/data/repositories/payments_repository.dart';
import 'package:asociacion_comunal_app/data/services/api_client.dart';
import 'package:asociacion_comunal_app/data/services/session_storage_service.dart';
import 'package:asociacion_comunal_app/viewmodels/payments_viewmodel.dart';
import 'package:asociacion_comunal_app/views/payments/payment_receipt_dialog.dart';
import 'package:asociacion_comunal_app/views/payments/simulated_payment_sheet.dart';

import 'critical_rules_test.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  SharedPreferences.setMockInitialValues({});

  group('Resident Features - NotificationsRepository', () {
    late NotificationsRepository repository;

    setUp(() {
      repository = NotificationsRepository();
    });

    test('Genera notificación de cuota mensual si no ha pagado el mes en curso', () {
      final now = DateTime.now();
      final currentMonth = '${now.year}-${now.month.toString().padLeft(2, '0')}';

      final notifications = repository.buildNotifications(
        payments: [],
        polls: [],
        meetings: [],
        projects: [],
        currentMemberId: 10,
      );

      final feeNotif = notifications.firstWhere((n) => n.type == 'PAYMENT');
      expect(feeNotif.title, contains('Cuota Mensual'));
      expect(feeNotif.message, contains(currentMonth));
      expect(feeNotif.targetTabIndex, equals(1)); // Pestaña de Pagos
      expect(feeNotif.isRead, isFalse);
    });

    test('No genera notificación de cuota si ya está pagada en el mes actual', () {
      final now = DateTime.now();
      final currentMonth = '${now.year}-${now.month.toString().padLeft(2, '0')}';

      final paidFee = PaymentModel(
        id: 1,
        idMiembro: 10,
        nombreMiembro: 'Carlos Martínez',
        duiMiembro: '01234567-8',
        monto: 10.0,
        periodoMes: currentMonth,
        fechaPago: now.toIso8601String(),
        metodoPago: 'TARJETA_CREDITO',
        estado: 'PAGADA',
        idProyecto: null,
      );

      final notifications = repository.buildNotifications(
        payments: [paidFee],
        polls: [],
        meetings: [],
        projects: [],
        currentMemberId: 10,
      );

      final hasFeeNotif = notifications.any((n) => n.type == 'PAYMENT');
      expect(hasFeeNotif, isFalse);
    });

    test('Genera notificaciones de votación activa y reunión con proyecto vinculado', () {
      final poll = VotingModel(
        id: 5,
        titulo: 'Alumbrado Led',
        descripcion: 'Votación para alumbrado',
        fechaInicio: DateTime.now().toIso8601String(),
        fechaFin: DateTime.now().add(const Duration(days: 2)).toIso8601String(),
        estado: 'ABIERTA',
        totalOpciones: 2,
        totalVotos: 0,
        opciones: [],
      );

      final meeting = MeetingModel(
        id: 12,
        titulo: 'Asamblea Extraordinaria',
        fechaHora: DateTime.now().add(const Duration(days: 3)).toIso8601String(),
        lugar: 'Cancha Comunal',
        tipo: 'EXTRAORDINARIA',
        estado: 'PROGRAMADA',
        totalConvocados: 120,
        totalAsistentes: 0,
        porcentajeAsistencia: 0.0,
        idProyecto: 3,
        nombreProyecto: 'Parque Infantil',
        descripcion: 'Discusión de avance',
      );

      final notifications = repository.buildNotifications(
        payments: [],
        polls: [poll],
        meetings: [meeting],
        projects: [],
        currentMemberId: 10,
      );

      final voteNotif = notifications.firstWhere((n) => n.type == 'VOTE');
      expect(voteNotif.targetTabIndex, equals(3));
      expect(voteNotif.message, contains('Alumbrado Led'));

      final meetingNotif = notifications.firstWhere((n) => n.type == 'MEETING');
      expect(meetingNotif.targetTabIndex, equals(2));
      expect(meetingNotif.message, contains('Parque Infantil'));
    });

    test('Marcado de lectura y conteo de no leídos funciona correctamente', () {
      final notifications = repository.buildNotifications(
        payments: [],
        polls: [],
        meetings: [],
        projects: [],
        currentMemberId: 10,
      );

      expect(repository.countUnread(notifications), equals(notifications.length));

      final first = notifications.first;
      repository.markAsRead(first.id);

      final updated = repository.buildNotifications(
        payments: [],
        polls: [],
        meetings: [],
        projects: [],
        currentMemberId: 10,
      );

      expect(repository.countUnread(updated), equals(notifications.length - 1));
    });
  });

  group('Resident Features - ProjectModel & MeetingModel Data Contract', () {
    test('ProjectModel deserializa desglose financiero y vinculación de votación', () {
      final json = {
        'id': 7,
        'nombre': 'Pavimentación Pasaje 3',
        'descripcion': 'Mejora de calles',
        'presupuesto': 12000.0,
        'estado': 'EN_EJECUCION',
        'montoRecaudado': 7200.0,
        'montoPendiente': 4800.0,
        'progresoPorcentaje': 60.0,
        'idVotacion': 2,
        'tituloVotacion': 'Consulta Vial 2026',
        'totalAportantes': 45,
        'aportePorMiembro': 150.0,
      };

      final project = ProjectModel.fromJson(json);

      expect(project.id, equals(7));
      expect(project.montoRecaudado, equals(7200.0));
      expect(project.montoPendiente, equals(4800.0));
      expect(project.progresoPorcentaje, equals(60.0));
      expect(project.hasVoting, isTrue);
      expect(project.tituloVotacion, equals('Consulta Vial 2026'));
      expect(project.totalAportantes, equals(45));
      expect(project.aportePorMiembro, equals(150.0));
      expect(project.canReceiveContributions, isTrue);
    });

    test('MeetingModel deserializa proyecto vinculado y descripción', () {
      final json = {
        'id': 15,
        'titulo': 'Reunión de Avance',
        'descripcion': 'Revisión con el contratista',
        'fechaHora': '2026-10-15T18:00:00',
        'lugar': 'Casa Comunal',
        'tipo': 'ORDINARIA',
        'estado': 'CONVOCADA',
        'idProyecto': 7,
        'nombreProyecto': 'Pavimentación Pasaje 3',
        'totalConvocados': 120,
        'totalAsistentes': 60,
        'porcentajeAsistencia': 50.0,
      };

      final meeting = MeetingModel.fromJson(json);

      expect(meeting.id, equals(15));
      expect(meeting.idProyecto, equals(7));
      expect(meeting.nombreProyecto, equals('Pavimentación Pasaje 3'));
      expect(meeting.hasLinkedProject, isTrue);
      expect(meeting.descripcion, equals('Revisión con el contratista'));
    });

    test('PaymentModel distingue correctamente cuota mensual de aporte a proyecto', () {
      const cuota = PaymentModel(
        id: 101,
        idMiembro: 5,
        nombreMiembro: 'Carlos Martínez',
        duiMiembro: '01234567-8',
        monto: 10.0,
        periodoMes: '2026-09',
        fechaPago: '2026-09-24T10:00:00',
        metodoPago: 'TARJETA_CREDITO',
        estado: 'PAGADA',
        idProyecto: null,
      );

      const aporte = PaymentModel(
        id: 102,
        idMiembro: 5,
        nombreMiembro: 'Carlos Martínez',
        duiMiembro: '01234567-8',
        monto: 50.0,
        periodoMes: '2026-09',
        fechaPago: '2026-09-24T11:00:00',
        metodoPago: 'TRANSFERENCIA',
        estado: 'PAGADA',
        idProyecto: 4,
        nombreProyecto: 'Seguridad y Cámaras',
      );

      expect(cuota.isMonthlyFee, isTrue);
      expect(cuota.isProjectContribution, isFalse);
      expect(cuota.tipoEtiqueta, equals('Cuota Mensual'));

      expect(aporte.isMonthlyFee, isFalse);
      expect(aporte.isProjectContribution, isTrue);
      expect(aporte.tipoEtiqueta, equals('Aporte a Proyecto'));
    });
  });

  group('Resident Features - PaymentsRepository Integration', () {
    test('processSimulatedPayment envía payload completo a /api/aportaciones', () async {
      late Map<String, dynamic> capturedBody;

      final mockClient = MockClient((request) async {
        if (request.url.path == '/api/aportaciones' && request.method == 'POST') {
          capturedBody = jsonDecode(request.body) as Map<String, dynamic>;
          return http.Response(
            jsonEncode({
              'id': 888,
              'idMiembro': capturedBody['idMiembro'],
              'nombreMiembro': 'Carlos Martínez',
              'duiMiembro': '01234567-8',
              'idProyecto': capturedBody['idProyecto'],
              'nombreProyecto': capturedBody['idProyecto'] != null ? 'Luminarias' : null,
              'monto': capturedBody['monto'],
              'periodoMes': capturedBody['periodoMes'],
              'fechaPago': '2026-09-24T20:00:00',
              'estado': 'PAGADA',
              'metodoPago': capturedBody['metodoPago'],
              'referencia': capturedBody['referencia'],
            }),
            201,
            headers: {'content-type': 'application/json'},
          );
        }
        return http.Response('Not Found', 404);
      });

      final storage = FakeSecureStorage();
      await storage.write(key: 'asociacion_access_token', value: 'fake_jwt');
      final sessionStorage = await SessionStorageService.init(secureStorage: storage);
      final apiClient = ApiClient(storage: sessionStorage, client: mockClient);
      final repo = PaymentsRepository(api: apiClient);

      final result = await repo.processSimulatedPayment(
        idMiembro: 42,
        monto: 25.0,
        periodoMes: '2026-09',
        metodoSeleccionado: 'TARJETA_CREDITO',
        idProyecto: 8,
      );

      expect(capturedBody['idMiembro'], equals(42));
      expect(capturedBody['monto'], equals(25.0));
      expect(capturedBody['periodoMes'], equals('2026-09'));
      expect(capturedBody['idProyecto'], equals(8));
      expect(capturedBody['metodoPago'], equals('TRANSFERENCIA'));
      expect(capturedBody['referencia'], isNotNull);

      expect(result.id, equals(888));
      expect(result.isProjectContribution, isTrue);
    });
  });

  group('Resident Features - UI Components', () {
    testWidgets('PaymentReceiptDialog muestra datos completos de comprobante', (tester) async {
      const payment = PaymentModel(
        id: 777,
        idMiembro: 12,
        nombreMiembro: 'Carlos Martínez',
        duiMiembro: '01234567-8',
        monto: 10.0,
        periodoMes: '2026-09',
        fechaPago: '2026-09-24T14:30:00',
        metodoPago: 'TARJETA_CREDITO',
        estado: 'PAGADA',
        idProyecto: null,
      );

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: PaymentReceiptDialog(
              payment: payment,
              methodName: 'Visa •••• 4242',
              conceptTitle: 'Cuota Mensual Comunal',
            ),
          ),
        ),
      );

      expect(find.text('Comprobante Electrónico'), findsOneWidget);
      expect(find.text('Cuota de Vigilancia Mensual'), findsOneWidget);
      expect(find.text('\$10.00'), findsOneWidget);
      expect(find.text('Visa •••• 4242'), findsOneWidget);
      expect(find.text('Compartir'), findsOneWidget);
      expect(find.text('Listo'), findsOneWidget);
    });

    testWidgets('SimulatedPaymentSheet renderiza opciones de pago Visa, Mastercard y Google Pay', (tester) async {
      final mockClient = MockClient((request) async => http.Response('{}', 200));
      final storage = FakeSecureStorage();
      final sessionStorage = await SessionStorageService.init(secureStorage: storage);
      final apiClient = ApiClient(storage: sessionStorage, client: mockClient);
      final repo = PaymentsRepository(api: apiClient);
      final vm = PaymentsViewModel(paymentsRepository: repo);

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SimulatedPaymentSheet(
              viewModel: vm,
              idMiembro: 10,
              title: 'Pagar Cuota Mensual',
              description: 'Cuota correspondiente al período 2026-09.',
              defaultAmount: 10.0,
              periodoMes: '2026-09',
            ),
          ),
        ),
      );

      expect(find.text('Pagar Cuota Mensual'), findsAtLeastNWidgets(1));
      expect(find.text('Visa •••• 4242'), findsAtLeastNWidgets(1));
      expect(find.text('Mastercard •••• 8888'), findsOneWidget);
      expect(find.text('Google Pay'), findsOneWidget);
      expect(find.text('+ Otra Tarjeta'), findsOneWidget);
      expect(find.text('Pagar \$10.00'), findsOneWidget);
    });

    testWidgets('SimulatedPaymentSheet muestra alerta no bloqueante si ya aportó a proyecto y permite confirmar aporte adicional', (tester) async {
      final mockClient = MockClient((request) async => http.Response('{}', 200));
      final storage = FakeSecureStorage();
      final sessionStorage = await SessionStorageService.init(secureStorage: storage);
      final apiClient = ApiClient(storage: sessionStorage, client: mockClient);
      final repo = PaymentsRepository(api: apiClient);
      final vm = PaymentsViewModel(paymentsRepository: repo);

      // Simular que el usuario ya realizó un aporte previo de $30.00 al proyecto 5
      vm.payments.add(
        const PaymentModel(
          id: 101,
          idMiembro: 10,
          nombreMiembro: 'Carlos',
          duiMiembro: '00000000-0',
          idProyecto: 5,
          nombreProyecto: 'Cámaras de Seguridad',
          periodoMes: '2026-09',
          monto: 30.0,
          fechaPago: '2026-09-20',
          metodoPago: 'TARJETA_CREDITO',
          estado: 'REGISTRADA',
        ),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SimulatedPaymentSheet(
              viewModel: vm,
              idMiembro: 10,
              title: 'Aporte a Cámaras de Seguridad',
              description: 'Contribución solidaria para financiamiento comunal.',
              defaultAmount: 25.0,
              isAmountEditable: true,
              periodoMes: '2026-09',
              idProyecto: 5,
              nombreProyecto: 'Cámaras de Seguridad',
            ),
          ),
        ),
      );

      // Debe mostrar la alerta informativa de aportación previa acumulada
      expect(find.text('Aviso de Aportación Adicional'), findsOneWidget);
      expect(find.textContaining('1 aporte previo por un total acumulado de \$30.00'), findsOneWidget);
      // El botón debe permitir confirmar el aporte adicional sin bloquear
      expect(find.text('Confirmar aporte adicional (\$25.00)'), findsOneWidget);
    });

    testWidgets('SimulatedPaymentSheet bloquea aportación principal si la cuota del período ya fue saldada', (tester) async {
      final mockClient = MockClient((request) async => http.Response('{}', 200));
      final storage = FakeSecureStorage();
      final sessionStorage = await SessionStorageService.init(secureStorage: storage);
      final apiClient = ApiClient(storage: sessionStorage, client: mockClient);
      final repo = PaymentsRepository(api: apiClient);
      final vm = PaymentsViewModel(paymentsRepository: repo);

      // Simular cuota mensual ordinaria ya pagada para 2026-09
      vm.payments.add(
        const PaymentModel(
          id: 102,
          idMiembro: 10,
          nombreMiembro: 'Carlos',
          duiMiembro: '00000000-0',
          idProyecto: null,
          nombreProyecto: null,
          periodoMes: '2026-09',
          monto: 15.0,
          fechaPago: '2026-09-05',
          metodoPago: 'TARJETA_CREDITO',
          estado: 'REGISTRADA',
        ),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SimulatedPaymentSheet(
              viewModel: vm,
              idMiembro: 10,
              title: 'Cuota de Mantenimiento',
              description: 'Período 2026-09.',
              defaultAmount: 15.0,
              periodoMes: '2026-09',
              idProyecto: null,
            ),
          ),
        ),
      );

      // Debe mostrar el banner de cuota saldada y botón bloqueado
      expect(find.text('Cuota mensual ya saldada'), findsOneWidget);
      expect(find.text('Cuota del período ya liquidada'), findsOneWidget);
    });
  });
}

