package backend;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.regex.*;

public class TestDebug {
    public static void main(String[] args) throws Exception {
        String path = "E:\\SWP_version\\thang-backend2\\all_lines.txt";

        Pattern SECTION_HEADER = Pattern.compile("PH.*?N?\\s*(I{1,3}|IV|V|VI)(?![a-zA-Z])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Pattern QUESTION_HEADER = Pattern.compile("^\\W*(C.*?u|Question|B.*?i)\\s*(\\d+)[.:\\)\\s]?", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Pattern ANSWER_SECTION = Pattern.compile("(đáp\\s*án|d[aá]p\\s*[aá]n|answer\\s*key|bảng\\s*đáp\\s*án|B.NG\\s*D[AÁ]P|BANG\\s*DAP)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            int qCount = 0;
            String currentSectionType = "MULTIPLE_CHOICE";

            while ((line = br.readLine()) != null) {
                String plain = line.replace("**", "").trim();

                if (ANSWER_SECTION.matcher(plain).find()) {
                    System.out.println("HIT ANSWER_SECTION at line: " + plain);
                    break;
                }

                Matcher sm = SECTION_HEADER.matcher(plain);
                if (sm.find()) {
                    System.out.println("MATCHED SECTION: " + sm.group(1) + " -> " + plain);
                    currentSectionType = "SECTION_" + sm.group(1);
                    continue;
                }

                Matcher qm = QUESTION_HEADER.matcher(plain);
                if (qm.find()) {
                    qCount++;
                    System.out.println("MATCHED Q: " + qm.group(2) + " [Type: " + currentSectionType + "] -> " + plain);
                }
            }
            System.out.println("Total questions parsed: " + qCount);
        }
    }
}