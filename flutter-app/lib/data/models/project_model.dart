/// Modelo de proyecto comunitario.
class ProjectModel {
  final int id;
  final String nombre;
  final String descripcion;
  final double presupuesto;
  final String? fechaCreacion;
  final String estado;

  const ProjectModel({
    required this.id,
    required this.nombre,
    required this.descripcion,
    required this.presupuesto,
    this.fechaCreacion,
    required this.estado,
  });

  factory ProjectModel.fromJson(Map<String, dynamic> json) {
    return ProjectModel(
      id: json['id'] as int? ?? 0,
      nombre: json['nombre'] as String? ?? 'Proyecto Comunal',
      descripcion: json['descripcion'] as String? ?? '',
      presupuesto: (json['presupuesto'] as num?)?.toDouble() ?? 0.0,
      fechaCreacion: json['fechaCreacion'] as String?,
      estado: json['estado'] as String? ?? 'EN_PLANIFICACION',
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'nombre': nombre,
        'descripcion': descripcion,
        'presupuesto': presupuesto,
        'fechaCreacion': fechaCreacion,
        'estado': estado,
      };
}
