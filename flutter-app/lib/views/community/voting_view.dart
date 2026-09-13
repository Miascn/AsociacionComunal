import 'package:flutter/material.dart';
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import '../../core/theme/app_colors.dart';
import '../../core/widgets/bouncy_tap.dart';
import '../../core/widgets/custom_button.dart';
import '../../core/widgets/empty_state.dart';
import '../../core/widgets/fade_slide_entrance.dart';
import '../../core/widgets/glass_container.dart';
import '../../core/widgets/neo_glass_container.dart';
import '../../core/widgets/status_badge.dart';
import '../../data/models/voting_model.dart';
import '../../viewmodels/voting_viewmodel.dart';

/// Pantalla de Votaciones Comunitarias y Consulta Electoral con Glassmorphism.
class VotingView extends StatefulWidget {
  final VotingViewModel viewModel;
  final int? idMiembro;

  const VotingView({
    super.key,
    required this.viewModel,
    this.idMiembro,
  });

  @override
  State<VotingView> createState() => _VotingViewState();
}

class _VotingViewState extends State<VotingView> {
  int? _selectedOptionId;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      widget.viewModel.loadPolls(idMiembro: widget.idMiembro);
    });
  }

  void _confirmVote(VotingModel poll, VotingOptionModel option) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Confirmar Voto'),
        content: Text(
          '¿Estás seguro de que deseas registrar tu voto por:\n\n"${option.descripcion}"?\n\nEsta acción no se puede revertir.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancelar'),
          ),
          ElevatedButton(
            onPressed: () async {
              Navigator.pop(ctx);
              final success = await widget.viewModel.castVote(
                idVotacion: poll.id,
                idOpcion: option.idOpcion,
                idMiembro: widget.idMiembro,
              );
              if (mounted && success) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text('¡Tu voto ha sido registrado exitosamente!'),
                    backgroundColor: AppColors.successGreen,
                  ),
                );
                setState(() => _selectedOptionId = null);
              }
            },
            child: const Text('Emitir Voto'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Votaciones y Consultas'),
      ),
      body: AmbientBackground(
        child: RefreshIndicator(
          onRefresh: () => widget.viewModel.loadPolls(idMiembro: widget.idMiembro),
          color: AppColors.brandBlue,
          child: SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(20, 10, 20, 40),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Participación Ciudadana',
                  style: TextStyle(
                    fontSize: 22,
                    fontWeight: FontWeight.bold,
                    color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  'Decisiones comunitarias, proyectos y elecciones comunales',
                  style: TextStyle(
                    fontSize: 13,
                    color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                  ),
                ),
                const SizedBox(height: 20),

                ListenableBuilder(
                  listenable: widget.viewModel,
                  builder: (context, _) {
                    if (widget.viewModel.isLoading && widget.viewModel.polls.isEmpty) {
                      return const Center(
                        child: Padding(
                          padding: EdgeInsets.symmetric(vertical: 40),
                          child: CircularProgressIndicator(),
                        ),
                      );
                    }

                    final polls = widget.viewModel.polls;
                    if (polls.isEmpty) {
                      return EmptyStateWidget(
                        icon: FluentIcons.poll_24_regular,
                        title: 'No hay votaciones activas',
                        message: 'Los procesos electorales y consultas vecinales aparecerán aquí.',
                        actionLabel: 'Actualizar',
                        onAction: () => widget.viewModel.loadPolls(idMiembro: widget.idMiembro),
                      );
                    }

                    return ListView.separated(
                      shrinkWrap: true,
                      physics: const NeverScrollableScrollPhysics(),
                      itemCount: polls.length,
                      separatorBuilder: (_, __) => const SizedBox(height: 18),
                      itemBuilder: (context, index) {
                        final poll = polls[index];
                        final hasVoted = widget.viewModel.hasUserVoted(poll.id);

                        return FadeSlideEntrance(
                          delay: Duration(milliseconds: index * 100),
                          child: _buildPollCard(poll, hasVoted, isDark),
                        );
                      },
                    );
                  },
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildPollCard(VotingModel poll, bool hasVoted, bool isDark) {
    return NeoGlassContainer(
      padding: const EdgeInsets.all(20),
      borderRadius: BorderRadius.circular(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              StatusBadge.fromStatus(poll.estado),
              if (hasVoted)
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: AppColors.successGreenLight,
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: AppColors.successGreen.withValues(alpha: 0.4)),
                  ),
                  child: const Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(FluentIcons.checkmark_circle_16_regular, size: 14, color: AppColors.successGreen),
                      SizedBox(width: 4),
                      Text(
                        'YA PARTICIPASTE',
                        style: TextStyle(
                          fontSize: 10,
                          fontWeight: FontWeight.bold,
                          color: AppColors.successGreen,
                        ),
                      ),
                    ],
                  ),
                ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            poll.titulo,
            style: TextStyle(
              fontSize: 17,
              fontWeight: FontWeight.bold,
              color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            poll.descripcion,
            style: TextStyle(
              fontSize: 13,
              color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
            ),
          ),
          const SizedBox(height: 16),

          // Lista de Opciones
          ...poll.opciones.map((opcion) {
            final isSelected = _selectedOptionId == opcion.idOpcion;
            final percentage = poll.totalVotos > 0
                ? (opcion.votos / poll.totalVotos)
                : 0.0;

            // Si ya votó o la votación está cerrada, mostrar resultados
            if (hasVoted || poll.isClosed) {
              return Padding(
                padding: const EdgeInsets.only(bottom: 10),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Expanded(
                          child: Text(
                            opcion.descripcion,
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                              color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                            ),
                          ),
                        ),
                        Text(
                          '${opcion.votos} votos (${(percentage * 100).toStringAsFixed(1)}%)',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 6),
                    ClipRRect(
                      borderRadius: BorderRadius.circular(6),
                      child: LinearProgressIndicator(
                        value: percentage,
                        minHeight: 8,
                        backgroundColor: isDark ? const Color(0xFF1E293B) : const Color(0xFFE2E8F0),
                        valueColor: AlwaysStoppedAnimation<Color>(
                          percentage > 0.5 ? AppColors.neoEmerald : AppColors.brandBlue,
                        ),
                      ),
                    ),
                  ],
                ),
              );
            }

            // Si la votación está abierta y aún no vota, permitir selección con BouncyTap
            return Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: BouncyTap(
                scaleDown: 0.97,
                onTap: () => setState(() => _selectedOptionId = opcion.idOpcion),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(14),
                    color: isSelected
                        ? (isDark ? const Color(0xFF1E293B) : AppColors.brandBlue.withValues(alpha: 0.10))
                        : (isDark ? const Color(0x301E293B) : const Color(0x0A0F172A)),
                    border: Border.all(
                      color: isSelected
                          ? (isDark ? AppColors.neoEmerald : AppColors.brandBlue)
                          : (isDark ? const Color(0x20FFFFFF) : const Color(0x180F172A)),
                      width: isSelected ? 1.6 : 1.0,
                    ),
                  ),
                  child: Row(
                    children: [
                      Icon(
                        isSelected ? FluentIcons.radio_button_24_filled : FluentIcons.radio_button_24_regular,
                        color: isSelected
                            ? (isDark ? AppColors.neoEmerald : AppColors.brandBlue)
                            : (isDark ? Colors.grey : AppColors.textMutedLight),
                        size: 20,
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          opcion.descripcion,
                          style: TextStyle(
                            fontSize: 14,
                            fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                            color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            );
          }),

          // Botón para emitir voto
          if (poll.isOpen && !hasVoted) ...[
            const SizedBox(height: 14),
            CustomButton(
              text: 'Emitir Mi Voto',
              isLoading: widget.viewModel.isLoading,
              onPressed: _selectedOptionId != null
                  ? () {
                      final selected = poll.opciones
                          .firstWhere((o) => o.idOpcion == _selectedOptionId);
                      _confirmVote(poll, selected);
                    }
                  : null,
            ),
          ],
        ],
      ),
    );
  }
}
