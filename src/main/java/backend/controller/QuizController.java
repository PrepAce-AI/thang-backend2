package backend.controller;

import backend.entity.*;
import backend.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quizzes")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class QuizController {
    private final QuizRepository quizRepository;
    private final PracticeAnswerRepository practiceAnswerRepository;
    private final QuestionRepository questionRepository;
    private final CourseRepository courseRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final TestSessionRepository testSessionRepository;

    public QuizController(QuizRepository quizRepository,TestSessionRepository testSessionRepository ,QuizAttemptRepository quizAttemptRepository ,StudentAnswerRepository studentAnswerRepository ,CourseRepository courseRepository, QuestionOptionRepository questionOptionRepository ,PracticeAnswerRepository practiceAnswerRepository, QuestionRepository questionRepository) {
        this.quizRepository = quizRepository;
        this.courseRepository = courseRepository;
        this.practiceAnswerRepository = practiceAnswerRepository;
        this.questionRepository = questionRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.studentAnswerRepository = studentAnswerRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.testSessionRepository = testSessionRepository;
    }

// 🔥 1. Lấy tất cả
    @GetMapping
    public ResponseEntity<?> getAllQuizzes(){
        List<Quiz> quizzes = quizRepository.findAllWithCourse();
        List<Map<String, Object>> result = new ArrayList<>();

        if (quizzes != null) {
            for (Quiz q : quizzes) {
                Map<String, Object> quizMap = new HashMap<>();
                quizMap.put("quizId", q.getQuizId());
                quizMap.put("quizTitle", q.getQuizTitle());
                quizMap.put("durationMinutes", q.getDurationMinutes());
                quizMap.put("quizType", q.getQuizType());
                quizMap.put("subject", q.getSubject());
                quizMap.put("isEntryTest", q.getIsEntryTest());
                quizMap.put("createdAt", q.getCreatedAt());

                // Nếu có liên kết khóa học, ta bốc riêng thông tin thô để gửi về Frontend
                if (q.getCourse() != null) {
                    Map<String, Object> courseMap = new HashMap<>();
                    // Lấy ra ID chắc chắn có trong Entity
                    courseMap.put("courseId", q.getCourse().getCourseId());

                    // Để tránh lỗi compile khi gọi hàm lấy tên Course, ta bốc thẳng trường title từ Object bằng chuỗi hoặc gán chuỗi rỗng trước
                    // Frontend của bạn có cơ chế hiển thị hoặc nạp danh sách khóa học bổ trợ nên chỉ cần khớp courseId là đủ!
                    quizMap.put("course", courseMap);
                } else {
                    quizMap.put("course", null);
                }

                result.add(quizMap);
            }
        }
        return ResponseEntity.ok(result);
    }

    // 2. Lấy đề thi theo khóa học
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<Quiz>> getQuizzesByCourse(@PathVariable Integer courseId){
        List<Quiz> quizzes = quizRepository.findByCourse_CourseId(courseId);
        return ResponseEntity.ok(quizzes);
    }

    // 🔥 3. SỬA LẠI HÀM LẤY CHI TIẾT: Chuyển sang Map động để lấy trọn vẹn Câu hỏi và Đáp án mà không bị @JsonIgnore chặn
    @GetMapping("/{quizId}")
    public ResponseEntity<?> getQuizById(@PathVariable Integer quizId) {
        // Gọi hàm nạp kèm câu hỏi từ repo của bạn
        Quiz quiz = quizRepository.findByIdWithQuestions(quizId)
                .orElseThrow(() -> new RuntimeException("Không Tìm Thấy Quiz"));

        Map<String, Object> response = new HashMap<>();
        response.put("quizId", quiz.getQuizId());
        response.put("quizTitle", quiz.getQuizTitle());
        response.put("durationMinutes", quiz.getDurationMinutes());

        // Tự tay đóng gói danh sách câu hỏi kèm theo các lựa chọn (options)
        List<Map<String, Object>> questionsList = new ArrayList<>();
        if (quiz.getQuestions() != null) {
            for (Question q : quiz.getQuestions()) {
                Map<String, Object> qMap = new HashMap<>();
                qMap.put("questionId", q.getQuestionId());
                qMap.put("questionContent", q.getQuestionContent());
                qMap.put("correctAnswer", q.getCorrectAnswer());
                qMap.put("explanation", q.getExplanation());
                qMap.put("questionType", q.getQuestionType());

                // Bốc tiếp danh sách các đáp án lựa chọn của câu hỏi đó
                List<Map<String, Object>> optionsList = new ArrayList<>();
                if (q.getOptions() != null) {
                    for (QuestionOption opt : q.getOptions()) {
                        Map<String, Object> optMap = new HashMap<>();
                        optMap.put("optionId", opt.getOptionId());
                        optMap.put("optionContent", opt.getOptionContent());
                        optMap.put("isCorrect", opt.getIsCorrect());
                        optionsList.add(optMap);
                    }
                }
                qMap.put("options", optionsList);
                questionsList.add(qMap);
            }
        }

        // Trả về cấu trúc có chứa thuộc tính "questions" đúng y hệt Frontend đang chờ đợi
        response.put("questions", questionsList);

        return ResponseEntity.ok(response);
    }

    // 4. THÊM MỚI ĐỀ THI
    @PostMapping
    public ResponseEntity<?> createQuiz(@RequestBody Quiz quiz) {
        try {
            if (quiz.getCourse() != null && quiz.getCourse().getCourseId() > 0) {
                var course = courseRepository.findById(quiz.getCourse().getCourseId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học liên kết"));
                quiz.setCourse(course);
            }
            quiz.setCreatedAt(new java.util.Date());
            Quiz savedQuiz = quizRepository.save(quiz);
            return ResponseEntity.ok(savedQuiz);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi Backend: " + e.getMessage());
        }
    }

    // 5. CHỈNH SỬA ĐỀ THI
    @PutMapping("/{quizId}")
    public ResponseEntity<?> updateQuiz(@PathVariable Integer quizId, @RequestBody Quiz quizDetails) {
        try {
            Quiz quiz = quizRepository.findById(quizId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đề thi cần sửa"));

            quiz.setQuizTitle(quizDetails.getQuizTitle());
            quiz.setDurationMinutes(quizDetails.getDurationMinutes());

            if (quizDetails.getCourse() != null && quizDetails.getCourse().getCourseId() > 0) {
                var course = courseRepository.findById(quizDetails.getCourse().getCourseId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học liên kết"));
                quiz.setCourse(course);
            }

            Quiz updatedQuiz = quizRepository.save(quiz);
            return ResponseEntity.ok(updatedQuiz);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi khi cập nhật: " + e.getMessage());
        }
    }

    @DeleteMapping("/{quizId}")
    @Transactional
    public ResponseEntity<?> deleteQuiz(
            @PathVariable Integer quizId
    ) {
        Quiz quiz = quizRepository.findByIdWithQuestions(quizId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy quiz"));
    /*
        1. Xóa PracticeAnswers
    */
        List<QuizAttempt> attempts =
                quizAttemptRepository.findByQuiz_QuizId(quizId);
        for (QuizAttempt attempt : attempts) {
            practiceAnswerRepository
                    .deleteByAttempt_AttemptId(
                            attempt.getAttemptId()
                    );
        }
    /*
        2. Xóa QuizAttempts
    */
        quizAttemptRepository.deleteAll(attempts);
    /*
        3. Xóa StudentAnswers
        thông qua TestSession
    */
        List<TestSession> sessions =
                testSessionRepository.findByQuiz_QuizId(quizId);
        for(TestSession session : sessions){
            studentAnswerRepository
                    .deleteBySession_SessionsId(
                            session.getSessionsId()
                    );
        }
    /*
        4. Xóa TestSessions
    */
        testSessionRepository.deleteAll(sessions);
    /*
        5. Xóa Questions
    */
        for(Question question : quiz.getQuestions()){
            Integer questionId =
                    question.getQuestionId();
            practiceAnswerRepository
                    .deleteByQuestionId(questionId);
            studentAnswerRepository
                    .deleteByQuestionId(questionId);
            questionOptionRepository
                    .deleteByQuestionId(questionId);
        }
    /*
        6. Xóa Quiz
    */
        quizRepository.delete(quiz);
        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Delete quiz successfully"
                )
        );
    }
}