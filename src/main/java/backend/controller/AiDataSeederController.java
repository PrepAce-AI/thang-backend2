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
        // Nếu đã có câu hỏi mẫu cũ, không xóa (để tránh lỗi khóa ngoại FK) mà sẽ UPDATE trực tiếp nội dung
        boolean hasOldDummy = false;
        List<Question> oldDummy = questionRepository.findAll().stream()
                .filter(q -> q.getQuestionContent() != null && q.getQuestionContent().contains("Câu hỏi mẫu số"))
                .toList();
        
        if (!oldDummy.isEmpty()) {
            hasOldDummy = true;
        }

        List<Quiz> quizzes = quizRepository.findAll();
        if (quizzes.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Database không có dữ liệu Quiz để tạo câu hỏi."
            ));
        }

        Random rand = new Random();
        int totalQuestionsCreated = 0;

        for (Quiz quiz : quizzes) {
            if ("ENTRY_TEST".equals(quiz.getQuizType()) || "PRACTICE".equals(quiz.getQuizType()) || "MOCK_EXAM".equals(quiz.getQuizType())) {
                String subject = quiz.getSubject() != null ? quiz.getSubject().trim().toLowerCase() : "toán";
                String subName = "Toán";
                
                if (subject.contains("anh") || subject.contains("english")) {
                    subName = "Tiếng Anh";
                } else if (subject.contains("lý") || subject.contains("physics")) {
                    subName = "Vật Lý";
                } else if (subject.contains("hóa") || subject.contains("chem")) {
                    subName = "Hóa Học";
                }

                List<Question> questionsToSave = new ArrayList<>();
                Set<String> usedContents = new HashSet<>();
                
                // Lọc ra các câu hỏi mẫu của Quiz hiện tại (nếu có)
                List<Question> existingDummiesForQuiz = oldDummy.stream()
                        .filter(q -> q.getQuiz().getQuizId().equals(quiz.getQuizId()))
                        .toList();

                int limit = existingDummiesForQuiz.isEmpty() ? 250 : existingDummiesForQuiz.size();

                for (int i = 0; i < limit; i++) {
                    Question q;
                    if (!existingDummiesForQuiz.isEmpty()) {
                        q = existingDummiesForQuiz.get(i);
                    } else {
                        q = new Question();
                        q.setQuiz(quiz);
                    }
                    
                    q.setSubject(subName); // Save formatted subject name
                    q.setDifficulty(rand.nextInt(3) + 1); // 1-3
                    q.setQuestionType("CHOICE");

                    // Sinh dữ liệu động và đảm bảo không trùng lặp
                    String[] qData;
                    int attempts = 0;
                    do {
                        qData = generateProceduralQuestion(subName, rand);
                        attempts++;
                        // Tránh infinite loop nếu hết hoán vị (dù xác suất thấp)
                        if (attempts > 50) {
                            qData[1] += " (Mã: #" + rand.nextInt(99999) + ")";
                        }
                    } while (usedContents.contains(qData[1]) && attempts < 100);
                    usedContents.add(qData[1]);
                    
                    q.setTopic(qData[0]);
                    q.setQuestionContent(qData[1]);
                    q.setExplanation("Lời giải chi tiết: Câu hỏi thuộc chuyên đề " + qData[0] + " môn " + subName);

                    // Trộn đáp án
                    int assignedCorrectIndex = rand.nextInt(4);
                    int wrongIdx = 1;
                    
                    for (int j = 0; j < 4; j++) {
                        backend.entity.QuestionOption opt;
                        if (!existingDummiesForQuiz.isEmpty() && q.getOptions().size() > j) {
                            opt = q.getOptions().get(j);
                        } else {
                            opt = new backend.entity.QuestionOption();
                            opt.setQuestion(q);
                            q.getOptions().add(opt);
                        }
                        
                        if (j == assignedCorrectIndex) {
                            opt.setOptionContent(qData[2]); // Đáp án đúng luôn ở vị trí số 2 trong mảng qData
                            opt.setIsCorrect(true);
                        } else {
                            opt.setOptionContent(qData[2 + wrongIdx]);
                            opt.setIsCorrect(false);
                            wrongIdx++;
                        }
                    }
                    questionsToSave.add(q);
                }
                questionRepository.saveAll(questionsToSave);
                totalQuestionsCreated += limit;
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", String.format("Đã sinh thành công %d câu hỏi ngẫu nhiên bằng thuật toán.", totalQuestionsCreated)
        ));
    }

    private String[] generateProceduralQuestion(String subject, Random rand) {
        if ("Toán".equals(subject)) {
            int type = rand.nextInt(15); 
            int a = rand.nextInt(90) + 2; // Từ 2 đến 91
            int b = rand.nextInt(90) + 2;
            if (type == 0) return new String[]{"Đại số", "Giải phương trình log_" + a + "(x-" + b + ") = 2. Nghiệm x là?", "x = " + (a * a + b), "x = " + (a + b), "x = " + (a * a - b), "x = " + (a * a)};
            else if (type == 1) return new String[]{"Giải tích", "Tính đạo hàm của hàm số y = " + a + "x^3 - " + b + "x.", "y' = " + (3 * a) + "x^2 - " + b, "y' = " + (a) + "x^2 - " + b, "y' = " + (3 * a) + "x^2 + " + b, "y' = " + (a) + "x - " + b};
            else if (type == 2) return new String[]{"Hình học", "Thể tích khối chóp S.ABCD có đáy là hình vuông cạnh " + a + "a, chiều cao h = " + b + "a là?", "V = " + (a * a * b) + "a^3 / 3", "V = " + (a * a * b) + "a^3", "V = " + (a * b) + "a^3 / 3", "V = " + (a * a) + "a^3 / 3"};
            else if (type == 3) return new String[]{"Tích phân", "Tính nguyên hàm của f(x) = " + (2 * a) + "x + " + b + ".", "F(x) = " + a + "x^2 + " + b + "x + C", "F(x) = " + (2 * a) + "x^2 + " + b + "x + C", "F(x) = " + a + "x^2 + C", "F(x) = " + b + "x + C"};
            else if (type == 4) return new String[]{"Xác suất", "Gieo một con xúc xắc cân đối " + a + " lần. Xác suất để không có mặt 6 xuất hiện là?", "(5/6)^" + a, "(1/6)^" + a, "1 - (5/6)^" + a, "5/6"};
            else if (type == 5) return new String[]{"Lượng giác", "Tập nghiệm của phương trình sin(x) = " + (a%2==0?"1/2":"√3/2") + " là?", (a%2==0?"x = π/6 + k2π hoặc x = 5π/6 + k2π":"x = π/3 + k2π hoặc x = 2π/3 + k2π"), "x = π/4 + k2π", "x = ±π/3 + k2π", "x = kπ"};
            else if (type == 6) return new String[]{"Số phức", "Tìm môđun của số phức z = " + a + " + " + b + "i.", "|z| = √" + (a*a + b*b), "|z| = " + (a+b), "|z| = " + (a*a + b*b), "|z| = " + a};
            else if (type == 7) return new String[]{"Hình học Oxyz", "Trong không gian Oxyz, mặt cầu tâm I(1; -2; " + a + ") bán kính R = " + b + " có phương trình là:", "(x - 1)^2 + (y + 2)^2 + (z - " + a + ")^2 = " + (b*b), "(x + 1)^2 + (y - 2)^2 + (z + " + a + ")^2 = " + (b*b), "(x - 1)^2 + (y + 2)^2 + (z - " + a + ")^2 = " + b, "(x + 1)^2 + (y - 2)^2 + (z - " + a + ")^2 = " + (b*b)};
            else if (type == 8) return new String[]{"Đại số", "Cấp số cộng (u_n) có u_1 = " + a + ", công sai d = " + b + ". Số hạng u_3 là?", "" + (a + 2*b), "" + (a + 3*b), "" + (a + b), "" + (a - 2*b)};
            else if (type == 9) return new String[]{"Giải tích", "Giá trị lớn nhất của hàm số y = -x^2 + " + (2*a) + "x + " + b + " trên đoạn [-10, 10] là?", "" + (a*a + b), "" + (b), "" + (a*a - b), "" + (2*a + b)};
            else if (type == 10) return new String[]{"Tích phân", "Tích phân từ 0 đến 1 của hàm số f(x) = e^x là?", "e - 1", "e", "1 - e", "e + 1"};
            else if (type == 11) return new String[]{"Hình học Oxyz", "Vectơ pháp tuyến của mặt phẳng (P): " + a + "x - " + b + "y + z - 1 = 0 là?", "n = (" + a + "; -" + b + "; 1)", "n = (" + a + "; " + b + "; 1)", "n = (" + a + "; -" + b + "; -1)", "n = (-" + a + "; " + b + "; 1)"};
            else if (type == 12) return new String[]{"Số phức", "Số phức liên hợp của z = " + a + " - " + b + "i là?", "" + a + " + " + b + "i", "-" + a + " - " + b + "i", "" + a + " - " + b + "i", "-" + a + " + " + b + "i"};
            else if (type == 13) return new String[]{"Lượng giác", "Chu kỳ của hàm số y = cos(" + a + "x) là?", "2π/" + a, "π/" + a, "2π", "π"};
            else return new String[]{"Tổ hợp", "Có bao nhiêu cách chọn 2 học sinh từ " + (a + 10) + " học sinh?", "C(2, " + (a+10) + ")", "A(2, " + (a+10) + ")", "P(2)", "2!"};
        } else if ("Tiếng Anh".equals(subject)) {
            int type = rand.nextInt(10);
            String[] verbs = {"play", "work", "study", "run", "eat", "design", "build"};
            String v = verbs[rand.nextInt(verbs.length)];
            if (type == 0) return new String[]{"Ngữ pháp", "If I _______ you, I would " + v + " harder. (Question #" + rand.nextInt(1000) + ")", "were", "am", "was", "will be"};
            else if (type == 1) return new String[]{"Từ vựng", "She usually _______ up at 6 AM every day to " + v + ".", "wakes", "wake", "waking", "is waking"};
            else if (type == 2) return new String[]{"Đọc hiểu", "Choose the correct sentence: (Variant " + rand.nextInt(1000) + ")", "He is so short that he cannot " + v + ".", "He is not enough tall to " + v + ".", "He is very short to " + v + ".", "Such short is he to " + v + "."};
            else if (type == 3) return new String[]{"Giao tiếp", "- 'How beautiful is this?' - 'It is _______.'", "very beautiful", "too much beautiful", "so beautiful", "such beautiful"};
            else if (type == 4) return new String[]{"Phát âm", "Choose the word whose underlined part is pronounced differently. (Set #" + rand.nextInt(100) + ")", "played", "worked", "stopped", "missed"};
            else if (type == 5) return new String[]{"Ngữ pháp", "He asked me what _______ doing at that time.", "I was", "was I", "I am", "am I"};
            else if (type == 6) return new String[]{"Từ vựng", "The government has _______ new measures to combat inflation.", "introduced", "done", "made", "taken"};
            else if (type == 7) return new String[]{"Trọng âm", "Choose the word with a different stress pattern.", "develop", "understand", "engineer", "volunteer"};
            else if (type == 8) return new String[]{"Câu điều kiện", "Unless you _______ hard, you will fail the exam.", "work", "don't work", "working", "worked"};
            else return new String[]{"Mệnh đề quan hệ", "The man _______ lives next door is a doctor.", "who", "which", "whom", "whose"};
        } else if ("Vật Lý".equals(subject)) {
            int type = rand.nextInt(8);
            int a = rand.nextInt(10) + 1;
            int b = rand.nextInt(10) + 1;
            if (type == 0) return new String[]{"Cơ học", "Một vật dao động với phương trình x = " + a + "cos(" + b + "πt). Biên độ dao động là?", "A = " + a + " cm", "A = " + b + " cm", "A = " + (a * 2) + " cm", "A = " + (a * b) + " cm"};
            else if (type == 1) return new String[]{"Điện học", "Cho dòng điện I = " + a + "A qua điện trở R = " + b + "Ω. Công suất tỏa nhiệt là?", "P = " + (a * a * b) + " W", "P = " + (a * b) + " W", "P = " + (a * a / b) + " W", "P = " + (a * b * b) + " W"};
            else if (type == 2) return new String[]{"Quang học", "Chiết suất của môi trường là " + (a * 0.1 + 1) + ". Tốc độ ánh sáng trong môi trường này là?", "v = 3.10^8 / " + (a * 0.1 + 1) + " m/s", "v = 3.10^8 m/s", "v = " + (a * 0.1 + 1) + " m/s", "v = 3.10^8 * " + (a * 0.1 + 1) + " m/s"};
            else if (type == 3) return new String[]{"Nhiệt học", "Khí lý tưởng ở T1 = 300K, V1 = " + a + " lít. Tăng lên T2 = 600K đẳng áp, V2 là?", "V2 = " + (a * 2) + " lít", "V2 = " + (a) + " lít", "V2 = " + (a / 2) + " lít", "V2 = " + (a * 4) + " lít"};
            else if (type == 4) return new String[]{"Sóng cơ học", "Một sóng cơ có tần số f = " + a + " Hz, bước sóng λ = " + b + " m. Tốc độ truyền sóng là?", "v = " + (a * b) + " m/s", "v = " + (a + b) + " m/s", "v = " + (a / (double)b) + " m/s", "v = " + (b / (double)a) + " m/s"};
            else if (type == 5) return new String[]{"Lượng tử ánh sáng", "Năng lượng của phôtôn ánh sáng có tần số f = " + a + ".10^14 Hz là? (h=6.625e-34)", "E = " + (6.625 * a) + ".10^-20 J", "E = " + (a) + ".10^-19 J", "E = 6.625.10^-34 J", "E = 3.10^8 J"};
            else if (type == 6) return new String[]{"Hạt nhân nguyên tử", "Hạt nhân ^" + (a+10) + "_" + a + "X có số nơtron là?", "" + 10, "" + a, "" + (a+10), "" + (2*a+10)};
            else return new String[]{"Điện xoay chiều", "Điện áp u = " + (a*100) + "cos(100πt). Giá trị hiệu dụng là?", "" + (a*100/1.414) + " V", "" + (a*100) + " V", "" + (a*50) + " V", "" + (a*200) + " V"};
        } else {
            int type = rand.nextInt(8);
            int a = rand.nextInt(10) + 1;
            if (type == 0) return new String[]{"Hóa vô cơ", "Hòa tan " + (a * 0.1) + " mol Fe vào HCl dư thu được bao nhiêu lít H2 (đktc)?", (a * 0.1 * 22.4) + " lít", (a * 0.1 * 11.2) + " lít", (a * 0.2 * 22.4) + " lít", (a * 0.1 * 2) + " lít"};
            else if (type == 1) return new String[]{"Hóa hữu cơ", "Đốt cháy hoàn toàn " + a + " mol CH4 thu được bao nhiêu mol CO2?", a + " mol", (a * 2) + " mol", (a / 2) + " mol", "1 mol"};
            else if (type == 2) return new String[]{"Nhận biết", "Chất nào sau đây dùng để nhận biết ion Ba2+?", "H2SO4", "HCl", "NaOH", "NaCl"};
            else if (type == 3) return new String[]{"Polime", "Tơ nilon-6,6 được điều chế bằng phản ứng nào?", "Trùng ngưng", "Trùng hợp", "Thủy phân", "Oxi hóa khử"};
            else if (type == 4) return new String[]{"Hóa học vô cơ", "Kim loại nào sau đây có tính khử mạnh nhất?", "K", "Na", "Mg", "Al"};
            else if (type == 5) return new String[]{"Este - Lipit", "Este X có công thức phân tử C" + (a+2) + "H" + ((a+2)*2) + "O2. X thuộc loại este nào?", "Este no, đơn chức, mạch hở", "Este không no", "Este đa chức", "Este vòng"};
            else if (type == 6) return new String[]{"Cacbohiđrat", "Chất nào sau đây thuộc loại đisaccarit?", "Saccarozơ", "Glucozơ", "Tinh bột", "Xenlulozơ"};
            else return new String[]{"Amin - Amino axit", "Dung dịch chất nào sau đây làm quỳ tím chuyển sang màu xanh?", "Metylamin", "Anilin", "Glyxin", "Axit glutamic"};
        }
    }
    
    @PostMapping("/fix-old-data")
    public ResponseEntity<?> fixOldData(Authentication authentication) {
        String email = authentication.getName();
        Integer studentId = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        // 1. Sửa câu hỏi rác (Thiếu môn học/chuyên đề)
        List<Question> badQuestions = questionRepository.findAll().stream()
                .filter(q -> q.getSubject() == null || q.getSubject().isBlank() || q.getTopic() == null || q.getTopic().isBlank())
                .toList();

        Random rand = new Random();
        String[] subjects = {"Toán", "Vật Lý", "Hóa Học", "Tiếng Anh"};
        String[][] topics = {
                {"Đại số", "Giải tích", "Hình học", "Tích phân"}, // Toán
                {"Cơ học", "Nhiệt học", "Điện học", "Quang học"}, // Lý
                {"Hóa vô cơ", "Hóa hữu cơ", "Hóa phân tích", "Hóa lý"}, // Hóa
                {"Ngữ pháp", "Từ vựng", "Đọc hiểu", "Viết"} // Anh
        };

        for (Question q : badQuestions) {
            int subIdx = rand.nextInt(subjects.length);
            q.setSubject(subjects[subIdx]);
            q.setTopic(topics[subIdx][rand.nextInt(topics[subIdx].length)]);
        }
        questionRepository.saveAll(badQuestions);

        // 2. Xóa các lượt thi (QuizAttempts) và PracticeAnswers của user hiện tại
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
                "message", String.format("Đã vá %d câu hỏi rác. Đã xóa %d bài làm cũ và %d câu trả lời lỗi của bạn. Hãy seed lại data để biểu đồ đẹp hơn!", badQuestions.size(), deletedAttempts, deletedAnswers)
        ));
    }
}
