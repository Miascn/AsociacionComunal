import 'package:flutter/material.dart';
import '../data/models/directiva_model.dart';
import '../data/models/meeting_model.dart';
import '../data/models/payment_model.dart';
import '../data/models/project_model.dart';
import '../data/models/voting_model.dart';
import '../data/repositories/community_repository.dart';
import '../data/repositories/payments_repository.dart';
import '../data/repositories/voting_repository.dart';

/// ViewModel para el Dashboard de Inicio del residente.
class HomeViewModel extends ChangeNotifier {
  final PaymentsRepository _paymentsRepository;
  final CommunityRepository _communityRepository;
  final VotingRepository? _votingRepository;

  bool _isLoading = false;
  String? _errorMessage;
  List<PaymentModel> _recentPayments = [];
  List<MeetingModel> _upcomingMeetings = [];
  List<ProjectModel> _activeProjects = [];
  List<DirectivaMemberModel> _directiva = [];
  VotingModel? _pendingVoting;
  double _totalContributed = 0.0;
  double _pendingBalance = 0.0;

  HomeViewModel({
    required PaymentsRepository paymentsRepository,
    required CommunityRepository communityRepository,
    VotingRepository? votingRepository,
  })  : _paymentsRepository = paymentsRepository,
        _communityRepository = communityRepository,
        _votingRepository = votingRepository;

  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  List<PaymentModel> get recentPayments => _recentPayments;
  List<MeetingModel> get upcomingMeetings => _upcomingMeetings;
  List<ProjectModel> get activeProjects => _activeProjects;
  List<DirectivaMemberModel> get directiva => _directiva;
  VotingModel? get pendingVoting => _pendingVoting;
  double get totalContributed => _totalContributed;
  double get pendingBalance => _pendingBalance;

  MeetingModel? get nextMeeting =>
      _upcomingMeetings.isNotEmpty ? _upcomingMeetings.first : null;

  int get activeProjectsCount =>
      _activeProjects.where((p) => p.estado == 'EN_PROCESO' || p.estado == 'ACTIVO' || p.estado == 'EN_EJECUCION').length;

  int get completedProjectsCount =>
      _activeProjects.where((p) => p.estado == 'COMPLETADO' || p.estado == 'FINALIZADO').length;

  ProjectModel? get featuredProject =>
      _activeProjects.isNotEmpty ? _activeProjects.first : null;

  /// Retorna el saludo apropiado según la hora del día.
  static String getGreeting([DateTime? time]) {
    final now = time ?? DateTime.now();
    final hour = now.hour;
    if (hour >= 5 && hour < 12) {
      return 'Buenos días';
    } else if (hour >= 12 && hour < 19) {
      return 'Buenas tardes';
    } else {
      return 'Buenas noches';
    }
  }

  Future<void> loadDashboardData({int? idMiembro}) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final paymentsFuture = _paymentsRepository.getMyPayments(idMiembro: idMiembro);
      final meetingsFuture = _communityRepository.getMeetings();
      final projectsFuture = _communityRepository.getProjects();
      final directivaFuture = _communityRepository.getDirectivaActual();
      final pollsFuture = _votingRepository?.getPolls(estado: 'ABIERTA');

      final results = await Future.wait([
        paymentsFuture,
        meetingsFuture,
        projectsFuture,
        directivaFuture,
        if (pollsFuture != null) pollsFuture,
      ]);

      _recentPayments = results[0] as List<PaymentModel>;
      final allMeetings = results[1] as List<MeetingModel>;
      _upcomingMeetings = allMeetings.where((m) => m.isUpcoming).toList();
      _activeProjects = results[2] as List<ProjectModel>;
      _directiva = results[3] as List<DirectivaMemberModel>;

      _totalContributed = _paymentsRepository.calculateTotalPaid(_recentPayments);

      _pendingBalance = _recentPayments
          .where((p) => !p.isPaid)
          .fold(0.0, (sum, p) => sum + p.monto);

      // Evaluar si existe votación abierta pendiente de voto para el miembro
      _pendingVoting = null;
      if (_votingRepository != null && results.length > 4 && idMiembro != null) {
        final openPolls = results[4] as List<VotingModel>;
        for (final poll in openPolls) {
          final alreadyVoted = await _votingRepository!.hasVoted(poll.id, idMiembro);
          if (!alreadyVoted) {
            _pendingVoting = poll;
            break;
          }
        }
      }
    } catch (_) {
      _errorMessage = 'No fue posible actualizar todos los datos del portal.';
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}
