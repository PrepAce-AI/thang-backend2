package backend.service;

import backend.dto.request.ChangePasswordRequest;
import backend.dto.request.RegisterRequest;
import backend.dto.request.VerifyEmailRequest;
import backend.entity.User;
import backend.repository.UserRepository;

import java.util.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${google.client.id}")
    private String googleClientId;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, JwtService jwtService, EmailService emailService, TokenBlacklistService tokenBlacklistService){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    //Normal Register - ĐÃ NÂNG CẤP (UC-03)
    public User register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        // Kiểm tra định dạng số điện thoại Việt Nam
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            if (!request.getPhone().matches("^(0[3|5|7|8|9])+([0-9]{8})$")) {
                throw new RuntimeException("Số điện thoại không đúng định dạng Việt Nam.");
            }
        }

        // Kiểm tra độ mạnh mật khẩu (BR-UC03-03)
        String password = request.getPassword();
        if (password == null || !password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            throw new RuntimeException("Mật khẩu tối thiểu 8 ký tự, bao gồm cả chữ hoa, chữ thường và ít nhất 1 chữ số.");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        
        // HASH PASSWORD (Mã hóa với cost factor >= 12 cấu hình trong SecurityConfig)
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        
        // Cấu hình vai trò
        int roleId = 3; // Mặc định STUDENT
        String roleName = "STUDENT";
        String status = "PENDING";

        if ("TEACHER".equalsIgnoreCase(request.getRole())) {
            roleId = 2;
            roleName = "TEACHER";
            status = "PENDING_APPROVAL"; // Giáo viên cần Admin phê duyệt
        }

        user.setRoleId(roleId);
        user.setRoleName(roleName);
        user.setAccountStatus(status);
        user.setCreatedAt(new Date());
        user.setTokenVersion(1);
        user.setFailedAttempts(0);
        user.setOtpFailedAttempts(0);
        user.setOtpResendCount(0);

        String otp = generateOTP(); // OTP 6 chữ số
        user.setVerificationCode(otp);
        user.setVerificationExpiry(new Date(System.currentTimeMillis() + 10 * 60 * 1000)); // Hiệu lực 10 phút

        User savedUser = userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), otp);

        System.out.println("OTP Code: " + otp);
        return savedUser;
    }

    //Normal Login - ĐÃ NÂNG CẤP (UC-01)
    public Map<String, Object> login(String email, String password, boolean rememberMe) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        // Kiểm tra tài khoản bị khóa tạm thời (BR-UC01-01)
        if (user.getLockoutExpiry() != null && user.getLockoutExpiry().after(new Date())) {
            long minutesLeft = (user.getLockoutExpiry().getTime() - System.currentTimeMillis()) / 60000;
            throw new RuntimeException("Tài khoản đang bị khóa tạm thời. Vui lòng thử lại sau " + (minutesLeft > 0 ? minutesLeft : 1) + " phút.");
        }

        boolean isMatch = passwordEncoder.matches(password, user.getPasswordHash());

        if (!user.getAccountStatus().equals("ACTIVE") && !user.getAccountStatus().equals("PENDING_APPROVAL")) {
            throw new RuntimeException("Please verify your email first !!!");
        }

        if (!isMatch) {
            // Tăng số lần nhập sai
            int attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);
            if (attempts >= 5) {
                user.setLockoutExpiry(new Date(System.currentTimeMillis() + 15 * 60 * 1000)); // Khóa 15 phút
                userRepository.save(user);
                emailService.sendLockoutWarningEmail(user.getEmail());
                throw new RuntimeException("Tài khoản bị khóa tạm thời 15 phút do nhập sai mật khẩu quá 5 lần.");
            }
            userRepository.save(user);
            throw new RuntimeException("Mật khẩu không chính xác.");
        }

        // Đăng nhập thành công -> Reset bộ đếm sai
        user.setFailedAttempts(0);
        user.setLockoutExpiry(null);
        userRepository.save(user);

        // Tạo cặp token Access Token & Refresh Token (BR-UC01-02)
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, rememberMe);

        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("user", user);

        return response;
    }

    //GOOGLE LOGIN + REGISTER - ĐÃ NÂNG CẤP
    public Map<String, Object> googleAuth(String idTokenString) {
        try {
            System.out.println("GOOGLE ID TOKEN: " + idTokenString);

            GoogleIdTokenVerifier verifier =
                    new GoogleIdTokenVerifier.Builder(
                            GoogleNetHttpTransport.newTrustedTransport(),
                            JacksonFactory.getDefaultInstance()
                    )
                            .setAudience(Collections.singletonList(googleClientId))
                            .setIssuer("https://accounts.google.com")
                            .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new RuntimeException("Invalid Google Token");
            }
            GoogleIdToken.Payload payload = idToken.getPayload();

            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");

            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setEmail(email);
                        newUser.setFullName(name);
                        newUser.setAvatarUrl(picture);
                        newUser.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString())); // google user
                        newUser.setRoleId(3);
                        newUser.setRoleName("STUDENT");
                        newUser.setAccountStatus("ACTIVE");
                        newUser.setCreatedAt(new Date());
                        newUser.setTokenVersion(1);
                        newUser.setFailedAttempts(0);
                        newUser.setOtpFailedAttempts(0);
                        newUser.setOtpResendCount(0);
                        return userRepository.save(newUser);
                    });

            if (!"ACTIVE".equals(user.getAccountStatus())) {
                user.setAccountStatus("ACTIVE");
                userRepository.save(user);
            }

            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user, true); // Mặc định remember me cho Google

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("user", user);

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Google Auth Failed");
        }
    }

    //Logout - ĐÃ NÂNG CẤP (UC-02)
    public void logout(String token) {
        if (token == null) return;
        String jwt = token.replace("Bearer ", "");
        try {
            Date expiration = jwtService.extractExpiration(jwt);
            // Đưa token vào blacklist (sử dụng in-memory thread-safe)
            tokenBlacklistService.blacklistToken(jwt, expiration.getTime());
            System.out.println("Token blacklisted successfully");
        } catch (Exception e) {
            // Token hết hạn sẵn hoặc bị lỗi phân tích -> Bỏ qua
        }
    }

    //Change Password - ĐÃ NÂNG CẤP (UC-06)
    public void changePassword(String email, ChangePasswordRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User Not Found!!!"));

        // Kiểm tra khóa đổi mật khẩu tạm thời
        if (user.getChangePwLockoutExpiry() != null && user.getChangePwLockoutExpiry().after(new Date())) {
            long minutesLeft = (user.getChangePwLockoutExpiry().getTime() - System.currentTimeMillis()) / 60000;
            throw new RuntimeException("Tính năng đổi mật khẩu đang bị khóa tạm thời. Thử lại sau " + (minutesLeft > 0 ? minutesLeft : 1) + " phút.");
        }

        // Kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPasswordHash())) {
            int attempts = user.getChangePwFailedAttempts() + 1;
            user.setChangePwFailedAttempts(attempts);
            if (attempts >= 5) {
                user.setChangePwLockoutExpiry(new Date(System.currentTimeMillis() + 15 * 60 * 1000)); // Khóa 15 phút
                userRepository.save(user);
                throw new RuntimeException("Nhập sai mật khẩu hiện tại quá 5 lần. Tính năng đổi mật khẩu bị khóa 15 phút.");
            }
            userRepository.save(user);
            throw new RuntimeException("Mật khẩu hiện tại không chính xác.");
        }

        // Kiểm tra trùng mật khẩu cũ (BR-UC06-02)
        if (passwordEncoder.matches(req.getNewPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu mới không được trùng với mật khẩu hiện tại.");
        }

        // Kiểm tra độ mạnh mật khẩu mới
        if (req.getNewPassword() == null || !req.getNewPassword().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            throw new RuntimeException("Mật khẩu mới tối thiểu 8 ký tự, bao gồm cả chữ hoa, chữ thường và chữ số.");
        }

        // Thành công -> Đổi mật khẩu
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setChangePwFailedAttempts(0);
        user.setChangePwLockoutExpiry(null);
        userRepository.save(user);

        // Gửi email thông báo
        emailService.sendPasswordChangeNotification(user.getEmail());
    }

    //Forgot Password - ĐÃ NÂNG CẤP (UC-05)
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // Hiển thị thành công giả lập để tránh dò quét tài khoản (UC-05.1)
            System.out.println("Forgot password request: Email not found (silenced): " + email);
            return;
        }

        // Tạo Secure Token (UUID) lưu vào DB (BR-UC05-01)
        String resetUuid = UUID.randomUUID().toString();
        user.setResetToken(resetUuid);
        user.setResetTokenExpiry(new Date(System.currentTimeMillis() + 15 * 60 * 1000)); // Hạn 15 phút
        userRepository.save(user);

        String link = "http://localhost:5173/reset-password?token=" + resetUuid;
        emailService.sendOtp(email, link); // Gửi mail link reset
        System.out.println("RESET LINK: " + link);
    }

    //Reset Password - ĐÃ NÂNG CẤP (UC-05)
    public void resetPassword(String token, String newPassword) {
        // Tìm User theo resetToken dạng UUID trong DB (đảm bảo chỉ dùng 1 lần)
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Đường dẫn khôi phục mật khẩu không hợp lệ hoặc đã được sử dụng."));

        // Kiểm tra hết hạn token
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().before(new Date())) {
            throw new RuntimeException("Đường dẫn khôi phục mật khẩu đã hết hạn (15 phút). Vui lòng yêu cầu lại.");
        }

        // Kiểm tra độ mạnh mật khẩu mới
        if (newPassword == null || !newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            throw new RuntimeException("Mật khẩu mới tối thiểu 8 ký tự, bao gồm cả chữ hoa, chữ thường và chữ số.");
        }

        // Cập nhật mật khẩu mới
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        
        // Vô hiệu hóa toàn bộ Token cũ bằng cách tăng tokenVersion lên 1 đơn vị (BR-UC05-02)
        user.setTokenVersion(user.getTokenVersion() + 1);

        // Hủy liên kết reset token để không cho sử dụng lần thứ 2
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        // Gửi email xác nhận thành công
        emailService.sendResetConfirmationEmail(user.getEmail());
    }

    //-----------------------------------------------------------------------------------------------------------

    //GENERATE OTP
    private String generateOTP() {
        return String.valueOf((int)(Math.random() * 900000) + 100000);
    }

    //Gửi lại OTP (Đọc yêu cầu bổ sung UC-03)
    public void resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User Not Found !!!"));

        if ("ACTIVE".equals(user.getAccountStatus())) {
            throw new RuntimeException("Tài khoản đã kích hoạt.");
        }

        // Giới hạn tối đa 3 lần resend OTP (BR-UC03-02)
        if (user.getOtpResendCount() >= 3) {
            throw new RuntimeException("Bạn đã vượt quá giới hạn gửi lại mã OTP (tối đa 3 lần).");
        }

        String newOtp = generateOTP();
        user.setVerificationCode(newOtp);
        user.setVerificationExpiry(new Date(System.currentTimeMillis() + 10 * 60 * 1000)); // 10 phút
        user.setOtpResendCount(user.getOtpResendCount() + 1);
        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), newOtp);
        System.out.println("Gửi lại OTP: " + newOtp);
    }

    //VERIFY EMAIL - ĐÃ NÂNG CẤP (UC-03)
    public User verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User Not Found !!!"));

        if ("ACTIVE".equals(user.getAccountStatus())) {
            throw new RuntimeException("Account Already Verified");
        }

        if (user.getVerificationCode() == null) {
            throw new RuntimeException("OTP Not Found");
        }

        // Check OTP hết hạn
        if (user.getVerificationExpiry().before(new Date())) {
            throw new RuntimeException("Mã OTP đã hết hiệu lực. Vui lòng bấm gửi lại mã.");
        }

        // Check OTP nhập sai
        if (!user.getVerificationCode().equals(request.getOtp())) {
            int attempts = user.getOtpFailedAttempts() + 1;
            user.setOtpFailedAttempts(attempts);
            if (attempts >= 5) {
                // Hủy phiên đăng ký nếu sai quá 5 lần (BR-UC03-02)
                user.setVerificationCode(null);
                user.setVerificationExpiry(null);
                user.setOtpFailedAttempts(0);
                user.setOtpResendCount(0);
                userRepository.save(user);
                throw new RuntimeException("Nhập sai OTP quá 5 lần. Phiên đăng ký của bạn đã bị hủy bỏ. Vui lòng thực hiện đăng ký lại.");
            }
            userRepository.save(user);
            throw new RuntimeException("Mã OTP không chính xác. Bạn còn " + (5 - attempts) + " lần thử.");
        }

        // ACTIVE ACCOUNT
        user.setAccountStatus("ACTIVE");
        user.setVerificationCode(null);
        user.setVerificationExpiry(null);
        user.setOtpFailedAttempts(0);
        user.setOtpResendCount(0);

        return userRepository.save(user);
    }

    //-----------------------------------------------------------------------------------------------------------
                                                            //AVATAR
    //UPDATE AVATAR
    public void updateAvatar(String token, String avatarUrl){
        //Bo cai "Bearer "
        String jwt = token.replace("Bearer ", "");

        String email = jwtService.extractUsername(jwt);

        User user = userRepository.findByEmail(email).orElseThrow(() ->  new RuntimeException("User Not Found !!!"));
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
    }

    //GET BY EMAIL
    public User getByEmail(String mail){
        return userRepository.findByEmail(mail).orElseThrow(() -> new RuntimeException("User Not Found !!!"));
    }

    //GET PROFILE
    public User getProfile(String token){
        String jwt = token.replace("Bearer ", "");
        String email = jwtService.extractUsername(jwt);

        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User Not Found !!!"));
    }
}
