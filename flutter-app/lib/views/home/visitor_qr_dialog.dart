import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/widgets/bouncy_tap.dart';

/// Diálogo interactivo para generación y consulta del Pase Digital de Visita QR
/// según la especificación Stitch (Colonia Conecta) para la Caseta de Vigilancia.
class VisitorQrDialog extends StatefulWidget {
  final String residentName;
  final String residentUnit;

  const VisitorQrDialog({
    super.key,
    required this.residentName,
    required this.residentUnit,
  });

  static Future<void> show(BuildContext context, {required String residentName, required String residentUnit}) {
    return showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => VisitorQrDialog(
        residentName: residentName,
        residentUnit: residentUnit,
      ),
    );
  }

  @override
  State<VisitorQrDialog> createState() => _VisitorQrDialogState();
}

class _VisitorQrDialogState extends State<VisitorQrDialog> {
  int _selectedTypeIndex = 0;
  final List<String> _types = ['Invitado', 'Delivery / Pedido', 'Familiar', 'Servicio Técnico'];

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      padding: const EdgeInsets.fromLTRB(20, 16, 20, 32),
      decoration: BoxDecoration(
        color: isDark ? const Color(0xFF1E293B) : Colors.white,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(28)),
        boxShadow: const [
          BoxShadow(
            color: Color(0x33000000),
            blurRadius: 30,
            offset: Offset(0, -10),
          ),
        ],
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Drag handle
          Container(
            width: 44,
            height: 4.5,
            decoration: BoxDecoration(
              color: Colors.grey.withValues(alpha: 0.3),
              borderRadius: BorderRadius.circular(3),
            ),
          ),
          const SizedBox(height: 16),
          // Encabezado
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  Container(
                    width: 38,
                    height: 38,
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(10),
                      color: AppColors.stitchTeal.withValues(alpha: 0.12),
                    ),
                    child: const Icon(
                      Icons.qr_code_2_rounded,
                      color: AppColors.stitchTeal,
                      size: 24,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Pase Digital de Visita',
                        style: TextStyle(
                          fontSize: 17,
                          fontWeight: FontWeight.bold,
                          color: isDark ? Colors.white : AppColors.stitchTextPrimary,
                        ),
                      ),
                      Text(
                        'Garita de Control · Acceso Rápido',
                        style: TextStyle(
                          fontSize: 12,
                          color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
              IconButton(
                onPressed: () => Navigator.of(context).pop(),
                icon: const Icon(Icons.close_rounded),
                color: isDark ? Colors.white70 : Colors.black54,
              ),
            ],
          ),
          const SizedBox(height: 18),
          // Selector de tipo de visita
          SizedBox(
            height: 36,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: _types.length,
              separatorBuilder: (_, __) => const SizedBox(width: 8),
              itemBuilder: (context, index) {
                final isSelected = _selectedTypeIndex == index;
                return BouncyTap(
                  onTap: () => setState(() => _selectedTypeIndex = index),
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 200),
                    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(20),
                      color: isSelected
                          ? AppColors.stitchSapphire
                          : (isDark ? const Color(0xFF0F172A) : const Color(0xFFF1F5F9)),
                    ),
                    child: Center(
                      child: Text(
                        _types[index],
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
                          color: isSelected
                              ? Colors.white
                              : (isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary),
                        ),
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
          const SizedBox(height: 20),
          // Tarjeta QR Scannable
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: isDark ? const Color(0xFF0F172A) : const Color(0xFFF8FAFC),
              borderRadius: BorderRadius.circular(24),
              border: Border.all(
                color: isDark ? const Color(0xFF334155) : const Color(0xFFE2E8F0),
              ),
            ),
            child: Column(
              children: [
                // Visual QR representation
                Container(
                  width: 170,
                  height: 170,
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(16),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withValues(alpha: 0.08),
                        blurRadius: 16,
                        offset: const Offset(0, 4),
                      ),
                    ],
                  ),
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      // Simulated QR Pattern Grid
                      GridView.builder(
                        physics: const NeverScrollableScrollPhysics(),
                        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 7,
                          crossAxisSpacing: 3,
                          mainAxisSpacing: 3,
                        ),
                        itemCount: 49,
                        itemBuilder: (context, i) {
                          final isCorner = (i < 3 || (i >= 4 && i <= 6) || (i >= 7 && i <= 9) || (i >= 11 && i <= 13) || (i >= 35 && i <= 37) || (i >= 42 && i <= 44));
                          final isPattern = isCorner || (i % 2 == 0 && i % 3 != 0) || (i == 24);
                          return Container(
                            decoration: BoxDecoration(
                              color: isPattern ? const Color(0xFF0F172A) : Colors.white,
                              borderRadius: BorderRadius.circular(isCorner ? 2 : 1),
                            ),
                          );
                        },
                      ),
                      // Center Emblem
                      Container(
                        width: 32,
                        height: 32,
                        decoration: BoxDecoration(
                          color: AppColors.stitchTeal,
                          borderRadius: BorderRadius.circular(8),
                          border: Border.all(color: Colors.white, width: 2),
                        ),
                        child: const Icon(
                          Icons.security_rounded,
                          color: Colors.white,
                          size: 16,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 14),
                // Folio y Datos
                Text(
                  'FOLIO #QR-2026-${widget.residentUnit.replaceAll(RegExp(r'[^0-9]'), '')}B',
                  style: const TextStyle(
                    fontFamily: 'monospace',
                    fontSize: 12.5,
                    fontWeight: FontWeight.bold,
                    letterSpacing: 1.2,
                    color: AppColors.stitchSapphire,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  '${widget.residentUnit} · Autoriza: ${widget.residentName}',
                  style: TextStyle(
                    fontSize: 12,
                    color: isDark ? AppColors.stitchTextMuted : AppColors.stitchTextSecondary,
                  ),
                ),
                const SizedBox(height: 10),
                // Expiración con píldora
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: AppColors.stitchEmeraldContainer,
                    borderRadius: BorderRadius.circular(12),
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
                      const SizedBox(width: 6),
                      const Text(
                        'Válido por 4 horas (expira hoy 11:59 PM)',
                        style: TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.w600,
                          color: Color(0xFF065F46),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          // Botón Compartir
          BouncyTap(
            onTap: () {
              Navigator.of(context).pop();
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text('Pase de visita copiado al portapapeles para compartir.'),
                  backgroundColor: AppColors.stitchSapphire,
                  duration: Duration(seconds: 3),
                ),
              );
            },
            child: Container(
              height: 52,
              width: double.infinity,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(26),
                gradient: const LinearGradient(
                  colors: [AppColors.stitchSapphire, AppColors.stitchBlueLight],
                ),
                boxShadow: [
                  BoxShadow(
                    color: AppColors.stitchSapphire.withValues(alpha: 0.3),
                    blurRadius: 14,
                    offset: const Offset(0, 5),
                  ),
                ],
              ),
              child: const Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.share_rounded, color: Colors.white, size: 20),
                  SizedBox(width: 8),
                  Text(
                    'Compartir Pase con el Visitante',
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
        ],
      ),
    );
  }
}
