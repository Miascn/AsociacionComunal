import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/widgets/custom_button.dart';
import '../../core/widgets/custom_text_field.dart';
import '../../core/widgets/glass_container.dart';
import '../../data/services/session_storage_service.dart';
import '../../viewmodels/auth_viewmodel.dart';

/// Pantalla de inicio de sesión con estética Glassmorphism.
class LoginView extends StatefulWidget {
  final AuthViewModel viewModel;
  final SessionStorageService storage;

  const LoginView({
    super.key,
    required this.viewModel,
    required this.storage,
  });

  @override
  State<LoginView> createState() => _LoginViewState();
}

class _LoginViewState extends State<LoginView> {
  final _formKey = GlobalKey<FormState>();
  final _usernameController = TextEditingController();
  final _passwordController = TextEditingController();

  @override
  void dispose() {
    _usernameController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  void _handleLogin() {
    if (_formKey.currentState?.validate() ?? false) {
      widget.viewModel.login(
        _usernameController.text.trim(),
        _passwordController.text,
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
                  // Logo Comunal con efecto Frosted Glass
                  GlassContainer.frosted(
                    padding: const EdgeInsets.all(20),
                    borderRadius: BorderRadius.circular(32),
                    child: Container(
                      width: 64,
                      height: 64,
                      decoration: const BoxDecoration(
                        shape: BoxShape.circle,
                        gradient: AppColors.brandGradient,
                      ),
                      child: const Icon(
                        Icons.apartment_rounded,
                        color: Colors.white,
                        size: 34,
                      ),
                    ),
                  ),
                  const SizedBox(height: 18),

                  // Títulos
                  Text(
                    'Asociación Comunal',
                    style: TextStyle(
                      fontSize: 26,
                      fontWeight: FontWeight.w800,
                      letterSpacing: -0.5,
                      color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    'Portal móvil para residentes y miembros',
                    style: TextStyle(
                      fontSize: 14,
                      color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                    ),
                  ),
                  const SizedBox(height: 32),

                  // Tarjeta Glassmórfica de Formulario
                  GlassContainer.frosted(
                    padding: const EdgeInsets.all(26),
                    borderRadius: BorderRadius.circular(28),
                    child: Form(
                      key: _formKey,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Iniciar Sesión',
                            style: TextStyle(
                              fontSize: 19,
                              fontWeight: FontWeight.bold,
                              color: isDark
                                  ? AppColors.textPrimaryDark
                                  : AppColors.textPrimaryLight,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            'Ingresa tu usuario asignado o número de DUI',
                            style: TextStyle(
                              fontSize: 13,
                              color: isDark
                                  ? AppColors.textMutedDark
                                  : AppColors.textSecondaryLight,
                            ),
                          ),
                          const SizedBox(height: 22),

                          // Campo Usuario
                          CustomTextField(
                            controller: _usernameController,
                            label: 'Usuario o DUI',
                            hint: 'Ej. admin o 01234567-8',
                            prefixIcon: Icons.person_outline,
                            validator: (val) {
                              if (val == null || val.trim().isEmpty) {
                                return 'Por favor ingresa tu usuario o DUI';
                              }
                              return null;
                            },
                          ),
                          const SizedBox(height: 16),

                          // Campo Contraseña
                          CustomTextField(
                            controller: _passwordController,
                            label: 'Contraseña',
                            hint: '••••••••',
                            prefixIcon: Icons.lock_outline,
                            isPassword: true,
                            textInputAction: TextInputAction.done,
                            validator: (val) {
                              if (val == null || val.isEmpty) {
                                return 'Por favor ingresa tu contraseña';
                              }
                              return null;
                            },
                          ),
                          const SizedBox(height: 18),

                          // Banner de error (si existe)
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
                                  border: Border.all(
                                    color: AppColors.errorRed.withValues(alpha: 0.3),
                                  ),
                                ),
                                child: Row(
                                  children: [
                                    const Icon(
                                      Icons.error_outline,
                                      color: AppColors.errorRed,
                                      size: 20,
                                    ),
                                    const SizedBox(width: 10),
                                    Expanded(
                                      child: Text(
                                        error,
                                        style: const TextStyle(
                                          color: AppColors.errorRed,
                                          fontSize: 12.5,
                                          fontWeight: FontWeight.w500,
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                              );
                            },
                          ),

                          // Botón Ingresar
                          ListenableBuilder(
                            listenable: widget.viewModel,
                            builder: (context, _) {
                              return CustomButton(
                                text: 'Ingresar al Portal',
                                isLoading: widget.viewModel.isLoading,
                                onPressed: _handleLogin,
                                icon: Icons.arrow_forward,
                              );
                            },
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 24),

                  // Indicador de conexión oficial segura
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(
                        Icons.verified_user_outlined,
                        size: 15,
                        color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                      ),
                      const SizedBox(width: 6),
                      Text(
                        'Conexión segura SSL • Portal Oficial',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w500,
                          color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
