package backend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {
    // Lưu trữ token bị thu hồi: Key là token, Value là thời điểm hết hạn (milliseconds)
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    /**
     * Thêm token vào danh sách đen.
     * @param token Token cần thu hồi
     * @param expirationTimeMs Thời điểm hết hạn của token tính bằng Epoch Milliseconds
     */
    public void blacklistToken(String token, long expirationTimeMs) {
        if (token != null) {
            blacklist.put(token, expirationTimeMs);
        }
    }

    /**
     * Kiểm tra xem token đã bị thu hồi chưa.
     * @param token Token cần kiểm tra
     * @return true nếu bị thu hồi, false nếu còn hiệu lực
     */
    public boolean isBlacklisted(String token) {
        if (token == null) {
            return true;
        }
        Long expiry = blacklist.get(token);
        if (expiry == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiry) {
            blacklist.remove(token); // Tự động xóa nếu đã quá hạn
            return false;
        }
        return true;
    }

    /**
     * Tự động dọn dẹp các token đã hết hạn trong bộ nhớ đệm mỗi 10 phút.
     */
    @Scheduled(fixedDelay = 600000)
    public void cleanExpiredTokens() {
        long now = System.currentTimeMillis();
        blacklist.entrySet().removeIf(entry -> now > entry.getValue());
    }
}
