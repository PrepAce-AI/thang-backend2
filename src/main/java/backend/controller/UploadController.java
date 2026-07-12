package backend.controller;

import backend.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.io.File;
import java.util.UUID;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/upload")
public class UploadController {

    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping("/video")
    public ResponseEntity<?> uploadVideo(@RequestParam("file") MultipartFile file) {
        try {
            // Lấy đường dẫn tuyệt đối tới thư mục src/main/resources/static/uploads/videos/
            String uploadDir = new File("src/main/resources/static/uploads/videos/").getAbsolutePath();
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs(); // Tự động tạo thư mục nếu chưa tồn tại
            }

            // Tạo tên file ngẫu nhiên (UUID) để không bị trùng lặp tên
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString() + extension;

            // Lưu file vào ổ cứng
            File serverFile = new File(dir, newFilename);
            file.transferTo(serverFile);

            // Tạo URL trả về cho Frontend (Dựa trên WebMvcConfig ánh xạ /uploads/**)
            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            String fileUrl = baseUrl + "/uploads/videos/" + newFilename;

            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi khi upload video: " + e.getMessage()));
        }
    }

    @PostMapping("/file")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Lấy đường dẫn tuyệt đối tới thư mục src/main/resources/static/uploads/files/
            String uploadDir = new File("src/main/resources/static/uploads/files/").getAbsolutePath();
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs(); // Tự động tạo thư mục nếu chưa tồn tại
            }

            // Tạo tên file ngẫu nhiên (UUID) để không bị trùng lặp tên
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString() + extension;

            // Lưu file vào ổ cứng
            File serverFile = new File(dir, newFilename);
            file.transferTo(serverFile);

            // Tạo URL trả về cho Frontend (Dựa trên WebMvcConfig ánh xạ /uploads/**)
            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            String fileUrl = baseUrl + "/uploads/files/" + newFilename;

            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi khi upload tài liệu/ảnh: " + e.getMessage()));
        }
    }
}
