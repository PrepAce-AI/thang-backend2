package backend.service;

import backend.entity.Question;
import backend.entity.QuestionOption;
import backend.entity.Quiz;
import backend.repository.CourseRepository;
import backend.repository.QuestionOptionRepository;
import backend.repository.QuestionRepository;
import backend.repository.QuizRepository;
import backend.repository.WordImportQuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * =====================================================================
 * WordImportService v5 – Bóc tách Đề thi cục bộ bằng Regex + Pandoc (Đã Fix Đáp Án)
 * =====================================================================
 */
@Service
@RequiredArgsConstructor

public class WordImportService {

    private final QuizRepository quizRepository;
    private final WordImportQuizRepository wordImportQuizRepository;
    private final CourseRepository courseRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final String BACKEND_BASE_URL = "http://localhost:8080";
    private static final String MEDIA_BASE_DIR = "word-media";
    private static final List<String> PANDOC_CANDIDATES = buildPandocCandidates();

    private static List<String> buildPandocCandidates() {
        List<String> candidates = new ArrayList<>();
        String envPath = System.getenv("PANDOC_PATH");
        if (envPath != null && !envPath.isBlank()) candidates.add(envPath.trim());
        candidates.add("pandoc");
        String userHome = System.getProperty("user.home"); 
        if (userHome != null) {
            candidates.add(userHome.replace("\\", "/") + "/AppData/Local/Pandoc/pandoc.exe");
        }
        candidates.add("C:/Program Files/Pandoc/pandoc.exe");
        candidates.add("C:/Program Files (x86)/Pandoc/pandoc.exe");
        return Collections.unmodifiableList(candidates);
    }

