package backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Câu hỏi trả về khi ĐANG làm bài — không kèm đáp án đúng, không kèm explanation. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PracticeQuestionDto {
    private Integer questionId;
    private String questionContent;
    private String topic;
    private String questionType;
    private List<PracticeOptionDto> options;
}
