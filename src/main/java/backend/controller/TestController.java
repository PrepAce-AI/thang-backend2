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

import java.util.*;

@RestController
@RequestMapping("/api/tests")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class TestController {

    private final TestService testService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public TestController(TestService testService, JwtService jwtService, UserRepository userRepository) {
        this.testService = testService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
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
    public ResponseEntity<String> submitTest(
            @PathVariable int sessionsId,
            @RequestHeader("Authorization") String token) {

        try {
            String jwt = token.replace("Bearer ", "");
            String email = jwtService.extractUsername(jwt);
            User user = userRepository.findByEmail(email).orElseThrow();

            testService.submitTest(sessionsId, user.getId());
            return ResponseEntity.ok("Nộp Bài Thành Công !!!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi nộp bài");
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
            @PathVariable int sessionsId,
            @PathVariable int questionId,
            @RequestBody Map<String, Object> body) {
        try {
            float score = Float.parseFloat(body.get("score").toString());
            String comment = (String) body.get("comment");

            testService.gradeEssayAnswer(sessionsId, questionId, score, comment);
            return ResponseEntity.ok("Chấm điểm câu tự luận thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi chấm điểm: " + e.getMessage());
        }
    }
}