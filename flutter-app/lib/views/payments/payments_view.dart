import 'package:flutter/material.dart';
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/glass_container.dart';
import '../../core/widgets/bouncy_tap.dart';
import '../../core/widgets/empty_state.dart';
import '../../core/widgets/fade_slide_entrance.dart';
import '../../core/widgets/neo_glass_container.dart';
import '../../core/widgets/status_badge.dart';
import '../../data/models/payment_model.dart';
import '../../viewmodels/payments_viewmodel.dart';
import 'payment_receipt_dialog.dart';
import 'simulated_payment_sheet.dart';

/// Pantalla de Pagos y Aportaciones con filtros avanzados, pasarela simulada y recibos oficiales.
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
    SimulatedPaymentSheet.show(
      context,
      viewModel: widget.viewModel,
      idMiembro: widget.idMiembro ?? 1,
      title: 'Cuota Mensual de Vigilancia',
      description: 'Seguridad y mantenimiento de la colonia (${widget.viewModel.currentPeriod})',
      defaultAmount: PaymentsViewModel.cuotaVigilanciaMensual,
      isAmountEditable: false,
      periodoMes: widget.viewModel.currentPeriod,
      onPaymentSuccess: (p) {
        widget.viewModel.loadPayments(idMiembro: widget.idMiembro);
      },
    );
  }

  void _showPayProjectContributionSheet() {
    SimulatedPaymentSheet.show(
      context,
      viewModel: widget.viewModel,
      idMiembro: widget.idMiembro ?? 1,
      title: 'Aporte Voluntario a Proyecto Comunal',
      description: 'Contribución solidaria para el desarrollo de obras (${widget.viewModel.currentPeriod})',
      defaultAmount: 25.00,
      isAmountEditable: true,
      periodoMes: widget.viewModel.currentPeriod,
      onPaymentSuccess: (p) {
        widget.viewModel.loadPayments(idMiembro: widget.idMiembro);
      },
    );
  }

  void _showPaymentDetails(PaymentModel payment) {
    PaymentReceiptDialog.show(
      context,
      payment: payment,
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
              // Encabezado
              FadeSlideEntrance(
                delay: Duration.zero,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Pagos y Aportaciones',
                      style: TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                        color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Gestión de cuotas de vigilancia y aportes para proyectos comunitarios',
                      style: TextStyle(
                        fontSize: 13,
                        color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 18),

              // Tarjetas de Resumen Financiero
              FadeSlideEntrance(
                delay: const Duration(milliseconds: 90),
                child: ListenableBuilder(
                  listenable: widget.viewModel,
                  builder: (context, _) {
                    return Row(
                      children: [
                        Expanded(
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
                                      'Total Aportado',
                                      style: TextStyle(
                                        fontSize: 11.5,
                                        fontWeight: FontWeight.w600,
                                        color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                                      ),
                                    ),
                                    NeoGlassContainer.circle(
                                      size: 28,
                                      child: const Icon(
                                        FluentIcons.checkmark_circle_24_regular,
                                        size: 16,
                                        color: AppColors.neoEmerald,
                                      ),
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
                        const SizedBox(width: 12),
                        Expanded(
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
                                      'Cuota Pendiente',
                                      style: TextStyle(
                                        fontSize: 11.5,
                                        fontWeight: FontWeight.w600,
                                        color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                                      ),
                                    ),
                                    NeoGlassContainer.circle(
                                      size: 28,
                                      child: const Icon(
                                        FluentIcons.clock_24_regular,
                                        size: 16,
                                        color: AppColors.warningAmber,
                                      ),
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
                      ],
                    );
                  },
                ),
              ),
              const SizedBox(height: 16),

              // Tarjeta de Cuota Mensual de Vigilancia
              FadeSlideEntrance(
                delay: const Duration(milliseconds: 140),
                child: ListenableBuilder(
                  listenable: widget.viewModel,
                  builder: (context, _) {
                    final isPaid = widget.viewModel.isCurrentMonthPaid;

                    return NeoGlassContainer(
                      padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 16),
                      borderRadius: BorderRadius.circular(22),
                      accentColor: isPaid
                          ? AppColors.neoEmerald.withValues(alpha: 0.08)
                          : AppColors.brandBlue.withValues(alpha: 0.08),
                      child: Column(
                        children: [
                          Row(
                            children: [
                              NeoGlassContainer.circle(
                                size: 46,
                                accentColor: isPaid
                                    ? AppColors.neoEmerald.withValues(alpha: 0.15)
                                    : AppColors.brandBlue.withValues(alpha: 0.15),
                                child: Icon(
                                  isPaid
                                      ? FluentIcons.shield_checkmark_24_filled
                                      : FluentIcons.shield_dismiss_24_filled,
                                  color: isPaid ? AppColors.neoEmerald : AppColors.brandBlue,
                                  size: 24,
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
                                            fontSize: 15,
                                            fontWeight: FontWeight.bold,
                                            color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                                          ),
                                        ),
                                        const SizedBox(width: 8),
                                        Container(
                                          padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2.5),
                                          decoration: BoxDecoration(
                                            borderRadius: BorderRadius.circular(6),
                                            color: isPaid
                                                ? AppColors.neoEmerald.withValues(alpha: 0.15)
                                                : AppColors.warningAmber.withValues(alpha: 0.15),
                                          ),
                                          child: Text(
                                            isPaid ? 'PAGADA' : 'PENDIENTE',
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
                                          ? 'Cuota de \$10.00 cubierta para este período.'
                                          : 'Cuota obligatoria de seguridad: \$10.00 / mes.',
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
                          if (!isPaid) ...[
                            const SizedBox(height: 14),
                            BouncyTap(
                              onTap: _showPayVigilanciaSheet,
                              child: Container(
                                width: double.infinity,
                                height: 44,
                                decoration: BoxDecoration(
                                  gradient: const LinearGradient(
                                    colors: [AppColors.brandBlue, AppColors.electricIndigo],
                                  ),
                                  borderRadius: BorderRadius.circular(14),
                                  boxShadow: [
                                    BoxShadow(
                                      color: AppColors.brandBlue.withValues(alpha: 0.3),
                                      blurRadius: 10,
                                      offset: const Offset(0, 4),
                                    ),
                                  ],
                                ),
                                child: const Row(
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  children: [
                                    Icon(FluentIcons.payment_20_filled, color: Colors.white, size: 18),
                                    SizedBox(width: 8),
                                    Text(
                                      'Pagar Cuota Mensual (\$10.00)',
                                      style: TextStyle(
                                        color: Colors.white,
                                        fontWeight: FontWeight.bold,
                                        fontSize: 13.5,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ],
                        ],
                      ),
                    );
                  },
                ),
              ),
              const SizedBox(height: 14),

              // Botón de Aporte Voluntario a Proyecto
              FadeSlideEntrance(
                delay: const Duration(milliseconds: 170),
                child: BouncyTap(
                  onTap: _showPayProjectContributionSheet,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                    decoration: BoxDecoration(
                      color: isDark ? const Color(0x221E293B) : const Color(0x0C0F172A),
                      borderRadius: BorderRadius.circular(18),
                      border: Border.all(
                        color: AppColors.electricIndigo.withValues(alpha: 0.3),
                      ),
                    ),
                    child: Row(
                      children: [
                        NeoGlassContainer.circle(
                          size: 38,
                          accentColor: AppColors.electricIndigo.withValues(alpha: 0.15),
                          child: const Icon(
                            FluentIcons.handshake_24_regular,
                            color: AppColors.electricIndigo,
                            size: 20,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Aportar a un Proyecto Comunal',
                                style: TextStyle(
                                  fontSize: 13.5,
                                  fontWeight: FontWeight.bold,
                                  color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                                ),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                'Realiza aportes solidarios para obras aprobadas',
                                style: TextStyle(
                                  fontSize: 11.5,
                                  color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                                ),
                              ),
                            ],
                          ),
                        ),
                        const Icon(
                          FluentIcons.chevron_right_20_regular,
                          size: 16,
                          color: AppColors.electricIndigo,
                        ),
                      ],
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 18),

              // Filtros de navegación
              FadeSlideEntrance(
                delay: const Duration(milliseconds: 200),
                child: ListenableBuilder(
                  listenable: widget.viewModel,
                  builder: (context, _) {
                    final currentFilter = widget.viewModel.filter;
                    final filters = [
                      {'key': 'TODOS', 'label': 'Todos'},
                      {'key': 'CUOTAS', 'label': 'Cuotas'},
                      {'key': 'PROYECTOS', 'label': 'Proyectos'},
                      {'key': 'PAGADOS', 'label': 'Pagadas'},
                    ];

                    return SingleChildScrollView(
                      scrollDirection: Axis.horizontal,
                      child: Row(
                        children: filters.map((f) {
                          final key = f['key']!;
                          final label = f['label']!;
                          final isSelected = currentFilter == key;

                          return Padding(
                            padding: const EdgeInsets.only(right: 8),
                            child: BouncyTap(
                              onTap: () => widget.viewModel.setFilter(key),
                              child: AnimatedContainer(
                                duration: const Duration(milliseconds: 200),
                                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(14),
                                  color: isSelected
                                      ? (isDark ? const Color(0xFF1E3A8A) : AppColors.brandBlue)
                                      : (isDark ? const Color(0x221E293B) : const Color(0x0C0F172A)),
                                  border: Border.all(
                                    color: isSelected
                                        ? AppColors.brandBlue
                                        : (isDark ? const Color(0x20FFFFFF) : const Color(0x140F172A)),
                                  ),
                                ),
                                child: Text(
                                  label,
                                  style: TextStyle(
                                    fontSize: 12,
                                    fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
                                    color: isSelected
                                        ? Colors.white
                                        : (isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight),
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
              const SizedBox(height: 16),

              // Lista de Pagos
              FadeSlideEntrance(
                delay: const Duration(milliseconds: 240),
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
                      if (widget.viewModel.errorMessage != null) {
                        return EmptyStateWidget(
                          icon: FluentIcons.warning_24_regular,
                          title: 'No se pudieron cargar las aportaciones',
                          message: widget.viewModel.errorMessage!,
                          actionLabel: 'Reintentar',
                          onAction: () => widget.viewModel.loadPayments(idMiembro: widget.idMiembro),
                        );
                      }
                      return EmptyStateWidget(
                        icon: FluentIcons.receipt_24_regular,
                        title: 'No hay aportaciones registradas',
                        message: 'Tus cuotas y aportes registrados aparecerán aquí con su recibo oficial.',
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
                        final isProject = payment.isProjectContribution;

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
                                  accentColor: isProject
                                      ? AppColors.electricIndigo.withValues(alpha: 0.15)
                                      : AppColors.brandBlue.withValues(alpha: 0.15),
                                  child: Icon(
                                    isProject
                                        ? FluentIcons.handshake_24_filled
                                        : FluentIcons.shield_checkmark_24_filled,
                                    color: isProject ? AppColors.electricIndigo : AppColors.brandBlue,
                                    size: 20,
                                  ),
                                ),
                                const SizedBox(width: 14),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Row(
                                        children: [
                                          Container(
                                            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                            decoration: BoxDecoration(
                                              color: (isProject ? AppColors.electricIndigo : AppColors.brandBlue)
                                                  .withValues(alpha: 0.12),
                                              borderRadius: BorderRadius.circular(5),
                                            ),
                                            child: Text(
                                              payment.tipoEtiqueta.toUpperCase(),
                                              style: TextStyle(
                                                fontSize: 9,
                                                fontWeight: FontWeight.bold,
                                                color: isProject ? AppColors.electricIndigo : AppColors.brandBlue,
                                              ),
                                            ),
                                          ),
                                          const SizedBox(width: 6),
                                          Text(
                                            payment.periodoMes,
                                            style: TextStyle(
                                              fontSize: 11,
                                              color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                                            ),
                                          ),
                                        ],
                                      ),
                                      const SizedBox(height: 4),
                                      Text(
                                        isProject
                                            ? (payment.nombreProyecto ?? 'Proyecto Comunal')
                                            : 'Cuota Mensual de Vigilancia',
                                        style: TextStyle(
                                          fontWeight: FontWeight.bold,
                                          fontSize: 13.5,
                                          color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                                        ),
                                      ),
                                      const SizedBox(height: 2),
                                      Text(
                                        'Ref: ${payment.referencia ?? "SIM-${payment.id}"}',
                                        style: TextStyle(
                                          fontSize: 11,
                                          fontFamily: 'monospace',
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
                                        fontWeight: FontWeight.w900,
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
