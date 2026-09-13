import 'package:flutter/material.dart';
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import '../../core/theme/app_colors.dart';
import '../../core/widgets/empty_state.dart';
import '../../core/widgets/glass_container.dart';
import '../../core/widgets/neo_glass_container.dart';
import '../../core/widgets/status_badge.dart';
import '../../viewmodels/housing_viewmodel.dart';

/// Pantalla de detalle de la vivienda asignada al miembro.
class HousingDetailView extends StatefulWidget {
  final HousingViewModel viewModel;
  final int idMiembro;

  const HousingDetailView({
    super.key,
    required this.viewModel,
    required this.idMiembro,
  });

  @override
  State<HousingDetailView> createState() => _HousingDetailViewState();
}

class _HousingDetailViewState extends State<HousingDetailView> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      widget.viewModel.loadHousing(idMiembro: widget.idMiembro);
    });
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Mi Vivienda'),
      ),
      body: AmbientBackground(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
          child: ListenableBuilder(
            listenable: widget.viewModel,
            builder: (context, _) {
              if (widget.viewModel.isLoading) {
                return const Center(
                  child: Padding(
                    padding: EdgeInsets.symmetric(vertical: 60),
                    child: CircularProgressIndicator(),
                  ),
                );
              }

              final housing = widget.viewModel.housing;
              if (housing == null) {
                return EmptyStateWidget(
                  icon: FluentIcons.home_24_regular,
                  title: 'Vivienda no asignada',
                  message: 'Aún no tienes un lote o vivienda asociada a tu expediente.',
                  actionLabel: 'Reintentar',
                  onAction: () => widget.viewModel.loadHousing(idMiembro: widget.idMiembro),
                );
              }

              return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Tarjeta Principal Neo-Glass de la Casa
                  NeoGlassContainer(
                    padding: const EdgeInsets.all(24),
                    borderRadius: BorderRadius.circular(26),
                    child: Column(
                      children: [
                        NeoGlassContainer.circle(
                          size: 64,
                          child: Icon(
                            FluentIcons.home_24_regular,
                            color: isDark ? Colors.white : AppColors.brandBlue,
                            size: 30,
                          ),
                        ),
                        const SizedBox(height: 14),
                        Text(
                          housing.codigo,
                          style: TextStyle(
                            fontSize: 22,
                            fontWeight: FontWeight.bold,
                            color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          housing.sector,
                          style: TextStyle(
                            fontSize: 14,
                            color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                          ),
                        ),
                        const SizedBox(height: 12),
                        StatusBadge.fromStatus(housing.estado),
                      ],
                    ),
                  ),
                  const SizedBox(height: 22),

                  // Información de Ubicación
                  Text(
                    'Ubicación y Referencia',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                      color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                    ),
                  ),
                  const SizedBox(height: 10),
                  NeoGlassContainer(
                    padding: const EdgeInsets.all(18),
                    borderRadius: BorderRadius.circular(20),
                    child: Column(
                      children: [
                        _buildRow(FluentIcons.location_24_regular, 'Dirección', housing.direccion, isDark),
                        if (housing.referencia != null && housing.referencia!.isNotEmpty) ...[
                          Divider(height: 20, color: isDark ? Colors.white10 : Colors.black12),
                          _buildRow(FluentIcons.navigation_24_regular, 'Referencia', housing.referencia!, isDark),
                        ],
                      ],
                    ),
                  ),
                  const SizedBox(height: 22),

                  // Censo Familiar
                  Text(
                    'Censo de Habitantes',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                      color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                    ),
                  ),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Expanded(
                        child: NeoGlassContainer(
                          padding: const EdgeInsets.all(16),
                          borderRadius: BorderRadius.circular(20),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Adultos',
                                style: TextStyle(
                                  fontSize: 12,
                                  fontWeight: FontWeight.w600,
                                  color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                                ),
                              ),
                              const SizedBox(height: 6),
                              Text(
                                '${housing.adultos}',
                                style: TextStyle(
                                  fontSize: 24,
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
                          borderRadius: BorderRadius.circular(20),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Menores',
                                style: TextStyle(
                                  fontSize: 12,
                                  fontWeight: FontWeight.w600,
                                  color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                                ),
                              ),
                              const SizedBox(height: 6),
                              Text(
                                '${housing.menores}',
                                style: TextStyle(
                                  fontSize: 24,
                                  fontWeight: FontWeight.bold,
                                  color: isDark ? Colors.white : AppColors.textPrimaryLight,
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
          ),
        ),
      ),
    );
  }

  Widget _buildRow(IconData icon, String title, String value, bool isDark) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        NeoGlassContainer.circle(
          size: 34,
          child: Icon(icon, size: 16, color: isDark ? Colors.white70 : AppColors.brandBlue),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: TextStyle(fontSize: 12, color: isDark ? AppColors.textMutedDark : AppColors.textSecondaryLight),
              ),
              const SizedBox(height: 2),
              Text(
                value,
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