    // ─── Các mẫu Regex bóc tách cấu trúc ─────────────────────────────────────────
    private static final Pattern QUESTION_HEADER =
            Pattern.compile("^\\*{0,2}\\s*(C[âa]u|Question|Bài)\\s*(\\d+)\\s*\\*{0,2}\\s*[.:)]\\s*", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern OPTION_LINE = Pattern.compile("^\\W*([A-D])[.)\\s]\\s*(.*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern OPTION_TF = Pattern.compile("^\\W*([a-d])[.)\\s]\\s*(.*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECTION_HEADER = Pattern.compile("P\\s*H\\s*[^\\sN]{0,2}\\s*N\\s*(I{1,3}|IV|V|VI)(?![a-zA-Z])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ANSWER_SECTION = Pattern.compile("(đáp\\s*án|d[aá]p\\s*[aá]n|answer\\s*key|bảng\\s*đáp\\s*án)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public static class ImportResult {
        public Integer quizId;
        public String quizTitle;
        public int totalQuestions;
        public int multipleChoiceCount;
        public int trueFalseCount;
        public int shortAnswerCount;
        public List<ParsedQuestion> previewQuestions = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
    }

    public static class ParsedQuestion {
        public int number;
        public int origNum;
        public String type;
        public String content;
        public String correctAnswer;
        public List<OptionItem> options = new ArrayList<>();

        public ParsedQuestion() {}

        public static class OptionItem {
            public String label;
            public String content;
            public boolean isCorrect;

            public OptionItem() {}

            public OptionItem(String label, String content, boolean isCorrect) {
                this.label = label;
                this.content = content;
                this.isCorrect = isCorrect;
            }
        }
    }

    @Transactional
    public ImportResult importFromWord(MultipartFile file, String quizTitle, String subject, int durationMins, Integer courseId) throws IOException {
        List<ParsedQuestion> questions = parseWordFile(file);
        if (questions.isEmpty()) throw new IllegalArgumentException("Không phân tích được câu hỏi nào từ file Word.");

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO Quizzes (quiz_title, quiz_type, subject, duration_minutes, is_entry_test, course_id, created_at) VALUES (?, 'PRACTICE', ?, ?, 0, ?, GETDATE())",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, quizTitle);
            ps.setString(2, subject);
            ps.setInt(3, durationMins);
            if (courseId != null) ps.setInt(4, courseId); else ps.setNull(4, java.sql.Types.INTEGER);
            return ps;
        }, keyHolder);

        Integer newQuizId = keyHolder.getKey().intValue();

        Quiz savedQuiz = new Quiz();
        savedQuiz.setQuizId(newQuizId);

        for (ParsedQuestion pq : questions) {
            Question q = new Question();
            q.setQuiz(savedQuiz);
            q.setQuestionContent(pq.content);
            q.setQuestionType(pq.type);
            q.setCorrectAnswer(pq.correctAnswer);
            q.setCreatedAt(new Date());
            Question savedQuestion = questionRepository.save(q);

            if (pq.options != null && !pq.options.isEmpty()) {
                List<QuestionOption> opts = new ArrayList<>();
                for (ParsedQuestion.OptionItem oi : pq.options) {
                    QuestionOption opt = new QuestionOption();
                    opt.setQuestion(savedQuestion);

                    String optionContent = oi.label + ". " + oi.content;

                    System.out.println("==========================");
                    System.out.println(optionContent.length());
                    System.out.println(optionContent);

                    opt.setOptionContent(oi.label + ". " + oi.content);
                    opt.setIsCorrect(oi.isCorrect);
                    opts.add(opt);
                }
                questionOptionRepository.saveAll(opts);
            }
        }

        ImportResult result = buildResult(questions);
        result.quizId = newQuizId;
        result.quizTitle = quizTitle; // Khắc phục lỗi rỗng tiêu đề khi dùng Mock Object
        return result;
    }

    public ImportResult previewFromWord(MultipartFile file) throws IOException {
        List<ParsedQuestion> questions = parseWordFile(file);
        ImportResult result = buildResult(questions);
        result.quizId = -1;
        return result;
    }

    private List<ParsedQuestion> parseWordFile(MultipartFile file) throws IOException {
        byte[] fileBytes = file.getBytes();
        String sessionId = md5Hash(fileBytes);
        Path workDir = Paths.get(MEDIA_BASE_DIR, sessionId);
        Path tempDocx = workDir.resolve("input.docx");
        Path outputMd  = workDir.resolve("output.md");
        Path mediaDir  = workDir.resolve("media");

        boolean alreadyExtracted = Files.exists(mediaDir) && Files.list(mediaDir).findAny().isPresent();

        if (!alreadyExtracted || !Files.exists(outputMd)) {
            Files.createDirectories(workDir);
            Files.write(tempDocx, fileBytes);
            runPandoc(findPandoc(), tempDocx, outputMd, mediaDir, sessionId);
            try { Files.deleteIfExists(tempDocx); } catch (Exception ignored) {}
        }

        String markdown = Files.readString(outputMd, StandardCharsets.UTF_8);
        markdown = java.text.Normalizer.normalize(markdown, java.text.Normalizer.Form.NFC);

        List<ParsedQuestion> result = parseMarkdownLocally(markdown);

        result.removeIf(q -> q.content == null || q.content.length() < 3);
        cleanupOldSessions(10);
        return result;
    }

    // ─── THUẬT TOÁN PARSE REGEX NỘI BỘ NÂNG CẤP ĐỒNG HÀNG PHƯƠNG ÁN ──────────────────
    private List<ParsedQuestion> parseMarkdownLocally(String markdown) {
        List<String> lines = Arrays.stream(markdown.split("\\r?\\n"))
                .map(String::stripTrailing)
                .collect(Collectors.toList());

        List<ParsedQuestion> result = new ArrayList<>();
        ParsedQuestion current = null;
        StringBuilder contentBuilder = new StringBuilder();

        String currentSectionType = "MULTIPLE_CHOICE";
        String currentAnswerSectionType = "MULTIPLE_CHOICE";
        int globalCounter = 0;

        Map<String, Map<Integer, Object>> tableAnswers = new HashMap<>();
        tableAnswers.put("MULTIPLE_CHOICE", new HashMap<>());
        tableAnswers.put("SHORT_ANSWER", new HashMap<>());
        tableAnswers.put("TRUE_FALSE", new HashMap<>());
        List<Integer> tableHeaders = new ArrayList<>();

        boolean inAnswerSection = false;
        boolean inExplanationSection = false;

        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            String plain = raw.replaceAll("^>\\s*", "").trim();
            String stripped = stripMarkdown(plain);

            if (plain.isEmpty()) continue;

            if (plain.contains("Hết") || plain.contains("---") || plain.contains("|-|")) {
                continue;
            }

            String lowerPlain = plain.toLowerCase();
            if (lowerPlain.contains("thí sinh trả lời") || lowerPlain.contains("thí sinh chỉ chọn") || lowerPlain.contains("mỗi câu hỏi thí sinh")) {
                continue;
            }

            if (Pattern.compile("(lời\\s*giải\\s*chi\\s*tiết|hướng\\s*dẫn\\s*giải)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(stripped).find()) {
                inExplanationSection = true;
                if (current != null && !inAnswerSection) {
                    finalizeQuestion(current, contentBuilder.toString().trim());
                    if (!result.contains(current)) result.add(current);
                    current = null;
                    contentBuilder = new StringBuilder();
                }
                continue;
            }

            if (!inAnswerSection && ANSWER_SECTION.matcher(stripped).find()) {
                inAnswerSection = true;
                if (current != null) {
                    finalizeQuestion(current, contentBuilder.toString().trim());
                    if (!result.contains(current)) result.add(current);
                    current = null;
                    contentBuilder = new StringBuilder();
                }
            }

            if (inAnswerSection) {
                String cleanPlain = plain.replaceAll("\\*", "").replaceAll("_", "").trim();
                String upperClean = cleanPlain.toUpperCase();
                if (upperClean.contains("PHẦN I") || upperClean.contains("PHẦN II") || upperClean.contains("PHẦN III")) {
                    if (upperClean.contains("PHẦN I.")) currentAnswerSectionType = "MULTIPLE_CHOICE";
                    if (upperClean.contains("PHẦN II.")) currentAnswerSectionType = "TRUE_FALSE";
                    if (upperClean.contains("PHẦN III.")) currentAnswerSectionType = "SHORT_ANSWER";
                    continue;
                }

                String[] tokens = cleanPlain.split("\\s+");
                if (tokens.length == 0 || tokens[0].isEmpty()) continue;

                if (cleanPlain.startsWith("Câu")) {
                    tableHeaders.clear();
                    for (String token : tokens) {
                        try { tableHeaders.add(Integer.parseInt(token.replaceAll("[^0-9]", ""))); } catch(Exception ignored) {}
                    }
                    continue;
                }

                if ("MULTIPLE_CHOICE".equals(currentAnswerSectionType) && cleanPlain.startsWith("Chọn")) {
                    for (int j = 1; j < tokens.length; j++) {
                        int colIdx = j - 1;
                        if (colIdx < tableHeaders.size()) {
                            tableAnswers.get("MULTIPLE_CHOICE").put(tableHeaders.get(colIdx), tokens[j].toUpperCase().replaceAll("[^A-D]", ""));
                        }
                    }
                }

                if ("SHORT_ANSWER".equals(currentAnswerSectionType) && cleanPlain.toLowerCase().startsWith("đáp án")) {
                    for (int j = 2; j < tokens.length; j++) {
                        int colIdx = j - 2;
                        if (colIdx < tableHeaders.size()) {
                            tableAnswers.get("SHORT_ANSWER").put(tableHeaders.get(colIdx), tokens[j]);
                        }
                    }
                }

                if ("TRUE_FALSE".equals(currentAnswerSectionType) && cleanPlain.matches("^[a-d][\\).\\s].*")) {
                    List<Boolean> rowAnswers = new ArrayList<>();
                    for (String token : tokens) {
                        if (token.equalsIgnoreCase("Đ") || token.equalsIgnoreCase("Đúng")) rowAnswers.add(true);
                        else if (token.equalsIgnoreCase("S") || token.equalsIgnoreCase("Sai")) rowAnswers.add(false);
                    }
                    String optLabel = tokens[0].replaceAll("[^a-d]", "").toLowerCase();
                    for (int j = 0; j < rowAnswers.size() && j < tableHeaders.size(); j++) {
                        int qNum = tableHeaders.get(j);
                        Map<Integer, Object> tfMap = tableAnswers.get("TRUE_FALSE");
                        tfMap.putIfAbsent(qNum, new HashMap<String, Boolean>());
                        @SuppressWarnings("unchecked")
                        Map<String, Boolean> qTfMap = (Map<String, Boolean>) tfMap.get(qNum);
                        qTfMap.put(optLabel, rowAnswers.get(j));
                    }
                }
                continue;
            }

            Matcher sectionMatch = SECTION_HEADER.matcher(stripped);
            if (sectionMatch.find()) {
                String roman = sectionMatch.group(1).toUpperCase();
                currentSectionType = switch (roman) {
                    case "I"   -> "MULTIPLE_CHOICE";
                    case "II"  -> "TRUE_FALSE";
                    case "III" -> "SHORT_ANSWER";
                    default    -> currentSectionType;
                };
                continue;
            }

            // Nhận diện dòng bắt đầu Câu hỏi mới
            Matcher qMatch = QUESTION_HEADER.matcher(plain);
            if (qMatch.find()) {
                int origNum = Integer.parseInt(qMatch.group(2));

                if (!inExplanationSection) {
                    if (current != null) {
                        finalizeQuestion(current, contentBuilder.toString().trim());
                        if (!result.contains(current)) result.add(current);
                    }
                    globalCounter++;
                    current = new ParsedQuestion();
                    current.number = globalCounter;
                    current.origNum = origNum;

                    // NÂNG CẤP: Tự động điều chỉnh Type nếu gặp câu hỏi đặc biệt
                    if (plain.toLowerCase().contains("số cách điền") || plain.toLowerCase().contains("giá trị của")) {
                        current.type = "SHORT_ANSWER";
                    } else {
                        current.type = currentSectionType;
                    }

                    contentBuilder = new StringBuilder();

                    // SỬA: Gọt sạch dấu ** thừa ở đầu câu hỏi
                    String afterHeader = plain.substring(qMatch.end()).trim().replaceAll("^\\*\\*", "").replaceAll("\\*\\*$", "");
                    if (!afterHeader.isEmpty()) {
                        contentBuilder.append(convertImagesToHtml(afterHeader));
                    }
                } else {
                    final String sType = currentSectionType;
                    current = result.stream()
                            .filter(q -> q.origNum == origNum && sType.equals(q.type))
                            .findFirst()
                            .orElse(null);
                }
                continue;
            }
            if (plain.contains("số cách điền") || plain.contains("giá trị của") || plain.contains("tính")) {
                currentSectionType = "SHORT_ANSWER";
                current.type = "SHORT_ANSWER";
            }

            if (current == null) continue;

            boolean isBlockquote = raw.startsWith(">");
            if (isBlockquote || current.options.size() > 0 || looksLikeInlineMCOptions(plain) || looksLikeInlineTfOptions(plain)) {
                // FIX LỖI 2: Cho phép lưu phương án trực tiếp vào Object thay vì bỏ qua luồng dữ liệu
                if (looksLikeInlineMCOptions(plain)) {
                    parseInlineMCOptions(plain, current);
                    continue;
                }
                if (looksLikeInlineTfOptions(plain)) {
                    parseInlineTfOptions(plain, current);
                    continue;
                }

                Matcher optM = OPTION_LINE.matcher(plain);
                if (optM.matches() && ("MULTIPLE_CHOICE".equals(current.type))) {
                    String label = optM.group(1).toUpperCase();
                    String content = convertImagesToHtml(optM.group(2).trim());
                    if (content.isEmpty()) content = "(xem hình trong đề)";

                    content = content.replaceAll("^\\s*\\*\\*\\s*", "").replaceAll("\\*\\*\\s*$", "").trim();

                    if (current.options.stream().noneMatch(o -> o.label.equalsIgnoreCase(label))) {
                        current.options.add(new ParsedQuestion.OptionItem(label, content, false));
                    }
                    continue;
                }

                Matcher tfM = OPTION_TF.matcher(plain);
                if (tfM.matches() && "TRUE_FALSE".equals(current.type)) {
                    String label = tfM.group(1).toLowerCase();
                    String content = convertImagesToHtml(tfM.group(2).trim());
                    if (current.options.stream().noneMatch(o -> o.label.equalsIgnoreCase(label))) {
                        current.options.add(new ParsedQuestion.OptionItem(label, content, false));
                    }
                    continue;
                }
            }

            if (current.options.isEmpty()) {
                String htmlContent = convertImagesToHtml(plain);
                if (contentBuilder.length() > 0) contentBuilder.append(" ");
                contentBuilder.append(htmlContent);
            }
        }

        if (current != null) {
            finalizeQuestion(current, contentBuilder.toString().trim());
            result.add(current);
        }

        for (ParsedQuestion q : result) {
            Map<Integer, Object> sectionAns = tableAnswers.get(q.type);
            Integer lookupKey = (sectionAns != null && sectionAns.containsKey(q.origNum)) ? q.origNum : q.number;
            if (sectionAns != null && sectionAns.containsKey(lookupKey)) {
                if ("MULTIPLE_CHOICE".equals(q.type)) {
                    String ans = (String) sectionAns.get(lookupKey);
                    q.correctAnswer = ans;
                    for (ParsedQuestion.OptionItem opt : q.options) {
                        opt.isCorrect = opt.label.equalsIgnoreCase(ans);
                    }
                } else if ("SHORT_ANSWER".equals(q.type)) {
                    q.correctAnswer = (String) sectionAns.get(lookupKey);
                } else if ("TRUE_FALSE".equals(q.type)) {
                    @SuppressWarnings("unchecked")
                    Map<String, Boolean> ansMap = (Map<String, Boolean>) sectionAns.get(lookupKey);
                    if (ansMap != null) {
                        for (ParsedQuestion.OptionItem opt : q.options) {
                            if (ansMap.containsKey(opt.label.toLowerCase())) {
                                opt.isCorrect = (boolean) ansMap.get(opt.label.toLowerCase());
                            }
                        }
                        StringBuilder sb = new StringBuilder();
                        for (ParsedQuestion.OptionItem opt : q.options) {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append(opt.label.toLowerCase()).append(":")
                              .append(opt.isCorrect ? "Đ" : "S");
                        }
                        q.correctAnswer = sb.toString();
                    }
                }
            }
        }
        return result;
    }

    private void finalizeQuestion(ParsedQuestion q, String content) {
        if (content == null || content.isBlank()) {
            q.content = "(Câu " + q.number + " — xem nội dung trong file gốc)";
        } else {
            q.content = content.trim().replaceAll("\\*\\*", "");
        }
        if (q.type == null) {
            if (q.options.size() >= 2) {
                boolean hasMC = q.options.stream().anyMatch(o -> o.label.matches("[A-D]"));
                q.type = hasMC ? "MULTIPLE_CHOICE" : "TRUE_FALSE";
            } else {
                q.type = "SHORT_ANSWER";
            }
        }
    }

    private String convertImagesToHtml(String text) {
        if (text == null || !text.contains("![")) return text;
        Pattern imgPat = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)(?:\\{([^}]*)\\})?");
        Matcher m = imgPat.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String altText = m.group(1);
            String rawPath = m.group(2).trim();
            String attrs = m.group(3);
            String imgUrl = pathToUrl(rawPath);
            String widthStyle = "auto", heightStyle = "auto";

