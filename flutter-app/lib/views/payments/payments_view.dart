import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/bouncy_tap.dart';
import '../../core/widgets/community_top_header.dart';
import '../../core/widgets/empty_state.dart';
import '../../core/widgets/fade_slide_entrance.dart';
import '../../data/models/payment_model.dart';
import '../../viewmodels/payments_viewmodel.dart';
import 'payment_receipt_dialog.dart';
import 'simulated_payment_sheet.dart';

/// Pantalla de Pagos y Contribuciones
/// Replicando con máxima fidelidad la interfaz Stitch (Colonia Conecta).
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
  int _selectedFilterIndex = 0; // 0: Todos, 1: Cuotas, 2: Proyectos

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

  void _showPaymentDetails(PaymentModel payment) {
    PaymentReceiptDialog.show(
      context,
      payment: payment,
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      backgroundColor: isDark ? const Color(0xFF0F172A) : AppColors.stitchCanvasLight,
      body: SafeArea(
        bottom: false,
        child: RefreshIndicator(
          onRefresh: () => widget.viewModel.loadPayments(idMiembro: widget.idMiembro),
          color: AppColors.stitchSapphire,
          child: SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 110),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // 1. Barra Superior Estandarizada
                const FadeSlideEntrance(
                  delay: Duration.zero,
                  child: CommunityTopHeader(
                    title: 'Pagos',
                    subtitle: 'Residencial Las Flores',
                    residentName: 'Carlos Mendoza',
                  ),
                ),
                const SizedBox(height: 12),

                // 2. Encabezado Financiero
                FadeSlideEntrance(
                  delay: const Duration(milliseconds: 40),
                  child: _buildFinancialHeader(isDark),
                ),
                const SizedBox(height: 16),

                // 3. Grid Fintech de Métricas Financieras
                FadeSlideEntrance(
                  delay: const Duration(milliseconds: 80),
                  child: _buildFintechMetricsGrid(isDark),
                ),
                const SizedBox(height: 16),

                // 4. Banner Prompt de Cuota Pendiente
                ListenableBuilder(
                  listenable: widget.viewModel,
                  builder: (context, _) {
                    if (!widget.viewModel.hasPendingCuota) {
                      return const SizedBox.shrink();
                    }
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 16),
                      child: FadeSlideEntrance(
                        delay: const Duration(milliseconds: 120),
                        child: _buildPendingPromptBanner(isDark),
                      ),
                    );
                  },
                ),

                // 5. Sección de Movimientos
                FadeSlideEntrance(
                  delay: const Duration(milliseconds: 160),
                  child: _buildMovementsSection(isDark),
                ),
                const SizedBox(height: 24),

                // 6. Botón Descargar Estado de Cuenta
                FadeSlideEntrance(
                  delay: const Duration(milliseconds: 200),
                  child: _buildDownloadAccountStatementButton(isDark),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  /// 2. Encabezado Financiero
  Widget _buildFinancialHeader(bool isDark) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
              decoration: BoxDecoration(
                color: const Color(0xFFD1FAE5),
                borderRadius: BorderRadius.circular(20),
              ),
              child: const Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.account_balance_wallet_rounded, size: 14, color: Color(0xFF065F46)),
                  SizedBox(width: 5),
                  Text(
                    'RESUMEN DE CUENTA',
                    style: TextStyle(
                      fontSize: 10,
                      fontWeight: FontWeight.w800,
                      letterSpacing: 0.6,
                      color: Color(0xFF065F46),
                    ),
                  ),
                ],
              ),
            ),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3.5),
              decoration: BoxDecoration(
                color: isDark ? const Color(0xFF1E293B) : const Color(0xFFEEF2FF),
                borderRadius: BorderRadius.circular(16),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Container(
                    width: 6,
                    height: 6,
                    decoration: const BoxDecoration(
                      shape: BoxShape.circle,
                      color: AppColors.stitchEmerald,
                    ),
                  ),
                  const SizedBox(width: 5),
                  Text(
                    widget.viewModel.hasPendingCuota ? 'Al día (1 pendiente)' : 'Solvente',
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                      color: isDark ? Colors.white70 : AppColors.stitchTextSecondary,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
        Text(
          'Mis Pagos y Contribuciones',
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.w800,
            letterSpacing: -0.5,
            color: isDark ? Colors.white : AppColors.stitchTextPrimary,
          ),
        ),
        const SizedBox(height: 4),
        Row(
          children: [
            const Icon(Icons.cottage_outlined, size: 16, color: AppColors.stitchTextSecondary),
            const SizedBox(width: 6),
            Text(
              'Casa #42-B · Carlos Mendoza',
              style: TextStyle(
                fontSize: 13,
                color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
              ),
            ),
          ],
        ),
      ],
    );
  }

  /// 3. Grid Fintech de Métricas Financieras
  Widget _buildFintechMetricsGrid(bool isDark) {
    return ListenableBuilder(
      listenable: widget.viewModel,
      builder: (context, _) {
        final pending = widget.viewModel.hasPendingCuota ? PaymentsViewModel.cuotaVigilanciaMensual : 0.0;
        final totalPaid = widget.viewModel.totalPagado;

        return Column(
          children: [
            // Tarjeta Hero: Pendiente Actual
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(24),
                gradient: AppColors.stitchPaymentGradient,
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFF0D2B68).withValues(alpha: 0.3),
                    blurRadius: 18,
                    offset: const Offset(0, 8),
                  ),
                ],
              ),
              child: Stack(
                children: [
                  Positioned(
                    right: -20,
                    bottom: -20,
                    child: Container(
                      width: 100,
                      height: 100,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: Colors.white.withValues(alpha: 0.06),
                      ),
                    ),
                  ),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          const Text(
                            'PENDIENTE ACTUAL',
                            style: TextStyle(
                              fontSize: 10.5,
                              fontWeight: FontWeight.w800,
                              letterSpacing: 0.8,
                              color: Color(0xFF93C5FD),
                            ),
                          ),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3.5),
                            decoration: BoxDecoration(
                              color: Colors.white.withValues(alpha: 0.16),
                              borderRadius: BorderRadius.circular(14),
                            ),
                            child: Row(
                              children: [
                                const Icon(Icons.event_note_rounded, size: 13, color: Colors.white),
                                const SizedBox(width: 4),
                                Text(
                                  'Cuota ${widget.viewModel.currentPeriod}',
                                  style: const TextStyle(
                                    fontSize: 11,
                                    fontWeight: FontWeight.w600,
                                    color: Colors.white,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 10),
                      Text(
                        Formatters.currency(pending),
                        style: const TextStyle(
                          fontSize: 38,
                          fontWeight: FontWeight.w800,
                          color: Colors.white,
                          letterSpacing: -1.0,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Flexible(
                            child: Text(
                              pending > 0 ? 'Vence el 30 de Noviembre' : 'Al día con tus pagos',
                              style: TextStyle(
                                fontSize: 12,
                                color: Colors.white.withValues(alpha: 0.85),
                              ),
                              overflow: TextOverflow.ellipsis,
                              maxLines: 1,
                            ),
                          ),
                          const SizedBox(width: 8),
                          if (pending > 0)
                            BouncyTap(
                              onTap: _showPayVigilanciaSheet,
                              child: Container(
                                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                                decoration: BoxDecoration(
                                  color: Colors.white,
                                  borderRadius: BorderRadius.circular(20),
                                  boxShadow: [
                                    BoxShadow(
                                      color: Colors.black.withValues(alpha: 0.1),
                                      blurRadius: 6,
                                      offset: const Offset(0, 2),
                                    ),
                                  ],
                                ),
                                child: const Row(
                                  children: [
                                    Text(
                                      'Pagar',
                                      style: TextStyle(
                                        fontSize: 12.5,
                                        fontWeight: FontWeight.bold,
                                        color: AppColors.stitchSapphire,
                                      ),
                                    ),
                                    SizedBox(width: 4),
                                    Icon(Icons.chevron_right_rounded, size: 16, color: AppColors.stitchSapphire),
                                  ],
                                ),
                              ),
                            ),
                        ],
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 12),
            // Fila de 2 Tarjetas Secundarias
            Row(
              children: [
                // Pagado este mes
                Expanded(
                  child: Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: isDark ? const Color(0xFF1E293B) : Colors.white,
                      borderRadius: BorderRadius.circular(20),
                      border: Border.all(
                        color: isDark ? const Color(0xFF334155) : const Color(0xFFE2E8F0),
                      ),
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withValues(alpha: isDark ? 0.2 : 0.03),
                          blurRadius: 8,
                          offset: const Offset(0, 3),
                        ),
                      ],
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              'Pagado este mes',
                              style: TextStyle(
                                fontSize: 11.5,
                                fontWeight: FontWeight.w600,
                                color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                              ),
                            ),
                            Container(
                              width: 26,
                              height: 26,
                              decoration: BoxDecoration(
                                shape: BoxShape.circle,
                                color: isDark ? const Color(0xFF0F172A) : const Color(0xFFF1F5F9),
                              ),
                              child: const Icon(Icons.calendar_month_rounded, size: 15, color: AppColors.stitchTextSecondary),
                            ),
                          ],
                        ),
                        const SizedBox(height: 10),
                        Text(
                          Formatters.currency(widget.viewModel.hasPendingCuota ? 0.0 : 15.0),
                          style: TextStyle(
                            fontSize: 22,
                            fontWeight: FontWeight.w800,
                            color: isDark ? Colors.white : AppColors.stitchTextPrimary,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          'Noviembre activo',
                          style: TextStyle(
                            fontSize: 11,
                            color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                // Aportado 2026
                Expanded(
                  child: Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: isDark ? const Color(0xFF1E293B) : Colors.white,
                      borderRadius: BorderRadius.circular(20),
                      border: Border.all(
                        color: isDark ? const Color(0xFF334155) : const Color(0xFFE2E8F0),
                      ),
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withValues(alpha: isDark ? 0.2 : 0.03),
                          blurRadius: 8,
                          offset: const Offset(0, 3),
                        ),
                      ],
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              'Aportado 2026',
                              style: TextStyle(
                                fontSize: 11.5,
                                fontWeight: FontWeight.w600,
                                color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                              ),
                            ),
                            Container(
                              width: 26,
                              height: 26,
                              decoration: const BoxDecoration(
                                shape: BoxShape.circle,
                                color: Color(0xFFD1FAE5),
                              ),
                              child: const Icon(Icons.verified_rounded, size: 15, color: Color(0xFF059669)),
                            ),
                          ],
                        ),
                        const SizedBox(height: 10),
                        Text(
                          Formatters.currency(totalPaid > 0 ? totalPaid : 165.0),
                          style: const TextStyle(
                            fontSize: 22,
                            fontWeight: FontWeight.w800,
                            color: AppColors.stitchEmerald,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          '${widget.viewModel.cuotasPagadas.isNotEmpty ? widget.viewModel.cuotasPagadas.length : 11} cuotas cumplidas',
                          style: TextStyle(
                            fontSize: 11,
                            color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ],
        );
      },
    );
  }

  /// 4. Banner Prompt de Cuota Pendiente
  Widget _buildPendingPromptBanner(bool isDark) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: isDark ? const Color(0xFF1E293B) : const Color(0xFFEEF2FF),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(
          color: isDark ? const Color(0xFF334155) : const Color(0xFFC7D2FE),
        ),
      ),
      child: Row(
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: const BoxDecoration(
              shape: BoxShape.circle,
              color: AppColors.stitchSapphire,
            ),
            child: const Icon(Icons.notifications_active_rounded, color: Colors.white, size: 20),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Tienes 1 cuota pendiente',
                  style: TextStyle(
                    fontSize: 13.5,
                    fontWeight: FontWeight.bold,
                    color: isDark ? Colors.white : AppColors.stitchTextPrimary,
                  ),
                ),
                Text(
                  'Evita recargos por mora en tu estado.',
                  style: TextStyle(
                    fontSize: 11.5,
                    color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                  ),
                ),
              ],
            ),
          ),
          BouncyTap(
            onTap: _showPayVigilanciaSheet,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              decoration: BoxDecoration(
                color: AppColors.stitchSapphire,
                borderRadius: BorderRadius.circular(20),
              ),
              child: const Row(
                children: [
                  Text(
                    'Pagar \$15.00',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  SizedBox(width: 4),
                  Icon(Icons.arrow_forward_rounded, color: Colors.white, size: 14),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// 5. Sección de Movimientos
  Widget _buildMovementsSection(bool isDark) {
    return ListenableBuilder(
      listenable: widget.viewModel,
      builder: (context, _) {
        final all = widget.viewModel.payments;
        final cuotas = widget.viewModel.cuotasList;
        final proyectos = widget.viewModel.proyectosList;

        List<PaymentModel> displayList;
        if (_selectedFilterIndex == 1) {
          displayList = cuotas;
        } else if (_selectedFilterIndex == 2) {
          displayList = proyectos;
        } else {
          displayList = all;
        }

        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Movimientos',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: isDark ? Colors.white : AppColors.stitchTextPrimary,
                  ),
                ),
                Row(
                  children: [
                    const Icon(Icons.tune_rounded, size: 16, color: AppColors.stitchSapphire),
                    const SizedBox(width: 4),
                    Text(
                      'Filtrar',
                      style: TextStyle(
                        fontSize: 12.5,
                        fontWeight: FontWeight.bold,
                        color: isDark ? AppColors.stitchBlueLight : AppColors.stitchSapphire,
                      ),
                    ),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 12),
            // Filter Pills Selector
            Container(
              padding: const EdgeInsets.all(4),
              decoration: BoxDecoration(
                color: isDark ? const Color(0xFF1E293B) : const Color(0xFFF1F5F9),
                borderRadius: BorderRadius.circular(24),
              ),
              child: Row(
                children: [
                  _buildFilterTab(0, 'Todos (${all.length})', isDark),
                  _buildFilterTab(1, 'Cuotas (${cuotas.length})', isDark),
                  _buildFilterTab(2, 'Proyectos (${proyectos.length})', isDark),
                ],
              ),
            ),
            const SizedBox(height: 14),
            // Items List
            if (displayList.isEmpty)
              const EmptyStateWidget(
                icon: Icons.receipt_long_rounded,
                title: 'Sin movimientos',
                message: 'No se encontraron registros bajo el filtro seleccionado.',
              )
            else
              ListView.separated(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: displayList.length,
                separatorBuilder: (_, __) => const SizedBox(height: 10),
                itemBuilder: (context, index) {
                  return _buildMovementCard(isDark, displayList[index]);
                },
              ),
          ],
        );
      },
    );
  }

  Widget _buildFilterTab(int index, String label, bool isDark) {
    final isSelected = _selectedFilterIndex == index;
    return Expanded(
      child: BouncyTap(
        onTap: () => setState(() => _selectedFilterIndex = index),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 200),
          padding: const EdgeInsets.symmetric(vertical: 8),
          decoration: BoxDecoration(
            color: isSelected
                ? (isDark ? const Color(0xFF0F172A) : Colors.white)
                : Colors.transparent,
            borderRadius: BorderRadius.circular(20),
            boxShadow: isSelected
                ? [
                    BoxShadow(
                      color: Colors.black.withValues(alpha: isDark ? 0.2 : 0.06),
                      blurRadius: 6,
                      offset: const Offset(0, 2),
                    ),
                  ]
                : null,
          ),
          child: Center(
            child: Text(
              label,
              style: TextStyle(
                fontSize: 11.5,
                fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
                color: isSelected
                    ? (isDark ? Colors.white : AppColors.stitchTextPrimary)
                    : (isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildMovementCard(bool isDark, PaymentModel item) {
    final isPaid = item.isPaid;
    final isProject = item.isProjectContribution;

    Color iconBg;
    Color iconColor;
    IconData icon;

    if (!isPaid) {
      iconBg = const Color(0xFFEEF2FF);
      iconColor = AppColors.stitchSapphire;
      icon = Icons.build_circle_rounded;
    } else if (isProject) {
      iconBg = const Color(0xFFCCFBF1);
      iconColor = AppColors.stitchTeal;
      icon = Icons.videocam_rounded;
    } else {
      iconBg = const Color(0xFFD1FAE5);
      iconColor = AppColors.stitchEmerald;
      icon = Icons.check_circle_rounded;
    }

    final title = isProject
        ? (item.nombreProyecto ?? 'Aporte Comunal')
        : 'Cuota Mantenimiento - ${item.periodoMes}';

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: isDark ? const Color(0xFF1E293B) : Colors.white,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: isDark ? const Color(0xFF334155) : const Color(0xFFE2E8F0),
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: isDark ? 0.2 : 0.03),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(12),
                  color: iconBg,
                ),
                child: Icon(icon, color: iconColor, size: 20),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                        color: isDark ? Colors.white : AppColors.stitchTextPrimary,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 2),
                    if (!isPaid)
                      const Row(
                        children: [
                          Icon(Icons.schedule_rounded, size: 12, color: Color(0xFFDC2626)),
                          SizedBox(width: 4),
                          Text(
                            'Vencimiento: 30 Nov 2026',
                            style: TextStyle(
                              fontSize: 11,
                              fontWeight: FontWeight.w600,
                              color: Color(0xFFDC2626),
                            ),
                          ),
                        ],
                      )
                    else
                      Text(
                        'Pagado el ${Formatters.date(item.fechaPago)}',
                        style: TextStyle(
                          fontSize: 11,
                          color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                        ),
                      ),
                  ],
                ),
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(
                    Formatters.currency(item.monto),
                    style: TextStyle(
                      fontSize: 15.5,
                      fontWeight: FontWeight.w800,
                      color: isDark ? Colors.white : AppColors.stitchTextPrimary,
                    ),
                  ),
                  const SizedBox(height: 3),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2.5),
                    decoration: BoxDecoration(
                      color: isPaid ? const Color(0xFFD1FAE5) : const Color(0xFFEEF2FF),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Text(
                      isPaid ? '✓ Aprobado' : '● Pendiente',
                      style: TextStyle(
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                        color: isPaid ? const Color(0xFF065F46) : AppColors.stitchSapphire,
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 12),
          const Divider(height: 1, color: Color(0xFFF1F5F9)),
          const SizedBox(height: 10),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              if (!isPaid) ...[
                Text(
                  'Servicios generales y alumbrado',
                  style: TextStyle(
                    fontSize: 11,
                    color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                  ),
                ),
                BouncyTap(
                  onTap: _showPayVigilanciaSheet,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
                    decoration: BoxDecoration(
                      color: AppColors.stitchSapphire,
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: const Row(
                      children: [
                        Text(
                          'Pagar',
                          style: TextStyle(color: Colors.white, fontSize: 11.5, fontWeight: FontWeight.bold),
                        ),
                        SizedBox(width: 4),
                        Icon(Icons.arrow_forward_rounded, color: Colors.white, size: 12),
                      ],
                    ),
                  ),
                ),
              ] else ...[
                Row(
                  children: [
                    Icon(
                      item.metodoPago.toUpperCase().contains('TRANS')
                          ? Icons.account_balance_rounded
                          : Icons.credit_card_rounded,
                      size: 14,
                      color: AppColors.stitchTextSecondary,
                    ),
                    const SizedBox(width: 6),
                    Text(
                      item.metodoPago.toUpperCase().contains('TRANS')
                          ? 'Transferencia Bancaria'
                          : 'Visa •••• 4242',
                      style: TextStyle(
                        fontSize: 11.5,
                        fontWeight: FontWeight.w500,
                        color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                      ),
                    ),
                  ],
                ),
                BouncyTap(
                  onTap: () => _showPaymentDetails(item),
                  child: const Row(
                    children: [
                      Text(
                        'Ver comprobante',
                        style: TextStyle(
                          fontSize: 11.5,
                          fontWeight: FontWeight.bold,
                          color: AppColors.stitchSapphire,
                        ),
                      ),
                      SizedBox(width: 4),
                      Icon(Icons.receipt_long_rounded, size: 14, color: AppColors.stitchSapphire),
                    ],
                  ),
                ),
              ],
            ],
          ),
        ],
      ),
    );
  }

  /// 6. Botón Descargar Estado de Cuenta
  Widget _buildDownloadAccountStatementButton(bool isDark) {
    return Column(
      children: [
        BouncyTap(
          onTap: () {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(
                content: Text('Generando Estado de Cuenta oficial en PDF con firma digital...'),
                backgroundColor: AppColors.stitchSapphire,
                duration: Duration(seconds: 2),
              ),
            );
          },
          child: Container(
            height: 52,
            width: double.infinity,
            decoration: BoxDecoration(
              color: isDark ? const Color(0xFF1E293B) : const Color(0xFFEEF2FF),
              borderRadius: BorderRadius.circular(26),
              border: Border.all(
                color: isDark ? const Color(0xFF334155) : const Color(0xFFC7D2FE),
              ),
            ),
            child: const Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.download_rounded, color: AppColors.stitchSapphire, size: 20),
                SizedBox(width: 8),
                Text(
                  'Descargar Estado de Cuenta (PDF)',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                    color: AppColors.stitchSapphire,
                  ),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 8),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.lock_outline_rounded, size: 12, color: AppColors.stitchTextSecondary),
            const SizedBox(width: 4),
            Text(
              'Auditoría transparente protegida por administración',
              style: TextStyle(
                fontSize: 11,
                color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
              ),
            ),
          ],
        ),
      ],
    );
  }
}
