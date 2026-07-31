package backend.controller;

import backend.entity.PracticeAnswer;
import backend.entity.Question;
import backend.entity.Quiz;
import backend.entity.QuizAttempt;
import backend.repository.PracticeAnswerRepository;
import backend.repository.QuestionRepository;
import backend.repository.QuizAttemptRepository;
import backend.repository.QuizRepository;
import backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * Controller dùng để tạo Mock Data lịch sử làm bài (QuizAttempt & PracticeAnswer)
 * nhằm phục vụ việc demo tính năng AI (Gap Diagnosis, Score Forecasting, University Advising).
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiDataSeederController {

    private final QuizAttemptRepository quizAttemptRepository;
    private final PracticeAnswerRepository practiceAnswerRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    @PostMapping("/seed-mock-data")
    public ResponseEntity<?> seedMockData(Authentication authentication) {
        String email = authentication.getName();
        Integer studentId = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        log.info("Seeding Mock Data for student ID: {}", studentId);

        List<Quiz> quizzes = quizRepository.findAll();
        List<Question> questions = questionRepository.findAll();

        if (quizzes.isEmpty() || questions.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Database không có dữ liệu Quiz hoặc Question để tạo Mock Data."
            ));
        }

        Random rand = new Random();
        int attemptsCreated = 0;
        int answersCreated = 0;

        // Lần 1: 4 điểm (10/25), Lần 2: 6 điểm (15/25), Lần 3: 8 điểm (20/25)
        int[] targetCorrects = {10, 15, 20};

        // Tạo 3 QuizAttempts ngẫu nhiên
        for (int i = 0; i < 3; i++) {
            Quiz quiz = quizzes.get(rand.nextInt(quizzes.size()));

            QuizAttempt attempt = new QuizAttempt();
            attempt.setQuiz(quiz);
            attempt.setStudentId(studentId);
            attempt.setStartedAt(new Date(System.currentTimeMillis() - (86400000L * (3 - i))));
            attempt.setSubmittedAt(new Date(System.currentTimeMillis() - (86400000L * (3 - i)) + 3600000L));
            attempt.setTotalQuestions(25);

            int correctCount = 0;
            List<PracticeAnswer> answersToSave = new ArrayList<>();
            int targetCorrect = targetCorrects[i];

            // Mỗi attempt tạo 25 câu hỏi ngẫu nhiên từ kho
            for (int j = 0; j < 25; j++) {
                Question q = questions.get(rand.nextInt(questions.size()));
                
                PracticeAnswer pa = new PracticeAnswer();
                pa.setAttempt(attempt);
                pa.setQuestion(q);
                pa.setQuestionOrder(j + 1);
                
                // Quyết định câu này đúng hay sai để đạt đủ targetCorrect
                int remainingQuestions = 25 - j;
                int remainingCorrectsNeeded = targetCorrect - correctCount;
                
                boolean isCorrect;
                if (remainingCorrectsNeeded >= remainingQuestions) {
                    isCorrect = true; // Phải đúng hết các câu còn lại
                } else if (remainingCorrectsNeeded <= 0) {
                    isCorrect = false; // Đã đủ số câu đúng
                } else {
                    isCorrect = rand.nextBoolean(); // Random cho đến khi đủ
                }

                // Nếu học sinh cố tình sai, ta ưu tiên sai ở môn Toán (Tích phân/Hình học) để AI chẩn đoán được
                if (!isCorrect && "Toán".equalsIgnoreCase(q.getSubject()) && 
                    (q.getTopic() != null && (q.getTopic().contains("Tích phân") || q.getTopic().contains("Hình")))) {
                    isCorrect = false; // Chắc chắn sai phần này
                }

                pa.setIsCorrect(isCorrect);
                answersToSave.add(pa);

                if (isCorrect) {
                    correctCount++;
                }
            }

            attempt.setCorrectCount(correctCount);
            attempt.setScore((correctCount * 10.0) / 25.0);

            quizAttemptRepository.save(attempt);
            practiceAnswerRepository.saveAll(answersToSave);
            
            attemptsCreated++;
            answersCreated += answersToSave.size();
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", String.format("Đã tạo %d lượt thi (QuizAttempts) và %d câu trả lời (PracticeAnswers) cho học sinh ID %d. AI đã sẵn sàng hoạt động!", attemptsCreated, answersCreated, studentId)
        ));
    }

    @PostMapping("/seed-questions")
    public ResponseEntity<?> seedQuestions() {
        List<Quiz> quizzes = quizRepository.findAll();
        if (quizzes.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Database không có dữ liệu Quiz để tạo câu hỏi."
            ));
        }

        int totalQuestionsCreated = 0;
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        for (Quiz quiz : quizzes) {
            if ("ENTRY_TEST".equals(quiz.getQuizType()) || "PRACTICE".equals(quiz.getQuizType()) || "MOCK_EXAM".equals(quiz.getQuizType())) {

                if (quiz.getQuizTitle() != null && quiz.getQuizTitle().contains("Hệ Thống Chấm Điểm")) {
                    continue; // Bỏ qua không bơm câu hỏi rác vào đề này
                }

                String subject = quiz.getSubject() != null ? quiz.getSubject().trim().toLowerCase() : "toán";
                String subName = "Toán";
                String[] jsonResources = {
                    "seeder/math_0101.json", "seeder/math_0202.json", "seeder/math_0303.json", "seeder/math_0404.json", "seeder/math_0505.json"
                };

                if (subject.contains("anh") || subject.contains("english")) {
                    subName = "Tiếng Anh";
                    jsonResources = new String[] {
                        "seeder/english_0101.json", "seeder/english_0202.json", "seeder/english_0303.json", "seeder/english_0404.json", "seeder/english_0505.json"
                    };
                } else if (subject.contains("lý") || subject.contains("physics")) {
                    subName = "Vật Lý";
                    jsonResources = new String[] {
                        "seeder/physics_0101.json", "seeder/physics_0202.json", "seeder/physics_0303.json", "seeder/physics_0404.json", "seeder/physics_0505.json"
                    };
                } else if (subject.contains("hóa") || subject.contains("chem")) {
                    subName = "Hóa Học";
                    jsonResources = new String[] {
                        "seeder/chemistry_0101.json", "seeder/chemistry_0202.json", "seeder/chemistry_0303.json", "seeder/chemistry_0404.json", "seeder/chemistry_0505.json"
                    };
                } else if (subject.contains("sinh") || subject.contains("bio")) {
                    subName = "Sinh Học";
                    jsonResources = new String[] {
                        "seeder/biology_0101.json", "seeder/biology_0202.json", "seeder/biology_0303.json", "seeder/biology_0404.json", "seeder/biology_0505.json"
                    };
                }

                List<Question> questionsToSave = new ArrayList<>();
                for (String jsonResource : jsonResources) {
                    try {
                        java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(jsonResource);
                        if (is == null) {
                            log.error("Cannot find resource: {}", jsonResource);
                            continue;
                        }
                        
                        List<Map<String, Object>> qList = mapper.readValue(is, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                        
                        for (Map<String, Object> map : qList) {
                            Question q = new Question();
                            q.setQuiz(quiz);
                            q.setSubject(subName);
                            q.setDifficulty((Integer) map.getOrDefault("difficulty", 2));
                            q.setQuestionType((String) map.get("questionType"));
                            q.setTopic((String) map.get("topic"));
                            q.setQuestionContent((String) map.get("questionContent"));
                            q.setExplanation((String) map.get("explanation"));
                            q.setCorrectAnswer((String) map.get("correctAnswer"));
                            
                            List<Map<String, Object>> options = (List<Map<String, Object>>) map.get("options");
                            if (options != null) {
                                for (Map<String, Object> optMap : options) {
                                    backend.entity.QuestionOption opt = new backend.entity.QuestionOption();
                                    opt.setQuestion(q);
                                    opt.setOptionContent((String) optMap.get("content"));
                                    opt.setIsCorrect((Boolean) optMap.get("isCorrect"));
                                    q.getOptions().add(opt);
                                }
                            }
                            questionsToSave.add(q);
                        }
                    } catch (Exception e) {
                        log.error("Error reading JSON from {}", jsonResource, e);
                    }
                }
                questionRepository.saveAll(questionsToSave);
                totalQuestionsCreated += questionsToSave.size();
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", String.format("Đã sinh thành công %d câu hỏi từ CSDL Đề Thi PDF thực tế.", totalQuestionsCreated)
        ));
    }

    @PostMapping("/fix-old-data")
    public ResponseEntity<?> fixOldData(Authentication authentication) {
        String email = authentication.getName();
        Integer studentId = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        List<QuizAttempt> userAttempts = quizAttemptRepository.findByStudentIdOrderBySubmittedAtDesc(studentId);
        int deletedAttempts = 0;
        int deletedAnswers = 0;
        for (QuizAttempt attempt : userAttempts) {
            List<PracticeAnswer> answers = practiceAnswerRepository.findByAttemptIdWithQuestions(attempt.getAttemptId());
            deletedAnswers += answers.size();
            practiceAnswerRepository.deleteAll(answers);
            quizAttemptRepository.delete(attempt);
            deletedAttempts++;
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", String.format("Đã xóa %d bài làm cũ và %d câu trả lời lỗi của bạn.", deletedAttempts, deletedAnswers)
        ));
    }
}
