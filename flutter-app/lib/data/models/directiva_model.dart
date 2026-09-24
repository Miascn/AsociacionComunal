/// Modelo que representa a un miembro de la Junta Directiva comunal.
/// Corresponde al DTO AsignacionCargoResponse del backend.
class DirectivaMemberModel {
  final int id;
  final int? idMiembro;
  final String nombreMiembro;
  final String? duiMiembro;
  final String? telefonoMiembro;
  final int? idCargo;
  final String nombreCargo;
  final int? nivelJerarquico;
  final int? idPeriodo;
  final String? nombrePeriodo;
  final String? estadoPeriodo;
  final String? fechaAsignacion;
  final String? fechaFin;
  final String? motivoSalida;
  final String estado;

  const DirectivaMemberModel({
    required this.id,
    this.idMiembro,
    required this.nombreMiembro,
    this.duiMiembro,
    this.telefonoMiembro,
    this.idCargo,
    required this.nombreCargo,
    this.nivelJerarquico,
    this.idPeriodo,
    this.nombrePeriodo,
    this.estadoPeriodo,
    this.fechaAsignacion,
    this.fechaFin,
    this.motivoSalida,
    required this.estado,
  });

  factory DirectivaMemberModel.fromJson(Map<String, dynamic> json) {
    return DirectivaMemberModel(
      id: json['id'] as int? ?? 0,
      idMiembro: json['idMiembro'] as int?,
      nombreMiembro: json['nombreMiembro'] as String? ?? 'Directivo Comunal',
      duiMiembro: json['duiMiembro'] as String?,
      telefonoMiembro: json['telefonoMiembro'] as String?,
      idCargo: json['idCargo'] as int?,
      nombreCargo: json['nombreCargo'] as String? ?? 'Cargo Comunal',
      nivelJerarquico: json['nivelJerarquico'] as int?,
      idPeriodo: json['idPeriodo'] as int?,
      nombrePeriodo: json['nombrePeriodo'] as String?,
      estadoPeriodo: json['estadoPeriodo'] as String?,
      fechaAsignacion: json['fechaAsignacion'] as String?,
      fechaFin: json['fechaFin'] as String?,
      motivoSalida: json['motivoSalida'] as String?,
      estado: json['estado'] as String? ?? 'ACTIVO',
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'idMiembro': idMiembro,
        'nombreMiembro': nombreMiembro,
        'duiMiembro': duiMiembro,
        'telefonoMiembro': telefonoMiembro,
        'idCargo': nombreCargo,
        'nombreCargo': nombreCargo,
        'nivelJerarquico': nivelJerarquico,
        'idPeriodo': idPeriodo,
        'nombrePeriodo': nombrePeriodo,
        'estadoPeriodo': estadoPeriodo,
        'fechaAsignacion': fechaAsignacion,
        'fechaFin': fechaFin,
        'motivoSalida': motivoSalida,
        'estado': estado,
      };
}
