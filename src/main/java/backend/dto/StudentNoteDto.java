package backend.dto;

import lombok.Data;
import java.util.Date;

@Data
public class StudentNoteDto {
    private Integer id;
    private String content;
    private Integer timestampSeconds;
    private Date createdAt;
}
