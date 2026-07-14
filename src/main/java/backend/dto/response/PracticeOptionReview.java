package backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lựa chọn trong màn hình KẾT QUẢ — lúc này mới được lộ isCorrect. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PracticeOptionReview {
    private Integer optionId;
    private String optionContent;
    private boolean correct;   // đáp án đúng thật sự (FE tô xanh)
    private boolean selected;  // học sinh đã chọn (FE tô đỏ nếu selected && !correct)
}
