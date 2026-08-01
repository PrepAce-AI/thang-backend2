package backend.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class CourseDetailResponse {
    private Integer id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private java.math.BigDecimal price;
    private List<ChapterDto> chapters;
    private Integer students = 0;
    
    private Integer teacherId;
    private String teacherName;
    
    private Integer subjectId;
    private String subjectName;
}