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

    //Bat Dau Bai Thi
    @Transactional
    public TestSessionResponse startTest(StartTestRequest request, Integer studentId, String ipAddress, String userAgent){
        Quiz quiz = quizRepository.findById(request.getQuizId()).orElseThrow(() ->  new RuntimeException("Khong tim thay quiz"));
        //Xu ly ngoai le thoi gian theo mon hoc (Toan va Van)
        int durationMinutes = calculateDuration(quiz);
        TestSession session = new TestSession();
        session.setQuiz(quiz);

        User student = new User();
        student.setId(studentId);
        session.setStudent(student);

        session.setStartedAt(new Date());
        session.setRemainingTime(quiz.getDurationMinutes() * 60); //Chuyen sang giay
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

    //Lay cau hoi
    public List<QuestionResponse> getQuestions (int sessionsId, Integer studentId){
        TestSession session = testSessionRepository.findBySessionsIdAndStudentId(sessionsId, studentId)
                .orElseThrow(() -> new RuntimeException("Phiên Thi Không Tồn Tại"));

        Integer quizId = session.getQuiz().getQuizId();

        // Force load questions + options
        List<Question> questions =
                questionRepository.findByQuizId(quizId);

        for (Question q : questions) {
            System.out.println(
                    "Question "
                            + q.getQuestionId()
                            + " options = "
                            + q.getOptions().size()
            );
        }

        // ===== DEBUG =====
        System.out.println("Quiz ID = " + quizId);

        for (Question q : questions) {
            System.out.println(
                    "Question ID = "
                            + q.getQuestionId()
                            + ", options = "
                            + q.getOptions().size()
            );
        }
        // =================

        List<QuestionResponse> responses = new ArrayList<>();

        for(Question q : questions){
            QuestionResponse qr = new QuestionResponse();
            qr.setQuestionId(q.getQuestionId());
            qr.setContent(q.getQuestionContent());
            qr.setExplanation(q.getExplanation() != null ? q.getExplanation() : "");
            qr.setQuestionType(q.getQuestionType());
            List<OptionResponse> options = new ArrayList<>();

            // Debug
            System.out.println("Câu " + q.getQuestionId() + " có " + (q.getOptions() != null ? q.getOptions().size() : 0) + " options");

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

    //Nop dap an
    @Transactional
    public void submitAnswer(int sessionsId, SubmitAnswerRequest requests, int studentId){
        TestSession session = testSessionRepository.findBySessionsIdAndStudentId(sessionsId, studentId)
                .orElseThrow(() -> new RuntimeException("Phien thi khong ton tai"));
        Question question = questionRepository.findById(requests.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Cau hoi khong ton tai"));

        // 🔥 THAY VÌ LUÔN LUÔN NEW MỚI, TA ĐI TÌM XEM ĐÃ CÓ CÂU TRẢ LỜI CŨ CHƯA
        StudentAnswer answer = studentAnswerRepository
                .findBySessionSessionsIdAndQuestionQuestionId(sessionsId, question.getQuestionId())
                .orElse(new StudentAnswer()); // Nếu chưa có (orElse) thì mới tạo thực thể mới hoàn toàn

        answer.setSession(session);
        answer.setQuestion(question);
        answer.setAnsweredAt(new Date());

        // PHÂN LOẠI XỬ LÝ:
        if ("ESSAY".equals(question.getQuestionType()) || "SHORT_ANSWER".equals(question.getQuestionType())) {
            // Nếu là tự luận/đáp án ngắn -> Cập nhật text mới và xóa sạch vết tích trắc nghiệm (nếu có)
            answer.setEssayAnswer(requests.getEssayAnswer());
            answer.setSelectedOption(null);
        } else {
            // Nếu là trắc nghiệm -> Cập nhật option mới và xóa sạch vết tích chữ viết tự luận (nếu có)
            if (requests.getSelectedOptionId() != null){
                QuestionOption selectedOption = questionOptionRepository.findById(requests.getSelectedOptionId())
                        .orElseThrow(() -> new RuntimeException("Lua chon khong ton tai"));
                answer.setSelectedOption(selectedOption);
                answer.setEssayAnswer(null);
            }
        }

        studentAnswerRepository.save(answer); // Spring Data JPA thấy có ID cũ sẽ tự động chạy lệnh UPDATE thay vì INSERT
    }

    @Transactional
    public TestResultResponse submitTest(int sessionsId, int studentId) {
        TestSession session = testSessionRepository.findBySessionsIdAndStudentId(sessionsId, studentId)
                .orElseThrow(() -> new RuntimeException("Phiên thi không tồn tại"));

        if ("SUBMITTED".equals(session.getStatus()) || "PENDING_GRADING".equals(session.getStatus())) {
            throw new RuntimeException("Bài thi đã được nộp trước đó");
        }

        List<StudentAnswer> studentAnswers = studentAnswerRepository.findBySessionSessionsId(sessionsId);
        List<Question> questions = session.getQuiz().getQuestions();

        int totalQuestions = questions.size();
        int correctCount = 0;
        boolean hasEssay = false; // Biến đánh dấu xem đề có tự luận không
        List<QuestionResult> resultDetails = new ArrayList<>();

        for (Question question : questions) {
            if ("COGNITIVE_LEVEL".equals(question.getQuestionType()) || "ESSAY".equals(question.getQuestionType())) {
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

            // TRƯỜNG HỢP 1: CÂU HỎI ĐIỀN ĐÁP ÁN NGẮN (TỰ ĐỘNG SO KHỚP CHUỖI)
            if ("SHORT_ANSWER".equals(question.getQuestionType())) {
                if (studentAnswer != null && studentAnswer.getEssayAnswer() != null && !studentAnswer.getEssayAnswer().trim().isEmpty()) {
                    String studentText = studentAnswer.getEssayAnswer().trim();
                    qr.setSelectedAnswer(studentText); // Lấy chữ học sinh gõ ra hiển thị

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
            // TRƯỜNG HỢP 2: CÂU HỎI TỰ LUẬN DÀI (CHỜ GIÁO VIÊN CHẤM ĐIỂM)
            else if ("ESSAY".equals(question.getQuestionType())) {
                qr.setSelectedAnswer(studentAnswer != null ? studentAnswer.getEssayAnswer() : "Chưa trả lời");
                qr.setCorrectedAnswer("Chờ giáo viên chấm điểm");
                qr.setCorrect(false);
                qr.setScore(studentAnswer != null ? studentAnswer.getScore() : null);
                qr.setTeacherComment(studentAnswer != null ? studentAnswer.getTeacherComment() : null);
            }
            // TRƯỜNG HỢP 3: CÂU HỎI TRẮC NGHIỆM CŨ CỦA BẠN
            else {
                if (studentAnswer != null && studentAnswer.getSelectedOption() != null) {
                    String selectedContent = studentAnswer.getSelectedOption().getOptionContent();
                    qr.setSelectedAnswer(selectedContent);
                    boolean isCorrect = question.getCorrectAnswer() != null
                            && question.getCorrectAnswer().trim().equalsIgnoreCase(selectedContent.trim());
                    qr.setCorrectedAnswer(question.getCorrectAnswer());
                    qr.setCorrect(isCorrect);
                    if (isCorrect) correctCount++;
                } else {
                    qr.setSelectedAnswer("Chưa trả lời");
                    qr.setCorrectedAnswer(question.getCorrectAnswer());
                    qr.setCorrect(false);
                }
            }
            resultDetails.add(qr);
        }

        // Cập nhật trạng thái Session dựa vào việc có câu tự luận hay không
        session.setSubmittedAt(new Date());

        if (hasEssay) {
            session.setStatus("PENDING_GRADING"); // Chờ giáo viên chấm điểm
            session.setScore(null); // Chưa có điểm tổng chính thức
        } else {
            // Nếu toàn bộ là trắc nghiệm thì tính điểm thang 10 luôn như cũ
            float score = totalQuestions > 0 ? ((float) correctCount / totalQuestions) * 10 : 0;
            score = Math.round(score * 100) / 100.0f;
            session.setScore(score);
            session.setStatus("SUBMITTED");
        }

        testSessionRepository.save(session);

        // Tạo response
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

    // === THAY THẾ TOÀN BỘ PHẦN getResult() ===
    @Transactional
    public TestResultResponse getResult(int sessionsId, int userId) {

        TestSession session = testSessionRepository.findById(sessionsId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getStudent().getId() != userId) {
            throw new RuntimeException("Access denied");
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
                    .findFirst()
                    .orElse(null);

            QuestionResult qr = new QuestionResult();
            qr.setQuestionId(question.getQuestionId());
            qr.setContent(question.getQuestionContent());
            qr.setExplanation(question.getExplanation());

            qr.setCorrectedAnswer(question.getCorrectAnswer());
            qr.setQuestionType(question.getQuestionType());

            // 1. NẾU LÀ CÂU TỰ LUẬN DÀI
            if ("ESSAY".equals(question.getQuestionType())) {
                qr.setSelectedAnswer(studentAnswer != null ? studentAnswer.getEssayAnswer() : "Chưa trả lời");
                boolean isCorrect = studentAnswer != null && studentAnswer.getScore() != null && studentAnswer.getScore() > 0;
                qr.setCorrect(isCorrect);

                // 🔥 THÊM ĐOẠN KIỂM TRA NÀY: Nếu đạt thì kích hoạt cộng dồn vào tổng số câu đúng
                if (isCorrect) {
                    correctAnswers++;
                }
                qr.setScore(studentAnswer != null ? studentAnswer.getScore() : null);
                qr.setTeacherComment(studentAnswer != null ? studentAnswer.getTeacherComment() : null);
            }
            // 2. NẾU LÀ CÂU ĐÁP ÁN NGẮN (SHORT_ANSWER)
            else if ("SHORT_ANSWER".equals(question.getQuestionType())) {
                if (studentAnswer != null && studentAnswer.getEssayAnswer() != null && !studentAnswer.getEssayAnswer().trim().isEmpty()) {
                    String studentText = studentAnswer.getEssayAnswer().trim();
                    qr.setSelectedAnswer(studentText); // Đưa chuỗi câu trả lời của học sinh vào DTO trả về

                    String correctAnswer = question.getCorrectAnswer() != null ? question.getCorrectAnswer().trim() : "";
                    boolean isCorrect = correctAnswer.equalsIgnoreCase(studentText);
                    qr.setCorrect(isCorrect);
                    if (isCorrect) correctAnswers++;
                } else {
                    qr.setSelectedAnswer("Chưa trả lời");
                    qr.setCorrect(false);
                }
            }
            // 3. NẾU LÀ CÂU TRẮC NGHIỆM TRUYỀN THỐNG
            else {
                String correctAnswer = question.getCorrectAnswer() != null ? question.getCorrectAnswer().trim() : "";
                if (studentAnswer != null && studentAnswer.getSelectedOption() != null) {
                    String selectedContent = studentAnswer.getSelectedOption().getOptionContent().trim();
                    qr.setSelectedAnswer(selectedContent);

                    boolean isCorrect = correctAnswer.equalsIgnoreCase(selectedContent);
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

        // Tính thời gian
        int timeSpent = 0;
        if (session.getStartedAt() != null && session.getSubmittedAt() != null) {
            long seconds = (session.getSubmittedAt().getTime() - session.getStartedAt().getTime()) / 1000;
            timeSpent = (int) seconds;
        }
        response.setTimeSpent(timeSpent);

        return response;
    }

    /**
     * Tính thời gian thi theo quy tắc thi Việt Nam
     * - Ngữ Văn: 120 phút
     * - Toán: 90 phút
     * - Các môn khác: 50 phút
     */

    /* ============================= TÍNH TOÁN THỜI GIAN =============================*/
    private int calculateDuration(Quiz quiz){
        String title = quiz.getQuizTitle() != null? quiz.getQuizTitle().toLowerCase() : "";

        //Uu tien kiem tra theo ten quiz
        if (title.contains("ngữ văn") || title.contains("văn") || title.contains("nguvan")){
            return 120;
        }

        if (title.contains("toán") || title.contains("math") || title.contains("toan")){
            return 90;
        }

        //Kiem tra theo mon hoc
        Integer subjectId = null;
        if (quiz.getCourse() != null){
            subjectId = quiz.getCourse().getSubjectId();
        }

        if (subjectId != null){
            // Bạn có thể chỉnh lại các mã subjectId cho đúng với database của mình
            if (subjectId == 4 || subjectId == 2) {           // Ngữ Văn / Literature
                return 120;
            }
            if (subjectId == 1) {                             // Toán / Mathematics
                return 90;
            }
        }

        return 50;
    }

    private int calculateTimeSpent(TestSession session){
        if (session.getSubmittedAt() == null || session.getStartedAt() == null){
            return session.getQuiz().getDurationMinutes() * 60; //Doi ra giay
        }

        long diffInMinutes = session.getSubmittedAt().getTime() - session.getStartedAt().getTime();
        return (int) (diffInMinutes / 1000);
    }

    @Transactional
    public void gradeEssayAnswer(int sessionId, int questionId, float score, String comment) {
        // 1. Tìm bản ghi câu trả lời tự luận đó để cập nhật điểm câu
        List<StudentAnswer> answers = studentAnswerRepository.findBySessionSessionsId(sessionId);
        StudentAnswer essayAns = answers.stream()
                .filter(sa -> sa.getQuestion().getQuestionId() == questionId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu trả lời"));

        essayAns.setScore(score);
        essayAns.setTeacherComment(comment);
        studentAnswerRepository.save(essayAns);

        // 2. Kiểm tra xem toàn bộ các câu tự luận trong Session này đã được chấm điểm chưa
        boolean isAllGraded = answers.stream()
                .filter(sa -> "ESSAY".equals(sa.getQuestion().getQuestionType())) // Hoặc "ESSAY"
                .allMatch(sa -> sa.getScore() != null);

        if (isAllGraded) {
            TestSession session = testSessionRepository.findById(sessionId).orElseThrow();

            // Tính tổng số câu trắc nghiệm đúng
            long correctChoices = answers.stream()
                    .filter(sa -> !"ESSAY".equals(sa.getQuestion().getQuestionType()))
                    .filter(sa -> {
                        String correct = sa.getQuestion().getCorrectAnswer();
                        String selected = sa.getSelectedOption() != null ? sa.getSelectedOption().getOptionContent() : "";
                        return correct != null && correct.trim().equalsIgnoreCase(selected.trim());
                    }).count();

            // Tính tổng điểm tự luận
            double totalEssayScore = answers.stream()
                    .filter(sa -> "ESSAY".equals(sa.getQuestion().getQuestionType()))
                    .mapToDouble(sa -> sa.getScore() != null ? sa.getScore() : 0.0)
                    .sum();

            // Công thức tính tổng điểm (Ví dụ: Trắc nghiệm chiếm bao nhiêu %, tự luận bao nhiêu điểm tùy bạn chia)
            // Giả sử mỗi câu trắc nghiệm được 1 điểm, câu tự luận cộng trực tiếp vào hệ số:
            float finalScore = (float) (correctChoices + totalEssayScore);

            session.setScore(finalScore);
            session.setStatus("SUBMITTED"); // Đổi trạng thái hoàn thành bài thi
            testSessionRepository.save(session);
        }
    }
    // 1. Lấy danh sách các session đang chờ chấm điểm
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

    // 2. Lấy chi tiết câu trả lời tự luận/đáp án ngắn của một session
    public List<Map<String, Object>> getEssayAnswersForTeacher(int sessionId) {
        List<StudentAnswer> answers = studentAnswerRepository.findBySessionSessionsId(sessionId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (StudentAnswer sa : answers) {
            // Chỉ lấy các câu hỏi thuộc dạng cần giáo viên xem xét hoặc chấm điểm
            if ("ESSAY".equals(sa.getQuestion().getQuestionType()) || "SHORT_ANSWER".equals(sa.getQuestion().getQuestionType())) {
                Map<String, Object> map = new HashMap<>();
                map.put("answerId", sa.getAnswerId());
                map.put("questionId", sa.getQuestion().getQuestionId());
                map.put("content", sa.getQuestion().getQuestionContent());
                map.put("selectedAnswer", sa.getEssayAnswer());
                map.put("correctedAnswer", sa.getQuestion().getCorrectAnswer());
                result.add(map);
            }
        }
        return result;
    }

    // 3. Giáo viên thực hiện chấm điểm cho từng câu
    @Transactional
    public void teacherGradeAnswer(int answerId, float score, String comment) {
        StudentAnswer sa = studentAnswerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu trả lời"));
        sa.setScore(score);
        sa.setTeacherComment(comment);
        studentAnswerRepository.save(sa);

        // Sau khi lưu điểm câu này, kiểm tra xem toàn bộ các câu ESSAY trong bài đã chấm hết chưa
        int sessionId = sa.getSession().getSessionsId();
        List<StudentAnswer> allAnswers = studentAnswerRepository.findBySessionSessionsId(sessionId);

        boolean isAllGraded = allAnswers.stream()
                .filter(a -> "TEXT".equals(a.getQuestion().getQuestionType()) || "ESSAY".equals(a.getQuestion().getQuestionType()))
                .allMatch(a -> a.getScore() != null);

        if (isAllGraded) {
            TestSession session = sa.getSession();

            // Tính số câu trắc nghiệm đúng (Choice)
            long correctChoices = allAnswers.stream()
                    .filter(a -> !"ESSAY".equals(a.getQuestion().getQuestionType()) && !"SHORT_ANSWER".equals(a.getQuestion().getQuestionType()))
                    .filter(a -> {
                        String correct = a.getQuestion().getCorrectAnswer();
                        String selected = a.getSelectedOption() != null ? a.getSelectedOption().getOptionContent() : "";
                        return correct != null && correct.trim().equalsIgnoreCase(selected.trim());
                    }).count();

            // Tính số câu đáp án ngắn đúng (Short_Answer)
            long correctShortAnswers = allAnswers.stream()
                    .filter(a -> "SHORT_ANSWER".equals(a.getQuestion().getQuestionType()))
                    .filter(a -> {
                        String correct = a.getQuestion().getCorrectAnswer();
                        String typed = a.getEssayAnswer();
                        return correct != null && typed != null && correct.trim().equalsIgnoreCase(typed.trim());
                    }).count();

            // Tính tổng điểm tự luận chấm tay
            double totalEssayScore = allAnswers.stream()
                    .filter(a -> "ESSAY".equals(a.getQuestion().getQuestionType()))
                    .mapToDouble(a -> a.getScore() != null ? a.getScore() : 0.0)
                    .sum();

            // Quy đổi ra tổng điểm thang 10 dựa trên tổng số câu hỏi trong Quiz
            int totalQuestions = session.getQuiz().getQuestions().size();
            float finalScore = totalQuestions > 0
                    ? ((float) (correctChoices + correctShortAnswers + totalEssayScore) / totalQuestions) * 10
                    : 0;
            finalScore = Math.round(finalScore * 100) / 100.0f;

            session.setScore(finalScore);
            session.setStatus("SUBMITTED"); // Hoàn thành bài thi
            testSessionRepository.save(session);
        }
    }
}
