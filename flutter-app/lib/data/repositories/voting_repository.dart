import '../../core/constants/api_constants.dart';
import '../models/voting_model.dart';
import '../services/api_client.dart';

/// Repositorio para la gestión de votaciones y emisión de votos del residente.
class VotingRepository {
  final ApiClient _api;

  VotingRepository({required ApiClient api}) : _api = api;

  /// Obtiene los procesos de votación disponibles.
  Future<List<VotingModel>> getPolls({String? estado}) async {
    try {
      final queryParams = <String, String>{};
      if (estado != null && estado.isNotEmpty) {
        queryParams['estado'] = estado;
      }

      final response = await _api.get(
        ApiConstants.votacionesEndpoint,
        queryParams: queryParams.isNotEmpty ? queryParams : null,
      );

      if (response is List) {
        return response
            .map((item) => VotingModel.fromJson(item as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (_) {
      return [];
    }
  }

  /// Consulta el detalle de una votación con sus opciones.
  Future<VotingModel?> getPollById(int id) async {
    try {
      final response = await _api.get('${ApiConstants.votacionesEndpoint}/$id');
      if (response is Map<String, dynamic>) {
        return VotingModel.fromJson(response);
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  /// Verifica si el miembro ya emitió su voto en una votación específica.
  Future<bool> hasVoted(int idVotacion, int idMiembro) async {
    try {
      final response = await _api.get(
        '${ApiConstants.votacionesEndpoint}/$idVotacion/mi-participacion',
        queryParams: {'miembroId': idMiembro.toString()},
      );
      if (response is Map<String, dynamic>) {
        return response['yaVoto'] as bool? ?? response['participo'] as bool? ?? false;
      }
      return false;
    } catch (_) {
      return false;
    }
  }

  /// Emite un voto por una opción.
  Future<bool> castVote({
    required int idVotacion,
    required int idOpcion,
    int? idMiembro,
  }) async {
    try {
      await _api.post(
        '/api/votos',
        body: CastVoteRequest(
          idVotacion: idVotacion,
          idOpcion: idOpcion,
          idMiembro: idMiembro,
        ).toJson(),
      );
      return true;
    } catch (e) {
      rethrow;
    }
  }
}
