import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/bouncy_tap.dart';
import '../../core/widgets/neo_glass_container.dart';
import '../../data/models/payment_model.dart';
import '../../viewmodels/payments_viewmodel.dart';
import 'payment_receipt_dialog.dart';

/// Modal interactivo de Pasarela de Pago Simulada (Visa, Mastercard, Google Pay, Nueva Tarjeta)
class SimulatedPaymentSheet extends StatefulWidget {
  final PaymentsViewModel viewModel;
  final int idMiembro;
  final String title;
  final String description;
  final double defaultAmount;
  final bool isAmountEditable;
  final String periodoMes;
  final int? idProyecto;
  final String? nombreProyecto;
  final void Function(PaymentModel payment)? onPaymentSuccess;

  const SimulatedPaymentSheet({
    super.key,
    required this.viewModel,
    required this.idMiembro,
    required this.title,
    required this.description,
    required this.defaultAmount,
    this.isAmountEditable = false,
    required this.periodoMes,
    this.idProyecto,
    this.nombreProyecto,
    this.onPaymentSuccess,
  });

  static Future<PaymentModel?> show(
    BuildContext context, {
    required PaymentsViewModel viewModel,
    required int idMiembro,
    required String title,
    required String description,
    required double defaultAmount,
    bool isAmountEditable = false,
    required String periodoMes,
    int? idProyecto,
    String? nombreProyecto,
    void Function(PaymentModel payment)? onPaymentSuccess,
  }) {
    return showModalBottomSheet<PaymentModel>(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (ctx) => SimulatedPaymentSheet(
        viewModel: viewModel,
        idMiembro: idMiembro,
        title: title,
        description: description,
        defaultAmount: defaultAmount,
        isAmountEditable: isAmountEditable,
        periodoMes: periodoMes,
        idProyecto: idProyecto,
        nombreProyecto: nombreProyecto,
        onPaymentSuccess: onPaymentSuccess,
      ),
    );
  }

  @override
  State<SimulatedPaymentSheet> createState() => _SimulatedPaymentSheetState();
}

enum _PaymentOption { visa, mastercard, googlePay, newCard }

