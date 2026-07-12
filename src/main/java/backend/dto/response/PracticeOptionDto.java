package backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lựa chọn trả về khi ĐANG làm bài — cố tình KHÔNG có field isCorrect
 * để đáp án đúng không bao giờ lộ ra Network tab của trình duyệt.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PracticeOptionDto {
    private Integer optionId;
    private String optionContent;
}
