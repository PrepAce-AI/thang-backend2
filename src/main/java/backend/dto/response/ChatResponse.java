package backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ChatResponse {
    private Integer chatId;
    private String question;
    private String aiResponse;
    private Date createdAt;
}
