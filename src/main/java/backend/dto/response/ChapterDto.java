package backend.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class ChapterDto {
    private Integer id;
    private String title;
    private Integer order;
    private List<LessonDto> lessons;
}