package backend.controller;

import backend.dto.request.PracticeSubmitRequest;
import backend.dto.response.PracticeResultResponse;
import backend.dto.response.PracticeStartResponse;
import backend.entity.User;
import backend.exceptions.BadRequestException;
import backend.repository.UserRepository;
import backend.service.JwtService;
import backend.service.PracticeTestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ENGINE THI THỐNG NHẤT: Kiểm tra đầu vào + Luyện đề + Thi thử
 * Base URL: /api/practice
 *
 * Luồng:
 *   1. GET  /api/practice/quizzes?type=&subject= → danh sách đề (metadata)
 *   2. POST /api/practice/start/{quizId}   → tạo attempt + 20/25 câu random (KHÔNG lộ đáp án)
 *   3. GET  /api/practice/attempt/{id}     → resume đề đang làm dở (F5 không mất bài)
 *   4. POST /api/practice/submit           → chấm server-side, trả chi tiết + explanation
 *   5. GET  /api/practice/result/{id}      → xem lại kết quả đã nộp
 *   6. GET  /api/practice/history          → lịch sử các lượt thi của học sinh
 */
@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeTestController {

    private final PracticeTestService practiceTestService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    /** Danh sách đề thi (lọc ?type=ENTRY_TEST|PRACTICE|MOCK_EXAM và/hoặc ?subject=Toán) */
    @GetMapping("/quizzes")
    public ResponseEntity<List<Map<String, Object>>> getQuizzes(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String subject) {
        return ResponseEntity.ok(practiceTestService.getQuizzes(type, subject));
    }

    /** Bắt đầu thi → attemptId + 20 câu (Entry Test) hoặc 25 câu (Luyện đề/Thi thử) ngẫu nhiên */
    @PostMapping("/start/{quizId}")
    public ResponseEntity<PracticeStartResponse> start(
            @PathVariable Integer quizId,
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestHeader(value = "X-Student-Id", required = false) Integer studentIdHeader) {

        Integer studentId = resolveStudentId(token, studentIdHeader);
        return ResponseEntity.ok(practiceTestService.start(studentId, quizId));
    }

    /** Nộp bài → điểm + chi tiết từng câu (đáp án đã chọn / đáp án đúng / lời giải) */
    @PostMapping("/submit")
    public ResponseEntity<PracticeResultResponse> submit(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestHeader(value = "X-Student-Id", required = false) Integer studentIdHeader,
            @Valid @RequestBody PracticeSubmitRequest request) {

        Integer studentId = resolveStudentId(token, studentIdHeader);
        return ResponseEntity.ok(practiceTestService.submit(studentId, request));
    }

    /** Resume: lấy lại đề đang làm dở + số giây còn lại (F5 giữa chừng không mất bài) */
    @GetMapping("/attempt/{attemptId}")
    public ResponseEntity<PracticeStartResponse> getAttempt(
            @PathVariable Integer attemptId,
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestHeader(value = "X-Student-Id", required = false) Integer studentIdHeader) {

        Integer studentId = resolveStudentId(token, studentIdHeader);
        return ResponseEntity.ok(practiceTestService.getAttempt(studentId, attemptId));
    }

    /** Lịch sử các lượt thi đã nộp của học sinh (mới nhất trước) */
    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestHeader(value = "X-Student-Id", required = false) Integer studentIdHeader) {

        Integer studentId = resolveStudentId(token, studentIdHeader);
        return ResponseEntity.ok(practiceTestService.getHistory(studentId));
    }

    /** Xem lại kết quả (khi F5 trang kết quả hoặc mở lại từ lịch sử) */
    @GetMapping("/result/{attemptId}")
    public ResponseEntity<PracticeResultResponse> getResult(
            @PathVariable Integer attemptId,
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestHeader(value = "X-Student-Id", required = false) Integer studentIdHeader) {

        Integer studentId = resolveStudentId(token, studentIdHeader);
        return ResponseEntity.ok(practiceTestService.getResult(studentId, attemptId));
    }

    /**
     * Xác định học sinh: ưu tiên JWT (Authorization: Bearer), fallback header X-Student-Id
     * (axiosClient của FE luôn gửi cả hai). Luyện đề bắt buộc đăng nhập.
     */
    private Integer resolveStudentId(String token, Integer studentIdHeader) {
        if (token != null && token.startsWith("Bearer ")) {
            try {
                String email = jwtService.extractUsername(token.replace("Bearer ", ""));
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    return user.getId();
                }
            } catch (Exception ignored) {
                // token hỏng/hết hạn → thử fallback header
            }
        }
        if (studentIdHeader != null) {
            return studentIdHeader;
        }
        throw new BadRequestException("Vui lòng đăng nhập để luyện đề.");
    }
}
