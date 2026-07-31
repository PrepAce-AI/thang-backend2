package backend.service;

import backend.dto.request.PracticeSubmitRequest;
import backend.dto.response.*;
import backend.entity.PracticeAnswer;
import backend.entity.Question;
import backend.entity.QuestionOption;
import backend.entity.Quiz;
import backend.entity.QuizAttempt;
import backend.exceptions.BadRequestException;
import backend.exceptions.ResourceNotFoundException;
import backend.repository.PracticeAnswerRepository;
import backend.repository.QuestionRepository;
import backend.repository.QuizAttemptRepository;
import backend.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ENGINE THI THỐNG NHẤT cho Kiểm tra đầu vào + Luyện đề + Thi thử:
 *  - start:   ENTRY_TEST bốc ngẫu nhiên đúng 20 câu, PRACTICE/MOCK_EXAM bốc 25 câu
 *             (ORDER BY NEWID() trên SQL Server), tạo QuizAttempt + PracticeAnswers
 *             (server ghi nhớ đề đã phát → chống gian lận, cho phép resume).
 *  - submit:  chấm server-side theo QuestionOptions.is_correct, tính điểm/10 + %,
 *             lưu lịch sử, trả chi tiết từng câu kèm explanation.
 *  - resume:  F5 giữa chừng vẫn lấy lại đúng đề + thời gian còn lại.
 *  - result/history: xem lại kết quả bất kỳ lúc nào.
 */
@Service
@RequiredArgsConstructor
public class PracticeTestService {

    /** Kiểm tra đầu vào: 20 câu / lượt */
    public static final int ENTRY_QUESTIONS_PER_TEST = 20;
    /** Luyện đề & Thi thử: 25 câu / lượt */
    public static final int PRACTICE_QUESTIONS_PER_TEST = 25;

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final PracticeAnswerRepository practiceAnswerRepository;
    private final AiGradingService aiGradingService;
    @org.springframework.context.annotation.Lazy
    private final AIService aiService;
    private final ObjectMapper objectMapper;

    /** Số câu phát cho 1 lượt thi, theo loại đề */
    public static int questionsPerTest(String quizType) {
        return "ENTRY_TEST".equals(quizType) ? ENTRY_QUESTIONS_PER_TEST : PRACTICE_QUESTIONS_PER_TEST;
    }

    // ─── DANH SÁCH ĐỀ ────────────────────────────────────────────────────────────

    /** Danh sách đề thi (metadata) — lọc theo loại (ENTRY_TEST/PRACTICE/MOCK_EXAM) và môn */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getQuizzes(String type, String subject) {

        List<Quiz> quizzes =
                quizRepository.findExamQuizzes(type, subject);

        List<Map<String, Object>> result = new ArrayList<>();

        for (Quiz q : quizzes) {

            Map<String, Object> m = new LinkedHashMap<>();

            m.put("quizId", q.getQuizId());
            m.put("quizTitle", q.getQuizTitle());
            m.put("subject", q.getSubject());
            m.put("quizType", q.getQuizType());
            m.put("durationMinutes", q.getDurationMinutes());
            m.put("questionsPerTest", questionsPerTest(q.getQuizType()));
            m.put("bankSize", 250);

            result.add(m);
        }

        return result;
    }

    // ─── BẮT ĐẦU THI ─────────────────────────────────────────────────────────────

    /** Bắt đầu 1 lượt thi: tạo attempt + bốc ngẫu nhiên 20/25 câu tùy loại đề */
    @Transactional
    public PracticeStartResponse start(Integer studentId, Integer quizId) {
        Quiz requestedQuiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi với id = " + quizId));

        // CHỌN NGẪU NHIÊN 1 MÃ ĐỀ (cùng môn, cùng loại)
        List<Quiz> sameSubjectQuizzes = quizRepository.findExamQuizzes(requestedQuiz.getQuizType(), requestedQuiz.getSubject());
        Quiz quiz = sameSubjectQuizzes.isEmpty() ? requestedQuiz : sameSubjectQuizzes.get(new Random().nextInt(sameSubjectQuizzes.size()));

        int need = questionsPerTest(quiz.getQuizType());
        List<Integer> randomIds = questionRepository.findRandomQuestionIds(quiz.getQuizId(), need);
        if (randomIds.isEmpty()) {
            throw new BadRequestException("Kho câu hỏi của đề này đang trống. Hãy chạy script sql/prepace_exam_system_v2.sql trước.");
        }

        List<Question> questions = questionRepository.findWithOptionsByIds(randomIds);
        Map<Integer, Question> byId = questions.stream()
                .collect(Collectors.toMap(Question::getQuestionId, Function.identity()));
        List<Question> ordered = randomIds.stream().map(byId::get).collect(Collectors.toList());

        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuiz(quiz);
        attempt.setStudentId(studentId);
        attempt.setStartedAt(new Date());
        attempt.setTotalQuestions(ordered.size());
        quizAttemptRepository.save(attempt);

        List<PracticeAnswer> blanks = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            PracticeAnswer pa = new PracticeAnswer();
            pa.setAttempt(attempt);
            pa.setQuestion(ordered.get(i));
            pa.setQuestionOrder(i + 1);
            blanks.add(pa);
        }
        practiceAnswerRepository.saveAll(blanks);

