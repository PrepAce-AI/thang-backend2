package backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Configuration
public class PayOSConfig {

    @Value("${payos.client-id}")
    private String clientId;

    @Value("${payos.api-key}")
    private String apiKey;

    @Value("${payos.checksum-key}")
    private String checksumKey;

    @Bean
    public PayOS payOS() {
        PayOS payos = new PayOS(clientId, apiKey, checksumKey);
        try {
            // Dùng Reflection để can thiệp vào ObjectMapper ẩn bên trong PayOS SDK
            // để tắt tính năng báo lỗi khi có field mới (ví dụ: expiredAt)
            for (java.lang.reflect.Field field : payos.getClass().getDeclaredFields()) {
                if (field.getType().getName().equals("com.fasterxml.jackson.databind.ObjectMapper")) {
                    field.setAccessible(true);
                    com.fasterxml.jackson.databind.ObjectMapper mapper =
                            (com.fasterxml.jackson.databind.ObjectMapper) field.get(payos);
                    if (mapper != null) {
                        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    }
                }
            }
        } catch (Exception e) {
            // Bỏ qua nếu không can thiệp được
        }
        return payos;
    }
}