package backend.controller;

import backend.entity.Notification;
import backend.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications") // Khớp với đầu /notifications của axiosClient
@CrossOrigin(origins = "*") // Mở cổng cho React gọi tới thoải mái
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // API lấy thông báo cho Học sinh/Giáo viên: GET http://localhost:8080/api/notifications?role=STUDENT&userId=5
    @GetMapping
    public ResponseEntity<List<Notification>> getUserNotifications(
            @RequestParam("role") String role,
            @RequestParam(value = "userId", required = false) Integer userId) {

        List<Notification> list = notificationService.getNotificationsForUser(role, userId);
        return ResponseEntity.ok(list);
    }
}