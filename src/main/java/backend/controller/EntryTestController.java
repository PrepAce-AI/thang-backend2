package backend.controller;

import backend.dto.request.SubmitQuizRequest;
import backend.dto.response.QuizResponse;
import backend.dto.response.QuizResultResponse;
import backend.entity.User;
import backend.repository.UserRepository;
import backend.service.EntryTestService;
import backend.service.JwtService;
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
    private final JwtService jwtService;
    private final UserRepository userRepository;

    /** Lấy danh sách tất cả Entry Test */
    @GetMapping
    public ResponseEntity<List<QuizResponse>> getAllEntryTests() {
        return ResponseEntity.ok(entryTestService.getAllEntryTests());
    }

    /** Lấy Entry Test theo courseId */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<QuizResponse> getEntryTestByCourse(@PathVariable Integer courseId) {
        return ResponseEntity.ok(entryTestService.getEntryTestByCourse(courseId));
    }

    /** Lấy danh sách entry test (alias cho getAllEntryTests) */
    @GetMapping("/quizzes")
    public ResponseEntity<List<QuizResponse>> getQuizzes() {
        return ResponseEntity.ok(entryTestService.getAllEntryTests());
    }

    /** Health check */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("status", "Entry Test API is running"));
    }

    /**
     * Bắt đầu bài kiểm tra đầu vào — dùng JWT để xác định học sinh.
     * Nếu chưa đăng nhập, dùng studentId = 0 (guest mode).
     */
//    @PostMapping("/start/{quizId}")
//    public ResponseEntity<?> startQuiz(
//            @PathVariable Integer quizId,
//            @RequestHeader(value = "Authorization", required = false) String token) {
//
//        Integer studentId = 0; // guest mặc định
//        if (token != null && token.startsWith("Bearer ")) {
//            try {
//                String jwt = token.replace("Bearer ", "");
//                String email = jwtService.extractUsername(jwt);
//                User user = userRepository.findByEmail(email).orElse(null);
//                if (user != null) {
//                    studentId = user.getId();
//                }
//            } catch (Exception ignored) {
//                // token lỗi → giữ nguyên guest
//            }
//        }
//        System.out.println("studentId = " + studentId);
//        return ResponseEntity.ok(entryTestService.startQuiz(quizId, studentId));
//    }
    @PostMapping("/start/{quizId}")
    public ResponseEntity<?> startQuiz(
            @PathVariable Integer quizId,
            @RequestHeader("Authorization") String token) {

        String jwt = token.replace("Bearer ", "");

        String email = jwtService.extractUsername(jwt);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        return ResponseEntity.ok(
                entryTestService.startQuiz(quizId, user.getId())
        );
    }

    /**
     * Nộp bài Entry Test — dùng JWT để xác định học sinh.
     */
//    @PostMapping("/submit")
//    public ResponseEntity<QuizResultResponse> submitEntryTest(
//            @RequestHeader(value = "Authorization", required = false) String token,
//            @Valid @RequestBody SubmitQuizRequest request) {
//
//        Integer studentId = 0;
//        if (token != null && token.startsWith("Bearer ")) {
//            try {
//                String jwt = token.replace("Bearer ", "");
//                String email = jwtService.extractUsername(jwt);
//                User user = userRepository.findByEmail(email).orElse(null);
//                if (user != null) {
//                    studentId = user.getId();
//                }
//            } catch (Exception ignored) {}
//        }
//        return ResponseEntity.ok(entryTestService.submitEntryTest(studentId, request));
//    }
    @PostMapping("/submit")
    public ResponseEntity<QuizResultResponse> submitEntryTest(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody SubmitQuizRequest request) {

        String jwt = token.replace("Bearer ", "");

        String email = jwtService.extractUsername(jwt);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        return ResponseEntity.ok(
                entryTestService.submitEntryTest(user.getId(), request)
        );
    }

    @GetMapping("/assessment/{attemptId}")
    public ResponseEntity<QuizResultResponse> getAssessment(
            @PathVariable Integer attemptId) {

        return ResponseEntity.ok(
                entryTestService.getAssessment(attemptId)
        );
    }

    /** Lịch sử làm Entry Test của học sinh */
    @GetMapping("/history")
    public ResponseEntity<List<QuizResultResponse>> getHistory(
            @RequestHeader("Authorization") String token) {

        String jwt = token.replace("Bearer ", "");
        String email = jwtService.extractUsername(jwt);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        return ResponseEntity.ok(entryTestService.getEntryTestHistory(user.getId()));
    }
}
