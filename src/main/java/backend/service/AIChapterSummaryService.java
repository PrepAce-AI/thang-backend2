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
        
        Nhiệm vụ:
        - Tóm tắt chương học.
        - Giải thích ngắn gọn, dễ hiểu.
        - Không dùng Markdown.
              Không dùng ký hiệu #, *, -, ``` hoặc bảng.
              Viết thành văn bản thuần.
        - Không trả lời lan man.
        - Trả về tiếng Việt.
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
        Optional<AIChapterSummary> oldSummary =
                summaryRepository.findByStudent_IdAndChapter_Id(studentId, chapterId);

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
        // 5. Gọi Gemini
        //--------------------------------------------

        String aiResult = geminiService.ask(
                SYSTEM_CONTEXT,
                prompt
        );

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
                .findByStudent_IdAndChapter_Id(studentId, chapterId)
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
}