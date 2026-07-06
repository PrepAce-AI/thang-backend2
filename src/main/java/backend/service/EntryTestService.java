package backend.service;

import backend.dto.request.SubmitQuizRequest;
import backend.dto.response.QuizResponse;
import backend.dto.response.QuizResultResponse;
import backend.entity.*;
import backend.dto.response.StartQuizResponse;
import backend.exceptions.BadRequestException;
import backend.exceptions.ResourceNotFoundException;
import backend.repository.QuestionRepository;
import backend.repository.QuizAttemptRepository;
import backend.repository.QuizRepository;
import backend.repository.TestSessionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * UC-13: Attempt Entry Test
 * Học sinh làm bài kiểm tra đầu vào để hệ thống đánh giá năng lực ban đầu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntryTestService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    // ─── Lấy danh sách đề Entry Test ───────────────────────────────────────────

    public List<QuizResponse> getAllEntryTests() {
        List<Quiz> tests = quizRepository.findAllEntryTests();
        return tests.stream().map(this::mapToQuizResponse).toList();
    }

    /**
     * Lấy đề Entry Test theo courseId.
     * Nếu courseId = null → lấy đề tổng quát (standalone).
     */
    public QuizResponse getEntryTestByCourse(Integer courseId) {
        Quiz quiz = quizRepository.findEntryTestByCourseId(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy Entry Test cho courseId: " + courseId));
        return mapToQuizResponse(quiz);
    }

    // ─── Nộp bài Entry Test ─────────────────────────────────────────────────────

    @Transactional
    public QuizResultResponse submitEntryTest(Integer studentId, SubmitQuizRequest request) {
        Quiz quiz = quizRepository.findById(request.getQuizId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz không tồn tại: " + request.getQuizId()));

        if (!"ENTRY_TEST".equals(quiz.getQuizType())) {
            throw new BadRequestException("Quiz này không phải Entry Test");
        }

        List<Question> questions = questionRepository.findByQuizId(quiz.getQuizId());
        if (questions.isEmpty()) {
            throw new BadRequestException("Entry Test chưa có câu hỏi nào");
        }

        Map<Integer, Integer> answers = request.getAnswers();
        int correctCount = 0;
        List<QuizResultResponse.QuestionResultDetail> details = new ArrayList<>();

        for (Question q : questions) {
            Integer selectedOptionId = Integer.valueOf(request.getAnswers().get(q.getQuestionId()));

            QuestionOption correctOption =
                    q.getOptions()
                            .stream()
                            .filter(QuestionOption::getIsCorrect)
                            .findFirst()
                            .orElse(null);

            boolean isCorrect =
                    correctOption != null &&
                            correctOption.getOptionId().equals(selectedOptionId);
            if (isCorrect) correctCount++;

            details.add(QuizResultResponse.QuestionResultDetail.builder()
                    .questionId(q.getQuestionId())
                    .questionContent(q.getQuestionContent())
                    .selectedAnswer(
                            selectedOptionId == null
                                    ? null
                                    : q.getOptions()
                                    .stream()
                                    .filter(o -> o.getOptionId().equals(selectedOptionId))
                                    .map(QuestionOption::getOptionContent)
                                    .findFirst()
                                    .orElse(null)
                    )
                    .correctAnswer(
                            correctOption == null
                                    ? null
                                    : correctOption.getOptionContent()
                    )
                    .isCorrect(isCorrect)
                    .build());
        }

        int total = questions.size();
        double score = total > 0 ? Math.round(((double) correctCount / total) * 100.0) / 10.0 : 0.0;
        double percentage = total > 0 ? (double) correctCount / total * 100 : 0.0;

        // Lưu attempt
        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuiz(quiz);
        attempt.setStudentId(studentId);
        attempt.setScore(score);
        attempt.setTotalQuestions(total);
        attempt.setCorrectCount(correctCount);
        attempt.setStartedAt(new Date());
        attempt.setSubmittedAt(new Date());
        QuizAttempt saved = quizAttemptRepository.save(attempt);

        log.info("Student {} submitted Entry Test quizId={}, score={}", studentId, quiz.getQuizId(), score);

        return QuizResultResponse.builder()
                .attemptId(saved.getAttemptId())
                .quizId(quiz.getQuizId())
                .quizTitle(quiz.getQuizTitle())
                .score(score)
                .totalQuestions(total)
                .correctCount(correctCount)
                .percentage(percentage)
                .level(classifyLevel(percentage))
                .details(details)
                .build();
    }

    // ─── Lịch sử Entry Test của student ────────────────────────────────────────

    public List<QuizResultResponse> getEntryTestHistory(Integer studentId) {
        return quizAttemptRepository.findEntryTestAttemptsByStudent(studentId)
                .stream()
                .map(a -> QuizResultResponse.builder()
                        .attemptId(a.getAttemptId())
                        .quizId(a.getQuiz().getQuizId())
                        .quizTitle(a.getQuiz().getQuizTitle())
                        .score(a.getScore())
                        .totalQuestions(a.getTotalQuestions())
                        .correctCount(a.getCorrectCount())
                        .percentage(a.getTotalQuestions() != null && a.getTotalQuestions() > 0
                                ? (double) a.getCorrectCount() / a.getTotalQuestions() * 100 : 0)
                        .level(classifyLevel(a.getTotalQuestions() != null && a.getTotalQuestions() > 0
                                ? (double) a.getCorrectCount() / a.getTotalQuestions() * 100 : 0))
                        .build())
                .toList();
    }

    // ─── Helper ─────────────────────────────────────────────────────────────────

    private QuizResponse mapToQuizResponse(Quiz quiz) {
        List<Question> questions = questionRepository.findByQuizId(quiz.getQuizId());

        List<QuizResponse.QuestionResponse> questionResponses = questions.stream()
                .map(q -> QuizResponse.QuestionResponse.builder()
                        .questionId(q.getQuestionId())
                        .questionContent(q.getQuestionContent())
                        .options(q.getOptions().stream()
                                .map(o -> QuizResponse.OptionResponse.builder()
                                        .optionId(o.getOptionId())
                                        .optionContent(o.getOptionContent())
                                        .build())
                                .toList())
                        .build())
                .toList();

        return QuizResponse.builder()
                .quizId(quiz.getQuizId())
                .quizTitle(quiz.getQuizTitle())
                .durationMinutes(quiz.getDurationMinutes())
                .quizType(quiz.getQuizType())
                .questions(questionResponses)
                .build();
    }

    /** Phân loại năng lực theo % đúng */
    private String classifyLevel(double percentage) {
        if (percentage >= 80) return "Giỏi";
        if (percentage >= 65) return "Khá";
        if (percentage >= 50) return "Trung bình";
        return "Yếu";
    }

    // ─── START QUIZ ─────────────────────────────────────────────────────────────────
    public StartQuizResponse startQuiz(Integer quizId, Integer studentId) {

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz không tồn tại: " + quizId));

        if (!"ENTRY_TEST".equals(quiz.getQuizType())) {
            throw new BadRequestException("Quiz này không phải Entry Test");
        }

        // Lấy câu hỏi
        List<Question> questions = questionRepository.findByQuizId(quizId);

        if (questions.isEmpty()) {
            throw new BadRequestException("Entry Test chưa có câu hỏi");
        }

        // Tạo attempt (session làm bài)
        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuiz(quiz);
        attempt.setStudentId(studentId);
        attempt.setStartedAt(new Date());

        QuizAttempt saved = quizAttemptRepository.save(attempt);

        // Map question
        List<QuizResponse.QuestionResponse> questionResponses =
                questions.stream().map(q ->
                        QuizResponse.QuestionResponse.builder()
                                .questionId(q.getQuestionId())
                                .questionContent(q.getQuestionContent())
                                .options(q.getOptions().stream()
                                        .map(o -> QuizResponse.OptionResponse.builder()
                                                .optionId(o.getOptionId())
                                                .optionContent(o.getOptionContent())
                                                .build())
                                        .toList())
                                .build()
                ).toList();

        return StartQuizResponse.builder()
                .attemptId(saved.getAttemptId())
                .quizId(quiz.getQuizId())
                .quizTitle(quiz.getQuizTitle())
                .questions(questionResponses)
                .build();
    }
}
