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

import java.util.*;

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
    private final backend.repository.PracticeAnswerRepository practiceAnswerRepository;

    // ─── Lấy danh sách đề Entry Test ───────────────────────────────────────────

    /** Số câu bốc ngẫu nhiên cho mỗi lượt Entry Test */
    private static final int QUESTIONS_PER_ENTRY_TEST = 20;

    public List<QuizResponse> getAllEntryTests() {
        // Chỉ trả metadata — KHÔNG kèm câu hỏi (kho 250 câu/đề, trả hết vừa nặng vừa lộ đề)
        return quizRepository.findAllEntryTests().stream()
                .map(quiz -> QuizResponse.builder()
                        .quizId(quiz.getQuizId())
                        .quizTitle(quiz.getQuizTitle())
                        .durationMinutes(quiz.getDurationMinutes())
                        .quizType(quiz.getQuizType())
                        .totalQuestions((int) questionRepository.countByQuiz_QuizId(quiz.getQuizId()))
                        .build())
                .toList();
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
        // ===================== Lấy Quiz =====================
        Quiz quiz;

        if (request.getSessionsId() != null) {
            QuizAttempt existing = quizAttemptRepository.findById(request.getSessionsId()).orElse(null);

            if (existing != null) {
                quiz = existing.getQuiz();
            } else {
                quiz = quizRepository.findById(request.getQuizId())
                        .orElseThrow(() -> new ResourceNotFoundException("Quiz không tồn tại"));
            }
        } else {
            quiz = quizRepository.findById(request.getQuizId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Quiz không tồn tại: " + request.getQuizId()));
        }

        // ===================== Attempt =====================
        QuizAttempt attempt;

        if (request.getSessionsId() != null) {
            attempt = quizAttemptRepository.findById(request.getSessionsId())
                    .orElse(new QuizAttempt());
        } else {
            attempt = new QuizAttempt();
        }

        Map<Integer, String> answers =
                request.getAnswers() == null
                        ? new HashMap<>()
                        : request.getAnswers();

        List<Question> questions =
                answers.isEmpty()
                        ? new ArrayList<>()
                        : questionRepository.findWithOptionsByIds(new ArrayList<>(answers.keySet()));

        int correctCount = 0;

        List<QuizResultResponse.QuestionResultDetail> details = new ArrayList<>();
        List<PracticeAnswer> practiceAnswers = new ArrayList<>();

        int orderIndex = 1;
        for (Question q : questions) {

            boolean isCorrect = false;
            String selectedAnswerText = null;
            String correctAnswerText = null;

            //--------------------------------------------------
            // SHORT ANSWER
            //--------------------------------------------------
            if ("SHORT_ANSWER".equals(q.getQuestionType())) {
                selectedAnswerText = answers.get(q.getQuestionId());
                correctAnswerText = q.getCorrectAnswer();
                if (selectedAnswerText != null &&
                        correctAnswerText != null &&
                        selectedAnswerText.trim().equalsIgnoreCase(correctAnswerText.trim())) {

                    isCorrect = true;
                }
                PracticeAnswer pa = new PracticeAnswer();
                pa.setAttempt(attempt);
                pa.setQuestion(q);
                pa.setEssayAnswer(selectedAnswerText);
                pa.setIsCorrect(isCorrect);
                pa.setQuestionOrder(orderIndex++);

                practiceAnswers.add(pa);

            }

            //--------------------------------------------------
            // MULTIPLE CHOICE
            //--------------------------------------------------
            else {
                String answerValue = answers.get(q.getQuestionId());
                QuestionOption selectedOption = null;
                if (answerValue != null) {
                    try {
                        Integer optionId = Integer.parseInt(answerValue);
                        selectedOption = q.getOptions()
                                .stream()
                                .filter(o -> o.getOptionId().equals(optionId))
                                .findFirst()
                                .orElse(null);

                    } catch (NumberFormatException ignored) {

                    }
                }
                QuestionOption correctOption =
                        q.getOptions()
                                .stream()
                                .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                                .findFirst()
                                .orElse(null);
                if (selectedOption != null) {
                    selectedAnswerText = selectedOption.getOptionContent();
                }
                if (correctOption != null) {
                    correctAnswerText = correctOption.getOptionContent();
                }
                isCorrect = selectedOption != null && Boolean.TRUE.equals(selectedOption.getIsCorrect());
                PracticeAnswer pa = new PracticeAnswer();
                pa.setAttempt(attempt);
                pa.setQuestion(q);
                pa.setSelectedOptionId(
                        selectedOption == null
                                ? null
                                : selectedOption.getOptionId()
                );
                pa.setIsCorrect(isCorrect);
                pa.setQuestionOrder(orderIndex++);
                practiceAnswers.add(pa);
            }
            if (isCorrect) {
                correctCount++;
            }
            details.add(
                    QuizResultResponse.QuestionResultDetail.builder()
                            .questionId(q.getQuestionId())
                            .questionContent(q.getQuestionContent())
                            .selectedAnswer(selectedAnswerText)
                            .correctAnswer(correctAnswerText)
                            .isCorrect(isCorrect)
                            .explanation(q.getExplanation())
                            .topic(q.getTopic())
                            .build()
            );
        }

        // ===================== Score =====================
        int total =
                attempt.getTotalQuestions() != null && attempt.getTotalQuestions() > 0
                        ? attempt.getTotalQuestions()
                        : Math.max(questions.size(), 1);
        double percentage = (double) correctCount / total * 100;
        double score =
                Math.round((percentage / 10.0) * 100.0) / 100.0;
        attempt.setQuiz(quiz);
        attempt.setStudentId(studentId);
        attempt.setScore(score);
        attempt.setCorrectCount(correctCount);
        attempt.setTotalQuestions(total);
        if (attempt.getStartedAt() == null) {
            attempt.setStartedAt(new Date());
        }
        attempt.setSubmittedAt(new Date());

        QuizAttempt saved = quizAttemptRepository.save(attempt);
        practiceAnswerRepository.saveAll(practiceAnswers);
        log.info(
                "Student {} submitted Entry Test quizId={}, score={}",
                studentId,
                quiz.getQuizId(),
                score
        );
        String level = classifyLevel(percentage);
        String color = switch (level) {
            case "Giỏi" -> "#22c55e";
            case "Khá" -> "#3b82f6";
            case "Trung bình" -> "#f59e0b";
            default -> "#ef4444";
        };
        return QuizResultResponse.builder()
                .attemptId(saved.getAttemptId())
                .quizId(quiz.getQuizId())
                .quizTitle(quiz.getQuizTitle())
                .score(score)
                .totalQuestions(total)
                .correctCount(correctCount)
                .correctAnswers(correctCount)
                .percentage(percentage)
                .accuracyPercent(percentage)
                .level(level)
                .levelColor(color)
                .summary("Bạn đạt mức " + level + ". Hệ thống khuyến nghị tiếp tục ôn tập các chủ đề còn yếu.")
                .recommendedStartLevel(level)
                .recommendations(List.of(
                        "Ôn tập các câu làm sai",
                        "Luyện thêm câu hỏi cùng chủ đề",
                        "Làm lại bài kiểm tra sau khi học"
                ))
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
                        .questionType(q.getQuestionType())
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
                .totalQuestions(questions.size())
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

    private String generateSummary(String level, double percentage) {
        return switch (level) {
            case "Giỏi" ->
                    "Bạn có nền tảng kiến thức rất tốt (" + String.format("%.1f", percentage)
                            + "%). Có thể bắt đầu ngay với các khóa học nâng cao.";
            case "Khá" ->
                    "Bạn đã có nền tảng khá vững (" + String.format("%.1f", percentage)
                            + "%). Nên củng cố thêm một số chuyên đề trước khi học nâng cao.";
            case "Trung bình" ->
                    "Bạn cần ôn lại các kiến thức nền (" + String.format("%.1f", percentage)
                            + "%) trước khi bắt đầu lộ trình chính.";
            default ->
                    "Ngu dốt ! Bạn nên học lại các kiến thức cơ bản để xây dựng nền tảng vững chắc trước khi tiếp tục.";
        };
    }

    private List<String> generateRecommendations(String level) {
        return switch (level) {
            case "Giỏi" -> List.of(
                    "Bắt đầu từ Level Nâng cao",
                    "Làm thêm Mock Test",
                    "Luyện đề theo thời gian thực"
            );
            case "Khá" -> List.of(
                    "Ôn tập các chuyên đề còn yếu",
                    "Làm thêm bài Practice",
                    "Hoàn thành khóa học Intermediate"
            );
            case "Trung bình" -> List.of(
                    "Học lại các kiến thức nền",
                    "Làm Practice sau mỗi chương",
                    "Thực hiện Adaptive Path"
            );
            default -> List.of(
                    "Bắt đầu từ khóa Foundation",
                    "Luyện tập mỗi ngày",
                    "Làm lại Entry Test sau khi hoàn thành khóa học"
            );
        };
    }

    // ─── START QUIZ ─────────────────────────────────────────────────────────────────
    public StartQuizResponse startQuiz(Integer quizId, Integer studentId) {

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz không tồn tại: " + quizId));

        if (!"ENTRY_TEST".equals(quiz.getQuizType()) && !"PRACTICE".equals(quiz.getQuizType()) && !"MOCK_EXAM".equals(quiz.getQuizType())) {
            throw new BadRequestException("Quiz này không hợp lệ để thi thử/kiểm tra");
        }

        // Bốc ngẫu nhiên đúng 20 câu cho ENTRY_TEST và 25 câu cho PRACTICE/MOCK_EXAM
        int numQuestions = "ENTRY_TEST".equals(quiz.getQuizType()) ? 20 : 25;
        List<Integer> randomIds = questionRepository.findRandomQuestionIds(quizId, numQuestions);
        if (randomIds.isEmpty()) {
            throw new BadRequestException("Đề thi này chưa có câu hỏi trong kho");
        }
        List<Question> questions = questionRepository.findWithOptionsByIds(randomIds);

        // Tạo attempt (session làm bài) — lưu số câu đã phát để submit chấm đúng mẫu số
        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuiz(quiz);
        attempt.setStudentId(studentId);
        attempt.setStartedAt(new Date());
        attempt.setTotalQuestions(questions.size());

        QuizAttempt saved = quizAttemptRepository.save(attempt);

        // Map question
        List<QuizResponse.QuestionResponse> questionResponses =
                questions.stream().map(q ->
                        QuizResponse.QuestionResponse.builder()
                                .questionId(q.getQuestionId())
                                .questionContent(q.getQuestionContent())
                                .questionType(q.getQuestionType())
                                .options(q.getOptions().stream()
                                        .map(o -> QuizResponse.OptionResponse.builder()
                                                .optionId(o.getOptionId())
                                                .optionContent(o.getOptionContent())
                                                .build())
                                        .toList())
                                .build()
                ).toList();

        return StartQuizResponse.builder()
                .sessionsId(saved.getAttemptId())   // FE dùng sessionsId
                .attemptId(saved.getAttemptId())
                .quizId(quiz.getQuizId())
                .quizTitle(quiz.getQuizTitle())
                .durationMinutes(quiz.getDurationMinutes())
                .remainingTime((quiz.getDurationMinutes() != null ? quiz.getDurationMinutes() : 20) * 60)
                .questions(questionResponses)
                .build();
    }

    //
    @Transactional(readOnly = true)
    public QuizResultResponse getAssessment(Integer attemptId) {

        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy bài làm"));

        Quiz quiz = attempt.getQuiz();

        int total = attempt.getTotalQuestions() == null ? 0 : attempt.getTotalQuestions();
        int correct = attempt.getCorrectCount() == null ? 0 : attempt.getCorrectCount();

        double percentage = total == 0 ? 0 : ((double) correct / total) * 100;

        String level = classifyLevel(percentage);

        String levelColor = switch (level) {
            case "Giỏi" -> "#22c55e";
            case "Khá" -> "#3b82f6";
            case "Trung bình" -> "#f59e0b";
            default -> "#ef4444";
        };

        return QuizResultResponse.builder()
                .attemptId(attempt.getAttemptId())
                .quizId(quiz.getQuizId())
                .quizTitle(quiz.getQuizTitle())
                .score(attempt.getScore())
                .totalQuestions(total)
                .correctCount(correct)
                .correctAnswers(correct)
                .percentage(percentage)
                .accuracyPercent(percentage)
                .level(level)
                .levelColor(levelColor)
                .summary(generateSummary(level, percentage))
                .recommendedStartLevel(level)
                .recommendations(generateRecommendations(level))
                // Chưa lưu chi tiết từng câu vào DB nên tạm thời trả rỗng
                .details(Collections.emptyList())

                .build();
    }
}
