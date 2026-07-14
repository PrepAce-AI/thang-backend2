package backend.config;

import backend.entity.StudySchedule;
import backend.repository.StudyScheduleRepository;
import backend.service.StudyScheduleService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;

@Component
public class ScheduleInitializer {

    private final StudyScheduleRepository studyScheduleRepository;
    private final StudyScheduleService studyScheduleService;

    public ScheduleInitializer(StudyScheduleRepository studyScheduleRepository, StudyScheduleService studyScheduleService) {
        this.studyScheduleRepository = studyScheduleRepository;
        this.studyScheduleService = studyScheduleService;
    }

    // Khi server Spring Boot vừa khởi động thành công và sẵn sàng chạy
    @EventListener(ApplicationReadyEvent.class)
    public void reScheduleAlarmsOnStartup() {
        System.out.println("🔄 [Hệ thống] Đang nạp lại các báo thức lịch học từ Database vào bộ nhớ...");

        LocalDate today = LocalDate.now();

        // Quét lấy tất cả các lịch học từ hôm nay trở đi mà trạng thái chưa được gửi thông báo
        List<StudySchedule> pendingSchedules = studyScheduleRepository.findAll().stream()
                .filter(s -> !s.getReminderSent() && !s.getScheduleDate().isBefore(today))
                .toList();

        // Đổ ngược lại vào bộ máy Scheduler
        for (StudySchedule schedule : pendingSchedules) {
            studyScheduleService.addDynamicReminder(schedule);
        }

        System.out.println("✅ [Hệ thống] Đã tái kích hoạt thành công " + pendingSchedules.size() + " báo thức động.");
    }
}