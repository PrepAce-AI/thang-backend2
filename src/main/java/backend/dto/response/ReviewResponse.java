package backend.dto.response;

import lombok.Data;
import java.util.Date;

@Data
public class ReviewResponse {
    private Integer id;
    private Integer rating;
    private String comment;
    private Date createdAt;
    private String userFullName;
    private String userAvatarUrl;
}