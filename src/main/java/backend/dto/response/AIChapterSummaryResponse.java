package backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIChapterSummaryResponse {

    private Integer summaryId;

    private Integer courseId;

    private String courseTitle;

    private Integer chapterId;

    private String chapterTitle;

    private String aiModel;

    private String summaryContent;

    private Date createdAt;

}