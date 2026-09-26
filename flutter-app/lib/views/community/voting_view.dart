import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/widgets/bouncy_tap.dart';
import '../../core/widgets/community_top_header.dart';
import '../../core/widgets/empty_state.dart';
import '../../core/widgets/fade_slide_entrance.dart';
import '../../data/models/voting_model.dart';
import '../../viewmodels/voting_viewmodel.dart';

/// Pantalla de Consulta Electoral y Decisión Comunal
/// Replicando con máxima fidelidad la interfaz Stitch (Colonia Conecta).
class VotingView extends StatefulWidget {
  final VotingViewModel viewModel;
  final int? idMiembro;
  final VoidCallback? onBack;

  const VotingView({
    super.key,
    required this.viewModel,
    this.idMiembro,
    this.onBack,
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
    final isDark = Theme.of(context).brightness == Brightness.dark;

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => Dialog(
        backgroundColor: Colors.transparent,
        insetPadding: const EdgeInsets.symmetric(horizontal: 24),
        child: Container(
          padding: const EdgeInsets.all(22),
          decoration: BoxDecoration(
            color: isDark ? const Color(0xFF1E293B) : Colors.white,
            borderRadius: BorderRadius.circular(24),
            border: Border.all(
              color: isDark ? const Color(0xFF334155) : const Color(0xFFE2E8F0),
            ),
            boxShadow: const [
              BoxShadow(
                color: Color(0x33000000),
                blurRadius: 28,
                offset: Offset(0, 8),
              ),
            ],
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 52,
                height: 52,
                decoration: const BoxDecoration(
                  shape: BoxShape.circle,
                  color: Color(0xFFEEF2FF),
                ),
                child: const Icon(
                  Icons.how_to_vote_rounded,
                  color: AppColors.stitchSapphire,
                  size: 26,
                ),
              ),
              const SizedBox(height: 16),
              Text(
                'Confirmar Voto Democrático',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: isDark ? Colors.white : AppColors.stitchTextPrimary,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                '¿Confirmas tu elección para "${poll.titulo}"?',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 12.5,
                  color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                ),
              ),
              const SizedBox(height: 14),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: const Color(0xFFEEF2FF),
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: const Color(0xFFC7D2FE)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'DECISIÓN SELECCIONADA',
                      style: TextStyle(
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                        color: AppColors.stitchSapphire,
                        letterSpacing: 0.5,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      option.texto,
                      style: const TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.bold,
                        color: AppColors.stitchNavy,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 14),
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: const Color(0xFFD1FAE5),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Row(
                  children: [
                    Icon(Icons.lock_rounded, size: 16, color: Color(0xFF065F46)),
                    SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        'Tu voto será cifrado y registrado en el acta electoral digital de forma irreversible.',
                        style: TextStyle(fontSize: 11, color: Color(0xFF065F46), fontWeight: FontWeight.w600),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),
              Row(
                children: [
                  Expanded(
                    child: BouncyTap(
                      onTap: () => Navigator.of(ctx).pop(),
                      child: Container(
                        height: 46,
                        decoration: BoxDecoration(
                          color: isDark ? const Color(0xFF334155) : const Color(0xFFF1F5F9),
                          borderRadius: BorderRadius.circular(23),
                        ),
                        child: Center(
                          child: Text(
                            'Cancelar',
                            style: TextStyle(
                              color: isDark ? Colors.white70 : AppColors.stitchTextSecondary,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: BouncyTap(
                      onTap: () async {
                        Navigator.of(ctx).pop();
                        final success = await widget.viewModel.submitVote(
                          idVotacion: poll.id,
                          idOpcion: option.id,
                          idMiembro: widget.idMiembro,
                        );
                        if (mounted && success) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                              content: Text('¡Voto emitido y sellado en el acta comunal exitosamente!'),
                              backgroundColor: AppColors.stitchEmerald,
                            ),
                          );
                        }
                      },
                      child: Container(
                        height: 46,
                        decoration: BoxDecoration(
                          color: AppColors.stitchSapphire,
                          borderRadius: BorderRadius.circular(23),
                        ),
                        child: const Center(
                          child: Text(
                            'Emitir Voto',
                            style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
                          ),
                        ),
                      ),
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

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      backgroundColor: isDark ? const Color(0xFF0F172A) : AppColors.stitchCanvasLight,
      body: SafeArea(
        bottom: false,
        child: RefreshIndicator(
          onRefresh: () => widget.viewModel.loadPolls(idMiembro: widget.idMiembro),
          color: AppColors.stitchSapphire,
          child: ListenableBuilder(
            listenable: widget.viewModel,
            builder: (context, _) {
              final polls = widget.viewModel.polls;

              if (polls.isEmpty) {
                return SingleChildScrollView(
                  physics: const AlwaysScrollableScrollPhysics(),
                  padding: const EdgeInsets.fromLTRB(16, 8, 16, 110),
                  child: Column(
                    children: [
                      CommunityTopHeader(
                        title: 'Votación Activa',
                        subtitle: 'Residencial Las Flores',
                        onBack: widget.onBack,
                        residentName: 'Carlos Mendoza',
                      ),
                      const SizedBox(height: 60),
                      const EmptyStateWidget(
                        icon: Icons.how_to_vote_rounded,
                        title: 'No hay votaciones activas',
                        message: 'Cuando la junta directiva convoque a consulta popular, podrás emitir tu voto aquí.',
                      ),
                    ],
                  ),
                );
              }

              final activePoll = polls.firstWhere((p) => p.isOpen, orElse: () => polls.first);

              return SingleChildScrollView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 110),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // 1. Barra Superior Estandarizada
                    CommunityTopHeader(
                      title: 'Votación Activa',
                      subtitle: 'Residencial Las Flores',
                      onBack: widget.onBack,
                      residentName: 'Carlos Mendoza',
                    ),
                    const SizedBox(height: 12),

                    // 2. Status Bar Pill & Timeline
                    FadeSlideEntrance(
                      delay: Duration.zero,
                      child: _buildStatusPillTimeline(isDark, activePoll),
                    ),
                    const SizedBox(height: 16),

                    // 3. Tarjeta Hero del Proyecto a Votar
                    FadeSlideEntrance(
                      delay: const Duration(milliseconds: 40),
                      child: _buildProjectContextHeroCard(isDark, activePoll),
                    ),
                    const SizedBox(height: 16),

                    // 4. Datos Clave de Transparencia (Grid 2x2)
                    FadeSlideEntrance(
                      delay: const Duration(milliseconds: 80),
                      child: _buildTransparencyKeyMetrics(isDark),
                    ),
                    const SizedBox(height: 18),

                    // 5. Selección de Decisión
                    FadeSlideEntrance(
                      delay: const Duration(milliseconds: 120),
                      child: _buildVotingDecisionSection(isDark, activePoll),
                    ),
                    const SizedBox(height: 20),

                    // 6. Botones de Acción
                    FadeSlideEntrance(
                      delay: const Duration(milliseconds: 160),
                      child: _buildActionButtons(isDark, activePoll),
                    ),
                  ],
                ),
              );
            },
          ),
        ),
      ),
    );
  }

  /// 2. Status Bar Pill & Timeline
  Widget _buildStatusPillTimeline(bool isDark, VotingModel poll) {
    final isOpen = poll.isOpen;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      decoration: BoxDecoration(
        color: isDark ? const Color(0xFF1E293B) : Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(
          color: isDark ? const Color(0xFF334155) : const Color(0xFFE2E8F0),
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: isDark ? 0.2 : 0.03),
            blurRadius: 6,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Flexible(
            child: Row(
              children: [
                Container(
                  width: 8,
                  height: 8,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: isOpen ? AppColors.stitchEmerald : AppColors.stitchTextSecondary,
                  ),
                ),
                const SizedBox(width: 8),
                Flexible(
                  child: Text(
                    isOpen ? 'VOTACIÓN ACTIVA' : 'CONSULTA FINALIZADA',
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w800,
                      letterSpacing: 0.6,
                      color: isOpen ? AppColors.stitchEmerald : AppColors.stitchTextSecondary,
                    ),
                    overflow: TextOverflow.ellipsis,
                    maxLines: 1,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.schedule_rounded, size: 14, color: AppColors.stitchSapphire),
              const SizedBox(width: 5),
              Text(
                isOpen ? 'Cierra en 2 días' : 'Escrutinio cerrado',
                style: TextStyle(
                  fontSize: 11.5,
                  fontWeight: FontWeight.w600,
                  color: isDark ? Colors.white70 : AppColors.stitchTextSecondary,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  /// 3. Tarjeta Hero del Proyecto a Votar
  Widget _buildProjectContextHeroCard(bool isDark, VotingModel poll) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: isDark ? const Color(0xFF1E293B) : Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(
          color: isDark ? const Color(0xFF334155) : const Color(0xFFE2E8F0),
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: isDark ? 0.2 : 0.04),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: const Color(0xFFCCFBF1),
                  borderRadius: BorderRadius.circular(14),
                ),
                child: const Row(
                  children: [
                    Icon(Icons.construction_rounded, size: 13, color: Color(0xFF0F766E)),
                    SizedBox(width: 5),
                    Text(
                      'Infraestructura y Vías',
                      style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.bold, color: Color(0xFF0F766E)),
                    ),
                  ],
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: isDark ? const Color(0xFF0F172A) : const Color(0xFFF1F5F9),
                  borderRadius: BorderRadius.circular(14),
                ),
                child: Text(
                  'Folio #VOT-2026-${poll.id.toString().padLeft(2, '0')}',
                  style: TextStyle(
                    fontSize: 10.5,
                    fontWeight: FontWeight.w600,
                    color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            poll.titulo,
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w800,
              letterSpacing: -0.4,
              color: isDark ? Colors.white : AppColors.stitchTextPrimary,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            poll.descripcion.isNotEmpty
                ? poll.descripcion
                : 'Bacheo integral con mezcla asfáltica en caliente y sellado de grietas desde la caseta de acceso norte hasta la glorieta central.',
            style: TextStyle(
              fontSize: 12.5,
              color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
              height: 1.4,
            ),
          ),
          const SizedBox(height: 14),
          // Foto preview
          ClipRRect(
            borderRadius: BorderRadius.circular(16),
            child: Container(
              height: 130,
              width: double.infinity,
              decoration: const BoxDecoration(
                gradient: LinearGradient(
                  colors: [Color(0xFF1E293B), Color(0xFF0F172A)],
                ),
              ),
              child: Stack(
                children: [
                  Center(
                    child: Icon(
                      Icons.landscape_rounded,
                      size: 48,
                      color: Colors.white.withValues(alpha: 0.15),
                    ),
                  ),
                  Positioned(
                    bottom: 10,
                    left: 12,
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: Colors.black.withValues(alpha: 0.65),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: const Row(
                        children: [
                          Icon(Icons.location_on_rounded, size: 12, color: Colors.white),
                          SizedBox(width: 4),
                          Text(
                            'Tramo: Av. Las Acacias (850m lineales)',
                            style: TextStyle(color: Colors.white, fontSize: 10.5, fontWeight: FontWeight.w500),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 14),
          // Pregunta oficial en caja tipo quote
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: isDark ? const Color(0xFF0F172A) : const Color(0xFFF8FAFC),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(
                color: isDark ? const Color(0xFF334155) : const Color(0xFFE2E8F0),
              ),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'PREGUNTA OFICIAL',
                  style: TextStyle(
                    fontSize: 9.5,
                    fontWeight: FontWeight.w800,
                    letterSpacing: 0.6,
                    color: AppColors.stitchSapphire,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  '"¿Aprueba usted la contratación y realización del proyecto comunal para su ejecución según el presupuesto establecido?"',
                  style: TextStyle(
                    fontSize: 13,
                    fontStyle: FontStyle.italic,
                    fontWeight: FontWeight.w600,
                    color: isDark ? Colors.white : AppColors.stitchTextPrimary,
                    height: 1.35,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  /// 4. Datos Clave de Transparencia (Grid 2x2)
  Widget _buildTransparencyKeyMetrics(bool isDark) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: isDark ? const Color(0xFF1E293B) : Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(
          color: isDark ? const Color(0xFF334155) : const Color(0xFFE2E8F0),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Expanded(
                child: Row(
                  children: [
                    Icon(Icons.analytics_rounded, size: 18, color: AppColors.stitchSapphire),
                    SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        'Datos Clave de Transparencia',
                        style: TextStyle(
                          fontSize: 14.5,
                          fontWeight: FontWeight.bold,
                          color: AppColors.stitchTextPrimary,
                        ),
                        overflow: TextOverflow.ellipsis,
                        maxLines: 1,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: AppColors.stitchEmeraldContainer,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Text(
                  'Auditado',
                  style: TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF065F46),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          // Grid 2x2
          Row(
            children: [
              _buildKeyMetricBox(
                icon: Icons.payments_rounded,
                label: 'Presupuesto total',
                value: '\$1,850.00',
                sublabel: '3 cotizaciones recibidas',
                valueColor: AppColors.stitchSapphire,
                isDark: isDark,
              ),
              const SizedBox(width: 10),
              _buildKeyMetricBox(
                icon: Icons.savings_rounded,
                label: 'Aporte por casa',
                value: '\$25.00',
                sublabel: 'Pago extraordinario único',
                valueColor: AppColors.stitchEmerald,
                isDark: isDark,
              ),
            ],
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              _buildKeyMetricBox(
                icon: Icons.speed_rounded,
                label: 'Tiempo de obra',
                value: '10 días',
                sublabel: 'Días hábiles continuos',
                valueColor: isDark ? Colors.white : AppColors.stitchTextPrimary,
                isDark: isDark,
              ),
              const SizedBox(width: 10),
              _buildKeyMetricBox(
                icon: Icons.how_to_vote_rounded,
                label: 'Quórum mínimo',
                value: '60 casas',
                sublabel: '80% de representatividad',
                valueColor: isDark ? Colors.white : AppColors.stitchTextPrimary,
                isDark: isDark,
              ),
            ],
          ),
          const SizedBox(height: 16),
          // Quórum meter
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: isDark ? const Color(0xFF0F172A) : const Color(0xFFEEF2FF),
              borderRadius: BorderRadius.circular(14),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: Text(
                        'Participación Actual del Censo',
                        style: TextStyle(fontSize: 11.5, fontWeight: FontWeight.bold, color: AppColors.stitchSapphire),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    SizedBox(width: 8),
                    Text(
                      '48 de 75 (64%)',
                      style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.stitchEmerald),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                ClipRRect(
                  borderRadius: BorderRadius.circular(4),
                  child: const LinearProgressIndicator(
                    value: 0.64,
                    minHeight: 7,
                    backgroundColor: Color(0xFFC7D2FE),
                    valueColor: AlwaysStoppedAnimation<Color>(AppColors.stitchEmerald),
                  ),
                ),
                const SizedBox(height: 6),
                const Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: Text(
                        'Faltan 12 votos para alcanzar quórum vinculante',
                        style: TextStyle(fontSize: 10.5, color: AppColors.stitchTextSecondary),
                        overflow: TextOverflow.ellipsis,
                        maxLines: 1,
                      ),
                    ),
                    SizedBox(width: 8),
                    Text(
                      'Meta: 60',
                      style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.bold, color: AppColors.stitchSapphire),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          // Botón Ficha Técnica
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
            decoration: BoxDecoration(
              color: isDark ? const Color(0xFF0F172A) : const Color(0xFFF8FAFC),
              borderRadius: BorderRadius.circular(14),
              border: Border.all(
                color: isDark ? const Color(0xFF334155) : const Color(0xFFE2E8F0),
              ),
            ),
            child: const Row(
              children: [
                Icon(Icons.description_rounded, size: 18, color: AppColors.stitchSapphire),
                SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Descargar cotización y ficha técnica',
                        style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.stitchSapphire),
                      ),
                      Text(
                        'Constructora Vial S.A. · 2.4 MB PDF',
                        style: TextStyle(fontSize: 10.5, color: AppColors.stitchTextSecondary),
                      ),
                    ],
                  ),
                ),
                Icon(Icons.download_rounded, size: 18, color: AppColors.stitchSapphire),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildKeyMetricBox({
    required IconData icon,
    required String label,
    required String value,
    required String sublabel,
    required Color valueColor,
    required bool isDark,
  }) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: isDark ? const Color(0xFF0F172A) : const Color(0xFFF8FAFC),
          borderRadius: BorderRadius.circular(14),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, size: 14, color: AppColors.stitchSapphire),
                const SizedBox(width: 5),
                Expanded(
                  child: Text(
                    label,
                    style: const TextStyle(fontSize: 10.5, color: AppColors.stitchTextSecondary),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 6),
            Text(
              value,
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800, color: valueColor),
            ),
            const SizedBox(height: 2),
            Text(
              sublabel,
              style: TextStyle(fontSize: 9.5, color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  }

  /// 5. Selección de Decisión
  Widget _buildVotingDecisionSection(bool isDark, VotingModel poll) {
    final alreadyVoted = widget.viewModel.hasUserVoted(poll.id);
    final isOpen = poll.isOpen;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Row(
          children: [
            Icon(Icons.how_to_vote_outlined, size: 18, color: AppColors.stitchSapphire),
            SizedBox(width: 6),
            Text(
              'Selecciona tu Decisión',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: AppColors.stitchTextPrimary,
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        if (alreadyVoted && isOpen) ...[
          Container(
            padding: const EdgeInsets.all(18),
            decoration: BoxDecoration(
              color: const Color(0xFFD1FAE5),
              borderRadius: BorderRadius.circular(20),
            ),
            child: const Row(
              children: [
                Icon(Icons.check_circle_rounded, color: Color(0xFF065F46), size: 28),
                SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '¡Tu voto ya ha sido sellado!',
                        style: TextStyle(fontSize: 14.5, fontWeight: FontWeight.bold, color: Color(0xFF065F46)),
                      ),
                      SizedBox(height: 3),
                      Text(
                        'Para preservar el secreto electoral y la equidad comunal, los resultados finales se publicarán al concluir el período de votación.',
                        style: TextStyle(fontSize: 11.5, color: Color(0xFF047857), height: 1.35),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ] else if (!isOpen) ...[
          // Resultados de Votación Cerrada
          Container(
            padding: const EdgeInsets.all(18),
            decoration: BoxDecoration(
              color: isDark ? const Color(0xFF1E293B) : Colors.white,
              borderRadius: BorderRadius.circular(20),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Escrutinio Final de la Consulta',
                  style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: AppColors.stitchSapphire),
                ),
                const SizedBox(height: 12),
                ...poll.opciones.map((opt) {
                  final total = poll.totalVotos;
                  final pct = total > 0 ? (opt.cantidadVotos / total) : 0.0;
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 10),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(opt.texto, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold)),
                            Text('${opt.cantidadVotos} votos (${(pct * 100).toStringAsFixed(0)}%)'),
                          ],
                        ),
                        const SizedBox(height: 4),
                        LinearProgressIndicator(value: pct, minHeight: 6),
                      ],
                    ),
                  );
                }),
              ],
            ),
          ),
        ] else ...[
          // Opciones interactivas de votación
          ...poll.opciones.map((option) {
            final isSelected = _selectedOptionId == option.id;
            final isApproved = option.texto.toLowerCase().contains('sí') || option.texto.toLowerCase().contains('aprobar');

            return Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: BouncyTap(
                onTap: () => setState(() => _selectedOptionId = option.id),
                child: Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: isDark ? const Color(0xFF1E293B) : Colors.white,
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(
                      color: isSelected
                          ? AppColors.stitchSapphire
                          : (isDark ? const Color(0xFF334155) : const Color(0xFFE2E8F0)),
                      width: isSelected ? 2.0 : 1.0,
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withValues(alpha: isDark ? 0.2 : 0.03),
                        blurRadius: 6,
                        offset: const Offset(0, 2),
                      ),
                    ],
                  ),
                  child: Row(
                    children: [
                      Container(
                        width: 44,
                        height: 44,
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(14),
                          color: isApproved ? const Color(0xFFD1FAE5) : const Color(0xFFFEE2E2),
                        ),
                        child: Icon(
                          isApproved ? Icons.thumb_up_rounded : Icons.thumb_down_rounded,
                          color: isApproved ? const Color(0xFF059669) : const Color(0xFFDC2626),
                          size: 20,
                        ),
                      ),
                      const SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              option.texto,
                              style: TextStyle(
                                fontSize: 14.5,
                                fontWeight: FontWeight.bold,
                                color: isDark ? Colors.white : AppColors.stitchTextPrimary,
                              ),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              isApproved
                                  ? 'Apoyo la ejecución del presupuesto establecido y me comprometo al aporte correspondiente.'
                                  : 'Considero que deben revisarse las cotizaciones o posponerse temporalmente la obra.',
                              style: TextStyle(
                                fontSize: 11.5,
                                color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                                height: 1.3,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 8),
                      Container(
                        width: 22,
                        height: 22,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: isSelected ? AppColors.stitchSapphire : Colors.transparent,
                          border: Border.all(
                            color: isSelected ? AppColors.stitchSapphire : const Color(0xFFCBD5E1),
                            width: 2,
                          ),
                        ),
                        child: isSelected
                            ? const Icon(Icons.check, size: 14, color: Colors.white)
                            : null,
                      ),
                    ],
                  ),
                ),
              ),
            );
          }),
          const SizedBox(height: 6),
          // Garantía de Voto Verificado
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: const Color(0xFFEEF2FF),
              borderRadius: BorderRadius.circular(18),
            ),
            child: const Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(Icons.verified_user_rounded, color: AppColors.stitchSapphire, size: 20),
                SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Expanded(
                            child: Text(
                              'Garantía de Voto Verificado',
                              style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.stitchSapphire),
                              overflow: TextOverflow.ellipsis,
                              maxLines: 1,
                            ),
                          ),
                          SizedBox(width: 8),
                          Text(
                            'Casa #42-B',
                            style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.bold, color: AppColors.stitchSapphire),
                          ),
                        ],
                      ),
                      SizedBox(height: 3),
                      Text(
                        'Tu voto es personal, intransferible y emitido legalmente a nombre del domicilio registrado. Una vez transmitido al libro de actas digital, quedará sellado de forma irreversible.',
                        style: TextStyle(fontSize: 11, color: Color(0xFF4338CA), height: 1.35),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ],
    );
  }

  /// 6. Botones de Acción
  Widget _buildActionButtons(bool isDark, VotingModel poll) {
    final alreadyVoted = widget.viewModel.hasUserVoted(poll.id);
    final isOpen = poll.isOpen;

    return Column(
      children: [
        if (isOpen && !alreadyVoted)
          BouncyTap(
            onTap: () {
              if (_selectedOptionId == null) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text('Por favor selecciona una de las opciones para continuar.'),
                    backgroundColor: Color(0xFFDC2626),
                  ),
                );
                return;
              }
              final opt = poll.opciones.firstWhere((o) => o.id == _selectedOptionId);
              _confirmVote(poll, opt);
            },
            child: Container(
              height: 52,
              width: double.infinity,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(26),
                color: AppColors.stitchSapphire,
                boxShadow: [
                  BoxShadow(
                    color: AppColors.stitchSapphire.withValues(alpha: 0.3),
                    blurRadius: 10,
                    offset: const Offset(0, 4),
                  ),
                ],
              ),
              child: const Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.how_to_vote_rounded, color: Colors.white, size: 20),
                  SizedBox(width: 8),
                  Text(
                    'Continuar a confirmación',
                    style: TextStyle(color: Colors.white, fontSize: 14.5, fontWeight: FontWeight.bold),
                  ),
                  SizedBox(width: 6),
                  Icon(Icons.arrow_forward_rounded, color: Colors.white, size: 16),
                ],
              ),
            ),
          ),
        const SizedBox(height: 10),
        BouncyTap(
          onTap: widget.onBack ?? () => Navigator.of(context).maybePop(),
          child: Container(
            height: 48,
            width: double.infinity,
            decoration: BoxDecoration(
              color: isDark ? const Color(0xFF1E293B) : const Color(0xFFF1F5F9),
              borderRadius: BorderRadius.circular(24),
            ),
            child: Center(
              child: Text(
                'Volver a Comunidad',
                style: TextStyle(
                  fontSize: 13.5,
                  fontWeight: FontWeight.bold,
                  color: isDark ? Colors.white70 : AppColors.stitchTextSecondary,
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}