        return buildStartResponse(attempt, quiz, ordered);
    }

    // ─── RESUME (F5 giữa chừng) ──────────────────────────────────────────────────

    /** Lấy lại đề đang làm dở theo attemptId — trả cả số giây còn lại */
    @Transactional(readOnly = true)
    public PracticeStartResponse getAttempt(Integer studentId, Integer attemptId) {
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt làm bài " + attemptId));
        if (!attempt.getStudentId().equals(studentId)) {
            throw new BadRequestException("Lượt làm bài này không thuộc về bạn.");
        }
        if (attempt.getSubmittedAt() != null) {
            throw new BadRequestException("ALREADY_SUBMITTED"); // FE bắt mã này để chuyển sang trang kết quả
        }

        List<Question> ordered = practiceAnswerRepository
                .findByAttemptIdWithQuestions(attemptId).stream()
                .sorted(Comparator.comparing(PracticeAnswer::getQuestionOrder))
                .map(PracticeAnswer::getQuestion)
                .collect(Collectors.toList());

        return buildStartResponse(attempt, attempt.getQuiz(), ordered);
    }

    private PracticeStartResponse buildStartResponse(QuizAttempt attempt, Quiz quiz, List<Question> ordered) {
        int durationMinutes = quiz.getDurationMinutes() == null ? 30 : quiz.getDurationMinutes();
        long elapsedSec = (System.currentTimeMillis() - attempt.getStartedAt().getTime()) / 1000;
        int remaining = (int) Math.max(0, durationMinutes * 60L - elapsedSec);

        List<PracticeQuestionDto> questionDtos = ordered.stream()
                .map(q -> new PracticeQuestionDto(
                        q.getQuestionId(),
                        q.getQuestionContent(),
                        q.getTopic(),
                        q.getQuestionType(),
                        q.getOptions().stream()
                                .map(o -> new PracticeOptionDto(o.getOptionId(), o.getOptionContent()))
                                .collect(Collectors.toList())))
                .collect(Collectors.toList());

        return PracticeStartResponse.builder()
                .attemptId(attempt.getAttemptId())
                .quizId(quiz.getQuizId())
                .quizTitle(quiz.getQuizTitle())
                .subject(quiz.getSubject())
                .quizType(quiz.getQuizType())
                .durationMinutes(durationMinutes)
                .remainingSeconds(remaining)
                .totalQuestions(ordered.size())
                .questions(questionDtos)
                .build();
    }

    // ─── NỘP BÀI & CHẤM ĐIỂM ─────────────────────────────────────────────────────

    /** Nộp bài: chấm điểm, lưu lịch sử, trả chi tiết từng câu kèm explanation */
    // ─── NỘP BÀI & CHẤM ĐIỂM HỖN HỢP (Trắc nghiệm + Ngắn + Tự luận) ────────────────
    @Transactional
    public PracticeResultResponse submit(Integer studentId, PracticeSubmitRequest request) {
        QuizAttempt attempt = quizAttemptRepository.findById(request.getAttemptId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt làm bài " + request.getAttemptId()));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new BadRequestException("Lượt làm bài này không thuộc về bạn.");
        }
        if (attempt.getSubmittedAt() != null) {
            throw new BadRequestException("Bài này đã được nộp rồi. Không thể nộp lại.");
        }

        List<PracticeAnswer> answers = practiceAnswerRepository
                .findByAttemptIdWithQuestions(attempt.getAttemptId());

        // Đón nhận dữ liệu dạng Map<Integer, String> linh hoạt
        Map<Integer, String> submitted = request.getAnswers() == null ? Map.of() : request.getAnswers();

        int correctCount = 0;
        boolean hasEssay = false;

        for (PracticeAnswer pa : answers) {
            Question question = pa.getQuestion();
            String userValue = submitted.get(question.getQuestionId());
            String qType = question.getQuestionType(); // CHOICE, SHORT_ANSWER, ESSAY

            // Nếu câu hỏi bị bỏ trống
            if (userValue == null || userValue.trim().isEmpty()) {
                pa.setSelectedOptionId(null);
                pa.setEssayAnswer(null);
                pa.setIsCorrect(false);
                continue;
            }

            // PHÂN LOẠI XỬ LÝ LOGIC THEO KIỂU CÂU HỎI:
            if ("CHOICE".equals(qType) || qType == null|| "MULTIPLE_CHOICE".equals(qType)) {
                // Trường hợp 1: Trắc nghiệm truyền thống
                Integer selectedId = Integer.parseInt(userValue);
                QuestionOption selected = question.getOptions().stream()
                        .filter(o -> o.getOptionId().equals(selectedId))
                        .findFirst()
                        .orElseThrow(() -> new BadRequestException(
                                "Lựa chọn " + selectedId + " không thuộc câu hỏi " + question.getQuestionId()));

                boolean isCorrect = Boolean.TRUE.equals(selected.getIsCorrect());
                pa.setSelectedOptionId(selectedId);
                pa.setEssayAnswer(null);
                pa.setIsCorrect(isCorrect);
                if (isCorrect) correctCount++;
            }
            else if ("SHORT_ANSWER".equals(qType)) {
                // Trường hợp 2: Đáp án ngắn -> Máy tự động khớp chuỗi chữ
                pa.setEssayAnswer(userValue);
                pa.setSelectedOptionId(null);

                boolean isCorrect = question.getCorrectAnswer() != null
                        && question.getCorrectAnswer().trim().equalsIgnoreCase(userValue.trim());
                pa.setIsCorrect(isCorrect);
                if (isCorrect) correctCount++;
            }
            else if ("ESSAY".equals(qType)) {
                // Trường hợp 3: Tự luận -> Lưu đoạn văn dài và bật cờ chờ giáo viên chấm
                pa.setEssayAnswer(userValue);
                pa.setSelectedOptionId(null);
                pa.setIsCorrect(false);
                hasEssay = true;
            }
        }
        practiceAnswerRepository.saveAll(answers);

        int total = answers.size();

        double finalScore = total == 0
                ? 0
                : BigDecimal.valueOf((double) correctCount / total * 10)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        attempt.setSubmittedAt(new Date());
        attempt.setCorrectCount(correctCount);

        if (hasEssay) {
            attempt.setStatus("PENDING_GRADING");
            attempt.setScore(null);
        } else {
            attempt.setStatus("COMPLETED");
            attempt.setScore(finalScore);
        }

        quizAttemptRepository.save(attempt);
        
        // Clear AI cache for this student since their data just changed
        aiService.clearCache(studentId);

        return buildResult(attempt, answers);
    }

    // ─── XEM LẠI KẾT QUẢ & LỊCH SỬ ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PracticeResultResponse getResult(Integer studentId, Integer attemptId) {
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt làm bài " + attemptId));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new BadRequestException("Lượt làm bài này không thuộc về bạn.");
        }
        if (attempt.getSubmittedAt() == null) {
            throw new BadRequestException("Bài này chưa được nộp, chưa có kết quả.");
        }

        List<PracticeAnswer> answers = practiceAnswerRepository.findByAttemptIdWithQuestions(attemptId);
        return buildResult(attempt, answers);
    }

    /** Lịch sử các lượt đã nộp (mới nhất trước) — hiển thị trên trang danh sách đề */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHistory(Integer studentId) {
        return quizAttemptRepository.findByStudentIdOrderBySubmittedAtDesc(studentId).stream()
                .filter(a -> a.getSubmittedAt() != null && a.getQuiz() != null)
                .limit(20)
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("attemptId", a.getAttemptId());
                    m.put("quizId", a.getQuiz().getQuizId());
                    m.put("quizTitle", a.getQuiz().getQuizTitle());
                    m.put("subject", a.getQuiz().getSubject());
                    m.put("quizType", a.getQuiz().getQuizType());
                    m.put("score", a.getScore());
                    m.put("correctCount", a.getCorrectCount());
                    m.put("totalQuestions", a.getTotalQuestions());
                    m.put("submittedAt", a.getSubmittedAt());
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ─── HELPER ──────────────────────────────────────────────────────────────────

    /** Phân loại năng lực theo % đúng (đồng bộ với EntryTestService) */
    public static String classifyLevel(double percentage) {
        if (percentage >= 80) return "Giỏi";
        if (percentage >= 65) return "Khá";
        if (percentage >= 50) return "Trung bình";
        return "Yếu";
    }

    private PracticeResultResponse buildResult(QuizAttempt attempt, List<PracticeAnswer> answers) {
        List<PracticeQuestionReview> details = answers.stream()
                .sorted(Comparator.comparing(PracticeAnswer::getQuestionOrder))
                .map(pa -> {
                    Question q = pa.getQuestion();
                    Integer correctOptionId = q.getOptions().stream()
                            .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                            .map(QuestionOption::getOptionId)
                            .findFirst().orElse(null);

                    List<PracticeOptionReview> optionReviews = q.getOptions().stream()
                            .map(o -> new PracticeOptionReview(
                                    o.getOptionId(),
                                    o.getOptionContent(),
                                    Boolean.TRUE.equals(o.getIsCorrect()),
                                    o.getOptionId().equals(pa.getSelectedOptionId())))
                            .collect(Collectors.toList());

                    return PracticeQuestionReview.builder()
                            .questionId(q.getQuestionId())
                            .questionOrder(pa.getQuestionOrder())
                            .questionContent(q.getQuestionContent())
                            .topic(q.getTopic())
                            .options(optionReviews)
                            .selectedOptionId(pa.getSelectedOptionId())
                            // 🔥 THÊM DÒNG NÀY: Để Frontend nhận được đoạn chữ học sinh đã gõ
                            .essayAnswer(pa.getEssayAnswer())
                            .questionType(q.getQuestionType())
                            .correctOptionId(correctOptionId)
                            .correct(Boolean.TRUE.equals(pa.getIsCorrect()))
                            // Sửa lại logic kiểm tra xem câu hỏi đã được làm hay chưa
                            .answered(pa.getSelectedOptionId() != null || (pa.getEssayAnswer() != null && !pa.getEssayAnswer().trim().isEmpty()))
                            .explanation(q.getExplanation())
                            .score(pa.getScore())
                            .teacherComment(pa.getTeacherComment())
                            .build();
                })
                .collect(Collectors.toList());

        int total = details.size();
        int correct = (int) details.stream().filter(PracticeQuestionReview::isCorrect).count();
        int unanswered = (int) details.stream().filter(d -> !d.isAnswered()).count();
        double percentage = total > 0
                ? BigDecimal.valueOf(correct * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0;

        Quiz quiz = attempt.getQuiz();
        return PracticeResultResponse.builder()
                .attemptId(attempt.getAttemptId())
                .quizId(quiz.getQuizId())
                .quizTitle(quiz.getQuizTitle())
                .subject(quiz.getSubject())
                .quizType(quiz.getQuizType())
                .score(attempt.getScore())
                .percentage(percentage)
                .level(classifyLevel(percentage))
                .correctCount(correct)
                .wrongCount(total - correct - unanswered)
                .unansweredCount(unanswered)
                .totalQuestions(total)
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .details(details)
                .build();
    }

    /* ============================= ENGINE CHẤM ĐIỂM DÀNH CHO GIÁO VIÊN =============================*/

    // 1. Lấy danh sách các lượt thi đang chờ giáo viên chấm điểm
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPendingGradingAttempts() {
        return quizAttemptRepository.findByStatus("PENDING_GRADING").stream()
                .map(a -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    // Ép tên key thành sessionsId để khớp với session.sessionsId bên React
                    map.put("sessionsId", a.getAttemptId());
                    map.put("quizTitle", a.getQuiz() != null ? a.getQuiz().getQuizTitle() : "Đề luyện tập");
                    return map;
                }).collect(Collectors.toList());
    }

    // 2. Lấy chi tiết câu hỏi tự luận để hiển thị lên form chấm bài cho giáo viên
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEssayAnswersForTeacher(int attemptId) {
        List<PracticeAnswer> answers = practiceAnswerRepository.findByAttemptIdWithQuestions(attemptId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (PracticeAnswer pa : answers) {
            String qType = pa.getQuestion().getQuestionType();
            if ("ESSAY".equals(qType) || "TEXT_OR_ESSAY".equals(qType)) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("answerId", pa.getPracticeAnswerId()); // ID câu trả lời để PUT điểm
                map.put("questionId", pa.getQuestion().getQuestionId());
                map.put("content", pa.getQuestion().getQuestionContent());
                map.put("selectedAnswer", pa.getEssayAnswer()); // Chữ học sinh gõ
                map.put("correctedAnswer", pa.getQuestion().getCorrectAnswer() != null ? pa.getQuestion().getCorrectAnswer() : "Chưa có đáp án mẫu."); // Đáp án hướng dẫn
                map.put("score", pa.getScore());
                map.put("comment", pa.getTeacherComment());
                result.add(map);
            }
        }
        return result;
    }

    // 3. Xử lý lưu điểm từng câu và tự động cộng dồn số câu đúng vào bài kiểm tra (Hệ 10)
    @Transactional
    public void teacherGradeAnswer(int practiceAnswerId, double score, String comment) {
        PracticeAnswer pa = practiceAnswerRepository.findById(practiceAnswerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu trả lời với id = " + practiceAnswerId));

        pa.setScore(score);
        pa.setTeacherComment(comment);

        // 🔥 THÊM ĐOẠN NÀY: Nếu giáo viên bấm "✓ Được" (score = 1), câu này được chốt trạng thái là ĐÚNG (true)
        pa.setIsCorrect(score > 0);
        practiceAnswerRepository.save(pa);

        // Lấy ra tất cả các câu trả lời của lượt thi này để quét lại một lượt
        Integer attemptId = pa.getAttempt().getAttemptId();
        List<PracticeAnswer> allAnswers = practiceAnswerRepository.findByAttemptIdWithQuestions(attemptId);

        // Kiểm tra xem toàn bộ các câu tự luận (ESSAY) của bài này đã được giáo viên click chấm điểm hết chưa
        boolean isAllGraded = allAnswers.stream()
                .filter(ans -> "ESSAY".equals(ans.getQuestion().getQuestionType()))
                .allMatch(ans -> ans.getScore() != null);

        // Nếu đã chấm xong hết toàn bộ câu tự luận -> Tiến hành cập nhật điểm tổng cho học sinh
        if (isAllGraded) {
            QuizAttempt attempt = pa.getAttempt();
            int totalQuestions = attempt.getTotalQuestions();

            // 🎯 Đếm lại tổng số câu ĐÚNG thực tế của cả bài (Bao gồm Trắc nghiệm + Ngắn + Tự luận vừa được duyệt Đúng)
            long actualCorrectCount = allAnswers.stream()
                    .filter(ans -> Boolean.TRUE.equals(ans.getIsCorrect()))
                    .count();

            attempt.setCorrectCount((int) actualCorrectCount);

            // 🎯 Tính điểm tổng hệ 10 chuẩn đét theo ý ông: (Số câu đúng / Tổng số câu) * 10
            double finalScore = totalQuestions > 0 ? (actualCorrectCount * 10.0 / totalQuestions) : 0;

            // Làm tròn lấy 2 chữ số thập phân (Ví dụ: 8.33, 7.50) giống hệt hàm submit gốc của ông
            finalScore = BigDecimal.valueOf(finalScore)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();

            attempt.setScore(finalScore);
            attempt.setStatus("COMPLETED"); // Cập nhật trạng thái hoàn thành để học sinh xem được kết quả
            quizAttemptRepository.save(attempt);
        }
    }

    @Transactional
    public void teacherGradeAttemptAnswer(int attemptId, int questionId, double score, String comment) {
        // Tự động tìm danh sách câu trả lời của lượt thi này
        List<PracticeAnswer> allAnswers = practiceAnswerRepository.findByAttemptIdWithQuestions(attemptId);

        // Lọc ra đúng câu hỏi tự luận mà giáo viên đang nhấn chấm
        PracticeAnswer targetAnswer = allAnswers.stream()
                .filter(pa -> pa.getQuestion().getQuestionId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu trả lời tự luận để chấm"));

        // Bắn ID câu trả lời sang hàm chấm điểm chuẩn đã tối ưu ở bộ lọc mới
        this.teacherGradeAnswer(targetAnswer.getPracticeAnswerId(), score, comment);
    }

}
