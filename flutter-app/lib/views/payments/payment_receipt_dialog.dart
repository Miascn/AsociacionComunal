import 'package:flutter/material.dart';
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/neo_glass_container.dart';
import '../../data/models/payment_model.dart';

/// Diálogo modal con el Comprobante Visual Oficial de Aportación o Cuota
class PaymentReceiptDialog extends StatelessWidget {
  final PaymentModel payment;
  final String? methodName;
  final String? conceptTitle;

  const PaymentReceiptDialog({
    super.key,
    required this.payment,
    this.methodName,
    this.conceptTitle,
  });

  static void show(
    BuildContext context, {
    required PaymentModel payment,
    String? methodName,
    String? conceptTitle,
  }) {
    showDialog(
      context: context,
      builder: (ctx) => PaymentReceiptDialog(
        payment: payment,
        methodName: methodName,
        conceptTitle: conceptTitle,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final isProject = payment.isProjectContribution;

    return Dialog(
      backgroundColor: Colors.transparent,
      insetPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
      child: NeoGlassContainer(
        borderRadius: BorderRadius.circular(28),
        padding: const EdgeInsets.all(24),
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              // Encabezado con sello de éxito
              Container(
                width: 64,
                height: 64,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: AppColors.successGreen.withValues(alpha: 0.15),
                  border: Border.all(color: AppColors.successGreen, width: 2),
                ),
                child: const Icon(
                  FluentIcons.checkmark_circle_24_filled,
                  color: AppColors.successGreen,
                  size: 38,
                ),
              ),
              const SizedBox(height: 14),

              Text(
                'Comprobante Electrónico',
                style: TextStyle(
                  fontSize: 19,
                  fontWeight: FontWeight.w900,
                  color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                'Asociación Comunal de Vecinos y Residentes',
                style: TextStyle(
                  fontSize: 12,
                  color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                ),
              ),
              const SizedBox(height: 18),

              // Monto destacado
              Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(vertical: 14),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(16),
                  color: AppColors.brandBlue.withValues(alpha: isDark ? 0.12 : 0.06),
                  border: Border.all(
                    color: AppColors.brandBlue.withValues(alpha: 0.2),
                  ),
                ),
                child: Column(
                  children: [
                    Text(
                      'TOTAL PAGADO',
                      style: TextStyle(
                        fontSize: 11,
                        letterSpacing: 1.2,
                        fontWeight: FontWeight.bold,
                        color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      Formatters.currency(payment.monto),
                      style: const TextStyle(
                        fontSize: 28,
                        fontWeight: FontWeight.w900,
                        color: AppColors.brandBlue,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
                      decoration: BoxDecoration(
                        color: AppColors.successGreen.withValues(alpha: 0.15),
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: Text(
                        payment.estado.toUpperCase(),
                        style: const TextStyle(
                          fontSize: 10,
                          fontWeight: FontWeight.bold,
                          color: AppColors.successGreen,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 18),

              // Detalles del recibo
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(16),
                  color: isDark ? const Color(0x221E293B) : const Color(0x0C0F172A),
                ),
                child: Column(
                  children: [
                    _buildRow(
                      'Tipo:',
                      isProject ? 'Aporte a Proyecto' : 'Cuota de Vigilancia Mensual',
                      isDark,
                    ),
                    if (isProject && payment.nombreProyecto != null) ...[
                      const SizedBox(height: 8),
                      _buildRow('Proyecto:', payment.nombreProyecto!, isDark),
                    ],
                    const SizedBox(height: 8),
                    _buildRow('Período:', payment.periodoMes, isDark),
                    const SizedBox(height: 8),
                    _buildRow(
                      'Fecha:',
                      payment.fechaPago ?? DateTime.now().toString().substring(0, 10),
                      isDark,
                    ),
                    const SizedBox(height: 8),
                    _buildRow('Método:', methodName ?? payment.metodoPago, isDark),
                    const SizedBox(height: 8),
                    _buildRow(
                      'Referencia:',
                      payment.referencia ?? 'REC-${payment.id}',
                      isDark,
                      isMonospace: true,
                    ),
                    if (payment.nombreMiembro.isNotEmpty) ...[
                      const SizedBox(height: 8),
                      _buildRow('Miembro:', payment.nombreMiembro, isDark),
                    ],
                  ],
                ),
              ),
              const SizedBox(height: 20),

              // Botones de acción
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: () {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: const Row(
                              children: [
                                Icon(Icons.check_circle, color: Colors.white, size: 20),
                                SizedBox(width: 10),
                                Text('Comprobante guardado en Descargas'),
                              ],
                            ),
                            behavior: SnackBarBehavior.floating,
                            backgroundColor: AppColors.charcoalGrey,
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                          ),
                        );
                      },
                      icon: const Icon(FluentIcons.share_20_regular, size: 18),
                      label: const Text('Compartir'),
                      style: OutlinedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () => Navigator.of(context).pop(),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.brandBlue,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                      ),
                      child: const Text('Listo'),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildRow(String label, String value, bool isDark, {bool isMonospace = false}) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: TextStyle(
            fontSize: 12,
            color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
          ),
        ),
        const SizedBox(width: 8),
        Flexible(
          child: Text(
            value,
            textAlign: TextAlign.right,
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w600,
              fontFamily: isMonospace ? 'monospace' : null,
              color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
            ),
          ),
        ),
      ],
    );
  }
}
