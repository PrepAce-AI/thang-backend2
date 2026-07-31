package backend.service;

import backend.entity.Notification;
import backend.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate; // 🔥 THÊM IMPORT NÀY
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;

    // 🔥 ĐÃ THÊM: Inject trực tiếp JdbcTemplate để xử lý các câu lệnh ép dữ liệu
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findTop10ByOrderByCreatedAtDesc();
    }

    @Transactional
    public Notification createNotification(String title, String content, String targetRole, Integer createdBy, Integer receiverId) {
        Notification noti = new Notification();
        noti.setTitle(title);
        noti.setContent(content);
        noti.setTargetRole(targetRole);
        noti.setCreatedAt(new Date());
        noti.setCreatedBy(createdBy);
        noti.setReceiverId(receiverId);

        // 1. Lưu qua JPA để lấy ra Notification_Id tự tăng trước
        Notification savedNoti = notificationRepository.save(noti);

        // 2. 🔥 ÉP CỨNG ĐỒNG BỘ: Sử dụng Native SQL để ghi đè cột user_id, chặn đứng lỗi dính NULL
        if (receiverId != null && savedNoti.getNotificationId() != null) {
            try {
                String sql = "UPDATE Notifications SET user_id = ? WHERE notification_id = ?";
                jdbcTemplate.update(sql, receiverId, savedNoti.getNotificationId());
                System.out.println("🚀 [Native SQL] Đã ép cứng ghi nhận cột user_id sang giá trị: " + receiverId);
            } catch (Exception e) {
                System.out.println("⚠️ Lỗi thực thi Native SQL: " + e.getMessage());
            }
        }

        return savedNoti;
    }

    @Transactional
    public void sendReplyNotification(Integer questionOwnerId, String replierName, String contentSummary, Integer createdBy) {
        try {
            String title = "💬 Phản hồi thảo luận mới";
            String content = replierName + " đã trả lời câu hỏi của bạn: \"" + contentSummary + "\"";

            // Gọi hàm xử lý ép ghi nhận dữ liệu
            this.createNotification(title, content, "STUDENT", createdBy, questionOwnerId);

            System.out.println("🎉 Đã tự động bắn thông báo phản hồi thảo luận cho User ID: " + questionOwnerId);
        } catch (Exception e) {
            System.out.println("⚠️ Lỗi kích hoạt lưu thông báo tự động: " + e.getMessage());
        }
    }

    public List<Notification> getNotificationsByRole(String role) {
        if ("ADMIN".equals(role)) {
            return notificationRepository.findAll();
        }
        return notificationRepository.findByTargetRoleOrTargetRole(role, "ALL");
    }

    public List<Notification> getNotificationsForUser(String role, Integer userId) {
        if (userId == null) {
            return getNotificationsByRole(role);
        }
        return notificationRepository.findMyNotifications(role, userId);
    }
}