            if (attrs != null) {
                try {
                    Matcher wMatch = Pattern.compile("width=\"([0-9.]+)in\"").matcher(attrs);
                    if (wMatch.find()) widthStyle = String.format("%.2fem", Double.parseDouble(wMatch.group(1)) * 6.0);
                    Matcher hMatch = Pattern.compile("height=\"([0-9.]+)in\"").matcher(attrs);
                    if (hMatch.find()) heightStyle = String.format("%.2fem", Double.parseDouble(hMatch.group(1)) * 6.0);
                } catch (Exception ignored) {}
            }
            String customStyle = "vertical-align:middle; display:inline-block; max-width:100%; ";
            if (!"auto".equals(widthStyle)) customStyle += "width:" + widthStyle + "; ";
            if (!"auto".equals(heightStyle)) customStyle += "height:" + heightStyle + "; ";

            String htmlImg = "<img src=\"" + imgUrl + "\" alt=\"" + altText.replace("\"", "") + "\" style=\"" + customStyle + "\">";
            m.appendReplacement(sb, Matcher.quoteReplacement(htmlImg));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String pathToUrl(String rawPath) {
        String normalized = rawPath.replace("\\", "/");
        int idx = normalized.indexOf("word-media/");
        if (idx >= 0) return BACKEND_BASE_URL + "/" + normalized.substring(idx);
        return BACKEND_BASE_URL + "/word-media/unknown/" + normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    private String stripMarkdown(String text) {
        // Gọt sạch dấu **, __, và các thẻ Markdown định dạng thừa thãi
        return text.replaceAll("\\*\\*", "").replaceAll("__", "").replaceAll("\\*", "").replaceAll("^#+\\s*", "").replaceAll("^>\\s*", "").trim();
    }

    private boolean looksLikeInlineMCOptions(String text) {
        String stripped = stripMarkdown(text);
        int count = 0;
        for (char c : new char[]{'A','B','C','D'}) { if (stripped.contains(c + ".") || stripped.contains(c + ")")) count++; }
        return count >= 2;
    }

    private void parseInlineMCOptions(String line, ParsedQuestion q) {
        q.type = "MULTIPLE_CHOICE";
        Pattern labelPat = Pattern.compile("\\*{0,2}([A-D])\\.\\*{0,2}\\s*", Pattern.CASE_INSENSITIVE);
        Matcher m = labelPat.matcher(line);
        List<int[]> positions = new ArrayList<>();
        while (m.find()) { positions.add(new int[]{m.start(), m.end(), m.start(1)}); }

        for (int i = 0; i < positions.size(); i++) {
            String label = line.substring(positions.get(i)[2], positions.get(i)[2] + 1).toUpperCase();
            int contentStart = positions.get(i)[1];
            int contentEnd = (i + 1 < positions.size()) ? positions.get(i + 1)[0] : line.length();
            String rawContent = line.substring(contentStart, contentEnd).trim().replaceAll("\\.$", "").trim();
            String content = convertImagesToHtml(rawContent);
            if (content.isEmpty()) content = "(xem hình trong đề)";

            content = content.replaceAll("^\\s*\\*\\*\\s*", "").replaceAll("\\*\\*\\s*$", "").trim();

            if (q.options.stream().noneMatch(o -> o.label.equalsIgnoreCase(label))) {
                q.options.add(new ParsedQuestion.OptionItem(label, content, false));
            }
        }
    }

    private boolean looksLikeInlineTfOptions(String text) {
        String stripped = stripMarkdown(text);
        int count = 0;
        for (char c : new char[]{'a','b','c','d'}) { if (stripped.contains(c + ")")) count++; }
        return count >= 2;
    }

    private void parseInlineTfOptions(String line, ParsedQuestion q) {
        q.type = "TRUE_FALSE";
        Pattern labelPat = Pattern.compile("\\*{0,2}([a-d])\\)\\*{0,2}\\s*", Pattern.CASE_INSENSITIVE);
        Matcher m = labelPat.matcher(line);
        List<int[]> positions = new ArrayList<>();
        while (m.find()) { positions.add(new int[]{m.start(), m.end(), m.start(1)}); }

        for (int i = 0; i < positions.size(); i++) {
            String label = line.substring(positions.get(i)[2], positions.get(i)[2] + 1).toLowerCase();
            int contentStart = positions.get(i)[1];
            int contentEnd = (i + 1 < positions.size()) ? positions.get(i + 1)[0] : line.length();
            String rawContent = line.substring(contentStart, contentEnd).trim().replaceAll("\\.$", "").trim();

            rawContent = rawContent.replaceAll("^\\s*\\*\\*\\s*", "").replaceAll("\\*\\*\\s*$", "").trim();

            String content = convertImagesToHtml(rawContent);
            if (content.isEmpty()) content = "(xem hình trong đề)";
            if (q.options.stream().noneMatch(o -> o.label.equalsIgnoreCase(label))) {
                q.options.add(new ParsedQuestion.OptionItem(label, content, false));
            }
        }
    }

    private String md5Hash(byte[] data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data);
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) { return UUID.randomUUID().toString(); }
    }

    private void cleanupOldSessions(int maxSessions) {
        try {
            Path mediaRoot = Paths.get(MEDIA_BASE_DIR);
            if (!Files.exists(mediaRoot)) return;
            List<Path> sessions = Files.list(mediaRoot).filter(Files::isDirectory).sorted((a, b) -> {
                try { return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a)); } catch (Exception e) { return 0; }
            }).collect(Collectors.toList());
            if (sessions.size() <= maxSessions) return;
            for (int i = maxSessions; i < sessions.size(); i++) deleteDirectory(sessions.get(i));
        } catch (Exception ignored) {}
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walk(dir).sorted(Comparator.reverseOrder()).forEach(path -> { try { Files.delete(path); } catch (Exception ignored) {} });
    }

    private String findPandoc() {
        for (String candidate : PANDOC_CANDIDATES) {
            try {
                ProcessBuilder pb = new ProcessBuilder(candidate, "--version");
                if (pb.start().waitFor() == 0) return candidate;
            } catch (Exception ignored) {}
        }
        throw new RuntimeException("Không tìm thấy Pandoc trên hệ thống.");
    }

    private void runPandoc(String pandocPath, Path inputDocx, Path outputMd, Path mediaDir, String sessionId) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(pandocPath, inputDocx.toAbsolutePath().toString(), "--extract-media=" + mediaDir.toAbsolutePath().toString(), "-t", "markdown", "--wrap=none", "-o", outputMd.toAbsolutePath().toString());
        try { if (pb.start().waitFor() != 0) throw new RuntimeException("Pandoc lỗi."); } catch (Exception e) { throw new IOException(e); }
    }

    private ImportResult buildResult(List<ParsedQuestion> questions) {
        ImportResult result = new ImportResult();
        result.totalQuestions = questions.size();
        result.multipleChoiceCount = (int) questions.stream().filter(q -> "MULTIPLE_CHOICE".equals(q.type)).count();
        result.trueFalseCount = (int) questions.stream().filter(q -> "TRUE_FALSE".equals(q.type)).count();
        result.shortAnswerCount = (int) questions.stream().filter(q -> "SHORT_ANSWER".equals(q.type)).count();
        result.previewQuestions = new ArrayList<>(questions);
        return result;
    }
}