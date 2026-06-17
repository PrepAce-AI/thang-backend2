package backend.controller;

import backend.dto.request.QuestionRequest;
import backend.dto.response.QuestionResponse;
import backend.service.AcademicQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class AcademicQuestionController {

    @Autowired
    private AcademicQuestionService questionService;

    // API Đăng câu hỏi (Yêu cầu phải đăng nhập)
    @PostMapping
    public ResponseEntity<QuestionResponse> postQuestion(@RequestBody QuestionRequest request) {
        return ResponseEntity.ok(questionService.createQuestion(request));
    }

    // API Lấy danh sách câu hỏi của một bài học (Yêu cầu phải đăng nhập)
    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<List<QuestionResponse>> getQuestionsByLesson(@PathVariable Integer lessonId) {
        return ResponseEntity.ok(questionService.getQuestionsByLesson(lessonId));
    }

    // API Trả lời câu hỏi (Yêu cầu đăng nhập)
    @PostMapping("/{questionId}/answers")
    public ResponseEntity<backend.dto.response.AnswerResponse> postAnswer(
            @PathVariable Integer questionId,
            @RequestBody backend.dto.request.AnswerRequest request) {
        return ResponseEntity.ok(questionService.createAnswer(questionId, request));
    }
}