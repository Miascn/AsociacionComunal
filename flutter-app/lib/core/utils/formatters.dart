import 'package:intl/intl.dart';

/// Formateadores para números, monedas, fechas y estados.
class Formatters {
  Formatters._();

  static final NumberFormat _currencyFormat = NumberFormat.currency(
    symbol: '\$',
    decimalDigits: 2,
  );

  static final DateFormat _dateFormat = DateFormat('dd MMM yyyy', 'es');
  static final DateFormat _timeFormat = DateFormat('hh:mm a', 'es');

  /// Formatea un valor numérico a moneda (\$ 1,250.00)
  static String currency(num? amount) {
    if (amount == null) return '\$0.00';
    return _currencyFormat.format(amount);
  }

  /// Formatea una fecha ISO (YYYY-MM-DD o timestamp)
  static String date(dynamic rawDate) {
    if (rawDate == null) return 'N/D';
    try {
      if (rawDate is DateTime) return _dateFormat.format(rawDate);
      final parsed = DateTime.parse(rawDate.toString());
      return _dateFormat.format(parsed);
    } catch (_) {
      return rawDate.toString();
    }
  }

  /// Formatea hora
  static String time(dynamic rawDate) {
    if (rawDate == null) return '';
    try {
      if (rawDate is DateTime) return _timeFormat.format(rawDate);
      final parsed = DateTime.parse(rawDate.toString());
      return _timeFormat.format(parsed);
    } catch (_) {
      return '';
    }
  }

  /// Obtiene las iniciales de un nombre
  static String initials(String? name) {
    if (name == null || name.trim().isEmpty) return '?';
    final parts = name.trim().split(RegExp(r'\s+'));
    if (parts.length == 1) return parts[0][0].toUpperCase();
    return '${parts[0][0]}${parts[1][0]}'.toUpperCase();
  }
}
