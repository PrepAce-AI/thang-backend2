package backend.dto.response;

import lombok.Data;
import java.util.Date;

@Data
public class QuestionResponse {
    private Integer id;
    private String content;
    private Date createdAt;
    private String userFullName; // Trả tên người hỏi để hiển thị lên màn hình
    private String userAvatarUrl;
}