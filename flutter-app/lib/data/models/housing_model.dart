/// Modelo de vivienda comunal asignada al residente.
class HousingModel {
  final int id;
  final String codigo;
  final String sector;
  final String direccion;
  final String? referencia;
  final int? idRepresentante;
  final String? representante;
  final String? fechaRegistro;
  final String estado;
  final int adultos;
  final int menores;

  const HousingModel({
    required this.id,
    required this.codigo,
    required this.sector,
    required this.direccion,
    this.referencia,
    this.idRepresentante,
    this.representante,
    this.fechaRegistro,
    required this.estado,
    required this.adultos,
    required this.menores,
  });

  int get totalHabitantes => adultos + menores;

  factory HousingModel.fromJson(Map<String, dynamic> json) {
    return HousingModel(
      id: json['id'] as int? ?? json['idVivienda'] as int? ?? 0,
      codigo: json['codigo'] as String? ?? 'VIV-000',
      sector: json['sector'] as String? ?? 'Principal',
      direccion: json['direccion'] as String? ?? 'Comunidad',
      referencia: json['referencia'] as String?,
      idRepresentante: json['idRepresentante'] as int?,
      representante: json['representante'] as String?,
      fechaRegistro: json['fechaRegistro'] as String?,
      estado: json['estado'] as String? ?? 'ACTIVA',
      adultos: json['adultos'] as int? ?? 1,
      menores: json['menores'] as int? ?? 0,
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'codigo': codigo,
        'sector': sector,
        'direccion': direccion,
        'referencia': referencia,
        'idRepresentante': idRepresentante,
        'representante': representante,
        'fechaRegistro': fechaRegistro,
        'estado': estado,
        'adultos': adultos,
        'menores': menores,
      };
}
