package backend.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class CourseDetailResponse {
    private Integer id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private List<ChapterDto> chapters;
}