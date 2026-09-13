import '../../core/constants/api_constants.dart';
import '../models/payment_model.dart';
import '../services/api_client.dart';

/// Repositorio para la consulta y registro de cuotas y aportaciones.
class PaymentsRepository {
  final ApiClient _api;

  PaymentsRepository({required ApiClient api}) : _api = api;

  /// Obtiene el listado de aportaciones del miembro conectado.
  Future<List<PaymentModel>> getMyPayments({int? idMiembro}) async {
    try {
      final queryParams = <String, String>{
        'page': '1',
        'size': '50',
      };
      if (idMiembro != null) {
        queryParams['idMiembro'] = idMiembro.toString();
      }

      final response = await _api.get(
        ApiConstants.aportacionesEndpoint,
        queryParams: queryParams,
      );

      if (response is Map<String, dynamic> && response['items'] is List) {
        final items = response['items'] as List;
        return items
            .map((item) => PaymentModel.fromJson(item as Map<String, dynamic>))
            .toList();
      } else if (response is List) {
        return response
            .map((item) => PaymentModel.fromJson(item as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (_) {
      // Retorna lista vacía si la base aún no tiene registros o no hay conexión directa
      return [];
    }
  }

  /// Calcula el total aportado por el residente.
  double calculateTotalPaid(List<PaymentModel> payments) {
    return payments
        .where((p) => p.isPaid)
        .fold(0.0, (sum, p) => sum + p.monto);
  }

  /// Registra una nueva aportación o pago mensual de vigilancia desde el celular.
  Future<PaymentModel> createPayment({
    required int idMiembro,
    required String periodoMes,
    required double monto,
    required String metodoPago,
    String? referencia,
    int? idProyecto,
  }) async {
    final response = await _api.post(
      ApiConstants.aportacionesEndpoint,
      body: {
        'idMiembro': idMiembro,
        'periodoMes': periodoMes,
        'monto': monto,
        'fechaPago': DateTime.now().toIso8601String().substring(0, 10),
        'metodoPago': metodoPago,
        'referencia': referencia ?? 'Pago móvil - Vigilancia mensual',
        if (idProyecto != null) 'idProyecto': idProyecto,
      },
    );

    if (response is Map<String, dynamic>) {
      return PaymentModel.fromJson(response);
    }
    throw Exception('Error al registrar el pago en el servidor.');
  }
}
