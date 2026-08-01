package backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    public String uploadVideo(MultipartFile file) throws IOException {
        String uniqueFilename = UUID.randomUUID().toString();
        
        // Tạo file tạm để tránh lỗi tràn RAM (OutOfMemoryError) với video lớn
        java.io.File tempFile = java.io.File.createTempFile("video-", ".tmp");
        file.transferTo(tempFile);
        
        try {
            Map uploadResult = cloudinary.uploader().uploadLarge(tempFile, ObjectUtils.asMap(
                    "resource_type", "video",
                    "public_id", "videos/" + uniqueFilename
            ));
            return uploadResult.get("secure_url").toString();
        } finally {
            tempFile.delete(); // Luôn xóa file tạm sau khi upload xong
        }
    }

    public String uploadFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        // Thêm extension vào UUID để Cloudinary URL có định dạng file rõ ràng
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        Map uploadResult = cloudinary.uploader().uploadLarge(file.getBytes(), ObjectUtils.asMap(
                "resource_type", "auto",
                "public_id", "materials/" + uniqueFilename
        ));
        return uploadResult.get("secure_url").toString();
    }
}
