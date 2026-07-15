package backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Bài làm Luyện Đề gửi từ Frontend.
 * answers: map questionId -> optionId học sinh đã chọn.
 * Câu bỏ trống thì không cần gửi (server tự tính là sai).
 */
@Data
public class PracticeSubmitRequest {

    @NotNull(message = "Thiếu attemptId")
    private Integer attemptId;

    private Map<Integer, String> answers = new HashMap<>();
}
