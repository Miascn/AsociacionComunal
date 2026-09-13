import '../models/housing_model.dart';
import '../services/api_client.dart';

/// Repositorio para la consulta de viviendas comunitarias asignadas a los residentes.
class HousingRepository {
  final ApiClient _api;

  HousingRepository({required ApiClient api}) : _api = api;

  /// Obtiene el listado de viviendas registradas.
  Future<List<HousingModel>> getAll() async {
    try {
      final response = await _api.get('/api/viviendas');
      if (response is List) {
        return response
            .map((item) => HousingModel.fromJson(item as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (_) {
      return [];
    }
  }

  /// Obtiene una vivienda específica por su ID.
  Future<HousingModel?> getById(int id) async {
    try {
      final response = await _api.get('/api/viviendas/$id');
      if (response is Map<String, dynamic>) {
        return HousingModel.fromJson(response);
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  /// Busca la vivienda correspondiente a un miembro representante.
  Future<HousingModel?> getByMemberId(int idMiembro) async {
    final list = await getAll();
    try {
      return list.firstWhere((h) => h.idRepresentante == idMiembro);
    } catch (_) {
      return list.isNotEmpty ? list.first : null;
    }
  }
}
