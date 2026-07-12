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
        // Chỉ dùng UUID để tránh URL quá dài vượt qua 255 ký tự của Database
        String uniqueFilename = UUID.randomUUID().toString();
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", "video",
                "public_id", "videos/" + uniqueFilename
        ));
        return uploadResult.get("secure_url").toString();
    }

    public String uploadFile(MultipartFile file) throws IOException {
        // Chỉ dùng UUID để tránh URL quá dài vượt qua 255 ký tự của Database
        String uniqueFilename = UUID.randomUUID().toString();
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", "auto",
                "public_id", "materials/" + uniqueFilename
        ));
        return uploadResult.get("secure_url").toString();
    }
}
