import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/widgets/custom_button.dart';
import '../../core/widgets/custom_text_field.dart';
import '../../core/widgets/glass_container.dart';
import '../../viewmodels/auth_viewmodel.dart';

/// Pantalla para cambio obligatorio de contraseña temporal.
class ChangePasswordView extends StatefulWidget {
  final AuthViewModel viewModel;

  const ChangePasswordView({super.key, required this.viewModel});

  @override
  State<ChangePasswordView> createState() => _ChangePasswordViewState();
}

class _ChangePasswordViewState extends State<ChangePasswordView> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _currentPasswordController;
  final _newPasswordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _currentPasswordController = TextEditingController(
      text: widget.viewModel.temporaryPassword ?? '',
    );
  }

  @override
  void dispose() {
    _currentPasswordController.dispose();
    _newPasswordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  void _handleSubmit() {
    if (_formKey.currentState?.validate() ?? false) {
      widget.viewModel.changePassword(
        _currentPasswordController.text,
        _newPasswordController.text,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      body: AmbientBackground(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 440),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  GlassContainer.frosted(
                    padding: const EdgeInsets.all(20),
                    borderRadius: BorderRadius.circular(30),
                    child: Container(
                      width: 60,
                      height: 60,
                      decoration: const BoxDecoration(
                        shape: BoxShape.circle,
                        color: AppColors.actionCoral,
                      ),
                      child: const Icon(
                        Icons.lock_reset,
                        color: Colors.white,
                        size: 32,
                      ),
                    ),
                  ),
                  const SizedBox(height: 18),
                  Text(
                    'Actualizar Contraseña',
                    style: TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                      color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    'Por seguridad debes reemplazar tu clave temporal antes de acceder.',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 13,
                      color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                    ),
                  ),
                  const SizedBox(height: 28),

                  GlassContainer.frosted(
                    padding: const EdgeInsets.all(24),
                    borderRadius: BorderRadius.circular(28),
                    child: Form(
                      key: _formKey,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          CustomTextField(
                            controller: _currentPasswordController,
                            label: 'Contraseña Temporal',
                            hint: '••••••••',
                            isPassword: true,
                            prefixIcon: Icons.key_outlined,
                            validator: (val) {
                              if (val == null || val.isEmpty) {
                                return 'Ingresa tu contraseña temporal';
                              }
                              return null;
                            },
                          ),
                          const SizedBox(height: 16),
                          CustomTextField(
                            controller: _newPasswordController,
                            label: 'Nueva Contraseña',
                            hint: 'Mínimo 12 caracteres',
                            isPassword: true,
                            prefixIcon: Icons.lock_outline,
                            onChanged: (_) => setState(() {}),
                            validator: (val) {
                              if (val == null || val.length < 12) {
                                return 'La contraseña debe tener al menos 12 caracteres';
                              }
                              if (val == _currentPasswordController.text) {
                                return 'La nueva contraseña debe ser diferente a la temporal';
                              }
                              return null;
                            },
                          ),
                          const SizedBox(height: 10),
                          _buildRequirementsCard(isDark),
                          const SizedBox(height: 16),
                          CustomTextField(
                            controller: _confirmPasswordController,
                            label: 'Confirmar Nueva Contraseña',
                            hint: 'Repite tu nueva contraseña',
                            isPassword: true,
                            prefixIcon: Icons.lock_outline,
                            textInputAction: TextInputAction.done,
                            onChanged: (_) => setState(() {}),
                            validator: (val) {
                              if (val != _newPasswordController.text) {
                                return 'Las contraseñas no coinciden';
                              }
                              return null;
                            },
                          ),
                          const SizedBox(height: 18),

                          ListenableBuilder(
                            listenable: widget.viewModel,
                            builder: (context, _) {
                              final error = widget.viewModel.errorMessage;
                              if (error == null) return const SizedBox.shrink();

                              return Container(
                                margin: const EdgeInsets.only(bottom: 16),
                                padding: const EdgeInsets.all(12),
                                decoration: BoxDecoration(
                                  color: AppColors.errorRedLight,
                                  borderRadius: BorderRadius.circular(12),
                                ),
                                child: Text(
                                  error,
                                  style: const TextStyle(
                                    color: AppColors.errorRed,
                                    fontSize: 12.5,
                                  ),
                                ),
                              );
                            },
                          ),

                          ListenableBuilder(
                            listenable: widget.viewModel,
                            builder: (context, _) {
                              return CustomButton(
                                text: 'Guardar y Continuar',
                                isLoading: widget.viewModel.isLoading,
                                onPressed: _handleSubmit,
                              );
                            },
                          ),
                          const SizedBox(height: 12),

                          Center(
                            child: TextButton(
                              onPressed: () => widget.viewModel.logout(),
                              child: const Text('Cancelar y salir'),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildRequirementsCard(bool isDark) {
    final newPass = _newPasswordController.text;
    final currentPass = _currentPasswordController.text;
    final confirmPass = _confirmPasswordController.text;

    final hasLength = newPass.length >= 12;
    final isDifferent = newPass.isNotEmpty && newPass != currentPass;
    final matches = confirmPass.isNotEmpty && newPass == confirmPass;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: isDark ? const Color(0xFF1E293B) : const Color(0xFFF1F5F9),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(
          color: isDark ? const Color(0xFF334155) : const Color(0xFFCBD5E1),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Requisitos obligatorios:',
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.bold,
              color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
            ),
          ),
          const SizedBox(height: 8),
          _buildRequirementItem(
            isMet: hasLength,
            text: 'Mínimo 12 caracteres (${newPass.length}/12)',
            isDark: isDark,
          ),
          const SizedBox(height: 6),
          _buildRequirementItem(
            isMet: isDifferent,
            text: 'Diferente a la contraseña temporal',
            isDark: isDark,
          ),
          if (confirmPass.isNotEmpty) ...[
            const SizedBox(height: 6),
            _buildRequirementItem(
              isMet: matches,
              text: matches ? 'Las contraseñas coinciden' : 'Las contraseñas no coinciden',
              isDark: isDark,
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildRequirementItem({
    required bool isMet,
    required String text,
    required bool isDark,
  }) {
    return Row(
      children: [
        Icon(
          isMet ? Icons.check_circle : Icons.radio_button_unchecked,
          size: 15,
          color: isMet ? Colors.green : (isDark ? Colors.white38 : Colors.black38),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            text,
            style: TextStyle(
              fontSize: 12,
              color: isMet
                  ? (isDark ? Colors.greenAccent : Colors.green.shade800)
                  : (isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight),
              fontWeight: isMet ? FontWeight.w600 : FontWeight.normal,
            ),
          ),
        ),
      ],
    );
  }
}
