import 'package:flutter/material.dart';
import '../data/models/meeting_model.dart';
import '../data/models/payment_model.dart';
import '../data/models/project_model.dart';
import '../data/repositories/community_repository.dart';
import '../data/repositories/payments_repository.dart';

/// ViewModel para el Dashboard de Inicio del residente.
class HomeViewModel extends ChangeNotifier {
  final PaymentsRepository _paymentsRepository;
  final CommunityRepository _communityRepository;

  bool _isLoading = false;
  List<PaymentModel> _recentPayments = [];
  List<MeetingModel> _upcomingMeetings = [];
  List<ProjectModel> _activeProjects = [];
  double _totalContributed = 0.0;
  double _pendingBalance = 0.0;

  HomeViewModel({
    required PaymentsRepository paymentsRepository,
    required CommunityRepository communityRepository,
  })  : _paymentsRepository = paymentsRepository,
        _communityRepository = communityRepository;

  bool get isLoading => _isLoading;
  List<PaymentModel> get recentPayments => _recentPayments;
  List<MeetingModel> get upcomingMeetings => _upcomingMeetings;
  List<ProjectModel> get activeProjects => _activeProjects;
  double get totalContributed => _totalContributed;
  double get pendingBalance => _pendingBalance;

  MeetingModel? get nextMeeting =>
      _upcomingMeetings.isNotEmpty ? _upcomingMeetings.first : null;

  Future<void> loadDashboardData({int? idMiembro}) async {
    _isLoading = true;
    notifyListeners();

    try {
      final paymentsFuture = _paymentsRepository.getMyPayments(idMiembro: idMiembro);
      final meetingsFuture = _communityRepository.getMeetings();
      final projectsFuture = _communityRepository.getProjects();

      final results = await Future.wait([paymentsFuture, meetingsFuture, projectsFuture]);

      _recentPayments = results[0] as List<PaymentModel>;
      _upcomingMeetings = results[1] as List<MeetingModel>;
      _activeProjects = results[2] as List<ProjectModel>;

      _totalContributed = _paymentsRepository.calculateTotalPaid(_recentPayments);

      // Si no hay cuotas pendientes explícitas, el balance pendiente es 0.00 (Al día)
      _pendingBalance = _recentPayments
          .where((p) => !p.isPaid)
          .fold(0.0, (sum, p) => sum + p.monto);
    } catch (_) {
      // Manejar error silenciosamente manteniendo listas existentes
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}
