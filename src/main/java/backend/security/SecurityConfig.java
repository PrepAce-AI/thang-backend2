package backend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // Cho phép Request dò đường OPTIONS
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/word-media/**").permitAll()  // ảnh công thức từ Word import
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/courses/delete-seed").permitAll()
                        .requestMatchers(
                                "/api/auth/**",
//                                "/api/entry-test",
//                                "/api/entry-test/ping",
//                                "/api/entry-test/course/**",
//                                "/api/entry-test/submit",
//                                "/api/entry-test/history",
                                "/api/ai/**",
                                "/uploads/**",
                                "/api/webhook/**"
                        ).permitAll()

                        // 🔥 ĐÃ THÊM: Ép endpoint tải tài liệu phải .authenticated() (Bắt buộc mang theo Token)
                        .requestMatchers("/api/materials/**").authenticated()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users/instructor/**"
                        ).permitAll()
                        // Đảm bảo chỉ những ai đã đăng nhập mới được thao tác xây dựng đề cương khóa học
                        .requestMatchers("/api/outlines/**").authenticated()
                        .requestMatchers("/api/payments/**").authenticated()
                        .requestMatchers("/api/entry-test/**").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/courses", "/api/courses/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )
                // 2. BẢO SPRING CHẠY QUA BỘ LỌC JWT TRƯỚC TIÊN
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Autowired
    private JwtFilter jwtFilter;
}