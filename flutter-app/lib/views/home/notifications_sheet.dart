import 'package:flutter/material.dart';
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/bouncy_tap.dart';
import '../../core/widgets/glass_container.dart';
import '../../core/widgets/pulsing_badge.dart';
import '../../data/models/notification_model.dart';

/// Modal Glassmórfico de Centro de Notificaciones y Avisos.
class NotificationsSheet extends StatefulWidget {
  const NotificationsSheet({super.key});

  static void show(BuildContext context) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (ctx) => const NotificationsSheet(),
    );
  }

  @override
  State<NotificationsSheet> createState() => _NotificationsSheetState();
}

class _NotificationsSheetState extends State<NotificationsSheet> {
  final List<NotificationModel> _notifications = [
    NotificationModel(
      id: '1',
      title: 'Próxima Asamblea General',
      message: 'Se convoca a todos los miembros a la asamblea del mes en la Casa Comunal.',
      timestamp: DateTime.now().subtract(const Duration(hours: 3)),
      type: 'MEETING',
    ),
    NotificationModel(
      id: '2',
      title: 'Proceso de Votación Abierto',
      message: 'Participa en la consulta sobre la instalación de luminarias solares.',
      timestamp: DateTime.now().subtract(const Duration(hours: 8)),
      type: 'VOTE',
    ),
    NotificationModel(
      id: '3',
      title: 'Aportación Mensual',
      message: 'Recuerda que la cuota de mantenimiento de este período ya está disponible.',
      timestamp: DateTime.now().subtract(const Duration(days: 1)),
      type: 'PAYMENT',
      isRead: true,
    ),
  ];

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return GlassContainer(
      borderRadius: const BorderRadius.vertical(top: Radius.circular(30)),
      padding: const EdgeInsets.fromLTRB(20, 16, 20, 32),
      gradient: LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: [
          (isDark ? const Color(0xF2131D33) : const Color(0xF5FFFFFF)),
          (isDark ? const Color(0xF20B1120) : const Color(0xFAF4F7FC)),
        ],
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
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
          const SizedBox(height: 18),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Centro de Avisos',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                ),
              ),
              TextButton(
                onPressed: () {
                  setState(() {
                    for (int i = 0; i < _notifications.length; i++) {
                      _notifications[i] = _notifications[i].copyWith(isRead: true);
                    }
                  });
                },
                child: const Text('Marcar leídos', style: TextStyle(fontSize: 12)),
              ),
            ],
          ),
          const SizedBox(height: 14),

          ConstrainedBox(
            constraints: BoxConstraints(
              maxHeight: MediaQuery.sizeOf(context).height * 0.55,
            ),
            child: ListView.separated(
              shrinkWrap: true,
              itemCount: _notifications.length,
              separatorBuilder: (_, __) => const SizedBox(height: 10),
              itemBuilder: (context, index) {
                final notif = _notifications[index];
                final iconData = notif.type == 'MEETING'
                    ? FluentIcons.calendar_clock_24_filled
                    : (notif.type == 'VOTE'
                        ? FluentIcons.vote_24_filled
                        : FluentIcons.payment_24_filled);
                final iconColor = notif.type == 'MEETING'
                    ? AppColors.actionMint
                    : (notif.type == 'VOTE'
                        ? AppColors.actionPurple
                        : AppColors.actionCoral);

                final iconWidget = Container(
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: iconColor.withValues(alpha: 0.15),
                  ),
                  child: Icon(iconData, color: iconColor, size: 20),
                );

                return BouncyTap(
                  scaleDown: 0.98,
                  onTap: () {
                    setState(() {
                      _notifications[index] = notif.copyWith(isRead: true);
                    });
                  },
                  child: Container(
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(18),
                      color: notif.isRead
                          ? (isDark ? const Color(0x201E293B) : const Color(0x20FFFFFF))
                          : (isDark ? const Color(0x401E293B) : Colors.white),
                      border: Border.all(
                        color: notif.isRead
                            ? Colors.transparent
                            : AppColors.brandBlue.withValues(alpha: 0.2),
                      ),
                    ),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        notif.isRead
                            ? iconWidget
                            : PulsingBadge(
                                minScale: 0.94,
                                maxScale: 1.08,
                                child: iconWidget,
                              ),
                        const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              notif.title,
                              style: TextStyle(
                                fontSize: 14,
                                fontWeight: notif.isRead ? FontWeight.w600 : FontWeight.bold,
                              ),
                            ),
                            const SizedBox(height: 3),
                            Text(
                              notif.message,
                              style: TextStyle(
                                fontSize: 12.5,
                                color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                              ),
                            ),
                            const SizedBox(height: 6),
                            Text(
                              Formatters.time(notif.timestamp),
                              style: TextStyle(
                                fontSize: 11,
                                color: isDark ? AppColors.textMutedDark : AppColors.textMutedLight,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              );
            },
            ),
          ),
        ],
      ),
    );
  }
}
