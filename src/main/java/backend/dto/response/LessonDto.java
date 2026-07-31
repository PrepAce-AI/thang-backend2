package backend.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class LessonDto {
    private Integer id;
    private String title;
    private String description;
    private String videoUrl;
    private String duration;
    private Integer order;
    // Thêm vào trong LessonDto
    private List<MaterialDto> materials;
    private Boolean isPreview;
}