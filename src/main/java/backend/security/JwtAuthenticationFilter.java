package backend.security;

import backend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    // 🔥 ĐÃ THÊM: Tiêm UserRepository vào để tìm kiếm User dưới DB bằng email
    @Autowired
    private backend.repository.UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 1. Nếu không có token, cho đi qua (Spring Security sẽ tự chặn lại sau vì thiếu quyền)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Tách lấy token (Bỏ 7 ký tự "Bearer ")
        jwt = authHeader.substring(7);

        try {
            // 3. Giải mã token để lấy email
            userEmail = jwtService.extractUsername(jwt);

            // 4. Nếu hợp lệ và chưa được phân quyền trong hệ thống
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 🔥 ĐÃ SỬA: Truy vấn DB và phân quyền động tinh gọn bằng cấu hình if-else chốt chặn STUDENT
                var userEntity = userRepository.findByEmail(userEmail).orElse(null);
                java.util.List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();

                if (userEntity != null) {
                    int roleId = userEntity.getRoleId();
                    String roleName;

                    if (roleId == 1) {
                        roleName = "ADMIN";
                    } else if (roleId == 2) {
                        roleName = "TEACHER";
                    } else {
                        roleName = "STUDENT"; // Thỏa mãn điều kiện Java, an toàn phòng thủ tuyệt đối
                    }

                    // Đóng mộc quyền chuẩn (Ví dụ: ROLE_STUDENT)
                    authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + roleName));
                }

                System.out.println("JWT EMAIL = " + userEmail);
                System.out.println("ROLE_ID = " + userEntity.getRoleId());
                System.out.println("AUTHORITIES = " + authorities);

                // Báo cho Spring Security biết user này là ai
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userEmail,
                        null,
                        authorities // 🔥 ĐÃ SỬA: Thay thế mảng rỗng bằng danh sách quyền động dịch từ DB
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Đóng mộc "Đã đăng nhập thành công"
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            System.out.println("Lỗi xác thực Token: " + e.getMessage());
        }

        // 5. Cho request đi tiếp vào Controller
        filterChain.doFilter(request, response);
    }
}