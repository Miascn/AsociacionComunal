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

  bool get isPaid {
    final s = estado.trim().toUpperCase();
    return s == 'PAGADA' ||
        s == 'REGISTRADA' ||
        s == 'APROBADA' ||
        s == 'COMPLETADA' ||
        (s.isNotEmpty && s != 'ANULADA' && s != 'CANCELADA' && s != 'RECHAZADA');
  }
  bool get isProjectContribution => idProyecto != null;
  bool get isMonthlyFee => idProyecto == null;
  String get tipoEtiqueta => isProjectContribution ? 'Aporte a Proyecto' : 'Cuota Mensual';

  factory PaymentModel.fromJson(Map<String, dynamic> json) {
    return PaymentModel(
      id: (json['idAportacion'] as num?)?.toInt() ?? (json['id'] as num?)?.toInt() ?? 0,
      idMiembro: (json['idMiembro'] as num?)?.toInt() ?? (json['id_miembro'] as num?)?.toInt() ?? 0,
      nombreMiembro: json['nombreMiembro'] as String? ?? json['nombre_miembro'] as String? ?? '',
      duiMiembro: json['duiMiembro'] as String? ?? json['dui_miembro'] as String? ?? '-',
      idProyecto: (json['idProyecto'] as num?)?.toInt() ?? (json['id_proyecto'] as num?)?.toInt(),
      nombreProyecto: json['nombreProyecto'] as String? ?? json['nombre_proyecto'] as String?,
      periodoMes: json['periodoMes'] as String? ?? json['periodo_mes'] as String? ?? '',
      monto: (json['monto'] as num?)?.toDouble() ?? 0.0,
      fechaPago: json['fechaPago'] as String? ?? json['fecha_pago'] as String?,
      metodoPago: json['metodoPago'] as String? ?? json['metodo_pago'] as String? ?? 'EFECTIVO',
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
