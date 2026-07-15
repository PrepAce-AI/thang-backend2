package backend.controller;

import backend.dto.response.AIChapterSummaryResponse;
import backend.repository.UserRepository;
import backend.service.AIChapterSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/chapter-summary")
@RequiredArgsConstructor
public class AIChapterSummaryController {

    private final AIChapterSummaryService aiChapterSummaryService;
    private final UserRepository userRepository;

    @GetMapping("/{chapterId}")
    public ResponseEntity<?> getSummary(
            Authentication authentication,
            @PathVariable Integer chapterId
    ) {

        String email = authentication.getName();

        Integer studentId = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        AIChapterSummaryResponse response =
                aiChapterSummaryService.getSummary(
                        studentId,
                        chapterId
                );

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }
}