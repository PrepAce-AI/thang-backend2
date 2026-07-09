package backend.service;
import backend.entity.Notification;
import backend.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findTop10ByOrderByCreatedAtDesc();
    }

    @Transactional
    public Notification createNotification(String title, String content, String targetRole, Integer createdBy) {
        Notification noti = new Notification();
        noti.setTitle(title);
        noti.setContent(content);
        noti.setTargetRole(targetRole);
        noti.setCreatedAt(new Date());
        noti.setCreatedBy(createdBy);

        return notificationRepository.save(noti);
    }

    public List<Notification> getNotificationsByRole(String role) {
        if ("ADMIN".equals(role)) {
            return notificationRepository.findAll();
        }
        return notificationRepository.findByTargetRoleOrTargetRole(role, "ALL");
    }
}
