package backend.controller;
import backend.dto.request.*;
import backend.service.JwtService;
import backend.entity.User;
import backend.repository.UserRepository;
import backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {
    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtService jwtService;
    public UserController(UserRepository userRepository, UserService userService, JwtService jwtService){
        this.userRepository = userRepository;
        this.userService = userService;
        this.jwtService = jwtService;
    }





    //LOGIN - REGISTER - GOOGLE ======================================================================
    @PostMapping("/register")
    public User register(@RequestBody RegisterRequest request){
        return userService.register(request);
    }

    @GetMapping("/users")
    public List<User> getAllUsers(){
        return userRepository.findAll();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            jakarta.servlet.http.HttpServletResponse response
    ){
        Map<String, Object> authData = userService.login(
                request.getEmail(),
                request.getPassword(),
                request.isRememberMe()
        );

        String accessToken = (String) authData.get("accessToken");
        String refreshToken = (String) authData.get("refreshToken");
        User user = (User) authData.get("user");

        // Thiết lập Cookie cho Access Token (BR-UC01-02)
        jakarta.servlet.http.Cookie accessCookie = new jakarta.servlet.http.Cookie("accessToken", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(15 * 60); // 15 phút
        response.addCookie(accessCookie);

        // Thiết lập Cookie cho Refresh Token (BR-UC01-02)
        jakarta.servlet.http.Cookie refreshCookie = new jakarta.servlet.http.Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(request.isRememberMe() ? 30 * 24 * 60 * 60 : 7 * 24 * 60 * 60); // 30 ngày hoặc 7 ngày
        response.addCookie(refreshCookie);

        return ResponseEntity.ok(Map.of("user", user));
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleAuth(
            @RequestBody Map<String, String> body,
            jakarta.servlet.http.HttpServletResponse response
    ) {
        String credential = body.get("credential");
        Map<String, Object> authData = userService.googleAuth(credential);

        String accessToken = (String) authData.get("accessToken");
        String refreshToken = (String) authData.get("refreshToken");
        User user = (User) authData.get("user");

        jakarta.servlet.http.Cookie accessCookie = new jakarta.servlet.http.Cookie("accessToken", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(15 * 60);
        response.addCookie(accessCookie);

        jakarta.servlet.http.Cookie refreshCookie = new jakarta.servlet.http.Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(30 * 24 * 60 * 60); // Google SSO mặc định 30 ngày
        response.addCookie(refreshCookie);

        return ResponseEntity.ok(Map.of("user", user));
    }
    //================================================================================================

    //AVATAR ======================================================================
    @GetMapping("/profile")
    public ResponseEntity<User> profile(@RequestHeader(value = "Authorization", required = false) String token, jakarta.servlet.http.HttpServletRequest request) {
        String jwt = null;
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    break;
                }
            }
        }
        if (jwt == null && token != null) {
            jwt = token.replace("Bearer ", "");
        }
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        String email = jwtService.extractUsername(jwt);
        return ResponseEntity.ok(userService.getByEmail(email));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getProfile(
            @RequestHeader(value = "Authorization", required = false) String token,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        String jwt = null;
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    break;
                }
            }
        }
        if (jwt == null && token != null) {
            jwt = token;
        }
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userService.getProfile(jwt);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/avatar")
    public ResponseEntity<?> updateAvatar(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, String> body,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        String jwt = null;
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    break;
                }
            }
        }
        if (jwt == null && token != null) {
            jwt = token.replace("Bearer ", "");
        }
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        String email = jwtService.extractUsername(jwt);

        User user = userRepository.findByEmail(email)
                .orElseThrow();

        user.setAvatarUrl(body.get("avatarUrl"));
        userRepository.save(user);

        return ResponseEntity.ok("OK");
    }
    //============================================================================

    //AVATAR ======================================================================
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody UpdateProfileRequest req,
            jakarta.servlet.http.HttpServletRequest request
    ){
        String jwt = null;
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    break;
                }
            }
        }
        if (jwt == null && token != null) {
            jwt = token.replace("Bearer ", "");
        }
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        String email = jwtService.extractUsername(jwt);

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User Not Found !!!"));

        user.setFullName(req.getFullName());
        // Kiểm tra format sđt Việt Nam trước khi lưu
        if (req.getPhone() != null && !req.getPhone().trim().isEmpty()) {
            if (!req.getPhone().matches("^(0[3|5|7|8|9])+([0-9]{8})$")) {
                throw new RuntimeException("Số điện thoại không hợp lệ");
            }
        }
        user.setPhone(req.getPhone());
        user.setSchool(req.getSchool());
        user.setBio(req.getBio());

        userRepository.save(user);

        return ResponseEntity.ok(user);
    }
    //============================================================================

    //FORGET - RESET - CHANGE PASSWORD - LOGOUT ======================================================================
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest req,
            @RequestHeader(value = "Authorization", required = false) String token,
            jakarta.servlet.http.HttpServletRequest request
    ){
        String jwt = null;
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    break;
                }
            }
        }
        if (jwt == null && token != null) {
            jwt = token.substring(7);
        }
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        String email = jwtService.extractUsername(jwt);
        userService.changePassword(email, req);

        return ResponseEntity.ok("Password Change Successfully!!!");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest req){
        userService.forgotPassword(req.getEmail());
        // Trả về thông điệp chung tránh dò quét tài khoản (UC-05.1)
        return ResponseEntity.ok(Map.of("message", "Nếu email tồn tại trong hệ thống, chúng tôi đã gửi link khôi phục mật khẩu."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req){
        userService.resetPassword(req.getToken(), req.getNewPassword());
        return ResponseEntity.ok("Password Has Been Updated !!!");
    }
    //========================================================================================================

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(
            @RequestBody VerifyEmailRequest req,
            jakarta.servlet.http.HttpServletResponse response
    ){
        User user = userService.verifyEmail(req);

        // Đăng nhập tự động ngay sau khi xác thực thành công (UC-03)
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, false);

        jakarta.servlet.http.Cookie accessCookie = new jakarta.servlet.http.Cookie("accessToken", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(15 * 60);
        response.addCookie(accessCookie);

        jakarta.servlet.http.Cookie refreshCookie = new jakarta.servlet.http.Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(refreshCookie);

        return ResponseEntity.ok(Map.of("message", "Verify Successfully", "user", user));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }
        userService.resendOtp(email);
        return ResponseEntity.ok(Map.of("message", "Mã OTP đã được gửi lại thành công."));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response
    ) {
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Missing refresh token"));
        }

        try {
            String email = jwtService.extractUsername(refreshToken);
            io.jsonwebtoken.Claims claims = jwtService.extractAllClaims(refreshToken);
            String type = claims.get("type", String.class);
            if (!"REFRESH_TOKEN".equals(type)) {
                throw new RuntimeException("Invalid token type");
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Integer tokenVersion = jwtService.extractTokenVersion(refreshToken);
            if (tokenVersion == null || tokenVersion != user.getTokenVersion()) {
                throw new RuntimeException("Token version mismatch");
            }

            // Sinh Access Token mới
            String newAccessToken = jwtService.generateAccessToken(user);

            // Ghi đè Cookie
            jakarta.servlet.http.Cookie accessCookie = new jakarta.servlet.http.Cookie("accessToken", newAccessToken);
            accessCookie.setHttpOnly(true);
            accessCookie.setPath("/");
            accessCookie.setMaxAge(15 * 60);
            response.addCookie(accessCookie);

            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            // Hủy Refresh Token nếu lỗi
            jakarta.servlet.http.Cookie refreshCookie = new jakarta.servlet.http.Cookie("refreshToken", null);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(0);
            response.addCookie(refreshCookie);
            return ResponseEntity.status(401).body(Map.of("message", "Invalid refresh token: " + e.getMessage()));
        }
    }

    //========================================================================================================
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response
    ){
        String token = null;
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        if (token == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        userService.logout(token);

        // Xóa Cookies ở trình duyệt (UC-02)
        jakarta.servlet.http.Cookie accessCookie = new jakarta.servlet.http.Cookie("accessToken", null);
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        response.addCookie(accessCookie);

        jakarta.servlet.http.Cookie refreshCookie = new jakarta.servlet.http.Cookie("refreshToken", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);

        return ResponseEntity.ok(Map.of("message", "Logout Successfully"));
    }
}
