import '../models/housing_model.dart';
import '../services/api_client.dart';

/// Repositorio para la consulta de la vivienda comunitaria asignada al residente.
///
/// NOTA DE ARQUITECTURA / MEJORA FUTURA (BACKEND REQUIRED):
/// En una fase posterior de backend debe exponerse un endpoint autenticado:
/// `GET /api/viviendas/mi-vivienda`
/// que determine el miembro directamente desde la sesión del token JWT y NO desde
/// un parámetro de ID enviado por el cliente móvil.
class HousingRepository {
  final ApiClient _api;

  HousingRepository({required ApiClient api}) : _api = api;

  /// Obtiene el listado de viviendas registradas.
  Future<List<HousingModel>> getAll() async {
    final response = await _api.get('/api/viviendas');
    if (response is List) {
      return response
          .map((item) => HousingModel.fromJson(item as Map<String, dynamic>))
          .toList();
    }
    return [];
  }

  /// Obtiene una vivienda específica por su ID.
  Future<HousingModel?> getById(int id) async {
    final response = await _api.get('/api/viviendas/$id');
    if (response is Map<String, dynamic>) {
      return HousingModel.fromJson(response);
    }
    return null;
  }

  /// Busca exclusivamente la vivienda correspondiente al miembro autenticado.
  /// No se permite seleccionar arbitrariamente la vivienda de otro miembro.
  /// Si el miembro no tiene vivienda asignada como representante, retorna null.
  Future<HousingModel?> getByMemberId(int idMiembro) async {
    final list = await getAll();
    for (final housing in list) {
      if (housing.idRepresentante == idMiembro) {
        return housing;
      }
    }
    return null;
  }
}
