package backend.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class FlashQuizResponseDTO {

    private List<FlashQuizQuestionDTO> questions;

}