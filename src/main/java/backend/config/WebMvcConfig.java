package backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.io.File;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // ─── 1. Uploads (ảnh, tài liệu thông thường) ───────────────────────────
        String uploadPath = new File("src/main/resources/static/uploads/").getAbsolutePath();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");

        // ─── 2. Word-Media (ảnh công thức từ Word import qua Pandoc) ───────────
        // Mỗi lần import tạo 1 sub-folder theo UUID trong thư mục này.
        // Frontend truy cập qua: /word-media/{uuid}/{image.png}
        String wordMediaPath = Paths.get("word-media").toAbsolutePath().toString();
        registry.addResourceHandler("/word-media/**")
                .addResourceLocations("file:" + wordMediaPath + "/")
                .setCachePeriod(86400); // Cache 1 ngày
    }
}