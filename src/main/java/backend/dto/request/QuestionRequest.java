package backend.dto.request;

import lombok.Data;

@Data
public class QuestionRequest {
    private String content;
    private Integer lessonId;
    private Integer timestampSeconds;
}