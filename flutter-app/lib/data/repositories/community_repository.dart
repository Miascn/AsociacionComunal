import '../../core/constants/api_constants.dart';
import '../models/directiva_model.dart';
import '../models/meeting_model.dart';
import '../models/project_model.dart';
import '../services/api_client.dart';

/// Repositorio para asambleas, proyectos comunitarios y junta directiva.
class CommunityRepository {
  final ApiClient _api;

  CommunityRepository({required ApiClient api}) : _api = api;

  /// Obtiene la lista de asambleas y reuniones convocadas.
  Future<List<MeetingModel>> getMeetings() async {
    final response = await _api.get(ApiConstants.reunionesEndpoint);
    if (response is List) {
      return response
          .map((item) => MeetingModel.fromJson(item as Map<String, dynamic>))
          .toList();
    }
    return [];
  }

  /// Obtiene los proyectos de la comunidad.
  Future<List<ProjectModel>> getProjects() async {
    final response = await _api.get(ApiConstants.proyectosEndpoint);
    if (response is List) {
      return response
          .map((item) => ProjectModel.fromJson(item as Map<String, dynamic>))
          .toList();
    }
    return [];
  }

  /// Obtiene la junta directiva actual de la Asociación Comunal.
  Future<List<DirectivaMemberModel>> getDirectivaActual() async {
    final response = await _api.get(ApiConstants.directivaActualEndpoint);
    if (response is List) {
      return response
          .map((item) => DirectivaMemberModel.fromJson(item as Map<String, dynamic>))
          .toList();
    }
    return [];
  }
}
