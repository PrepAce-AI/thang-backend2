package backend.controller;

import backend.dto.request.StartTestRequest;
import backend.dto.request.SubmitAnswerRequest;
import backend.dto.response.QuestionResponse;
import backend.dto.response.TestResultResponse;
import backend.dto.response.TestSessionResponse;
import backend.entity.User;
import backend.repository.UserRepository;
import backend.service.JwtService;
import backend.service.TestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import backend.service.PracticeTestService;
import java.util.*;

@RestController
@RequestMapping("/api/tests")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class TestController {

    private final TestService testService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PracticeTestService practiceTestService;
    // 🔥 CHÈN THÊM DÒNG NÀY VÀO ĐÂY
    public TestController(TestService testService, JwtService jwtService, UserRepository userRepository, PracticeTestService practiceTestService) {
        this.testService = testService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.practiceTestService = practiceTestService; // 🔥 GÁN GIÁ TRỊ VÀO ĐÂY
    }

    @PostMapping("/start")
    public ResponseEntity<TestSessionResponse> startTest(
            @RequestBody StartTestRequest request,
            @RequestHeader("Authorization") String token) {

        try {
            String jwt = token.replace("Bearer ", "");
            String email = jwtService.extractUsername(jwt);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            int userId = user.getId();

            TestSessionResponse response = testService.startTest(request, userId, null, null);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/{sessionsId}/questions")
    public ResponseEntity<List<QuestionResponse>> getQuestions(
            @PathVariable int sessionsId,
            @RequestHeader("Authorization") String token) {

        try {
            String jwt = token.replace("Bearer ", "");
            String email = jwtService.extractUsername(jwt);
            User user = userRepository.findByEmail(email).orElseThrow();

            return ResponseEntity.ok(testService.getQuestions(sessionsId, user.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(403).build();
        }
    }

    @PostMapping("/{sessionsId}/answer")
    public ResponseEntity<Void> submitAnswer(
            @PathVariable int sessionsId,
            @RequestBody SubmitAnswerRequest request,
            @RequestHeader("Authorization") String token) {

        try {
            String jwt = token.replace("Bearer ", "");
            String email = jwtService.extractUsername(jwt);
            User user = userRepository.findByEmail(email).orElseThrow();

            testService.submitAnswer(sessionsId, request, user.getId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(403).build();
        }
    }

    @PostMapping("/{sessionsId}/submit")
    public ResponseEntity<?> submitTest(
            @PathVariable int sessionsId,
            @RequestBody(required = false) Map<Long, String> answers, // Nhận cục Map đáp án gộp từ TestDoingPage
            @RequestHeader("Authorization") String token) {

        try {
            String jwt = token.replace("Bearer ", "");
            String email = jwtService.extractUsername(jwt);
            User user = userRepository.findByEmail(email).orElseThrow();

            // Gọi hàm Service phiên bản mới (truyền thêm cục answers vào cuối)
            TestResultResponse response = testService.submitTest(sessionsId, user.getId(), answers);

            // Trả về Object kết quả thi thay vì String thô để Frontend xử lý logic hiển thị
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi khi nộp bài: " + e.getMessage());
        }
    }

    @GetMapping("/{sessionsId}/result")
    public ResponseEntity<TestResultResponse> getResult(
            @PathVariable int sessionsId,
            @RequestHeader("Authorization") String token) {

        try {
            String jwt = token.replace("Bearer ", "");
            String email = jwtService.extractUsername(jwt);
            User user = userRepository.findByEmail(email).orElseThrow();

            return ResponseEntity.ok(testService.getResult(sessionsId, user.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(403).build();
        }
    }

    @PutMapping("/{sessionsId}/questions/{questionId}/grade")
    public ResponseEntity<String> gradeEssay(
            @PathVariable int sessionsId, // Lúc này Frontend đang truyền attemptId vào đây
            @PathVariable int questionId,
            @RequestBody Map<String, Object> body) {
        try {
            double score = Double.parseDouble(body.get("score").toString());
            String comment = (String) body.get("comment");

            // 🔥 ĐỔI DÒNG NÀY: Gọi sang bộ dịch vụ mới đã vá ở Bước 2
            // Ông nhớ @Autowired thêm private final PracticeTestService practiceTestService ở đầu file Controller nhé
            practiceTestService.teacherGradeAttemptAnswer(sessionsId, questionId, score, comment);

            return ResponseEntity.ok("Chấm điểm câu tự luận thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi chấm điểm: " + e.getMessage());
        }
    }
}