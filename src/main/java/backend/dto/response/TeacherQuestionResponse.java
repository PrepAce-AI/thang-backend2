package backend.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TeacherQuestionResponse extends QuestionResponse {
    private Integer courseId;
    private String courseTitle;
    private Integer chapterId;
    private String chapterTitle;
    private Integer lessonId;
    private String lessonTitle;
}
