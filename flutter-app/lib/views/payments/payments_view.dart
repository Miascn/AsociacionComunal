import 'package:flutter/material.dart';
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/bouncy_tap.dart';
import '../../core/widgets/empty_state.dart';
import '../../core/widgets/fade_slide_entrance.dart';
import '../../core/widgets/glass_container.dart';
import '../../core/widgets/neo_glass_container.dart';
import '../../core/widgets/status_badge.dart';
import '../../data/models/payment_model.dart';
import '../../viewmodels/payments_viewmodel.dart';

/// Pantalla de Pagos y Aportaciones con filtros e historial detallado.
class PaymentsView extends StatefulWidget {
  final PaymentsViewModel viewModel;
  final int? idMiembro;

  const PaymentsView({
    super.key,
    required this.viewModel,
    this.idMiembro,
  });

  @override
  State<PaymentsView> createState() => _PaymentsViewState();
}

class _PaymentsViewState extends State<PaymentsView> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      widget.viewModel.loadPayments(idMiembro: widget.idMiembro);
    });
  }

  void _showPayVigilanciaSheet() {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    String selectedMethod = 'TRANSFERENCIA';
    final refController = TextEditingController();
    bool isSubmitting = false;

    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (sheetContext) => StatefulBuilder(
        builder: (context, setSheetState) {
          return Padding(
            padding: EdgeInsets.only(
              bottom: MediaQuery.of(context).viewInsets.bottom,
            ),
            child: NeoGlassContainer(
              borderRadius: const BorderRadius.vertical(top: Radius.circular(28)),
              padding: const EdgeInsets.fromLTRB(24, 16, 24, 32),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Center(
                    child: Container(
                      width: 40,
                      height: 4,
                      decoration: BoxDecoration(
                        color: Colors.grey.withValues(alpha: 0.3),
                        borderRadius: BorderRadius.circular(2),
                      ),
                    ),
                  ),
                  const SizedBox(height: 18),
                  Row(
                    children: [
                      NeoGlassContainer.circle(
                        size: 44,
                        accentColor: AppColors.brandBlue.withValues(alpha: 0.12),
                        child: const Icon(FluentIcons.shield_checkmark_24_filled, color: AppColors.brandBlue, size: 22),
                      ),
                      const SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Cuota Mensual de Vigilancia',
                              style: TextStyle(
                                fontSize: 17,
                                fontWeight: FontWeight.bold,
                                color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                              ),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              'Pago obligatorio de seguridad comunal',
                              style: TextStyle(
                                fontSize: 12,
                                color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 20),

                  // Período y Monto
                  Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(16),
                      color: isDark ? const Color(0x301E293B) : const Color(0x0A0F172A),
                      border: Border.all(
                        color: isDark ? const Color(0x20FFFFFF) : const Color(0x140F172A),
                      ),
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'PERÍODO',
                              style: TextStyle(
                                fontSize: 11,
                                fontWeight: FontWeight.w600,
                                color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              widget.viewModel.currentPeriod,
                              style: TextStyle(
                                fontSize: 15,
                                fontWeight: FontWeight.bold,
                                color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                              ),
                            ),
                          ],
                        ),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [
                            Text(
                              'MONTO OBLIGATORIO',
                              style: TextStyle(
                                fontSize: 11,
                                fontWeight: FontWeight.w600,
                                color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              Formatters.currency(PaymentsViewModel.cuotaVigilanciaMensual),
                              style: const TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.bold,
                                color: AppColors.brandBlue,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),

                  // Método de Pago
                  Text(
                    'Método de pago',
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      Expanded(
                        child: BouncyTap(
                          scaleDown: 0.96,
                          onTap: () => setSheetState(() => selectedMethod = 'TRANSFERENCIA'),
                          child: Container(
                            padding: const EdgeInsets.symmetric(vertical: 12),
                            decoration: BoxDecoration(
                              borderRadius: BorderRadius.circular(14),
                              color: selectedMethod == 'TRANSFERENCIA'
                                  ? (isDark ? const Color(0xFF1E293B) : AppColors.brandBlue)
                                  : (isDark ? const Color(0x201E293B) : const Color(0x0A0F172A)),
                              border: Border.all(
                                color: selectedMethod == 'TRANSFERENCIA'
                                    ? AppColors.brandBlue
                                    : (isDark ? const Color(0x20FFFFFF) : const Color(0x140F172A)),
                              ),
                            ),
                            child: Center(
                              child: Text(
                                'Transferencia',
                                style: TextStyle(
                                  fontSize: 12.5,
                                  fontWeight: selectedMethod == 'TRANSFERENCIA' ? FontWeight.bold : FontWeight.normal,
                                  color: selectedMethod == 'TRANSFERENCIA'
                                      ? Colors.white
                                      : (isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight),
                                ),
                              ),
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: BouncyTap(
                          scaleDown: 0.96,
                          onTap: () => setSheetState(() => selectedMethod = 'EFECTIVO'),
                          child: Container(
                            padding: const EdgeInsets.symmetric(vertical: 12),
                            decoration: BoxDecoration(
                              borderRadius: BorderRadius.circular(14),
                              color: selectedMethod == 'EFECTIVO'
                                  ? (isDark ? const Color(0xFF1E293B) : AppColors.brandBlue)
                                  : (isDark ? const Color(0x201E293B) : const Color(0x0A0F172A)),
                              border: Border.all(
                                color: selectedMethod == 'EFECTIVO'
                                    ? AppColors.brandBlue
                                    : (isDark ? const Color(0x20FFFFFF) : const Color(0x140F172A)),
                              ),
                            ),
                            child: Center(
                              child: Text(
                                'Efectivo',
                                style: TextStyle(
                                  fontSize: 12.5,
                                  fontWeight: selectedMethod == 'EFECTIVO' ? FontWeight.bold : FontWeight.normal,
                                  color: selectedMethod == 'EFECTIVO'
                                      ? Colors.white
                                      : (isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight),
                                ),
                              ),
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),

                  // Referencia / Comprobante
                  Text(
                    'Comprobante o referencia (opcional)',
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                    ),
                  ),
                  const SizedBox(height: 8),
                  TextField(
                    controller: refController,
                    decoration: InputDecoration(
                      hintText: selectedMethod == 'TRANSFERENCIA'
                          ? 'Ej. Banco Agrícola #49281'
                          : 'Ej. Entregado a tesorero en asamblea',
                      filled: true,
                      fillColor: isDark ? const Color(0x251E293B) : const Color(0x0A0F172A),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(14),
                        borderSide: BorderSide(
                          color: isDark ? const Color(0x20FFFFFF) : const Color(0x180F172A),
                        ),
                      ),
                      enabledBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(14),
                        borderSide: BorderSide(
                          color: isDark ? const Color(0x20FFFFFF) : const Color(0x180F172A),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 24),

                  // Botón Confirmar
                  SizedBox(
                    width: double.infinity,
                    height: 50,
                    child: BouncyTap(
                      scaleDown: 0.98,
                      onTap: isSubmitting
                          ? null
                          : () async {
                              final idM = widget.idMiembro ?? 1;
                              setSheetState(() => isSubmitting = true);
                              try {
                                await widget.viewModel.registerPayment(
                                  idMiembro: idM,
                                  periodoMes: widget.viewModel.currentPeriod,
                                  monto: PaymentsViewModel.cuotaVigilanciaMensual,
                                  metodoPago: selectedMethod,
                                  referencia: refController.text.trim().isNotEmpty
                                      ? refController.text.trim()
                                      : 'Pago vigilancia ${widget.viewModel.currentPeriod}',
                                );
                                if (context.mounted) {
                                  Navigator.pop(context);
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    const SnackBar(
                                      content: Text('¡Pago de cuota de vigilancia registrado correctamente!'),
                                      backgroundColor: AppColors.neoEmerald,
                                    ),
                                  );
                                }
                              } catch (e) {
                                setSheetState(() => isSubmitting = false);
                                if (context.mounted) {
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    SnackBar(
                                      content: Text('Error: ${e.toString().replaceAll("Exception: ", "")}'),
                                      backgroundColor: AppColors.errorRed,
                                    ),
                                  );
                                }
                              }
                            },
                      child: Container(
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(16),
                          gradient: AppColors.brandGradient,
                          boxShadow: [
                            BoxShadow(
                              color: AppColors.brandBlue.withValues(alpha: 0.35),
                              blurRadius: 14,
                              offset: const Offset(0, 5),
                            ),
                          ],
                        ),
                        child: Center(
                          child: isSubmitting
                              ? const SizedBox(
                                  width: 22,
                                  height: 22,
                                  child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                                )
                              : const Row(
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  children: [
                                    Icon(FluentIcons.send_24_filled, color: Colors.white, size: 20),
                                    SizedBox(width: 8),
                                    Text(
                                      'Confirmar Pago de Vigilancia',
                                      style: TextStyle(
                                        color: Colors.white,
                                        fontSize: 15,
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                  ],
                                ),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  void _showPaymentDetails(PaymentModel payment) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (ctx) {
        final isDark = Theme.of(ctx).brightness == Brightness.dark;

        return NeoGlassContainer(
          borderRadius: const BorderRadius.vertical(top: Radius.circular(28)),
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(
                    color: Colors.grey.withValues(alpha: 0.3),
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 18),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Detalle de Aportación',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                    ),
                  ),
                  StatusBadge.fromStatus(payment.estado),
                ],
              ),
              const SizedBox(height: 20),
              _buildDetailRow('Concepto / Proyecto', payment.nombreProyecto ?? 'Cuota Mensual Comunal', isDark),
              _buildDetailRow('Período', payment.periodoMes, isDark),
              _buildDetailRow('Monto', Formatters.currency(payment.monto), isDark, isBold: true),
              _buildDetailRow('Fecha de Pago', Formatters.date(payment.fechaPago), isDark),
              _buildDetailRow('Método de Pago', payment.metodoPago, isDark),
              if (payment.referencia != null && payment.referencia!.isNotEmpty)
                _buildDetailRow('Referencia', payment.referencia!, isDark),
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: () => Navigator.pop(ctx),
                  child: const Text('Cerrar'),
                ),
              ),
              const SizedBox(height: 12),
            ],
          ),
        );
      },
    );
  }

  Widget _buildDetailRow(String title, String value, bool isDark, {bool isBold = false}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            title,
            style: TextStyle(
              fontSize: 13,
              color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
            ),
          ),
          Text(
            value,
            style: TextStyle(
              fontSize: 14,
              fontWeight: isBold ? FontWeight.bold : FontWeight.w600,
              color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return AmbientBackground(
      child: RefreshIndicator(
        onRefresh: () => widget.viewModel.loadPayments(idMiembro: widget.idMiembro),
        color: AppColors.brandBlue,
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 110),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              FadeSlideEntrance(
                delay: Duration.zero,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Pagos y Cuotas',
                      style: TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                        color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Historial financiero y aportaciones a la asociación',
                      style: TextStyle(
                        fontSize: 13,
                        color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),

              // Resumen de Saldo con NeoGlassContainer y BouncyTap
              FadeSlideEntrance(
                delay: const Duration(milliseconds: 90),
                child: ListenableBuilder(
                  listenable: widget.viewModel,
                  builder: (context, _) {
                    return Row(
                      children: [
                        Expanded(
                          child: BouncyTap(
                            scaleDown: 0.96,
                            child: NeoGlassContainer(
                              padding: const EdgeInsets.all(16),
                              borderRadius: BorderRadius.circular(22),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                    children: [
                                      Text(
                                        'Total Pagado',
                                        style: TextStyle(
                                          fontSize: 11.5,
                                          fontWeight: FontWeight.w600,
                                          color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                                        ),
                                      ),
                                      NeoGlassContainer.circle(
                                        size: 28,
                                        child: const Icon(FluentIcons.checkmark_circle_24_regular, size: 16, color: AppColors.neoEmerald),
                                      ),
                                    ],
                                  ),
                                  const SizedBox(height: 10),
                                  Text(
                                    Formatters.currency(widget.viewModel.totalPaid),
                                    style: TextStyle(
                                      fontSize: 20,
                                      fontWeight: FontWeight.bold,
                                      color: isDark ? Colors.white : AppColors.textPrimaryLight,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: BouncyTap(
                            scaleDown: 0.96,
                            child: NeoGlassContainer(
                              padding: const EdgeInsets.all(16),
                              borderRadius: BorderRadius.circular(22),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                    children: [
                                      Text(
                                        'Pendiente',
                                        style: TextStyle(
                                          fontSize: 11.5,
                                          fontWeight: FontWeight.w600,
                                          color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                                        ),
                                      ),
                                      NeoGlassContainer.circle(
                                        size: 28,
                                        child: const Icon(FluentIcons.clock_24_regular, size: 16, color: AppColors.warningAmber),
                                      ),
                                    ],
                                  ),
                                  const SizedBox(height: 10),
                                  Text(
                                    Formatters.currency(widget.viewModel.totalPending),
                                    style: TextStyle(
                                      fontSize: 20,
                                      fontWeight: FontWeight.bold,
                                      color: widget.viewModel.totalPending > 0
                                          ? AppColors.warningAmber
                                          : (isDark ? Colors.white : AppColors.textPrimaryLight),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                      ],
                    );
                  },
                ),
              ),
              const SizedBox(height: 16),

              // Tarjeta de Estado y Pago: Cuota Mensual de Vigilancia (Obligatorio)
              FadeSlideEntrance(
                delay: const Duration(milliseconds: 140),
                child: ListenableBuilder(
                  listenable: widget.viewModel,
                  builder: (context, _) {
                    final isPaid = widget.viewModel.isCurrentMonthPaid;

                    return BouncyTap(
                      scaleDown: 0.98,
                      onTap: isPaid ? null : _showPayVigilanciaSheet,
                      child: NeoGlassContainer(
                        padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
                        borderRadius: BorderRadius.circular(20),
                        accentColor: isPaid
                            ? AppColors.neoEmerald.withValues(alpha: 0.08)
                            : AppColors.brandBlue.withValues(alpha: 0.08),
                        child: Row(
                          children: [
                            NeoGlassContainer.circle(
                              size: 44,
                              accentColor: isPaid
                                  ? AppColors.neoEmerald.withValues(alpha: 0.15)
                                  : AppColors.brandBlue.withValues(alpha: 0.15),
                              child: Icon(
                                isPaid
                                    ? FluentIcons.shield_checkmark_24_filled
                                    : FluentIcons.shield_dismiss_24_filled,
                                color: isPaid ? AppColors.neoEmerald : AppColors.brandBlue,
                                size: 22,
                              ),
                            ),
                            const SizedBox(width: 14),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    children: [
                                      Text(
                                        'Vigilancia ${widget.viewModel.currentPeriod}',
                                        style: TextStyle(
                                          fontSize: 14.5,
                                          fontWeight: FontWeight.bold,
                                          color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                                        ),
                                      ),
                                      const SizedBox(width: 6),
                                      Container(
                                        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                        decoration: BoxDecoration(
                                          borderRadius: BorderRadius.circular(6),
                                          color: isPaid
                                              ? AppColors.neoEmerald.withValues(alpha: 0.15)
                                              : AppColors.warningAmber.withValues(alpha: 0.15),
                                        ),
                                        child: Text(
                                          isPaid ? 'AL DÍA' : 'OBLIGATORIO',
                                          style: TextStyle(
                                            fontSize: 9.5,
                                            fontWeight: FontWeight.bold,
                                            color: isPaid ? AppColors.neoEmerald : AppColors.warningAmber,
                                          ),
                                        ),
                                      ),
                                    ],
                                  ),
                                  const SizedBox(height: 3),
                                  Text(
                                    isPaid
                                        ? 'Cuota de seguridad comunal cubierta.'
                                        : 'Toca para registrar tu pago (\$10.00 / mes).',
                                    style: TextStyle(
                                      fontSize: 12,
                                      color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            if (!isPaid)
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(12),
                                  gradient: AppColors.brandGradient,
                                  boxShadow: [
                                    BoxShadow(
                                      color: AppColors.brandBlue.withValues(alpha: 0.3),
                                      blurRadius: 8,
                                      offset: const Offset(0, 3),
                                    ),
                                  ],
                                ),
                                child: const Text(
                                  'Pagar',
                                  style: TextStyle(
                                    color: Colors.white,
                                    fontSize: 12.5,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              ),
                          ],
                        ),
                      ),
                    );
                  },
                ),
              ),
              const SizedBox(height: 16),

              // Filtros de estado Neo-Glass (Selector unificado)
              FadeSlideEntrance(
                delay: const Duration(milliseconds: 180),
                child: ListenableBuilder(
                  listenable: widget.viewModel,
                  builder: (context, _) {
                    final currentFilter = widget.viewModel.filter;
                    final filters = ['TODOS', 'PAGADOS', 'PENDIENTES'];

                    return NeoGlassContainer(
                      padding: const EdgeInsets.all(5),
                      borderRadius: BorderRadius.circular(20),
                      child: Row(
                        children: filters.map((f) {
                          final isSelected = currentFilter == f;
                          return Expanded(
                            child: BouncyTap(
                              scaleDown: 0.94,
                              onTap: () => widget.viewModel.setFilter(f),
                              child: AnimatedContainer(
                                duration: const Duration(milliseconds: 200),
                                padding: const EdgeInsets.symmetric(vertical: 9),
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(16),
                                  color: isSelected
                                      ? (isDark ? const Color(0xFF1E293B) : AppColors.brandBlue)
                                      : Colors.transparent,
                                ),
                                child: Center(
                                  child: Text(
                                    f,
                                    style: TextStyle(
                                      fontSize: 11,
                                      fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
                                      color: isSelected
                                          ? Colors.white
                                          : (isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight),
                                    ),
                                  ),
                                ),
                              ),
                            ),
                          );
                        }).toList(),
                      ),
                    );
                  },
                ),
              ),
              const SizedBox(height: 18),

              // Lista de Pagos con NeoGlassContainer y BouncyTap
              FadeSlideEntrance(
                delay: const Duration(milliseconds: 270),
                child: ListenableBuilder(
                  listenable: widget.viewModel,
                  builder: (context, _) {
                    if (widget.viewModel.isLoading) {
                      return const Center(
                        child: Padding(
                          padding: EdgeInsets.symmetric(vertical: 40),
                          child: CircularProgressIndicator(),
                        ),
                      );
                    }

                    final list = widget.viewModel.filteredPayments;
                    if (list.isEmpty) {
                      return EmptyStateWidget(
                        icon: FluentIcons.receipt_24_regular,
                        title: 'No hay aportaciones registradas',
                        message: 'Tus pagos aparecerán aquí una vez registrados en tesorería.',
                        actionLabel: 'Actualizar',
                        onAction: () => widget.viewModel.loadPayments(idMiembro: widget.idMiembro),
                      );
                    }

                    return ListView.separated(
                      shrinkWrap: true,
                      physics: const NeverScrollableScrollPhysics(),
                      itemCount: list.length,
                      separatorBuilder: (_, __) => const SizedBox(height: 10),
                      itemBuilder: (context, index) {
                        final payment = list[index];
                        return BouncyTap(
                          scaleDown: 0.97,
                          onTap: () => _showPaymentDetails(payment),
                          child: NeoGlassContainer(
                            padding: const EdgeInsets.all(14),
                            borderRadius: BorderRadius.circular(18),
                            child: Row(
                              children: [
                                NeoGlassContainer.circle(
                                  size: 42,
                                  child: Icon(
                                    payment.isPaid
                                        ? FluentIcons.checkmark_circle_24_regular
                                        : FluentIcons.clock_24_regular,
                                    color: payment.isPaid ? AppColors.neoEmerald : AppColors.warningAmber,
                                    size: 22,
                                  ),
                                ),
                                const SizedBox(width: 14),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        payment.nombreProyecto ?? 'Cuota Mensual Comunal',
                                        style: TextStyle(
                                          fontWeight: FontWeight.bold,
                                          fontSize: 14,
                                          color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                                        ),
                                      ),
                                      const SizedBox(height: 2),
                                      Text(
                                        '${payment.periodoMes} • ${Formatters.date(payment.fechaPago)}',
                                        style: TextStyle(
                                          fontSize: 12,
                                          color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                                Column(
                                  crossAxisAlignment: CrossAxisAlignment.end,
                                  children: [
                                    Text(
                                      Formatters.currency(payment.monto),
                                      style: TextStyle(
                                        fontWeight: FontWeight.bold,
                                        fontSize: 15,
                                        color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                                      ),
                                    ),
                                    const SizedBox(height: 4),
                                    StatusBadge.fromStatus(payment.estado),
                                  ],
                                ),
                              ],
                            ),
                          ),
                        );
                      },
                    );
                  },
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
