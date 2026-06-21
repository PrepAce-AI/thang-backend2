package backend.dto.response;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class QuestionResponse {
    private Integer id;
    private String content;
    private Date createdAt;
    private Integer timestampSeconds;
    private String userFullName; // Trả tên người hỏi để hiển thị lên màn hình
    private String userAvatarUrl;
    private Integer userRoleId;
    private List<AnswerResponse> answers;
}
