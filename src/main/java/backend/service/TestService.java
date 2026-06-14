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
    public List<QuestionResponse> getQuestions (int sessionsId, int studentId){
        TestSession session = testSessionRepository.findBySessionsIdAndStudentId(sessionsId, studentId)
                                                    .orElseThrow(() -> new RuntimeException("Phiên Thi Không Tồn Tại"));
        List<Question> questions = new ArrayList<>(session.getQuiz().getQuestions());
        Collections.shuffle(questions); //Xao tron cau hoi

        List<QuestionResponse> responses = new ArrayList<>();
        for(Question q : questions){
            QuestionResponse qr = new QuestionResponse();
            qr.setQuestionId(q.getQuestionId());
            qr.setContent(q.getQuestionContent());
            qr.setExplanation(q.getExplanation());

            List<OptionResponse> options = new ArrayList<>();
            for (QuestionOption opt : q.getOptions()){
                options.add(new OptionResponse(opt.getOptionId(), opt.getOptionContent()));
            }
            qr.setOptions(options);

            responses.add(qr);
        }
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
    public TestResultResponse submitTest(int sessionsId, int studentId){
        TestSession session = testSessionRepository.findBySessionsIdAndStudentId(sessionsId, studentId).orElseThrow(() -> new RuntimeException("Phiên Thi Không Tồn Tại"));
        if("SUBMITTED".equals(session.getStatus())){
            throw new RuntimeException("Bài Thi Đã Được Nộp !!!");
        }

        //Lay tat ca dap an cua thi sinh trong bai thi nay
        List<StudentAnswer> studentAnswers = studentAnswerRepository.findBySessionSessionsId(sessionsId);

        //Lay tat ca cau hoi trong quiz
        List<Question> questions = session.getQuiz().getQuestions();
        int totalQuestions = questions.size();
        int correctCount = 0;

        List<QuestionResult> resultDetails = new ArrayList<>();
        for (Question quesiton : questions){
            StudentAnswer studentAnswer = studentAnswers
                    .stream()
                    .filter(sa -> sa.getQuestion().getQuestionId() == quesiton.getQuestionId())
                    .findFirst()
                    .orElse(null);

            QuestionResult qr = new QuestionResult();
            qr.setQuestionId(quesiton.getQuestionId());
            qr.setContent(quesiton.getQuestionContent());
            qr.setExplanation(quesiton.getExplanation());

            if (studentAnswer != null && studentAnswer.getSelectedOption() != null){
                String selectedContent = studentAnswer.getSelectedOption().getOptionContent();
                qr.setSelectedAnswer(selectedContent);

                boolean isCorrect = quesiton.getCorrectAnswer() != null &&
                                    quesiton.getCorrectAnswer().trim().equalsIgnoreCase(selectedContent);
                qr.setCorrectedAnswer(quesiton.getCorrectAnswer());
                qr.setCorrect(isCorrect);

                if (isCorrect) correctCount++; //Neu cau do dung thi correctCount +1 --> So cau dung
            }else{
                qr.setSelectedAnswer("Chưa Trả Lời");
                qr.setCorrectedAnswer(quesiton.getCorrectAnswer());
                qr.setCorrect(false);
            }

            resultDetails.add(qr);
        }
        float score = totalQuestions > 0 ? (float) correctCount / totalQuestions * 10 : 0;

        //Update session
        session.setSubmittedAt(new Date());
        session.setScore(score);
        session.setStatus("SUBMITTED");
        testSessionRepository.save(session);

        //Return result
        TestResultResponse response = new TestResultResponse();
        response.setSessionsId(sessionsId);
        response.setScore(Math.round(score * 10)/ 10.0f);
        response.setTotalQuestions(totalQuestions);
        response.setCorrectAnswers(correctCount);
        response.setTimeSpent(calculateTimeSpent(session));
        response.setSubmittedAt(session.getSubmittedAt());
        response.setQuestions(resultDetails);

        return response;
    }

    @Transactional
    public TestResultResponse getResult(int sessionsId, int userId) {

        TestSession session = testSessionRepository.findById(sessionsId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // Kiểm tra đúng chủ sở hữu bài thi
        if (session.getStudent().getId() != userId) {
            throw new RuntimeException("Access denied");
        }

        TestResultResponse response = new TestResultResponse();

        response.setSessionsId(session.getSessionsId());
        response.setScore(session.getScore());
        response.setSubmittedAt(session.getSubmittedAt());

        int totalQuestions = session.getQuiz().getQuestions().size();
        response.setTotalQuestions(totalQuestions);

        int correctAnswers = 0;
        List<QuestionResult> questionResults = new ArrayList<>();

        List<StudentAnswer> answers =
                studentAnswerRepository.findBySessionSessionsId(sessionsId);

        for (StudentAnswer answer : answers) {

            Question question = answer.getQuestion();

            String selectedAnswer = null;

            if (answer.getSelectedOption() != null) {
                selectedAnswer =
                        answer.getSelectedOption().getOptionContent();
            }

            String correctedAnswer = question.getCorrectAnswer();

            boolean isCorrect =
                    selectedAnswer != null
                            && selectedAnswer.equals(correctedAnswer);

            if (isCorrect) {
                correctAnswers++;
            }

            QuestionResult qr = new QuestionResult();

            qr.setQuestionId(question.getQuestionId());
            qr.setContent(question.getQuestionContent());
            qr.setSelectedAnswer(selectedAnswer);
            qr.setCorrectedAnswer(correctedAnswer);
            qr.setExplanation(question.getExplanation());
            qr.setCorrect(isCorrect);

            questionResults.add(qr);
        }

        response.setCorrectAnswers(correctAnswers);
        response.setQuestions(questionResults);

        int timeSpent = 0;

        if (session.getStartedAt() != null && session.getSubmittedAt() != null) {

            long seconds =
                    (session.getSubmittedAt().getTime()
                            - session.getStartedAt().getTime()) / 1000;

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
