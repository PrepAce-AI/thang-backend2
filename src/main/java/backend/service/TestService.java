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
        TestSession session = testSessionRepository.findBySessionsIdAndStudentId(sessionsId, studentId).orElseThrow(() -> new RuntimeException("Phien thi khong ton tai"));
        Question question = questionRepository.findById(requests.getQuestionId()).orElseThrow(() -> new RuntimeException("Cau hoi khong ton tai"));

        QuestionOption selectedOption = null;
        if (requests.getSelectedOptionId() != null){
            selectedOption = questionOptionRepository.findById(requests.getSelectedOptionId()).orElseThrow(() -> new RuntimeException("Lua chon khong ton tai"));
        }

        StudentAnswer answer = new StudentAnswer();
        answer.setSession(session);
        answer.setQuestion(question);
        answer.setSelectedOption(selectedOption);
        answer.setAnsweredAt(new Date());

        studentAnswerRepository.save(answer);
    }

    @Transactional
    public TestResultResponse submitTest(int sessionsId, int studentId) {
        TestSession session = testSessionRepository
                .findBySessionsIdAndStudentId(sessionsId, studentId)
                .orElseThrow(() -> new RuntimeException("Phiên thi không tồn tại"));
        if ("SUBMITTED".equals(session.getStatus())) {
            throw new RuntimeException("Bài thi đã được nộp");
        }
        // Lấy tất cả đáp án của thí sinh
        List<StudentAnswer> studentAnswers =
                studentAnswerRepository.findBySessionSessionsId(sessionsId);
        // Lấy toàn bộ câu hỏi của quiz
        List<Question> questions = session.getQuiz().getQuestions();
        int totalQuestions = questions.size();
        int correctCount = 0;
        List<QuestionResult> resultDetails = new ArrayList<>();

        for (Question question : questions) {

            StudentAnswer studentAnswer = studentAnswers.stream()
                    .filter(sa -> sa.getQuestion().getQuestionId()
                            == question.getQuestionId())
                    .findFirst()
                    .orElse(null);

            QuestionResult qr = new QuestionResult();
            qr.setQuestionId(question.getQuestionId());
            qr.setContent(question.getQuestionContent());
            qr.setExplanation(question.getExplanation());

            if (studentAnswer != null && studentAnswer.getSelectedOption() != null) {
                String selectedContent =
                        studentAnswer.getSelectedOption().getOptionContent();
                qr.setSelectedAnswer(selectedContent);
                boolean isCorrect =
                        question.getCorrectAnswer() != null
                                && question.getCorrectAnswer()
                                .trim()
                                .equalsIgnoreCase(selectedContent.trim());
                qr.setCorrectedAnswer(question.getCorrectAnswer());
                qr.setCorrect(isCorrect);
                if (isCorrect) {
                    correctCount++;
                }
            } else {
                qr.setSelectedAnswer("Chưa trả lời");
                qr.setCorrectedAnswer(question.getCorrectAnswer());
                qr.setCorrect(false);
            }
            resultDetails.add(qr);
        }

        // Điểm thang 10
        float score = totalQuestions > 0
                ? ((float) correctCount / totalQuestions) * 10
                : 0;

        // Làm tròn 2 chữ số
        score = Math.round(score * 100) / 100.0f;

        // Cập nhật session
        session.setSubmittedAt(new Date());
        session.setScore(score);
        session.setStatus("SUBMITTED");

        testSessionRepository.save(session);

        // Tạo response
        TestResultResponse response = new TestResultResponse();

        response.setSessionsId(session.getSessionsId());
        response.setScore(score);
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
                    .filter(sa -> sa.getQuestion().getQuestionId() == question.getQuestionId())
                    .findFirst()
                    .orElse(null);

            QuestionResult qr = new QuestionResult();
            qr.setQuestionId(question.getQuestionId());
            qr.setContent(question.getQuestionContent());
            qr.setExplanation(question.getExplanation());
            qr.setCorrectedAnswer(question.getCorrectAnswer());

            String correctAnswer = question.getCorrectAnswer() != null
                    ? question.getCorrectAnswer().trim() : "";

            if (studentAnswer != null && studentAnswer.getSelectedOption() != null) {
                String selectedContent = studentAnswer.getSelectedOption().getOptionContent().trim();

                qr.setSelectedAnswer(selectedContent);

                // === LOGIC SO SÁNH MẠNH HƠN ===
                boolean isCorrect = false;

                if (correctAnswer.equalsIgnoreCase(selectedContent)) {
                    isCorrect = true;
                } else {
                    // Xử lý trường hợp đáp án đúng là "x=2 hoặc x=3" nhưng option là "x=2 hoặc x=3"
                    String normalizedCorrect = correctAnswer.replace("hoặc", "hoặc").trim();
                    String normalizedSelected = selectedContent.replace("hoặc", "hoặc").trim();
                    isCorrect = normalizedCorrect.equalsIgnoreCase(normalizedSelected);
                }

                qr.setCorrect(isCorrect);
                if (isCorrect) correctAnswers++;

            } else {
                qr.setSelectedAnswer("Chưa trả lời");
                qr.setCorrect(false);
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
}
