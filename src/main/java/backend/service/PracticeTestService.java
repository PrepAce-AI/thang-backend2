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
        List<Quiz> quizzes = quizRepository.findExamQuizzes(type, subject);
        
        // Nhóm các đề thi theo môn học, chỉ trả về 1 thẻ đại diện cho mỗi môn
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        
        for (Quiz q : quizzes) {
            String subj = q.getSubject() == null ? "Khác" : q.getSubject();
            if (!grouped.containsKey(subj)) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("quizId", q.getQuizId()); // Dùng ID của 1 đề bất kỳ làm mỏ neo
                m.put("quizTitle", "Đề Thi " + subj);
                m.put("subject", subj);
                m.put("quizType", q.getQuizType());
                m.put("durationMinutes", q.getDurationMinutes() == null ? 30 : q.getDurationMinutes());
                m.put("questionsPerTest", questionsPerTest(q.getQuizType()));
                // Không count từng đề để tăng tốc độ, để mặc định bankSize lớn
                m.put("bankSize", 250); 
                m.put("quizIds", new ArrayList<Integer>()); // Lưu danh sách các đề cùng môn
                grouped.put(subj, m);
            }
            // Thêm quizId vào danh sách để bốc random sau này
            ((List<Integer>) grouped.get(subj).get("quizIds")).add(q.getQuizId());
        }
        
        return new ArrayList<>(grouped.values());
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
        Map<Integer, Object> submitted = request.getAnswers() == null ? Map.of() : request.getAnswers();

        double totalScore = 0;
        int correctCount = 0; // Only tracks fully correct multiple choice or exact match short answers for backward compatibility

        for (PracticeAnswer pa : answers) {
            Question question = pa.getQuestion();
            Object rawAnswer = submitted.get(question.getQuestionId());
            String qType = question.getQuestionType();
            
            if (qType == null) qType = "CHOICE";

            if ("CHOICE".equals(qType)) {
                Integer selectedId = null;
                if (rawAnswer != null) {
                    try {
                        selectedId = Integer.parseInt(rawAnswer.toString());
                    } catch (NumberFormatException e) {
                        // ignore invalid ID
                    }
                }

                Integer finalSelectedId = selectedId;
                QuestionOption selected = finalSelectedId == null ? null : question.getOptions().stream()
                        .filter(o -> o.getOptionId().equals(finalSelectedId))
                        .findFirst()
                        .orElse(null);

                boolean isCorrect = selected != null && Boolean.TRUE.equals(selected.getIsCorrect());
                pa.setSelectedOptionId(finalSelectedId);
                pa.setIsCorrect(isCorrect);
                
                if (isCorrect) {
                    correctCount++;
                    pa.setScore(10.0f);
                    totalScore += 10.0;
                } else {
                    pa.setScore(0f);
                }
            } else if ("TRUE_FALSE".equals(qType)) {
                int correctOpts = 0;
                int totalOpts = question.getOptions().size();
                try {
                    if (rawAnswer instanceof Map) {
                        Map<String, Boolean> tfAnswers = (Map<String, Boolean>) rawAnswer;
                        pa.setEssayAnswer(objectMapper.writeValueAsString(tfAnswers)); // Lưu dạng JSON vào essayAnswer

                        for (QuestionOption opt : question.getOptions()) {
                            Boolean studentChoice = tfAnswers.get(String.valueOf(opt.getOptionId()));
                            if (studentChoice != null && studentChoice.equals(opt.getIsCorrect())) {
                                correctOpts++;
                            }
                        }
                    }
                } catch (Exception e) {
                    // ignore
                }

                // Điểm theo luật Bộ GD: 1 đúng = 0.1, 2 = 0.25, 3 = 0.5, 4 = 1.0 (nhân hệ số 10)
                float scoreForTF = 0f;
                if (correctOpts == 1) scoreForTF = 1.0f;
                else if (correctOpts == 2) scoreForTF = 2.5f;
                else if (correctOpts == 3) scoreForTF = 5.0f;
                else if (correctOpts == 4) scoreForTF = 10.0f;

                pa.setScore(scoreForTF);
                pa.setIsCorrect(correctOpts == totalOpts && totalOpts > 0);
                if (pa.getIsCorrect()) correctCount++;
                totalScore += scoreForTF;
            } else if ("SHORT_ANSWER".equals(qType)) {
                String studentAns = rawAnswer != null ? String.valueOf(rawAnswer).trim() : null;
                pa.setEssayAnswer(studentAns);
                
                String correctAns = question.getCorrectAnswer();
                if (correctAns != null && studentAns != null && studentAns.equalsIgnoreCase(correctAns.trim())) {
                    pa.setIsCorrect(true);
                    pa.setScore(10.0f);
                    correctCount++;
                    totalScore += 10.0;
                } else {
                    pa.setIsCorrect(false);
                    pa.setScore(0f);
                }
            } else if ("ESSAY".equals(qType)) {
                String studentAns = rawAnswer != null ? String.valueOf(rawAnswer).trim() : null;
                pa.setEssayAnswer(studentAns);
                
                AiGradingService.GradingResult res = aiGradingService.gradeEssay(question.getQuestionContent(), studentAns);
                pa.setScore(res.score);
                pa.setTeacherComment(res.comment);
                pa.setIsCorrect(res.score >= 5.0f);
                if (pa.getIsCorrect()) correctCount++;
                totalScore += res.score;
            }
        }
        practiceAnswerRepository.saveAll(answers);

        int total = answers.size();
        double finalScore = total == 0 ? 0 : BigDecimal.valueOf(totalScore / total)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        attempt.setSubmittedAt(new Date());
        attempt.setCorrectCount(correctCount);
        attempt.setScore(finalScore);
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
                            .correctOptionId(correctOptionId)
                            .correct(Boolean.TRUE.equals(pa.getIsCorrect()))
                            .answered(pa.getSelectedOptionId() != null || (pa.getEssayAnswer() != null && !pa.getEssayAnswer().trim().isEmpty()))
                            .explanation(q.getExplanation())
                            .questionType(q.getQuestionType())
                            .essayAnswer(pa.getEssayAnswer())
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
}
