/// Modelo de asamblea o reunión comunal.
class MeetingModel {
  final int id;
  final String titulo;
  final String fechaHora;
  final String lugar;
  final String tipo;
  final String estado;
  final int totalConvocados;
  final int totalAsistentes;
  final double porcentajeAsistencia;

  const MeetingModel({
    required this.id,
    required this.titulo,
    required this.fechaHora,
    required this.lugar,
    required this.tipo,
    required this.estado,
    required this.totalConvocados,
    required this.totalAsistentes,
    required this.porcentajeAsistencia,
  });

  bool get isUpcoming =>
      estado.toUpperCase() == 'PROGRAMADA' || estado.toUpperCase() == 'CONVOCADA';

  factory MeetingModel.fromJson(Map<String, dynamic> json) {
    return MeetingModel(
      id: json['id'] as int? ?? 0,
      titulo: json['titulo'] as String? ?? 'Reunión Comunal',
      fechaHora: json['fechaHora'] as String? ?? '',
      lugar: json['lugar'] as String? ?? 'Casa Comunal',
      tipo: json['tipo'] as String? ?? 'ORDINARIA',
      estado: json['estado'] as String? ?? 'PROGRAMADA',
      totalConvocados: json['totalConvocados'] as int? ?? 0,
      totalAsistentes: json['totalAsistentes'] as int? ?? 0,
      porcentajeAsistencia:
          (json['porcentajeAsistencia'] as num?)?.toDouble() ?? 0.0,
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'titulo': titulo,
        'fechaHora': fechaHora,
        'lugar': lugar,
        'tipo': tipo,
        'estado': estado,
        'totalConvocados': totalConvocados,
        'totalAsistentes': totalAsistentes,
        'porcentajeAsistencia': porcentajeAsistencia,
      };
}
