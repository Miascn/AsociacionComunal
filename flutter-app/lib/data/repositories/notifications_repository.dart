import '../models/meeting_model.dart';
import '../models/notification_model.dart';
import '../models/payment_model.dart';
import '../models/project_model.dart';
import '../models/voting_model.dart';

/// Repositorio de notificaciones dinámicas del residente basado en eventos reales.
class NotificationsRepository {
  final Set<String> _readIds = {};

  List<NotificationModel> buildNotifications({
    required List<PaymentModel> payments,
    required List<VotingModel> polls,
    required List<MeetingModel> meetings,
    required List<ProjectModel> projects,
    required int? currentMemberId,
  }) {
    final list = <NotificationModel>[];
    final now = DateTime.now();
    final currentMonthStr = '${now.year}-${now.month.toString().padLeft(2, '0')}';

    // 1. Estado de cuota mensual
    final hasPaidCurrentMonth = payments.any(
      (p) => p.isMonthlyFee && p.periodoMes == currentMonthStr && p.isPaid,
    );

    if (!hasPaidCurrentMonth) {
      final id = 'notif_cuota_$currentMonthStr';
      list.add(NotificationModel(
        id: id,
        title: 'Cuota Mensual Pendiente',
        message: 'Tu cuota de mantenimiento de $currentMonthStr está lista para pago.',
        timestamp: DateTime(now.year, now.month, 1, 8, 0),
        type: 'PAYMENT',
        isRead: _readIds.contains(id),
        targetTabIndex: 1, // Pestaña de Pagos
      ));
    }

    // 2. Votaciones abiertas
    for (final poll in polls) {
      if (poll.isOpen) {
        final id = 'notif_poll_${poll.id}';
        list.add(NotificationModel(
          id: id,
          title: 'Votación Comunitaria Activa',
          message: '${poll.titulo}: Tu participación es importante para la colonia.',
          timestamp: DateTime.now().subtract(const Duration(hours: 4)),
          type: 'VOTE',
          isRead: _readIds.contains(id),
          targetTabIndex: 3, // Pestaña de Votaciones
          actionPayload: poll.id.toString(),
        ));
      }
    }

    // 3. Próximas reuniones convocadas
    for (final meeting in meetings) {
      if (meeting.isUpcoming) {
        final id = 'notif_meeting_${meeting.id}';
        final projectContext = meeting.nombreProyecto != null
            ? ' para tratar el proyecto "${meeting.nombreProyecto}"'
            : '';
        list.add(NotificationModel(
          id: id,
          title: 'Convocatoria: ${meeting.titulo}',
          message: 'Reunión en ${meeting.lugar} el ${meeting.fechaHora}$projectContext.',
          timestamp: DateTime.now().subtract(const Duration(hours: 12)),
          type: 'MEETING',
          isRead: _readIds.contains(id),
          targetTabIndex: 2, // Pestaña Comunidad (Reuniones)
          actionPayload: meeting.id.toString(),
        ));
      }
    }

    // 4. Proyectos en recaudación o ejecución
    for (final project in projects) {
      if (project.canReceiveContributions) {
        final id = 'notif_proj_${project.id}';
        list.add(NotificationModel(
          id: id,
          title: 'Proyecto Comunal: ${project.nombre}',
          message: 'Presupuesto: \$${project.presupuesto.toStringAsFixed(2)}. Ya puedes realizar tu aporte voluntario.',
          timestamp: DateTime.now().subtract(const Duration(days: 1)),
          type: 'PROJECT',
          isRead: _readIds.contains(id),
          targetTabIndex: 2, // Pestaña Comunidad (Proyectos)
          actionPayload: project.id.toString(),
        ));
      }
    }

    // Ordenar de más reciente a más antiguo
    list.sort((a, b) => b.timestamp.compareTo(a.timestamp));
    return list;
  }

  void markAsRead(String id) {
    _readIds.add(id);
  }

  void markAllAsRead(List<NotificationModel> items) {
    for (final item in items) {
      _readIds.add(item.id);
    }
  }

  int countUnread(List<NotificationModel> items) {
    return items.where((n) => !_readIds.contains(n.id) && !n.isRead).length;
  }
}
