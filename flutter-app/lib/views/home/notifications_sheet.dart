import 'package:flutter/material.dart';
import 'package:fluentui_system_icons/fluentui_system_icons.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/bouncy_tap.dart';
import '../../core/widgets/glass_container.dart';
import '../../core/widgets/pulsing_badge.dart';
import '../../data/models/notification_model.dart';

/// Modal Glassmórfico de Centro de Notificaciones y Avisos Comunitarios con Deep-Linking.
class NotificationsSheet extends StatefulWidget {
  final List<NotificationModel> initialNotifications;
  final VoidCallback? onNavigateToPayments;
  final VoidCallback? onNavigateToCommunity;
  final VoidCallback? onNavigateToVoting;

  const NotificationsSheet({
    super.key,
    required this.initialNotifications,
    this.onNavigateToPayments,
    this.onNavigateToCommunity,
    this.onNavigateToVoting,
  });

  static void show(
    BuildContext context, {
    required List<NotificationModel> notifications,
    VoidCallback? onNavigateToPayments,
    VoidCallback? onNavigateToCommunity,
    VoidCallback? onNavigateToVoting,
  }) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (ctx) => NotificationsSheet(
        initialNotifications: notifications,
        onNavigateToPayments: onNavigateToPayments,
        onNavigateToCommunity: onNavigateToCommunity,
        onNavigateToVoting: onNavigateToVoting,
      ),
    );
  }

  @override
  State<NotificationsSheet> createState() => _NotificationsSheetState();
}

class _NotificationsSheetState extends State<NotificationsSheet> {
  late List<NotificationModel> _notifications;

  @override
  void initState() {
    super.initState();
    _notifications = List.from(widget.initialNotifications);
  }

  void _handleNotificationTap(NotificationModel notif, int index) {
    setState(() {
      _notifications[index] = notif.copyWith(isRead: true);
    });

    Navigator.pop(context);

    // Deep link según el tab destino
    if (notif.targetTabIndex == 1) {
      widget.onNavigateToPayments?.call();
    } else if (notif.targetTabIndex == 2) {
      widget.onNavigateToCommunity?.call();
    } else if (notif.targetTabIndex == 3) {
      widget.onNavigateToVoting?.call();
    }
  }

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
              Row(
                children: [
                  Text(
                    'Centro de Avisos',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                    decoration: BoxDecoration(
                      color: AppColors.brandBlue.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Text(
                      '${_notifications.where((n) => !n.isRead).length} nuevos',
                      style: const TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.bold,
                        color: AppColors.brandBlue,
                      ),
                    ),
                  ),
                ],
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

          if (_notifications.isEmpty)
            Container(
              padding: const EdgeInsets.symmetric(vertical: 36, horizontal: 20),
              alignment: Alignment.center,
              child: Column(
                children: [
                  Icon(
                    FluentIcons.alert_off_24_regular,
                    size: 40,
                    color: isDark ? Colors.white38 : AppColors.textSecondaryLight,
                  ),
                  const SizedBox(height: 12),
                  Text(
                    'No tienes avisos pendientes',
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Te notificaremos sobre nuevas asambleas, votaciones y cuotas.',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 12,
                      color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                    ),
                  ),
                ],
              ),
            )
          else
            ConstrainedBox(
              constraints: BoxConstraints(
                maxHeight: MediaQuery.sizeOf(context).height * 0.58,
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
                          : (notif.type == 'PROJECT'
                              ? FluentIcons.building_retail_toolbox_24_filled
                              : FluentIcons.payment_24_filled));
                  final iconColor = notif.type == 'MEETING'
                      ? AppColors.actionMint
                      : (notif.type == 'VOTE'
                          ? AppColors.actionPurple
                          : (notif.type == 'PROJECT'
                              ? AppColors.brandSky
                              : AppColors.actionCoral));

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
                    onTap: () => _handleNotificationTap(notif, index),
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
                              : AppColors.brandBlue.withValues(alpha: 0.25),
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
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                  children: [
                                    Expanded(
                                      child: Text(
                                        notif.title,
                                        style: TextStyle(
                                          fontSize: 13.5,
                                          fontWeight: notif.isRead ? FontWeight.w600 : FontWeight.bold,
                                          color: isDark ? AppColors.textPrimaryDark : AppColors.textPrimaryLight,
                                        ),
                                      ),
                                    ),
                                    if (!notif.isRead)
                                      Container(
                                        width: 7,
                                        height: 7,
                                        decoration: const BoxDecoration(
                                          color: AppColors.brandBlue,
                                          shape: BoxShape.circle,
                                        ),
                                      ),
                                  ],
                                ),
                                const SizedBox(height: 3),
                                Text(
                                  notif.message,
                                  style: TextStyle(
                                    fontSize: 12,
                                    color: isDark ? AppColors.textSecondaryDark : AppColors.textSecondaryLight,
                                  ),
                                ),
                                const SizedBox(height: 6),
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                  children: [
                                    Text(
                                      Formatters.time(notif.timestamp),
                                      style: TextStyle(
                                        fontSize: 11,
                                        color: isDark ? AppColors.textMutedDark : AppColors.textMutedLight,
                                      ),
                                    ),
                                    Row(
                                      children: [
                                        Text(
                                          'Ver detalle',
                                          style: TextStyle(
                                            fontSize: 11,
                                            fontWeight: FontWeight.bold,
                                            color: isDark ? AppColors.brandSky : AppColors.brandBlue,
                                          ),
                                        ),
                                        const SizedBox(width: 2),
                                        Icon(
                                          FluentIcons.chevron_right_12_regular,
                                          size: 11,
                                          color: isDark ? AppColors.brandSky : AppColors.brandBlue,
                                        ),
                                      ],
                                    ),
                                  ],
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
