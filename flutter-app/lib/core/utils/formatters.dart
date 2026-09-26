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
      try {
        final dt = rawDate is DateTime ? rawDate : DateTime.parse(rawDate.toString());
        const months = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
        return '${dt.day.toString().padLeft(2, '0')} ${months[dt.month - 1]} ${dt.year}';
      } catch (_) {
        return rawDate.toString();
      }
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
      try {
        final dt = rawDate is DateTime ? rawDate : DateTime.parse(rawDate.toString());
        final hour = dt.hour % 12 == 0 ? 12 : dt.hour % 12;
        final minute = dt.minute.toString().padLeft(2, '0');
        final ampm = dt.hour >= 12 ? 'PM' : 'AM';
        return '$hour:$minute $ampm';
      } catch (_) {
        return '';
      }
    }
  }

  /// Formatea fecha y hora combinadas
  static String dateTime(dynamic rawDate) {
    if (rawDate == null) return 'N/D';
    final d = date(rawDate);
    final t = time(rawDate);
    if (t.isEmpty) return d;
    return '$d • $t';
  }

  /// Obtiene las iniciales de un nombre
  static String initials(String? name) {
    if (name == null || name.trim().isEmpty) return '?';
    final parts = name.trim().split(RegExp(r'\s+'));
    if (parts.length == 1) return parts[0][0].toUpperCase();
    return '${parts[0][0]}${parts[1][0]}'.toUpperCase();
  }
}
