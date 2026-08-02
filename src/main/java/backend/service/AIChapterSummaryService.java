package backend.service;

import backend.dto.response.AIChapterSummaryResponse;
import backend.entity.AIChapterSummary;
import backend.entity.Chapter;
import backend.entity.Course;
import backend.entity.Lesson;
import backend.entity.User;
import backend.repository.AIChapterSummaryRepository;
import backend.repository.ChapterRepository;
import backend.repository.CourseRepository;
import backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIChapterSummaryService {

    private final AIChapterSummaryRepository summaryRepository;
    private final ChapterRepository chapterRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    private final GeminiService geminiService;

    private static final String SYSTEM_CONTEXT = """
        Bạn là AI Learning Assistant của PrepAce.
        
        Luôn trả lời bằng TIẾNG VIỆT.
        
        Tuyệt đối KHÔNG được:
        - Dùng tiếng Anh.
        - Dùng tiếng Trung.
        - Dùng tiếng Nhật.
        - Dùng tiếng Hàn.
        - Dùng tiếng Nga.
        - Dùng tiếng Hindi.
        - Dùng bất kỳ từ hoặc ký tự thuộc ngôn ngữ khác.
        
        Nếu xuất hiện từ không phải tiếng Việt thì phải tự sinh lại câu đó.
        
        Chỉ sử dụng ký tự Unicode tiếng Việt chuẩn.
        
        Giải thích ngắn gọn, rõ ràng, đúng trọng tâm.
        
        Nếu là bài toán:
        - Trình bày theo từng bước.
        - Viết công thức bằng ký hiệu toán học thông thường.
        - Không dùng LaTeX.
        - Không dùng Markdown.
        - Không dùng HTML.
        - Không dùng bảng.
        
        Ví dụ:
        Bước 1: Tính Δ = b² − 4ac
        Bước 2: Thay số
        Bước 3: Kết luận
        
        Không được tự tạo ký tự lạ hoặc ký tự Unicode bất thường.
        
        Nếu không chắc chắn thì nói "Tôi không chắc chắn", không được bịa.
        """;

    /**
     * Sinh AI Summary sau khi hoàn thành Chapter
     */
    public AIChapterSummary generateSummary(
            Integer studentId,
            Integer courseId,
            Integer chapterId
    ) {

        //--------------------------------------------
        // 1. Nếu đã từng sinh rồi thì trả luôn
        //--------------------------------------------
        System.out.println("AAAAAAAAAAAA");
        Optional<AIChapterSummary> oldSummary =
                summaryRepository.findFirstByStudent_IdAndChapter_Id(studentId, chapterId);

        if (oldSummary.isPresent()) {
            return oldSummary.get();
        }

        //--------------------------------------------
        // 2. Lấy dữ liệu
        //--------------------------------------------

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));

        //--------------------------------------------
        // 3. Ghép nội dung Chapter
        //--------------------------------------------

        StringBuilder lessonContent = new StringBuilder();

        for (Lesson lesson : chapter.getLessons()) {

            lessonContent.append("Bài học: ")
                    .append(lesson.getTitle())
                    .append("\n");

            if (lesson.getDescription() != null) {
                lessonContent.append(lesson.getDescription())
                        .append("\n");
            }

            if (lesson.getMaterials() != null) {

                lesson.getMaterials().forEach(material -> {

                    if (material.getContent() != null) {
                        lessonContent
                                .append(material.getContent())
                                .append("\n");
                    }
                });
            }

            lessonContent.append("\n");
        }

        //--------------------------------------------
        // 4. Prompt
        //--------------------------------------------

        String prompt = """
                Bạn là AI gia sư của PrepAce.

                Hãy tạo bản tổng kết chương học.

                Yêu cầu:

                1. Tóm tắt nội dung chương.
                2. Liệt kê các kiến thức quan trọng.
                3. Các công thức cần nhớ.
                4. Những lỗi học sinh thường gặp.
                5. Mẹo ghi nhớ.
                6. Chuẩn bị cho chương tiếp theo.

                Không markdown.

                ======

                Tên khóa học:
                %s

                Tên chương:
                %s

                Nội dung:

                %s
                """.formatted(
                course.getTitle(),
                chapter.getTitle(),
                lessonContent
        );

        //--------------------------------------------
        // 5. Gọi Gemini (Có Fallback)
        //--------------------------------------------
        System.out.println("===== CALL OPENROUTER =====");
        String aiResult;

        try {

            System.out.println("========== SUMMARY CALL GEMINI ==========");
            System.out.println("COURSE: " + course.getTitle());
            System.out.println("CHAPTER: " + chapter.getTitle());
            System.out.println("PROMPT LENGTH: " + prompt.length());

            aiResult = geminiService.ask(
                    SYSTEM_CONTEXT,
                    prompt
            );

            System.out.println("========== SUMMARY RESULT ==========");
            System.out.println(aiResult);


            // phòng trường hợp AI trả null hoặc rỗng
            if (aiResult == null || aiResult.trim().isEmpty()) {
                throw new RuntimeException("AI returned empty response");
            }


        } catch (Exception e) {

            log.error(
                    "AI unavailable, using fallback summary: {}",
                    e.getMessage()
            );


            aiResult = generateFallbackSummary(
                    course,
                    chapter,
                    lessonContent.toString()
            );
        }

        System.out.println("===== OPENROUTER DONE =====");
        System.out.println(aiResult);

        //--------------------------------------------
        // 6. Lưu DB
        //--------------------------------------------

        AIChapterSummary summary = new AIChapterSummary();

        summary.setStudent(student);
        summary.setCourse(course);
        summary.setChapter(chapter);

        summary.setSummaryContent(aiResult);
        summary.setAiModel("gemini-2.5-flash");
        summary.setCreatedAt(new Date());

        summary = summaryRepository.save(summary);

        log.info("AI Summary generated. student={}, chapter={}",
                studentId,
                chapterId);

        return summary;
    }

    public AIChapterSummaryResponse getSummary(
            Integer studentId,
            Integer chapterId
    ) {

        AIChapterSummary summary = summaryRepository
                .findFirstByStudent_IdAndChapter_Id(studentId, chapterId)
                .orElse(null);

        if (summary == null) {
            return null;
        }

        return new AIChapterSummaryResponse(
                summary.getSummaryId(),
                summary.getCourse().getCourseId(),
                summary.getCourse().getTitle(),
                summary.getChapter().getId(),
                summary.getChapter().getTitle(),
                summary.getAiModel(),
                summary.getSummaryContent(),
                summary.getCreatedAt()
        );
    }

    private String generateFallbackSummary(
            Course course,
            Chapter chapter,
            String content
    ) {


        return """
    📚 TỔNG KẾT CHƯƠNG HỌC

    Khóa học:
    %s


    Chương:
    %s


    1. Tổng quan kiến thức:

    Chương học này cung cấp các kiến thức nền tảng và kỹ năng quan trọng.
    Học sinh cần nắm được các khái niệm chính, hiểu phương pháp giải quyết
    vấn đề và biết cách vận dụng vào bài tập.


    2. Kiến thức trọng tâm:

    - Hiểu các nội dung chính trong từng bài học.
    - Ghi nhớ các khái niệm quan trọng.
    - Luyện tập các dạng bài thường gặp.
    - Áp dụng kiến thức vào các bài kiểm tra.


    3. Công thức / nội dung cần nhớ:

    - Ôn lại toàn bộ công thức và quy tắc xuất hiện trong chương.
    - Ghi nhớ điều kiện áp dụng của từng phương pháp.


    4. Lỗi thường gặp:

    - Chưa hiểu bản chất vấn đề.
    - Nhầm lẫn giữa các công thức.
    - Thiếu bước kiểm tra kết quả.


    5. Mẹo ghi nhớ:

    - Học theo từng nhóm kiến thức.
    - Kết hợp xem bài giảng và làm bài tập.
    - Ôn tập thường xuyên.


    6. Chuẩn bị chương tiếp theo:

    Hãy hoàn thành bài tập luyện tập và kiểm tra lại kiến thức
    trước khi chuyển sang nội dung mới.


    (Bản tóm tắt dự phòng khi AI tạm thời không khả dụng)
    """.formatted(
                course.getTitle(),
                chapter.getTitle()
        );

    }
}