class _SimulatedPaymentSheetState extends State<SimulatedPaymentSheet>
    with SingleTickerProviderStateMixin {
  late _PaymentOption _selectedOption;
  late TextEditingController _amountController;
  final _cardNumberController = TextEditingController();
  final _expiryController = TextEditingController();
  final _cvvController = TextEditingController();
  final _holderController = TextEditingController();

  bool _isProcessing = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _selectedOption = _PaymentOption.visa;
    _amountController = TextEditingController(
      text: widget.defaultAmount.toStringAsFixed(2),
    );
  }

  @override
  void dispose() {
    _amountController.dispose();
    _cardNumberController.dispose();
    _expiryController.dispose();
    _cvvController.dispose();
    _holderController.dispose();
    super.dispose();
  }

  double get _currentAmount {
    return double.tryParse(_amountController.text.trim()) ?? widget.defaultAmount;
  }

  String get _paymentMethodName {
    return switch (_selectedOption) {
      _PaymentOption.visa => 'Visa Débito (•••• 4242)',
      _PaymentOption.mastercard => 'Mastercard Gold (•••• 8888)',
      _PaymentOption.googlePay => 'Google Pay',
      _PaymentOption.newCard =>
        'Tarjeta Directa (•••• ${_cardNumberController.text.length >= 4 ? _cardNumberController.text.substring(_cardNumberController.text.length - 4) : "0000"})',
    };
  }

  Future<void> _processPayment() async {
    final amount = _currentAmount;
    if (amount <= 0) {
      setState(() => _errorMessage = 'El monto debe ser mayor a cero.');
      return;
    }

    if (_selectedOption == _PaymentOption.newCard) {
      if (_cardNumberController.text.replaceAll(' ', '').length < 16) {
        setState(() => _errorMessage = 'Número de tarjeta incompleto (16 dígitos).');
        return;
      }
      if (_expiryController.text.length < 5) {
        setState(() => _errorMessage = 'Fecha de expiración inválida (MM/AA).');
        return;
      }
      if (_cvvController.text.length < 3) {
        setState(() => _errorMessage = 'Código CVV inválido (3 dígitos).');
        return;
      }
    }

    setState(() {
      _isProcessing = true;
      _errorMessage = null;
    });

    try {
      // 1. Simulación visual de handshake seguro con el procesador de tarjetas
      await Future.delayed(const Duration(milliseconds: 1600));

      final timestamp = DateTime.now().millisecondsSinceEpoch.toString().substring(7);
      final prefix = widget.idProyecto != null ? 'PROY' : 'CUOTA';
      final ref = 'TXN-$prefix-${widget.periodoMes}-$timestamp';

      // 2. Registro real en el backend
      final payment = await widget.viewModel.processSimulatedPayment(
        idMiembro: widget.idMiembro,
        periodoMes: widget.periodoMes,
        monto: amount,
        metodoSeleccionado: _paymentMethodName,
        referenciaGenerada: ref,
        idProyecto: widget.idProyecto,
      );

      if (!mounted) return;

      Navigator.of(context).pop(payment);
      widget.onPaymentSuccess?.call(payment);

      // 3. Mostrar recibo visual interactivo
      PaymentReceiptDialog.show(
        context,
        payment: payment,
        methodName: _paymentMethodName,
        conceptTitle: widget.title,
      );
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _isProcessing = false;
        _errorMessage = e.toString().replaceAll('Exception: ', '');
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
      ),
      child: NeoGlassContainer(
        borderRadius: const BorderRadius.vertical(top: Radius.circular(32)),
        padding: const EdgeInsets.fromLTRB(22, 16, 22, 28),
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Barra indicadora superior
              Center(
                child: Container(
                  width: 44,
                  height: 4.5,
                  decoration: BoxDecoration(
                    color: Colors.grey.withValues(alpha: 0.35),
                    borderRadius: BorderRadius.circular(3),
                  ),
                ),
              ),
              const SizedBox(height: 16),

              // Cabecera
              Row(
                children: [
                  NeoGlassContainer.circle(
                    size: 46,
                    accentColor: AppColors.brandBlue.withValues(alpha: 0.14),
                    child: const Icon(
                      FluentIcons.payment_24_filled,
                      color: AppColors.brandBlue,
                      size: 22,
                    ),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          widget.title,
                          style: TextStyle(
                            fontSize: 17,
                            fontWeight: FontWeight.bold,
                            color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          widget.description,
                          style: TextStyle(
                            fontSize: 12,
                            color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                          ),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    onPressed: _isProcessing ? null : () => Navigator.of(context).pop(),
                    icon: Icon(
                      Icons.close,
                      color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),

              // Tarjeta visual interactiva
              _buildDigitalCardPreview(isDark),
              const SizedBox(height: 22),

              // Selector de Métodos
              Text(
                'MÉTODO DE PAGO',
                style: TextStyle(
                  fontSize: 11,
                  fontWeight: FontWeight.w700,
                  letterSpacing: 0.5,
                  color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                ),
              ),
              const SizedBox(height: 10),
              _buildOptionSelector(isDark),
              const SizedBox(height: 16),

              // Formulario de Nueva Tarjeta si está seleccionada
              if (_selectedOption == _PaymentOption.newCard) ...[
                _buildNewCardForm(isDark),
                const SizedBox(height: 16),
              ],

              // Desglose del pago
              _buildPaymentBreakdown(isDark),
              const SizedBox(height: 16),

              // Mensaje de error si ocurre
              if (_errorMessage != null) ...[
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                  decoration: BoxDecoration(
                    color: AppColors.errorRed.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: AppColors.errorRed.withValues(alpha: 0.3)),
                  ),
                  child: Row(
                    children: [
                      const Icon(FluentIcons.error_circle_20_filled, color: AppColors.errorRed, size: 18),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          _errorMessage!,
                          style: const TextStyle(color: AppColors.errorRed, fontSize: 12),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 14),
              ],

              // Botón de acción principal con animación
              BouncyTap(
                onTap: _isProcessing ? null : _processPayment,
                child: Container(
                  width: double.infinity,
                  height: 52,
                  decoration: BoxDecoration(
                    gradient: const LinearGradient(
                      colors: [AppColors.brandBlue, AppColors.electricIndigo],
                    ),
                    borderRadius: BorderRadius.circular(16),
                    boxShadow: [
                      BoxShadow(
                        color: AppColors.brandBlue.withValues(alpha: 0.35),
                        blurRadius: 16,
                        offset: const Offset(0, 6),
                      ),
                    ],
                  ),
                  child: Center(
                    child: _isProcessing
                        ? const Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              SizedBox(
                                width: 20,
                                height: 20,
                                child: CircularProgressIndicator(
                                  color: Colors.white,
                                  strokeWidth: 2.2,
                                ),
                              ),
                              SizedBox(width: 12),
                              Text(
                                'Procesando pago seguro...',
                                style: TextStyle(
                                  color: Colors.white,
                                  fontWeight: FontWeight.bold,
                                  fontSize: 15,
                                ),
                              ),
                            ],
                          )
                        : Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              const Icon(FluentIcons.lock_shield_20_filled, color: Colors.white, size: 18),
                              const SizedBox(width: 8),
                              Text(
                                'Pagar ${Formatters.currency(_currentAmount)}',
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontWeight: FontWeight.bold,
                                  fontSize: 16,
                                ),
                              ),
                            ],
                          ),
                  ),
                ),
              ),
              const SizedBox(height: 10),
              Center(
                child: Text(
                  'Transacción cifrada TLS 1.3 • Simulación oficial para Colonia',
                  style: TextStyle(
                    fontSize: 11,
                    color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildDigitalCardPreview(bool isDark) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(22),
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: switch (_selectedOption) {
            _PaymentOption.visa => [const Color(0xFF1E3A8A), const Color(0xFF3B82F6)],
            _PaymentOption.mastercard => [const Color(0xFFC2410C), const Color(0xFFF97316)],
            _PaymentOption.googlePay => [const Color(0xFF111827), const Color(0xFF374151)],
            _PaymentOption.newCard => [const Color(0xFF312E81), const Color(0xFF6366F1)],
          },
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.22),
            blurRadius: 18,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Icon(FluentIcons.contact_card_group_24_regular, color: Colors.white70, size: 28),
              Text(
                switch (_selectedOption) {
                  _PaymentOption.visa => 'VISA',
                  _PaymentOption.mastercard => 'MASTERCARD',
                  _PaymentOption.googlePay => 'GPay',
                  _PaymentOption.newCard => 'TARJETA',
                },
                style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w900,
                  fontSize: 18,
                  letterSpacing: 1.5,
                ),
              ),
            ],
          ),
          const SizedBox(height: 22),
          Text(
            switch (_selectedOption) {
              _PaymentOption.visa => '••••  ••••  ••••  4242',
              _PaymentOption.mastercard => '••••  ••••  ••••  8888',
              _PaymentOption.googlePay => 'Cuenta vinculada a Google Pay',
              _PaymentOption.newCard => _cardNumberController.text.isEmpty
                  ? '••••  ••••  ••••  ••••'
                  : _cardNumberController.text,
            },
            style: const TextStyle(
              color: Colors.white,
              fontWeight: FontWeight.w600,
              fontSize: 16,
              letterSpacing: 2,
            ),
          ),
          const SizedBox(height: 18),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('TITULAR', style: TextStyle(color: Colors.white60, fontSize: 9)),
                  Text(
                    _selectedOption == _PaymentOption.newCard && _holderController.text.isNotEmpty
                        ? _holderController.text.toUpperCase()
                        : 'RESIDENTE ASOCIACIÓN',
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  const Text('VENCE', style: TextStyle(color: Colors.white60, fontSize: 9)),
                  Text(
                    _selectedOption == _PaymentOption.newCard && _expiryController.text.isNotEmpty
                        ? _expiryController.text
                        : (_selectedOption == _PaymentOption.visa ? '08/28' : '12/27'),
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildOptionSelector(bool isDark) {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          _buildMethodChip(
            option: _PaymentOption.visa,
            title: 'Visa •••• 4242',
            icon: Icons.credit_card,
            isDark: isDark,
          ),
          const SizedBox(width: 8),
          _buildMethodChip(
            option: _PaymentOption.mastercard,
            title: 'Mastercard •••• 8888',
            icon: Icons.credit_card_rounded,
            isDark: isDark,
          ),
          const SizedBox(width: 8),
          _buildMethodChip(
            option: _PaymentOption.googlePay,
            title: 'Google Pay',
            icon: FluentIcons.payment_20_regular,
            isDark: isDark,
          ),
          const SizedBox(width: 8),
          _buildMethodChip(
            option: _PaymentOption.newCard,
            title: '+ Otra Tarjeta',
            icon: Icons.add_card_rounded,
            isDark: isDark,
          ),
        ],
      ),
    );
  }

  Widget _buildMethodChip({
    required _PaymentOption option,
    required String title,
    required IconData icon,
    required bool isDark,
  }) {
    final isSelected = _selectedOption == option;
    return BouncyTap(
      onTap: _isProcessing ? null : () => setState(() => _selectedOption = option),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
        decoration: BoxDecoration(
          color: isSelected
              ? AppColors.brandBlue.withValues(alpha: isDark ? 0.25 : 0.12)
              : (isDark ? const Color(0x221E293B) : const Color(0x0C0F172A)),
          borderRadius: BorderRadius.circular(14),
          border: Border.all(
            color: isSelected
                ? AppColors.brandBlue
                : (isDark ? const Color(0x25FFFFFF) : const Color(0x180F172A)),
            width: isSelected ? 1.5 : 1,
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              icon,
              size: 16,
              color: isSelected ? AppColors.brandBlue : (isDark ? Colors.white70 : Colors.black87),
            ),
            const SizedBox(width: 6),
            Text(
              title,
              style: TextStyle(
                fontSize: 12,
                fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
                color: isSelected ? AppColors.brandBlue : (isDark ? Colors.white70 : Colors.black87),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildNewCardForm(bool isDark) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: isDark ? const Color(0x201E293B) : const Color(0x080F172A),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: isDark ? const Color(0x20FFFFFF) : const Color(0x140F172A)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          TextField(
            controller: _cardNumberController,
            keyboardType: TextInputType.number,
            inputFormatters: [
              FilteringTextInputFormatter.digitsOnly,
              LengthLimitingTextInputFormatter(16),
            ],
            onChanged: (_) => setState(() {}),
            decoration: const InputDecoration(
              labelText: 'Número de Tarjeta',
              hintText: '4000 1234 5678 9010',
              prefixIcon: Icon(Icons.credit_card),
            ),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _expiryController,
                  keyboardType: TextInputType.datetime,
                  inputFormatters: [LengthLimitingTextInputFormatter(5)],
                  onChanged: (_) => setState(() {}),
                  decoration: const InputDecoration(
                    labelText: 'Vence (MM/AA)',
                    hintText: '12/28',
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: TextField(
                  controller: _cvvController,
                  keyboardType: TextInputType.number,
                  obscureText: true,
                  inputFormatters: [
                    FilteringTextInputFormatter.digitsOnly,
                    LengthLimitingTextInputFormatter(4),
                  ],
                  decoration: const InputDecoration(
                    labelText: 'CVV',
                    hintText: '123',
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _holderController,
            textCapitalization: TextCapitalization.characters,
            onChanged: (_) => setState(() {}),
            decoration: const InputDecoration(
              labelText: 'Nombre del Titular',
              hintText: 'COMO APARECE EN LA TARJETA',
              prefixIcon: Icon(Icons.person_outline),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPaymentBreakdown(bool isDark) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: isDark ? const Color(0x261E293B) : const Color(0x0A0F172A),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        children: [
          if (widget.isAmountEditable) ...[
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Monto Voluntario (\$):',
                  style: TextStyle(
                    fontSize: 13,
                    color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                  ),
                ),
                SizedBox(
                  width: 100,
                  height: 38,
                  child: TextField(
                    controller: _amountController,
                    keyboardType: const TextInputType.numberWithOptions(decimal: true),
                    textAlign: TextAlign.right,
                    onChanged: (_) => setState(() {}),
                    decoration: const InputDecoration(
                      contentPadding: EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                      prefixText: '\$ ',
                    ),
                  ),
                ),
              ],
            ),
            const Divider(height: 20),
          ],
          _buildRow('Concepto:', widget.title, isDark),
          const SizedBox(height: 6),
          _buildRow('Período aplicado:', widget.periodoMes, isDark),
          const SizedBox(height: 6),
          _buildRow('Comisión bancaria:', 'Bonificada (\$0.00)', isDark, isHighlight: true),
          const Divider(height: 20),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Total a Debitar:',
                style: TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.bold,
                  color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                ),
              ),
              Text(
                Formatters.currency(_currentAmount),
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w900,
                  color: AppColors.successGreen,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildRow(String label, String value, bool isDark, {bool isHighlight = false}) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: TextStyle(
            fontSize: 12,
            color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
          ),
        ),
        Text(
          value,
          style: TextStyle(
            fontSize: 12,
            fontWeight: isHighlight ? FontWeight.bold : FontWeight.w600,
            color: isHighlight
                ? AppColors.successGreen
                : (isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight),
          ),
        ),
      ],
    );
  }
}
