package backend;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.text.Normalizer;

/**
 * Test standalone (khong can Spring) de verify chuc nang Word Import
 * voi file de thi Toan THPT 2026 thuc te.
 */
public class WordImportTest {

    static final String DOCX_PATH =
        "E:\\SWP_version\\thang-backend2\\thuvienhoclieu.com-De-thi-tot-nghipe-THPT-mon-Toan-2026-Ma-de-104 (1).docx";

    static final List<String> PANDOC_CANDIDATES = List.of(
        "pandoc",
        "C:/Users/ADMIN/AppData/Local/Pandoc/pandoc.exe",
        "C:/Program Files/Pandoc/pandoc.exe"
    );

    static final Pattern QUESTION_HEADER =
        Pattern.compile("^\\*{1,2}\\s*(C[\\u00e2a]u|Question|B\\u00e0i)\\s*(\\d+)\\s*\\*{0,2}\\s*[.:)]\\*{0,2}\\s*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    static final Pattern SECTION_HEADER = Pattern.compile(
        "^\\W*P\\s*H\\s*[^\\sN]{0,2}\\s*N\\s*(I{1,3}|IV|V|VI)(?![a-zA-Z])",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    static final Pattern ANSWER_SECTION = Pattern.compile(
        "(\u0111\u00e1p\\s*\u00e1n|d[a\u00e1]p\\s*[\u00e1a]n|answer\\s*key|b\u1ea3ng\\s*\u0111\u00e1p\\s*\u00e1n)",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    static final Pattern OPTION_LINE = Pattern.compile(
        "^\\W*([A-D])[.)\\s]\\s*(.*)", Pattern.CASE_INSENSITIVE);

    static final Pattern OPTION_TF = Pattern.compile(
        "^\\W*([a-d])[.)\\s]\\s*(.*)", Pattern.CASE_INSENSITIVE);

    static class Q {
        int number, origNum;
        String type, content, correctAnswer;
        List<String[]> options = new ArrayList<>();
        public String toString() {
            return String.format("  [%d] Cau%d %-15s dapan=%-4s opts=%d nd: %s",
                number, origNum, type, correctAnswer != null ? correctAnswer : "?",
                options.size(),
                content != null ? content.substring(0, Math.min(60, content.length())) : "(null)");
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== WordImport Test - De Toan THPT 2026 ===");

        String pandocPath = findPandoc();
        System.out.println("Pandoc: " + pandocPath);

        Path workDir = Paths.get("word-media-test");
        Files.createDirectories(workDir);
        Path outputMd = workDir.resolve("test_output.md");
        Path mediaDir = workDir.resolve("media");

        runPandoc(pandocPath, Paths.get(DOCX_PATH).toAbsolutePath(), outputMd, mediaDir.toAbsolutePath());
        System.out.println("Pandoc OK, md size: " + Files.size(outputMd) + " bytes");

        String markdown = Files.readString(outputMd, StandardCharsets.UTF_8);
        markdown = Normalizer.normalize(markdown, Normalizer.Form.NFC);

        List<Q> questions = parseMarkdown(markdown);
        questions.removeIf(q -> q.content == null || q.content.length() < 3);

        long mc = questions.stream().filter(q -> "MULTIPLE_CHOICE".equals(q.type)).count();
        long tf = questions.stream().filter(q -> "TRUE_FALSE".equals(q.type)).count();
        long sa = questions.stream().filter(q -> "SHORT_ANSWER".equals(q.type)).count();

        System.out.println();
        System.out.println("=== KET QUA ===");
        System.out.printf("Tong: %d (mong doi: 22)%n", questions.size());
        System.out.printf("MC  : %d (mong doi: 12)%n", mc);
        System.out.printf("TF  : %d (mong doi:  4)%n", tf);
        System.out.printf("SA  : %d (mong doi:  6)%n", sa);

        System.out.println();
        System.out.println("=== CHI TIET CAU HOI ===");
        questions.forEach(q -> {
            System.out.println(q);
            if ("TRUE_FALSE".equals(q.type)) {
                q.options.forEach(o -> System.out.printf("     %s) correct=%s%n", o[0], o[2]));
            }
        });

        System.out.println();
        System.out.println("=== KIEM TRA DAP AN PHAN I ===");
        String[] expected = {"B","C","D","C","B","A","A","D","B","A","A","B"};
        List<Q> mcList = questions.stream().filter(q -> "MULTIPLE_CHOICE".equals(q.type)).toList();
        int okCount = 0;
        for (int i = 0; i < Math.min(expected.length, mcList.size()); i++) {
            Q q = mcList.get(i);
            String ans = q.correctAnswer != null ? q.correctAnswer : "?";
            boolean ok = expected[i].equalsIgnoreCase(ans);
            if (ok) okCount++;
            System.out.printf("  Cau %2d (origNum=%d): expect=%s got=%-3s %s%n",
                i+1, q.origNum, expected[i], ans, ok ? "OK" : "FAIL");
        }
        System.out.printf("Dap an dung: %d/%d%n", okCount, Math.min(expected.length, mcList.size()));

        try { deleteDir(workDir); System.out.println("Xoa thu muc test OK"); }
        catch (Exception e) { System.out.println("Warn: " + e.getMessage()); }
    }

    static List<Q> parseMarkdown(String markdown) {
        List<String> lines = Arrays.stream(markdown.split("\\r?\\n"))
            .map(String::stripTrailing).collect(java.util.stream.Collectors.toList());

        List<Q> result = new ArrayList<>();
        Q current = null;
        StringBuilder content = new StringBuilder();
        String currentSectionType = "MULTIPLE_CHOICE";
        String currentAnswerSectionType = "MULTIPLE_CHOICE";
        int globalCounter = 0;
        boolean inAnswerSection = false;
        boolean inExplanation = false;

        Map<String, Map<Integer, Object>> tableAnswers = new HashMap<>();
        tableAnswers.put("MULTIPLE_CHOICE", new HashMap<>());
        tableAnswers.put("TRUE_FALSE", new HashMap<>());
        tableAnswers.put("SHORT_ANSWER", new HashMap<>());
        List<Integer> tableHeaders = new ArrayList<>();

        for (String raw : lines) {
            String plain = raw.replaceAll("^>\\s*", "").trim();
            String stripped = plain
                .replaceAll("\\*{1,3}", "")
                .replaceAll("^#+\\s*", "")
                .replaceAll("^>\\s*", "")
                .replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)(\\{[^}]*\\})?", "")
                .trim();

            if (stripped.isEmpty()) continue;

            if (Pattern.compile("(l\u1eddi\\s*gi\u1ea3i\\s*chi\\s*ti\u1ebft|h\u01b0\u1edbng\\s*d\u1eabn\\s*gi\u1ea3i)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(stripped).find()) {
                inExplanation = true;
                if (current != null && !inAnswerSection) {
                    finalizeQ(current, content.toString().trim());
                    if (!result.contains(current)) result.add(current);
                    current = null; content = new StringBuilder();
                }
                continue;
            }

            if (ANSWER_SECTION.matcher(stripped).find()) {
                inAnswerSection = true;
                if (current != null) {
                    finalizeQ(current, content.toString().trim());
                    if (!result.contains(current)) result.add(current);
                    current = null; content = new StringBuilder();
                }
                continue;
            }

            if (inAnswerSection) {
                if (Pattern.compile("^\\s*P\\s*H\\s*[^\\sN]{0,2}\\s*N\\s*(I{1,3}|IV|V|VI)(?![a-zA-Z])",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(plain).find()) {
                    if (plain.contains("III")) currentAnswerSectionType = "SHORT_ANSWER";
                    else if (plain.contains("II")) currentAnswerSectionType = "TRUE_FALSE";
                    else if (plain.contains("I")) currentAnswerSectionType = "MULTIPLE_CHOICE";
                    continue;
                }

                String[] tokens = plain.trim().split("\\s+");
                if (tokens.length == 0 || tokens[0].isEmpty()) continue;

                if (plain.startsWith("C\u00e2u") || plain.startsWith("Cau")) {
                    tableHeaders.clear();
                    for (String token : tokens) {
                        try { int n = Integer.parseInt(token.replaceAll("[^0-9]", ""));
                              if (n > 0) tableHeaders.add(n); }
                        catch (Exception ignored) {}
                    }
                    continue;
                }

                if ("MULTIPLE_CHOICE".equals(currentAnswerSectionType) && plain.startsWith("Ch\u1ecdn")) {
                    for (int j = 1; j < tokens.length; j++) {
                        int colIdx = j - 1;
                        if (colIdx < tableHeaders.size()) {
                            String ans = tokens[j].toUpperCase().replaceAll("[^A-D]", "");
                            if (!ans.isEmpty())
                                tableAnswers.get("MULTIPLE_CHOICE").put(tableHeaders.get(colIdx), ans);
                        }
                    }
                }

                if ("SHORT_ANSWER".equals(currentAnswerSectionType) && plain.toLowerCase().startsWith("\u0111\u00e1p \u00e1n")) {
                    for (int j = 2; j < tokens.length; j++) {
                        int colIdx = j - 2;
                        if (colIdx < tableHeaders.size())
                            tableAnswers.get("SHORT_ANSWER").put(tableHeaders.get(colIdx), tokens[j]);
                    }
                }

                if ("TRUE_FALSE".equals(currentAnswerSectionType) && plain.matches("^[a-d]\\).*")) {
                    List<Boolean> rowAnswers = new ArrayList<>();
                    for (String token : tokens) {
                        if (token.equalsIgnoreCase("\u0110") || token.equalsIgnoreCase("\u0110\u00fang")) rowAnswers.add(true);
                        else if (token.equalsIgnoreCase("S") || token.equalsIgnoreCase("Sai")) rowAnswers.add(false);
                    }
                    String optLabel = tokens[0].replaceAll("[^a-dA-D]", "").toLowerCase();
                    for (int j = 0; j < rowAnswers.size() && j < tableHeaders.size(); j++) {
                        int qNum = tableHeaders.get(j);
                        Map<Integer, Object> tfMap = tableAnswers.get("TRUE_FALSE");
                        tfMap.putIfAbsent(qNum, new HashMap<String, Boolean>());
                        @SuppressWarnings("unchecked")
                        Map<String, Boolean> qTfMap = (Map<String, Boolean>) tfMap.get(qNum);
                        qTfMap.put(optLabel, rowAnswers.get(j));
                    }
                }

                // Inline "1. A"
                Matcher akm = Pattern.compile("\\b(\\d{1,2})\\s*[.:\\-]\\s*([A-Da-d])\\b").matcher(plain);
                while (akm.find()) {
                    int qNum = Integer.parseInt(akm.group(1));
                    String ans = akm.group(2).toUpperCase();
                    tableAnswers.get(currentAnswerSectionType).putIfAbsent(qNum, ans);
                }
                continue;
            }

            Matcher sm = SECTION_HEADER.matcher(stripped);
            if (sm.find()) {
                String roman = sm.group(1).toUpperCase();
                currentSectionType = switch (roman) {
                    case "I"   -> "MULTIPLE_CHOICE";
                    case "II"  -> "TRUE_FALSE";
                    case "III" -> "SHORT_ANSWER";
                    default    -> currentSectionType;
                };
                continue;
            }

            Matcher qm = QUESTION_HEADER.matcher(plain);
            if (qm.find()) {
                int origNum = Integer.parseInt(qm.group(2));
                if (!inExplanation) {
                    if (current != null) {
                        finalizeQ(current, content.toString().trim());
                        if (!result.contains(current)) result.add(current);
                    }
                    globalCounter++;
                    current = new Q();
                    current.number = globalCounter;
                    current.origNum = origNum;
                    current.type = currentSectionType;
                    content = new StringBuilder();
                    String afterHeader = plain.substring(qm.end()).trim();
                    if (!afterHeader.isEmpty()) content.append(afterHeader);
                } else {
                    final String sType = currentSectionType;
                    current = result.stream()
                        .filter(q -> q.origNum == origNum && sType.equals(q.type))
                        .findFirst().orElse(null);
                }
                continue;
            }

            if (current == null) continue;

            boolean isBlockquote = raw.startsWith(">");
            if (isBlockquote || !current.options.isEmpty()) {
                Matcher optM = OPTION_LINE.matcher(stripped);
                if (optM.matches() && (current.type == null || "MULTIPLE_CHOICE".equals(current.type))) {
                    if (current.type == null) current.type = "MULTIPLE_CHOICE";
                    String label = optM.group(1).toUpperCase();
                    String optContent = optM.group(2).trim();
                    if (current.options.stream().noneMatch(o -> o[0].equalsIgnoreCase(label)))
                        current.options.add(new String[]{label, optContent, "false"});
                    continue;
                }
                Matcher tfM = OPTION_TF.matcher(stripped);
                if (tfM.matches() && !"MULTIPLE_CHOICE".equals(current.type) && !"SHORT_ANSWER".equals(current.type)) {
                    if (current.type == null) current.type = "TRUE_FALSE";
                    if ("TRUE_FALSE".equals(current.type)) {
                        String label = tfM.group(1).toLowerCase();
                        String optContent = tfM.group(2).trim();
                        if (current.options.stream().noneMatch(o -> o[0].equalsIgnoreCase(label)))
                            current.options.add(new String[]{label, optContent, "false"});
                        continue;
                    }
                }
            }

            if (current.options.isEmpty()) {
                if (content.length() > 0) content.append(" ");
                content.append(stripped);
            }
        }

        if (current != null) { finalizeQ(current, content.toString().trim()); result.add(current); }

        System.out.println("-- DEBUG bang dap an --");
        System.out.println("  MC: " + tableAnswers.get("MULTIPLE_CHOICE"));
        System.out.println("  TF: " + tableAnswers.get("TRUE_FALSE"));
        System.out.println("  SA: " + tableAnswers.get("SHORT_ANSWER"));
        System.out.println("-- END DEBUG --");

        // Apply answers using origNum
        for (Q q : result) {
            Map<Integer, Object> sectionAns = tableAnswers.get(q.type);
            Integer lookupKey = (sectionAns != null && sectionAns.containsKey(q.origNum))
                ? q.origNum : q.number;
            if (sectionAns != null && sectionAns.containsKey(lookupKey)) {
                if ("MULTIPLE_CHOICE".equals(q.type)) {
                    q.correctAnswer = (String) sectionAns.get(lookupKey);
                    for (String[] opt : q.options) opt[2] = String.valueOf(opt[0].equalsIgnoreCase(q.correctAnswer));
                } else if ("SHORT_ANSWER".equals(q.type)) {
                    q.correctAnswer = (String) sectionAns.get(lookupKey);
                } else if ("TRUE_FALSE".equals(q.type)) {
                    @SuppressWarnings("unchecked")
                    Map<String, Boolean> ansMap = (Map<String, Boolean>) sectionAns.get(lookupKey);
                    if (ansMap != null) {
                        for (String[] opt : q.options) {
                            Boolean val = ansMap.get(opt[0].toLowerCase());
                            if (val != null) opt[2] = String.valueOf(val);
                        }
                    }
                }
            }
        }
        return result;
    }

    static void finalizeQ(Q q, String contentStr) {
        q.content = (contentStr == null || contentStr.isBlank())
            ? "(Cau " + q.number + " xem file goc)" : contentStr.trim();
        if (q.type == null) {
            if (!q.options.isEmpty()) {
                boolean hasMC = q.options.stream().anyMatch(o -> o[0].matches("[A-D]"));
                q.type = hasMC ? "MULTIPLE_CHOICE" : "TRUE_FALSE";
            } else q.type = "SHORT_ANSWER";
        }
    }

    static String findPandoc() {
        for (String p : PANDOC_CANDIDATES) {
            try {
                Process proc = new ProcessBuilder(p, "--version").redirectErrorStream(true).start();
                if (proc.waitFor() == 0) return p;
            } catch (Exception ignored) {}
        }
        throw new RuntimeException("Khong tim thay Pandoc!");
    }

    static void runPandoc(String pandocPath, Path inputDocx, Path outputMd, Path mediaDir) throws Exception {
        Files.createDirectories(mediaDir);
        ProcessBuilder pb = new ProcessBuilder(
            pandocPath, inputDocx.toString(),
            "--extract-media=" + mediaDir.toString(),
            "-t", "markdown", "--wrap=none",
            "-o", outputMd.toString()
        );
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        String out = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int code = proc.waitFor();
        if (code != 0) throw new RuntimeException("Pandoc failed (exit " + code + "): " + out);
    }

    static void deleteDir(Path dir) throws Exception {
        if (!Files.exists(dir)) return;
        Files.walk(dir).sorted(Comparator.reverseOrder())
            .forEach(p -> { try { Files.delete(p); } catch (Exception ignored) {} });
    }
}
