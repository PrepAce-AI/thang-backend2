package backend.config;

import backend.controller.AiDataSeederController;
import backend.entity.User;
import backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@Slf4j

@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "app.seeder.enabled",
        havingValue = "true",
        matchIfMissing = false
)

public class AutoSeederConfig {

    private final AiDataSeederController aiDataSeederController;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @Bean
    public CommandLineRunner runAutoSeeder() {
        return args -> {
            log.info("=== BẮT ĐẦU CHẠY AUTO SEEDER ĐỂ FIX DỮ LIỆU AI ===");
            
            try {
                // Bắt buộc chuyển đổi kiểu dữ liệu cột trong database thành NVARCHAR để lưu được tiếng Việt (Đề phòng trường hợp database cũ lưu dưới dạng VARCHAR)
                try {
                    jdbcTemplate.execute("ALTER TABLE Questions ALTER COLUMN subject NVARCHAR(255)");
                    jdbcTemplate.execute("ALTER TABLE Questions ALTER COLUMN topic NVARCHAR(255)");
                    log.info("-> Đã ép kiểu cột subject và topic thành NVARCHAR thành công.");
                } catch (Exception e) {
                    log.warn("-> Không thể ALTER TABLE (có thể do cấu trúc DB đã chuẩn hoặc khóa ngoại chặn), bỏ qua bước ép kiểu. Lỗi: " + e.getMessage());
                }

                // 0. Xóa sổ 100% dữ liệu rác (Font lỗi) theo thứ bậc khóa ngoại
                try { jdbcTemplate.execute("DELETE FROM Notifications WHERE title LIKE '%?%' OR content LIKE '%?%'"); } catch (Exception ignored) {}
                
                /*
                // Xóa StudentAnswers -> PracticeAnswers -> QuizAttempts -> EntryTestSessions
                try { jdbcTemplate.execute("DELETE FROM StudentAnswers WHERE question_id IN (SELECT question_id FROM Questions WHERE question_content LIKE N'%Câu hỏi mẫu số%' OR subject LIKE '%?%' OR topic LIKE '%?%')"); } catch (Exception ignored) {}
                try { jdbcTemplate.execute("DELETE FROM PracticeAnswers WHERE question_id IN (SELECT question_id FROM Questions WHERE question_content LIKE N'%Câu hỏi mẫu số%' OR subject LIKE '%?%' OR topic LIKE '%?%')"); } catch (Exception ignored) {}
                
                // Xóa Options và Questions (quét sạch mọi câu hỏi Mock cũ)
                try { jdbcTemplate.execute("DELETE FROM QuestionOptions WHERE question_id IN (SELECT question_id FROM Questions WHERE question_content LIKE N'%Câu hỏi mẫu số%' OR subject LIKE '%?%' OR topic LIKE '%?%')"); } catch (Exception ignored) {}
                try { jdbcTemplate.execute("DELETE FROM Questions WHERE question_content LIKE N'%Câu hỏi mẫu số%' OR subject LIKE '%?%' OR topic LIKE '%?%'"); } catch (Exception ignored) {}
                
                // Fix các Quiz, Courses nếu bị lỗi
                try { jdbcTemplate.execute("UPDATE Courses SET title = N'Toán' WHERE title LIKE N'%?%'"); } catch (Exception ignored) {}
                try { jdbcTemplate.execute("UPDATE Courses SET subject = N'Toán' WHERE subject LIKE N'%?%'"); } catch (Exception ignored) {}
                try { jdbcTemplate.execute("UPDATE Quizzes SET title = N'Đề thi thử Toán' WHERE title LIKE N'%?%'"); } catch (Exception ignored) {}
                try { jdbcTemplate.execute("UPDATE Quizzes SET subject = N'Toán' WHERE subject LIKE N'%?%'"); } catch (Exception ignored) {}
                */
                log.info("-> Đã vô hiệu hóa việc tự động xóa/đổi tên dữ liệu (tránh hỏng DB khi import).");

                // 1. Tự động sinh 250 câu hỏi
                aiDataSeederController.seedQuestions();
                log.info("-> Đã sinh xong 250 câu hỏi (nếu chưa có).");

                // Tìm 1 học sinh ngẫu nhiên (hoặc admin) để mượn quyền fix data
                Optional<User> firstUser = userRepository.findAll().stream().findFirst();
                
                if (firstUser.isPresent()) {
                    User user = firstUser.get();
                    // Tạo một Authentication giả lập (Mock Auth)
                    Authentication mockAuth = new UsernamePasswordAuthenticationToken(user.getEmail(), null, java.util.Collections.emptyList());
                    
                    // 2. Dọn rác DB (Fix lỗi Khác)
                    aiDataSeederController.fixOldData(mockAuth);
                    log.info("-> Đã dọn rác DB (Fix lỗi 'Khác') cho user: {}", user.getEmail());

                    // 3. Bơm lại data bài thi ngẫu nhiên (Yếu Toán)
                    aiDataSeederController.seedMockData(mockAuth);
                    log.info("-> Đã bơm lịch sử làm bài thi (Mock Data) cho user: {}", user.getEmail());
                } else {
                    log.warn("Không tìm thấy user nào trong DB để seed data!");
                }

                log.info("=== HOÀN TẤT AUTO SEEDER ===");
                
            } catch (Exception e) {
                log.error("Lỗi khi chạy Auto Seeder: ", e);
            }
        };
    }
}
