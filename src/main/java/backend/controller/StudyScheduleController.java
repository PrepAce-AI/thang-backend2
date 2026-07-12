package backend.controller;

import backend.entity.StudySchedule;
import backend.service.StudyScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@CrossOrigin(origins = "http://localhost:3000") // Mở cổng cho ứng dụng React gọi tới
public class StudyScheduleController {

    private final StudyScheduleService studyScheduleService;

    public StudyScheduleController(StudyScheduleService studyScheduleService) {
        this.studyScheduleService = studyScheduleService;
    }

    // API lấy lịch học: GET http://localhost:8080/schedules?month=7&year=2026
    @GetMapping
    public ResponseEntity<List<StudySchedule>> getSchedules(
            @RequestParam("month") int month,
            @RequestParam("year") int year) {

        List<StudySchedule> list = studyScheduleService.getSchedulesByMonthAndYear(month, year);
        return ResponseEntity.ok(list);
    }

    // API bổ sung nếu sau này bạn muốn làm thêm chức năng nhấn nút "+ Thêm lịch học" từ giao diện React
    @PostMapping
    public ResponseEntity<StudySchedule> addSchedule(@RequestBody StudySchedule schedule) {
        StudySchedule savedSchedule = studyScheduleService.createSchedule(schedule);
        return ResponseEntity.ok(savedSchedule);
    }
}