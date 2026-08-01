package backend.controller;

import backend.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/upload")
public class UploadController {

    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping("/video")
    public ResponseEntity<?> uploadVideo(@RequestParam("file") MultipartFile file) {
        try {
            // Giới hạn Cloudinary Free là 100MB. Nếu file > 90MB, tự động chuyển sang lưu ở Local Storage.
            long fileSizeInMB = file.getSize() / (1024 * 1024);
            if (fileSizeInMB > 90) {
                String uploadDir = new java.io.File("src/main/resources/static/uploads/videos/").getAbsolutePath();
                java.io.File directory = new java.io.File(uploadDir);
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                
                String filename = java.util.UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                java.io.File serverFile = new java.io.File(directory.getAbsolutePath() + java.io.File.separator + filename);
                file.transferTo(serverFile);
                
                String fileUrl = "http://localhost:8080/uploads/videos/" + filename;
                return ResponseEntity.ok(Map.of("url", fileUrl));
            } else {
                // Video nhỏ gọn (<90MB) thì dùng Cloudinary
                String fileUrl = cloudinaryService.uploadVideo(file);
                return ResponseEntity.ok(Map.of("url", fileUrl));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi khi upload video lên Cloudinary: " + e.getMessage()));
        }
    }

    @PostMapping("/file")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Sử dụng Cloudinary để upload tài liệu/ảnh (Đảm bảo an toàn, bảo mật và chia sẻ dễ dàng)
            String fileUrl = cloudinaryService.uploadFile(file);
            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi khi upload tài liệu/ảnh lên Cloudinary: " + e.getMessage()));
        }
    }
}
