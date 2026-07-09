package backend.controller;

import backend.dto.request.SubmitQuizRequest;
import backend.dto.response.QuizResponse;
import backend.dto.response.QuizResultResponse;
import backend.service.EntryTestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * UC-13: Attempt Entry Test
 * Base URL: /api/entry-test
 */
@RestController
@RequestMapping("/api/entry-test")
@RequiredArgsConstructor
public class EntryTestController {

    private final EntryTestService entryTestService;

    /** Lấy danh sách tất cả Entry Test */
    @GetMapping
    public ResponseEntity<List<QuizResponse>> getAllEntryTests() {
        return ResponseEntity.ok(entryTestService.getAllEntryTests());
    }

    /** Lấy Entry Test theo courseId (null = đề tổng quát) */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<QuizResponse> getEntryTestByCourse(@PathVariable Integer courseId) {
        return ResponseEntity.ok(entryTestService.getEntryTestByCourse(courseId));
    }

    /**
     * Nộp bài Entry Test
     * Header: X-Student-Id: {studentId}   (sẽ thay bằng JWT extraction sau)
     */
    @PostMapping("/submit")
    public ResponseEntity<QuizResultResponse> submitEntryTest(
            @RequestHeader("X-Student-Id") Integer studentId,
            @Valid @RequestBody SubmitQuizRequest request) {
        return ResponseEntity.ok(entryTestService.submitEntryTest(studentId, request));
    }

    /** Lịch sử làm Entry Test của học sinh */
    @GetMapping("/history")
    public ResponseEntity<List<QuizResultResponse>> getHistory(
            @RequestHeader("X-Student-Id") Integer studentId) {
        return ResponseEntity.ok(entryTestService.getEntryTestHistory(studentId));
    }

    /** Health check */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("status", "Entry Test API is running"));
    }

    @GetMapping("/quizzes")
    public ResponseEntity<List<QuizResponse>> getQuizzes() {
        return ResponseEntity.ok(entryTestService.getAllEntryTests());
    }

    @PostMapping("/start/{quizId}")
    public ResponseEntity<?> startQuiz(
            @PathVariable Integer quizId,
            @RequestHeader("X-Student-Id") Integer studentId) {

        return ResponseEntity.ok(entryTestService.startQuiz(quizId, studentId));
    }
}
