/// Modelo para notificaciones y avisos comunitarios.
class NotificationModel {
  final String id;
  final String title;
  final String message;
  final DateTime timestamp;
  final String type; // 'MEETING', 'PAYMENT', 'PROJECT', 'VOTE', 'ANNOUNCEMENT'
  final bool isRead;
  final int targetTabIndex;
  final String? actionPayload;

  const NotificationModel({
    required this.id,
    required this.title,
    required this.message,
    required this.timestamp,
    required this.type,
    this.isRead = false,
    this.targetTabIndex = 0,
    this.actionPayload,
  });

  NotificationModel copyWith({bool? isRead}) {
    return NotificationModel(
      id: id,
      title: title,
      message: message,
      timestamp: timestamp,
      type: type,
      isRead: isRead ?? this.isRead,
      targetTabIndex: targetTabIndex,
      actionPayload: actionPayload,
    );
  }
}
