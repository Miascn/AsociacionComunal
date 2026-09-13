/// Modelo de aportación / pago para el residente.
class PaymentModel {
  final int id;
  final int idMiembro;
  final String nombreMiembro;
  final String duiMiembro;
  final int? idProyecto;
  final String? nombreProyecto;
  final String periodoMes;
  final double monto;
  final String? fechaPago;
  final String metodoPago;
  final String? referencia;
  final String estado;

  const PaymentModel({
    required this.id,
    required this.idMiembro,
    required this.nombreMiembro,
    required this.duiMiembro,
    this.idProyecto,
    this.nombreProyecto,
    required this.periodoMes,
    required this.monto,
    this.fechaPago,
    required this.metodoPago,
    this.referencia,
    required this.estado,
  });

  bool get isPaid => estado.toUpperCase() == 'PAGADA' || estado.toUpperCase() == 'REGISTRADA';

  factory PaymentModel.fromJson(Map<String, dynamic> json) {
    return PaymentModel(
      id: json['idAportacion'] as int? ?? json['id'] as int? ?? 0,
      idMiembro: json['idMiembro'] as int? ?? 0,
      nombreMiembro: json['nombreMiembro'] as String? ?? '',
      duiMiembro: json['duiMiembro'] as String? ?? '-',
      idProyecto: json['idProyecto'] as int?,
      nombreProyecto: json['nombreProyecto'] as String?,
      periodoMes: json['periodoMes'] as String? ?? '',
      monto: (json['monto'] as num?)?.toDouble() ?? 0.0,
      fechaPago: json['fechaPago'] as String?,
      metodoPago: json['metodoPago'] as String? ?? 'EFECTIVO',
      referencia: json['referencia'] as String?,
      estado: json['estado'] as String? ?? 'REGISTRADA',
    );
  }

  Map<String, dynamic> toJson() => {
        'idAportacion': id,
        'idMiembro': idMiembro,
        'nombreMiembro': nombreMiembro,
        'duiMiembro': duiMiembro,
        'idProyecto': idProyecto,
        'nombreProyecto': nombreProyecto,
        'periodoMes': periodoMes,
        'monto': monto,
        'fechaPago': fechaPago,
        'metodoPago': metodoPago,
        'referencia': referencia,
        'estado': estado,
      };
}
