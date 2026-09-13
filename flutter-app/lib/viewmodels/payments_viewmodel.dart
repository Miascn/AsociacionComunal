import 'package:flutter/material.dart';
import '../data/models/payment_model.dart';
import '../data/repositories/payments_repository.dart';

/// ViewModel para la gestión e historial de aportaciones del residente.
class PaymentsViewModel extends ChangeNotifier {
  final PaymentsRepository _paymentsRepository;

  bool _isLoading = false;
  List<PaymentModel> _payments = [];
  String _filter = 'TODOS'; // 'TODOS', 'PAGADOS', 'PENDIENTES'

  PaymentsViewModel({required PaymentsRepository paymentsRepository})
      : _paymentsRepository = paymentsRepository;

  bool get isLoading => _isLoading;
  String get filter => _filter;

  List<PaymentModel> get filteredPayments {
    if (_filter == 'PAGADOS') {
      return _payments.where((p) => p.isPaid).toList();
    } else if (_filter == 'PENDIENTES') {
      return _payments.where((p) => !p.isPaid).toList();
    }
    return _payments;
  }

  static const double cuotaVigilanciaMensual = 10.00;

  String get currentPeriod {
    final now = DateTime.now();
    return '${now.year}-${now.month.toString().padLeft(2, '0')}';
  }

  bool get isCurrentMonthPaid {
    return _payments.any((p) => p.periodoMes == currentPeriod && p.isPaid);
  }

  double get totalPaid =>
      _paymentsRepository.calculateTotalPaid(_payments);

  double get totalPending {
    final pendingList = _payments
        .where((p) => !p.isPaid)
        .fold(0.0, (sum, p) => sum + p.monto);
    if (!isCurrentMonthPaid && pendingList == 0.0) {
      return cuotaVigilanciaMensual;
    }
    return pendingList;
  }

  void setFilter(String newFilter) {
    _filter = newFilter;
    notifyListeners();
  }

  Future<void> loadPayments({int? idMiembro}) async {
    _isLoading = true;
    notifyListeners();

    try {
      _payments = await _paymentsRepository.getMyPayments(idMiembro: idMiembro);
    } catch (_) {
      // Ignorar fallo de red
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<bool> registerPayment({
    required int idMiembro,
    required String periodoMes,
    required double monto,
    required String metodoPago,
    String? referencia,
    int? idProyecto,
  }) async {
    _isLoading = true;
    notifyListeners();

    try {
      await _paymentsRepository.createPayment(
        idMiembro: idMiembro,
        periodoMes: periodoMes,
        monto: monto,
        metodoPago: metodoPago,
        referencia: referencia,
        idProyecto: idProyecto,
      );
      await loadPayments(idMiembro: idMiembro);
      return true;
    } catch (e) {
      await loadPayments(idMiembro: idMiembro);
      rethrow;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}
