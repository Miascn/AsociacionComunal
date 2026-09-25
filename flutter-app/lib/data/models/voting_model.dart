/// Modelo para procesos de votación comunitaria y encuestas.
class VotingOptionModel {
  final int idOpcion;
  final String descripcion;
  final int orden;
  final int votos;

  const VotingOptionModel({
    required this.idOpcion,
    required this.descripcion,
    required this.orden,
    this.votos = 0,
  });

  int get id => idOpcion;
  String get texto => descripcion;
  int get cantidadVotos => votos;

  factory VotingOptionModel.fromJson(Map<String, dynamic> json) {
    return VotingOptionModel(
      idOpcion: json['idOpcion'] as int? ?? json['id'] as int? ?? 0,
      descripcion: json['descripcion'] as String? ?? '',
      orden: json['orden'] as int? ?? 0,
      votos: json['votos'] as int? ?? 0,
    );
  }

  Map<String, dynamic> toJson() => {
        'idOpcion': idOpcion,
        'descripcion': descripcion,
        'orden': orden,
        'votos': votos,
      };
}

class VotingModel {
  final int id;
  final String titulo;
  final String descripcion;
  final int? idProyecto;
  final String? nombreProyecto;
  final String? fechaInicio;
  final String? fechaFin;
  final String estado; // BORRADOR, ABIERTA, CERRADA, CANCELADA
  final int totalOpciones;
  final int totalVotos;
  final List<VotingOptionModel> opciones;

  const VotingModel({
    required this.id,
    required this.titulo,
    required this.descripcion,
    this.idProyecto,
    this.nombreProyecto,
    this.fechaInicio,
    this.fechaFin,
    required this.estado,
    required this.totalOpciones,
    required this.totalVotos,
    required this.opciones,
  });

  bool get isOpen => estado.toUpperCase() == 'ABIERTA';
  bool get isClosed => estado.toUpperCase() == 'CERRADA';
  bool get yaVoto => false;

  factory VotingModel.fromJson(Map<String, dynamic> json) {
    var rawOpciones = json['opciones'] as List? ?? [];
    return VotingModel(
      id: json['id'] as int? ?? 0,
      titulo: json['titulo'] as String? ?? 'Votación Comunal',
      descripcion: json['descripcion'] as String? ?? '',
      idProyecto: json['idProyecto'] as int?,
      nombreProyecto: json['nombreProyecto'] as String?,
      fechaInicio: json['fechaInicio'] as String?,
      fechaFin: json['fechaFin'] as String?,
      estado: json['estado'] as String? ?? 'ABIERTA',
      totalOpciones: json['totalOpciones'] as int? ?? rawOpciones.length,
      totalVotos: json['totalVotos'] as int? ?? 0,
      opciones: rawOpciones
          .map((item) => VotingOptionModel.fromJson(item as Map<String, dynamic>))
          .toList(),
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'titulo': titulo,
        'descripcion': descripcion,
        'idProyecto': idProyecto,
        'nombreProyecto': nombreProyecto,
        'fechaInicio': fechaInicio,
        'fechaFin': fechaFin,
        'estado': estado,
        'totalOpciones': totalOpciones,
        'totalVotos': totalVotos,
        'opciones': opciones.map((o) => o.toJson()).toList(),
      };
}

class CastVoteRequest {
  final int idVotacion;
  final int idOpcion;
  final int? idMiembro;

  const CastVoteRequest({
    required this.idVotacion,
    required this.idOpcion,
    this.idMiembro,
  });

  Map<String, dynamic> toJson() => {
        'idVotacion': idVotacion,
        'idOpcion': idOpcion,
        if (idMiembro != null) 'idMiembro': idMiembro,
      };
}
