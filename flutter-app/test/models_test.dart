import 'package:flutter_test/flutter_test.dart';
import 'package:asociacion_comunal_app/data/models/auth_models.dart';
import 'package:asociacion_comunal_app/data/models/payment_model.dart';
import 'package:asociacion_comunal_app/data/models/meeting_model.dart';
import 'package:asociacion_comunal_app/data/models/project_model.dart';
import 'package:asociacion_comunal_app/data/models/voting_model.dart';
import 'package:asociacion_comunal_app/data/models/housing_model.dart';
import 'package:asociacion_comunal_app/core/utils/formatters.dart';

void main() {
  group('Pruebas de Modelos y Formateadores', () {
    test('UserDto y MemberDto deserializan correctamente', () {
      final userJson = {
        'id': 1,
        'username': 'residente1',
        'role': 'MIEMBRO',
        'passwordChangeRequired': false,
      };
      final user = UserDto.fromJson(userJson);
      expect(user.id, 1);
      expect(user.username, 'residente1');
      expect(user.role, 'MIEMBRO');
      expect(user.passwordChangeRequired, false);

      final memberJson = {
        'id': 10,
        'names': 'Carlos Roberto',
        'lastNames': 'Gómez Pérez',
        'status': 'ACTIVO',
      };
      final member = MemberDto.fromJson(memberJson);
      expect(member.id, 10);
      expect(member.fullName, 'Carlos Roberto Gómez Pérez');
      expect(member.firstName, 'Carlos');
      expect(member.status, 'ACTIVO');
    });

    test('PaymentModel calcula estado pagado y parsea campos', () {
      final paymentJson = {
        'idAportacion': 501,
        'idMiembro': 10,
        'nombreMiembro': 'Carlos Gómez',
        'duiMiembro': '12345678-9',
        'periodoMes': '2026-03',
        'monto': 25.50,
        'fechaPago': '2026-03-01',
        'metodoPago': 'TRANSFERENCIA',
        'estado': 'PAGADA',
      };
      final payment = PaymentModel.fromJson(paymentJson);
      expect(payment.id, 501);
      expect(payment.monto, 25.50);
      expect(payment.isPaid, true);
    });

    test('MeetingModel y ProjectModel deserializan correctamente', () {
      final meeting = MeetingModel.fromJson({
        'id': 1,
        'titulo': 'Asamblea General Ordinaria',
        'fechaHora': '2026-04-10T18:00:00',
        'lugar': 'Casa Comunal',
        'tipo': 'ORDINARIA',
        'estado': 'PROGRAMADA',
        'totalConvocados': 120,
        'totalAsistentes': 0,
        'porcentajeAsistencia': 0.0,
      });
      expect(meeting.isUpcoming, true);
      expect(meeting.titulo, 'Asamblea General Ordinaria');

      final project = ProjectModel.fromJson({
        'id': 2,
        'nombre': 'Alumbrado Solar Comunal',
        'descripcion': 'Instalación de luminarias solares en calle principal.',
        'presupuesto': 3500.00,
        'estado': 'EN_CURSO',
      });
      expect(project.id, 2);
      expect(project.presupuesto, 3500.00);
    });

    test('VotingModel y opciones deserializan correctamente', () {
      final votingJson = {
        'id': 5,
        'titulo': 'Consulta sobre Pavimentación',
        'descripcion': 'Aprobación del presupuesto para calle Los Almendros.',
        'estado': 'ABIERTA',
        'totalOpciones': 2,
        'totalVotos': 40,
        'opciones': [
          {'idOpcion': 101, 'descripcion': 'A favor', 'orden': 1, 'votos': 35},
          {'idOpcion': 102, 'descripcion': 'En contra', 'orden': 2, 'votos': 5},
        ]
      };
      final voting = VotingModel.fromJson(votingJson);
      expect(voting.id, 5);
      expect(voting.isOpen, true);
      expect(voting.opciones.length, 2);
      expect(voting.opciones.first.descripcion, 'A favor');
      expect(voting.opciones.first.votos, 35);
    });

    test('HousingModel deserializa y calcula censo', () {
      final housingJson = {
        'id': 14,
        'codigo': 'VIV-014',
        'sector': 'Sector Norte',
        'direccion': 'Polígono D, Lote 12',
        'referencia': 'Frente a parque comunal',
        'estado': 'ACTIVA',
        'adultos': 3,
        'menores': 2,
      };
      final housing = HousingModel.fromJson(housingJson);
      expect(housing.id, 14);
      expect(housing.codigo, 'VIV-014');
      expect(housing.totalHabitantes, 5);
      expect(housing.adultos, 3);
      expect(housing.menores, 2);
    });

    test('Formatters maneja moneda, iniciales y fechas', () {
      expect(Formatters.currency(860500.0), '\$860,500.00');
      expect(Formatters.currency(0.0), '\$0.00');
      expect(Formatters.currency(null), '\$0.00');

      expect(Formatters.initials('Carlos Gómez'), 'CG');
      expect(Formatters.initials('María'), 'M');
      expect(Formatters.initials(null), '?');
    });
  });
}
