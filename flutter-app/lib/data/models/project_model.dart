/// Modelo de proyecto comunitario.
class ProjectModel {
  final int id;
  final String nombre;
  final String descripcion;
  final double presupuesto;
  final String? fechaCreacion;
  final String estado;
  final double montoRecaudado;
  final double montoPendiente;
  final double progresoPorcentaje;
  final int? idVotacion;
  final String? tituloVotacion;
  final int totalAportantes;
  final double aportePorMiembro;

  const ProjectModel({
    required this.id,
    required this.nombre,
    required this.descripcion,
    required this.presupuesto,
    this.fechaCreacion,
    required this.estado,
    this.montoRecaudado = 0.0,
    this.montoPendiente = 0.0,
    this.progresoPorcentaje = 0.0,
    this.idVotacion,
    this.tituloVotacion,
    this.totalAportantes = 0,
    this.aportePorMiembro = 0.0,
  });

  bool get canReceiveContributions =>
      estado.toUpperCase() == 'APROBADO' ||
      estado.toUpperCase() == 'EN_EJECUCION' ||
      estado.toUpperCase() == 'EN_RECAUDACION';

  bool get hasVoting => tituloVotacion != null && tituloVotacion!.isNotEmpty;

  factory ProjectModel.fromJson(Map<String, dynamic> json) {
    final pres = (json['presupuesto'] as num?)?.toDouble() ?? 0.0;
    final rec = (json['montoRecaudado'] as num?)?.toDouble() ?? 0.0;
    final pend = (json['montoPendiente'] as num?)?.toDouble() ?? (pres > rec ? pres - rec : 0.0);
    final prog = (json['progresoPorcentaje'] as num?)?.toDouble() ??
        (pres > 0 ? ((rec / pres) * 100.0).clamp(0.0, 100.0) : 0.0);

    return ProjectModel(
      id: json['id'] as int? ?? 0,
      nombre: json['nombre'] as String? ?? 'Proyecto Comunal',
      descripcion: json['descripcion'] as String? ?? '',
      presupuesto: pres,
      fechaCreacion: json['fechaCreacion'] as String?,
      estado: json['estado'] as String? ?? 'BORRADOR',
      montoRecaudado: rec,
      montoPendiente: pend,
      progresoPorcentaje: prog,
      idVotacion: json['idVotacion'] as int?,
      tituloVotacion: json['tituloVotacion'] as String?,
      totalAportantes: json['totalAportantes'] as int? ?? 0,
      aportePorMiembro: (json['aportePorMiembro'] as num?)?.toDouble() ??
          (pres > 0 ? (pres / 50.0) : 0.0),
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'nombre': nombre,
        'descripcion': descripcion,
        'presupuesto': presupuesto,
        'fechaCreacion': fechaCreacion,
        'estado': estado,
        'montoRecaudado': montoRecaudado,
        'montoPendiente': montoPendiente,
        'progresoPorcentaje': progresoPorcentaje,
        'idVotacion': idVotacion,
        'tituloVotacion': tituloVotacion,
        'totalAportantes': totalAportantes,
        'aportePorMiembro': aportePorMiembro,
      };
}
