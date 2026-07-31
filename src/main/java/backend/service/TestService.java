package backend.service;

import backend.dto.request.SubmitAnswerRequest;
import backend.dto.request.StartTestRequest;
import backend.dto.response.*;
import backend.entity.*;
import backend.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TestService {
    private final TestSessionRepository testSessionRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;

    // Bắt đầu bài thi
    @Transactional
    public TestSessionResponse startTest(StartTestRequest request, Integer studentId, String ipAddress, String userAgent){
        Quiz quiz = quizRepository.findById(request.getQuizId()).orElseThrow(() -> new RuntimeException("Không tìm thấy bài trắc nghiệm"));

        TestSession session = new TestSession();
        session.setQuiz(quiz);

        User student = new User();
        student.setId(studentId);
        session.setStudent(student);

        session.setStartedAt(new Date());
        session.setRemainingTime(quiz.getDurationMinutes() * 60); // Chuyển sang giây
        session.setStatus("IN_PROGRESS");
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);

        TestSession saved = testSessionRepository.save(session);

        TestSessionResponse response = new TestSessionResponse();
        response.setSessionsId(saved.getSessionsId());
        response.setQuizId(quiz.getQuizId());
        response.setQuizTitle(quiz.getQuizTitle());
        response.setRemainingTime(saved.getRemainingTime());
        response.setStatus(saved.getStatus());
        response.setStartedAt(saved.getStartedAt());

        return response;
    }

    // Lấy câu hỏi
    public List<QuestionResponse> getQuestions (int sessionsId, Integer studentId){
        TestSession session = testSessionRepository.findBySessionsIdAndStudentId(sessionsId, studentId)
                .orElseThrow(() -> new RuntimeException("Phiên thi không tồn tại"));

        Integer quizId = session.getQuiz().getQuizId();
        List<Question> questions = questionRepository.findByQuizId(quizId);

        List<QuestionResponse> responses = new ArrayList<>();

        for(Question q : questions){
            QuestionResponse qr = new QuestionResponse();
            qr.setQuestionId(q.getQuestionId());
            qr.setContent(q.getQuestionContent());
            qr.setExplanation(q.getExplanation() != null ? q.getExplanation() : "");
            qr.setQuestionType(q.getQuestionType());

            List<OptionResponse> options = new ArrayList<>();
            if (q.getOptions() != null) {
                for (QuestionOption opt : q.getOptions()) {
                    options.add(new OptionResponse(opt.getOptionId(), opt.getOptionContent()));
                }
            }
            qr.setOptions(options);
            responses.add(qr);
        }

        Collections.shuffle(responses);
        return responses;
    }

    // Nộp từng đáp án (Auto-save khi làm bài)
    @Transactional
    public void submitAnswer(int sessionsId, SubmitAnswerRequest requests, int studentId){
        System.out.println("========== SUBMIT ANSWER ==========");
        System.out.println("Question = " + requests.getQuestionId());
        System.out.println("OptionId = " + requests.getSelectedOptionId());
        System.out.println("Essay    = " + requests.getEssayAnswer());
        TestSession session = testSessionRepository.findBySessionsIdAndStudentId(sessionsId, studentId)
                .orElseThrow(() -> new RuntimeException("Phiên thi không tồn tại"));
        Question question = questionRepository.findById(requests.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Câu hỏi không tồn tại"));

        StudentAnswer answer = studentAnswerRepository
                .findBySessionSessionsIdAndQuestionQuestionId(sessionsId, question.getQuestionId())
                .orElse(new StudentAnswer());

        answer.setSession(session);
        answer.setQuestion(question);
        answer.setAnsweredAt(new Date());

        if ("ESSAY".equals(question.getQuestionType()) || "SHORT_ANSWER".equals(question.getQuestionType())) {
            answer.setEssayAnswer(requests.getEssayAnswer());
            answer.setSelectedOption(null);
        } else {
            if (requests.getSelectedOptionId() != null){
                QuestionOption selectedOption = questionOptionRepository.findById(requests.getSelectedOptionId())
                        .orElseThrow(() -> new RuntimeException("Lựa chọn không tồn tại"));
                answer.setSelectedOption(selectedOption);
                answer.setEssayAnswer(null);
            }
        }

        studentAnswerRepository.save(answer);
    }

    // Nộp toàn bộ bài thi
    // Nộp toàn bộ bài thi (Bản nâng cấp: Nhận toàn bộ đáp án một lượt từ FE)
    @Transactional
    public TestResultResponse submitTest(int sessionsId, int studentId, Map<Long, String> answersRequest) {
        TestSession session = testSessionRepository.findBySessionsIdAndStudentId(sessionsId, studentId)
                .orElseThrow(() -> new RuntimeException("Phiên thi không tồn tại"));

        if ("SUBMITTED".equals(session.getStatus()) || "PENDING_GRADING".equals(session.getStatus())) {
            throw new RuntimeException("Bài thi đã được nộp trước đó");
        }

        // =========================================================================
        // 🔥 BƯỚC THÊM MỚI: TỰ ĐỘNG LƯU TOÀN BỘ ĐÁP ÁN TỪ FRONTEND VÀO DB TRƯỚC KHI TÍNH ĐIỂM
        // =========================================================================
        if (answersRequest != null && !answersRequest.isEmpty()) {
            for (Map.Entry<Long, String> entry : answersRequest.entrySet()) {
                Long questionId = entry.getKey();
                String userValue = entry.getValue();

                // Nếu câu hỏi bỏ trống không làm thì bỏ qua không lưu
                if (userValue == null || userValue.trim().isEmpty()) continue;

                Question question = questionRepository.findById(Math.toIntExact(questionId)).orElse(null);
                if (question == null) continue;

                // Tìm xem câu này trước đó đã có bản ghi chưa, chưa có thì tạo mới (tránh trùng lặp dữ liệu)
                StudentAnswer answer = studentAnswerRepository
                        .findBySessionSessionsIdAndQuestionQuestionId(sessionsId, question.getQuestionId())
                        .orElse(new StudentAnswer());

                answer.setSession(session);
                answer.setQuestion(question);
                answer.setAnsweredAt(new Date());

                // Kiểm tra loại câu hỏi để lưu vào đúng cột dữ liệu
                if ("ESSAY".equals(question.getQuestionType()) || "SHORT_ANSWER".equals(question.getQuestionType())) {
                    answer.setEssayAnswer(userValue); // Lưu chữ gõ tự luận / đáp án ngắn
                    answer.setSelectedOption(null);
                } else {
                    // Nếu là câu Trắc nghiệm -> Ép chuỗi String từ FE về Long để tìm OptionId
                    try {
                        Long optionId = Long.parseLong(userValue);
                        QuestionOption selectedOption = questionOptionRepository.findById(Math.toIntExact(optionId)).orElse(null);
                        answer.setSelectedOption(selectedOption);
                        answer.setEssayAnswer(null);
                    } catch (NumberFormatException e) {
                        answer.setSelectedOption(null); // Phòng hờ dữ liệu lỗi dạng chuỗi
                    }
                }
                studentAnswerRepository.save(answer);
            }
        }

        // =========================================================================
        // 2. CHẠY LOGIC TÍNH ĐIỂM TỰ ĐỘNG (Giữ nguyên logic gốc của ông cực chuẩn)
        // =========================================================================
        List<StudentAnswer> studentAnswers = studentAnswerRepository.findBySessionSessionsId(sessionsId);
        List<Question> questions = session.getQuiz().getQuestions();

        int totalQuestions = questions.size();
        int correctCount = 0;
        boolean hasEssay = false;
        List<QuestionResult> resultDetails = new ArrayList<>();

        for (Question question : questions) {
            if ("ENTER_TEST_OR_MOCK_OR_PRACTICE".equals(question.getQuestionType()) || "ESSAY".equals(question.getQuestionType())) {
                hasEssay = true;
            }

            StudentAnswer studentAnswer = studentAnswers.stream()
                    .filter(sa -> sa.getQuestion().getQuestionId().equals(question.getQuestionId()))
                    .findFirst().orElse(null);

            QuestionResult qr = new QuestionResult();
            qr.setQuestionId(question.getQuestionId());
            qr.setContent(question.getQuestionContent());
            qr.setExplanation(question.getExplanation());
            qr.setQuestionType(question.getQuestionType());

            // Kiểm tra Đáp án ngắn
            if ("SHORT_ANSWER".equals(question.getQuestionType())) {
                if (studentAnswer != null && studentAnswer.getEssayAnswer() != null && !studentAnswer.getEssayAnswer().trim().isEmpty()) {
                    String studentText = studentAnswer.getEssayAnswer().trim();
                    qr.setSelectedAnswer(studentText);

                    boolean isCorrect = question.getCorrectAnswer() != null
                            && question.getCorrectAnswer().trim().equalsIgnoreCase(studentText);
                    qr.setCorrectedAnswer(question.getCorrectAnswer());
                    qr.setCorrect(isCorrect);
                    if (isCorrect) correctCount++;
                } else {
                    qr.setSelectedAnswer("Chưa trả lời");
                    qr.setCorrectedAnswer(question.getCorrectAnswer());
                    qr.setCorrect(false);
                }
            }
            // Kiểm tra Tự luận
            else if ("ESSAY".equals(question.getQuestionType())) {
                qr.setSelectedAnswer(studentAnswer != null && studentAnswer.getEssayAnswer() != null ? studentAnswer.getEssayAnswer() : "Chưa trả lời");
                qr.setCorrectedAnswer("Chờ giáo viên chấm điểm");
                qr.setCorrect(false);
                qr.setScore(studentAnswer != null ? studentAnswer.getScore() : null);
                qr.setTeacherComment(studentAnswer != null ? studentAnswer.getTeacherComment() : null);
            }
            // Kiểm tra Trắc nghiệm (CHOICE)
            else {
                QuestionOption correctOption = question.getOptions().stream()
                        .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                        .findFirst().orElse(null);

                qr.setCorrectedAnswer(correctOption != null ? correctOption.getOptionContent() : null);

                if (studentAnswer != null && studentAnswer.getSelectedOption() != null) {
                    qr.setSelectedAnswer(studentAnswer.getSelectedOption().getOptionContent());
                    boolean isCorrect = correctOption != null && Objects.equals(studentAnswer.getSelectedOption().getOptionId(), correctOption.getOptionId());
                    qr.setCorrect(isCorrect);
                    if (isCorrect) correctCount++;
                } else {
                    qr.setSelectedAnswer("Chưa trả lời");
                    qr.setCorrect(false);
                }
            }
            resultDetails.add(qr);
        }

        session.setSubmittedAt(new Date());

        // Nếu bài làm có câu tự luận -> Chuyển sang trạng thái chờ Giáo viên chấm điểm
        if (hasEssay) {
            session.setStatus("PENDING_GRADING");
            session.setScore(null);
        } else {
            float score = totalQuestions > 0 ? ((float) correctCount / totalQuestions) * 10 : 0;
            score = Math.round(score * 100) / 100.0f;
            session.setScore(score);
            session.setStatus("SUBMITTED");
        }

        testSessionRepository.save(session);

        TestResultResponse response = new TestResultResponse();
        response.setSessionsId(session.getSessionsId());
        response.setScore(session.getScore() != null ? session.getScore() : 0.0f);
        response.setTotalQuestions(totalQuestions);
        response.setCorrectAnswers(correctCount);
        response.setTimeSpent(calculateTimeSpent(session));
        response.setSubmittedAt(session.getSubmittedAt());
        response.setQuestions(resultDetails);

        return response;
    }

    // Xem lại kết quả bài thi
    @Transactional
    public TestResultResponse getResult(int sessionsId, int userId) {
        TestSession session = testSessionRepository.findById(sessionsId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên thi"));

        if (session.getStudent().getId() != userId) {
            throw new RuntimeException("Không có quyền truy cập");
        }

        TestResultResponse response = new TestResultResponse();
        response.setSessionsId(session.getSessionsId());
        response.setScore(session.getScore() != null ? session.getScore() : 0f);
        response.setSubmittedAt(session.getSubmittedAt());

        List<Question> allQuestions = session.getQuiz().getQuestions();
        response.setTotalQuestions(allQuestions.size());

        List<StudentAnswer> answers = studentAnswerRepository.findBySessionSessionsId(sessionsId);

        int correctAnswers = 0;
        List<QuestionResult> questionResults = new ArrayList<>();

        for (Question question : allQuestions) {
            StudentAnswer studentAnswer = answers.stream()
                    .filter(sa -> sa.getQuestion().getQuestionId().equals(question.getQuestionId()))
                    .findFirst().orElse(null);

            QuestionResult qr = new QuestionResult();
            qr.setQuestionId(question.getQuestionId());
            qr.setContent(question.getQuestionContent());
            qr.setExplanation(question.getExplanation());
            qr.setQuestionType(question.getQuestionType());

            if ("SHORT_ANSWER".equals(question.getQuestionType())) {
                qr.setCorrectedAnswer(question.getCorrectAnswer());
                if (studentAnswer != null && studentAnswer.getEssayAnswer() != null) {
                    qr.setSelectedAnswer(studentAnswer.getEssayAnswer());
                    boolean isCorrect = question.getCorrectAnswer() != null && question.getCorrectAnswer().trim().equalsIgnoreCase(studentAnswer.getEssayAnswer().trim());
                    qr.setCorrect(isCorrect);
                    if (isCorrect) correctAnswers++;
                } else {
                    qr.setSelectedAnswer("Chưa trả lời");
                    qr.setCorrect(false);
                }
            } else if ("ESSAY".equals(question.getQuestionType())) {
                qr.setSelectedAnswer(studentAnswer != null && studentAnswer.getEssayAnswer() != null ? studentAnswer.getEssayAnswer() : "Chưa trả lời");
                qr.setCorrectedAnswer("Chờ giáo viên chấm điểm");
                qr.setCorrect(false);
                qr.setScore(studentAnswer != null ? studentAnswer.getScore() : null);
                qr.setTeacherComment(studentAnswer != null ? studentAnswer.getTeacherComment() : null);
            } else {
                QuestionOption correctOption = question.getOptions().stream()
                        .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                        .findFirst().orElse(null);
                qr.setCorrectedAnswer(correctOption != null ? correctOption.getOptionContent() : null);

                if (studentAnswer != null && studentAnswer.getSelectedOption() != null) {
                    qr.setSelectedAnswer(studentAnswer.getSelectedOption().getOptionContent());
                    boolean isCorrect = correctOption != null && Objects.equals(studentAnswer.getSelectedOption().getOptionId(), correctOption.getOptionId());
                    qr.setCorrect(isCorrect);
                    if (isCorrect) correctAnswers++;
                } else {
                    qr.setSelectedAnswer("Chưa trả lời");
                    qr.setCorrect(false);
                }
            }
            questionResults.add(qr);
        }

        response.setCorrectAnswers(correctAnswers);
        response.setQuestions(questionResults);
        response.setTimeSpent(calculateTimeSpent(session));

        return response;
    }

    /* ============================= PHẦN CHẤM ĐIỂM CỦA GIÁO VIÊN =============================*/

    // Lấy danh sách các session đang chờ chấm điểm
    public List<Map<String, Object>> getPendingGradingSessions() {
        List<TestSession> sessions = testSessionRepository.findByStatus("PENDING_GRADING");
        List<Map<String, Object>> result = new ArrayList<>();

        for (TestSession s : sessions) {
            Map<String, Object> map = new HashMap<>();
            map.put("sessionsId", s.getSessionsId());
            map.put("quizTitle", s.getQuiz().getQuizTitle());
            result.add(map);
        }
        return result;
    }

    // Lấy chi tiết câu trả lời tự luận để giáo viên chấm điểm
    public List<Map<String, Object>> getEssayAnswersForTeacher(int sessionId) {
        List<StudentAnswer> answers = studentAnswerRepository.findBySessionSessionsId(sessionId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (StudentAnswer sa : answers) {
            if ("ESSAY".equals(sa.getQuestion().getQuestionType())) {
                Map<String, Object> map = new HashMap<>();
                map.put("answerId", sa.getAnswerId());
                map.put("questionId", sa.getQuestion().getQuestionId());
                map.put("content", sa.getQuestion().getQuestionContent());
                map.put("selectedAnswer", sa.getEssayAnswer());
                map.put("score", sa.getScore());
                map.put("comment", sa.getTeacherComment());
                result.add(map);
            }
        }
        return result;
    }

    // Giáo viên chấm điểm từng câu tự luận và tự động tính lại tổng điểm Thang 10 của toàn bài
    @Transactional
    public void teacherGradeAnswer(int answerId, float score, String comment) {
        StudentAnswer sa = studentAnswerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu trả lời"));
        sa.setScore(score);
        sa.setTeacherComment(comment);
        studentAnswerRepository.save(sa);

        int sessionId = sa.getSession().getSessionsId();
        List<StudentAnswer> allAnswers = studentAnswerRepository.findBySessionSessionsId(sessionId);

        // Kiểm tra xem tất cả các câu tự luận (ESSAY) của bài này đã được chấm điểm chưa
        boolean isAllGraded = allAnswers.stream()
                .filter(a -> "ESSAY".equals(a.getQuestion().getQuestionType()))
                .allMatch(a -> a.getScore() != null);

        if (isAllGraded) {
            TestSession session = sa.getSession();
            int totalQuestions = session.getQuiz().getQuestions().size();

            // 1. Số câu trắc nghiệm đúng
            long correctChoices = allAnswers.stream()
                    .filter(a -> !"ESSAY".equals(a.getQuestion().getQuestionType()) && !"SHORT_ANSWER".equals(a.getQuestion().getQuestionType()))
                    .filter(a -> {
                        QuestionOption correctOption = a.getQuestion().getOptions().stream()
                                .filter(o -> Boolean.TRUE.equals(o.getIsCorrect())).findFirst().orElse(null);
                        return a.getSelectedOption() != null && correctOption != null
                                && Objects.equals(a.getSelectedOption().getOptionId(), correctOption.getOptionId());
                    }).count();

            // 2. Số câu đáp án ngắn đúng (Hệ thống tự chấm trước đó)
            long correctShortAnswers = allAnswers.stream()
                    .filter(a -> "SHORT_ANSWER".equals(a.getQuestion().getQuestionType()))
                    .filter(a -> {
                        String correct = a.getQuestion().getCorrectAnswer();
                        String typed = a.getEssayAnswer();
                        return correct != null && typed != null && correct.trim().equalsIgnoreCase(typed.trim());
                    }).count();

            // 3. Tổng số câu đúng (Trắc nghiệm + Đáp án ngắn)
            long totalAutoCorrectAnswers = correctChoices + correctShortAnswers;

            // 4. Giả sử mỗi câu hỏi trong đề có trọng số điểm bằng nhau (Ví dụ đề có 40 câu thì mỗi câu 10/40 = 0.25 điểm)
            // Điểm của phần tự động chấm quy về thang 10:
            float autoGradeScore = totalQuestions > 0 ? ((float) totalAutoCorrectAnswers / totalQuestions) * 10 : 0;

            // Điểm tự luận của giáo viên chấm (giả sử giáo viên nhập điểm câu tự luận trực tiếp trên thang 10 quy đổi của câu đó)
            double totalEssayScore = allAnswers.stream()
                    .filter(a -> "ESSAY".equals(a.getQuestion().getQuestionType()))
                    .mapToDouble(a -> a.getScore() != null ? a.getScore() : 0.0)
                    .sum();

            // Tổng điểm cuối cùng (Giới hạn tối đa là 10)
            float finalScore = (float) (autoGradeScore + totalEssayScore);
            if (finalScore > 10.0f) finalScore = 10.0f;
            finalScore = Math.round(finalScore * 100) / 100.0f;

            session.setScore(finalScore);
            session.setStatus("SUBMITTED");
            testSessionRepository.save(session);
        }
    }

    private int calculateTimeSpent(TestSession session){
        if (session.getSubmittedAt() == null || session.getStartedAt() == null){
            return 0;
        }
        long diffInMs = session.getSubmittedAt().getTime() - session.getStartedAt().getTime();
        return (int) (diffInMs / 1000); // Trả về giây
    }
    @org.springframework.transaction.annotation.Transactional
    public void gradeEssayAnswer(int sessionId, int questionId, float score, String comment) {
        StudentAnswer sa = studentAnswerRepository
                .findBySessionSessionsIdAndQuestionQuestionId(sessionId, questionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu trả lời tự luận để chấm điểm"));

        this.teacherGradeAnswer(sa.getAnswerId(), score, comment);
    }
}