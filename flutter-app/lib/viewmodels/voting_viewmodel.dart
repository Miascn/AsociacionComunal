import 'package:flutter/material.dart';
import '../data/models/voting_model.dart';
import '../data/repositories/voting_repository.dart';

/// ViewModel para la gestión de votaciones y participación electoral comunal.
class VotingViewModel extends ChangeNotifier {
  final VotingRepository _repository;

  bool _isLoading = false;
  List<VotingModel> _polls = [];
  final Map<int, bool> _votedMap = {};
  String? _errorMessage;

  VotingViewModel({required VotingRepository repository}) : _repository = repository;

  bool get isLoading => _isLoading;
  List<VotingModel> get polls => _polls;
  String? get errorMessage => _errorMessage;

  bool hasUserVoted(int idVotacion) => _votedMap[idVotacion] ?? false;

  Future<void> loadPolls({int? idMiembro}) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      _polls = await _repository.getPolls();

      if (idMiembro != null) {
        for (final poll in _polls) {
          final voted = await _repository.hasVoted(poll.id, idMiembro);
          _votedMap[poll.id] = voted;
        }
      }
    } catch (e) {
      _errorMessage = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<bool> submitVote({
    required int idVotacion,
    required int idOpcion,
    int? idMiembro,
  }) => castVote(idVotacion: idVotacion, idOpcion: idOpcion, idMiembro: idMiembro);

  Future<bool> castVote({
    required int idVotacion,
    required int idOpcion,
    int? idMiembro,
  }) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      await _repository.castVote(
        idVotacion: idVotacion,
        idOpcion: idOpcion,
        idMiembro: idMiembro,
      );
      _votedMap[idVotacion] = true;

      // Recargar votaciones para actualizar conteo
      await loadPolls(idMiembro: idMiembro);
      return true;
    } catch (e) {
      _errorMessage = e.toString();
      notifyListeners();
      return false;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}
