import 'package:flutter/material.dart';
import '../data/models/payment_model.dart';
import '../data/repositories/payments_repository.dart';

/// ViewModel para la gestión e historial de aportaciones del residente.
class PaymentsViewModel extends ChangeNotifier {
  final PaymentsRepository _paymentsRepository;

  bool _isLoading = false;
  String? _errorMessage;
  List<PaymentModel> _payments = [];
  String _filter = 'TODOS'; // 'TODOS', 'PAGADOS', 'PENDIENTES'

  PaymentsViewModel({required PaymentsRepository paymentsRepository})
      : _paymentsRepository = paymentsRepository;

  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  String get filter => _filter;

  List<PaymentModel> get filteredPayments {
    if (_filter == 'PAGADOS') {
      return _payments.where((p) => p.isPaid).toList();
    } else if (_filter == 'PENDIENTES') {
      return _payments.where((p) => !p.isPaid).toList();
    } else if (_filter == 'CUOTAS') {
      return _payments.where((p) => p.isMonthlyFee).toList();
    } else if (_filter == 'PROYECTOS') {
      return _payments.where((p) => p.isProjectContribution).toList();
    }
    return _payments;
  }

  List<PaymentModel> get monthlyFees =>
      _payments.where((p) => p.isMonthlyFee).toList();

  List<PaymentModel> get projectContributions =>
      _payments.where((p) => p.isProjectContribution).toList();

  double get totalMonthlyFeesPaid => _payments
      .where((p) => p.isMonthlyFee && p.isPaid)
      .fold(0.0, (sum, p) => sum + p.monto);

  double get totalProjectContributionsPaid => _payments
      .where((p) => p.isProjectContribution && p.isPaid)
      .fold(0.0, (sum, p) => sum + p.monto);

  static const double cuotaVigilanciaMensual = 10.00;

  String get currentPeriod {
    final now = DateTime.now();
    return '${now.year}-${now.month.toString().padLeft(2, '0')}';
  }

  bool get isCurrentMonthPaid {
    return _payments.any((p) => p.isMonthlyFee && p.periodoMes == currentPeriod && p.isPaid);
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
    _errorMessage = null;
    notifyListeners();

    try {
      _payments = await _paymentsRepository.getMyPayments(idMiembro: idMiembro);
    } catch (e) {
      _errorMessage = e.toString().replaceAll('Exception: ', '');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<PaymentModel> processSimulatedPayment({
    required int idMiembro,
    required String periodoMes,
    required double monto,
    required String metodoSeleccionado,
    String? referenciaGenerada,
    int? idProyecto,
  }) async {
    _isLoading = true;
    notifyListeners();

    try {
      final payment = await _paymentsRepository.processSimulatedPayment(
        idMiembro: idMiembro,
        periodoMes: periodoMes,
        monto: monto,
        metodoSeleccionado: metodoSeleccionado,
        referenciaGenerada: referenciaGenerada,
        idProyecto: idProyecto,
      );
      await loadPayments(idMiembro: idMiembro);
      return payment;
    } catch (e) {
      await loadPayments(idMiembro: idMiembro);
      rethrow;
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
