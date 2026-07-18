package backend.service;

import backend.entity.StudySchedule;
import backend.repository.StudyScheduleRepository;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class StudyScheduleService {

    private final StudyScheduleRepository studyScheduleRepository;
    private final NotificationService notificationService; // Tiêm dịch vụ thông báo vào
    private final ThreadPoolTaskScheduler taskScheduler;   // Tiêm bộ máy báo thức vào

    public StudyScheduleService(StudyScheduleRepository studyScheduleRepository,
                                NotificationService notificationService,
                                ThreadPoolTaskScheduler taskScheduler) {
        this.studyScheduleRepository = studyScheduleRepository;
        this.notificationService = notificationService;
        this.taskScheduler = taskScheduler;
    }

    @Transactional(readOnly = true)
    public List<StudySchedule> getSchedulesByMonthAndYear(int month, int year) {
        return studyScheduleRepository.findByMonthAndYear(month, year);
    }

    @Transactional
    public StudySchedule createSchedule(StudySchedule schedule) {
        schedule.setReminderSent(false); // Mặc định tạo mới là chưa gửi nhắc nhở
        StudySchedule savedSchedule = studyScheduleRepository.save(schedule);

        // 🔥 Kích hoạt đặt báo thức tự động ngay khi lưu thành công!
        addDynamicReminder(savedSchedule);

        return savedSchedule;
    }

    // 🔥 HÀM XỬ LÝ LÊN LỊCH HẸN GIỜ ĐỘNG
    public void addDynamicReminder(StudySchedule schedule) {
        try {
            // Ép Ngày và Chuỗi Giờ "19:00" thành một mốc LocalDateTime hoàn chỉnh
            LocalDateTime scheduleDateTime = LocalDateTime.of(
                    schedule.getScheduleDate(),
                    LocalTime.parse(schedule.getScheduleTime().trim())
            );

            // Thời điểm reo chuông nhắc nhở = Giờ học trừ đi 15 phút
            LocalDateTime reminderDateTime = scheduleDateTime.minusMinutes(10);
            Instant reminderInstant = reminderDateTime.atZone(ZoneId.systemDefault()).toInstant();

            // Nếu mốc thời gian nhắc nhở vẫn nằm ở tương lai thì mới đặt chuông báo
            if (reminderInstant.isAfter(Instant.now())) {
                taskScheduler.schedule(() -> {

                    // 🛎️ ĐOẠN CODE SẼ CHẠY KHI ĐÚNG GIỜ REO CHUÔNG:
                    String title = "⏰ Nhắc nhở: Sắp đến giờ học!";
                    String content = "Lịch học '" + schedule.getTitle() + "' của bạn sẽ bắt đầu lúc " + schedule.getScheduleTime() + ". Chuẩn bị sách vở thôi nào!";

                    // Gửi đích danh cho userId nhận thông báo
                    notificationService.createNotification(title, content, "STUDENT", 1, schedule.getUserId());

                    // Đánh dấu vào DB là lịch này đã gửi xong rồi
                    schedule.setReminderSent(true);
                    studyScheduleRepository.save(schedule);

                    System.out.println("🔔 [Báo thức] Đã gửi thông báo nhắc học thành công cho User ID: " + schedule.getUserId());

                }, reminderInstant);

                System.out.println("📌 [Hệ thống] Đã cài báo thức cho lịch '" + schedule.getTitle() + "' reo vào lúc: " + reminderDateTime);
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi tính toán thời gian lên lịch nhắc nhở: " + e.getMessage());
        }
    }
}