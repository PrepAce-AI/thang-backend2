package backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class YouTubeConfig {

    // Đây là mã API Key do Giáo viên cung cấp
    public static final String API_KEY = "AIzaSyAAIZPohlXnUtpyxIm0-B_hs-On9BxOM-c";

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
