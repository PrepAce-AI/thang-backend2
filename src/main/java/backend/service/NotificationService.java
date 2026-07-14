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
    // 🔥 ĐÃ CẬP NHẬT: Thêm tham số 'Integer receiverId' vào cuối hàm
    public Notification createNotification(String title, String content, String targetRole, Integer createdBy, Integer receiverId) {
        Notification noti = new Notification();
        noti.setTitle(title);
        noti.setContent(content);
        noti.setTargetRole(targetRole);
        noti.setCreatedAt(new Date());
        noti.setCreatedBy(createdBy);

        // 🔥 THÊM DÒNG NÀY: Để lưu ID học sinh nhận thông báo vào Database
        noti.setReceiverId(receiverId);

        return notificationRepository.save(noti);
    }

    public List<Notification> getNotificationsByRole(String role) {
        if ("ADMIN".equals(role)) {
            return notificationRepository.findAll();
        }
        return notificationRepository.findByTargetRoleOrTargetRole(role, "ALL");
    }
    // 🔥 THÊM HÀM NÀY VÀO TRONG NOTIFICATION_SERVICE
    public List<Notification> getNotificationsForUser(String role, Integer userId) {
        // Nếu không có userId truyền lên (ví dụ khách hoặc lỗi), tạm thời lấy theo role cũ
        if (userId == null) {
            return getNotificationsByRole(role);
        }
        // Gọi xuống Repository để lấy cả thông báo chung lẫn nhắc lịch cá nhân
        return notificationRepository.findMyNotifications(role, userId);
    }
}