package backend.security;

import backend.service.JwtService;
import backend.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private backend.repository.UserRepository userRepository;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String jwt = null;

        // 1. Đọc token từ Cookie accessToken
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    break;
                }
            }
        }

        // 2. Dự phòng: Nếu không có Cookie, đọc từ Header Authorization
        if (jwt == null) {
            final String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwt = authHeader.substring(7);
            }
        }

        // Nếu không tìm thấy token, cho request đi qua (Spring Security sẽ chặn nếu endpoint yêu cầu auth)
        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 3. Kiểm tra xem token có nằm trong Blacklist (đã đăng xuất) hay không
            if (tokenBlacklistService.isBlacklisted(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 4. Giải mã token để lấy email
            String userEmail = jwtService.extractUsername(jwt);

            // 5. Nếu email hợp lệ và chưa được cấu hình Authentication
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                var userEntity = userRepository.findByEmail(userEmail).orElse(null);

                if (userEntity != null) {
                    // 6. Kiểm tra tokenVersion để chống việc dùng lại token cũ sau khi đổi/quên mật khẩu
                    Integer tokenVersionInJwt = jwtService.extractTokenVersion(jwt);
                    if (tokenVersionInJwt != null && tokenVersionInJwt == userEntity.getTokenVersion()) {

                        int roleId = userEntity.getRoleId();
                        String roleName;

                        if (roleId == 1) {
                            roleName = "ADMIN";
                        } else if (roleId == 2) {
                            roleName = "TEACHER";
                        } else {
                            roleName = "STUDENT";
                        }

                        java.util.List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
                        authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + roleName));

                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userEmail,
                                null,
                                authorities
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        // Đóng mộc xác thực
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Lỗi xác thực Token: " + e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}