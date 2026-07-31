package backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "message is required")
    @Size(max = 4000, message = "Câu hỏi không vượt quá 4000 ký tự")
    private String message;

    /** Ngữ cảnh môn học (tùy chọn) — VD: "Toán 12", "Vật lý" */
    private String subject;
}
