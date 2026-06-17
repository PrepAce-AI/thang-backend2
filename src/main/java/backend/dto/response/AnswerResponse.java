package backend.dto.response;

import lombok.Data;
import java.util.Date;

@Data
public class AnswerResponse {
    private Integer id;
    private String content;
    private Date createdAt;
    private String userFullName;
    private String userAvatarUrl;
    private Integer userRoleId;
}
