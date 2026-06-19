package backend.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/materials")
public class MaterialController {

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadMaterial(@RequestParam String fileUrl) {
        try {
            // 1. Giải mã URL (đề phòng đường dẫn chứa khoảng trắng hoặc ký tự đặc biệt)
            String decodedPath = URLDecoder.decode(fileUrl, StandardCharsets.UTF_8);

            // 2. Trỏ trực tiếp tới vị trí file vật lý trên ổ cứng máy tính
            File file = new File("src/main/resources/static" + decodedPath);

            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);

            // 3. Thiết lập Header ép trình duyệt phải bật hộp thoại Tải xuống (Attachment)
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}