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

    @Value("${google.client.id}")
    private String googleClientId;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, JwtService jwtService, EmailService emailService){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    //Normal Register
    public User register(RegisterRequest request){
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRoleId(3); // STUDENT default
        user.setAccountStatus("PENDING");
        user.setCreatedAt(new Date());

        String otp = generateOTP();
        user.setVerificationCode(otp);
        user.setVerificationExpiry(new Date(System.currentTimeMillis() + 5 * 60 * 1000));

        User savedUser = userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), otp);

        System.out.println("OTP Code: " + otp);

        return savedUser;
    }

    //Normal Login
    public Map<String, Object> login(String email, String password){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        // 🛡️ ĐÃ SỬA: Chặn tài khoản BANNED lên đầu quy trình xử lý đăng nhập
        if ("BANNED".equalsIgnoreCase(user.getAccountStatus())) {
            throw new RuntimeException("⚠️ Tài khoản của bạn đã bị khóa do vi phạm tiêu chuẩn cộng đồng! Vui lòng liên hệ support@learnifyfuture.com để được hỗ trợ.");
        }

        boolean isMatch = password.equals(user.getPasswordHash()) || passwordEncoder.matches(password, user.getPasswordHash());

        if (!isMatch) {
            throw new RuntimeException("Wrong password");
        }

        // Kiểm tra kích hoạt Email (Dành cho tài khoản mới đăng ký ở trạng thái PENDING)
        if (!"ACTIVE".equalsIgnoreCase(user.getAccountStatus())){
            throw new RuntimeException("Please verify your email first !!!");
        }

        String token = jwtService.generateToken(user);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", user);

        return response;
    }

    //GOOGLE LOGIN + REGISTER
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
            System.out.println("VERIFY RESULT: " + idToken);

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
                        newUser.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                        newUser.setRoleId(3);
                        newUser.setRoleName("STUDENT");
                        newUser.setAccountStatus("ACTIVE");
                        newUser.setCreatedAt(new Date());
                        return userRepository.save(newUser);
                    });

            // 🛡️ ĐÃ SỬA: Chặn không cho tài khoản Google tự động vượt ngục nếu trạng thái đang là BANNED
            if ("BANNED".equalsIgnoreCase(user.getAccountStatus())) {
                throw new RuntimeException("⚠️ Tài khoản Google này đã bị khóa đăng nhập trên hệ thống!");
            }

            user.setAccountStatus("ACTIVE");
            userRepository.save(user);

            String token = jwtService.generateToken(user);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", user);

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage() != null ? e.getMessage() : "Google Auth Failed");
        }
    }

    //Logout
    public void logout(String token){
        String jwt = token.replace("Bearer ", "");
        String email = jwtService.extractUsername(jwt);

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User Not Found"));
        System.out.println(user.getEmail() + " logged out");
    }

    //Change Password
    public void changePassword(String email, ChangePasswordRequest req){
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User Not Found!!!"));
        if(!passwordEncoder.matches(req.getOldPassword(), user.getPasswordHash())){
            throw new RuntimeException("Old Password Is Incorrect!!!");
        }
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    //Forgot Password
    public void forgotPassword(String email){
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Email Not Found"));
        String resetToken = jwtService.generateResetToken(email);
        String link = "http://localhost:5173/reset-password?token=" + resetToken;

        emailService.sendOtp(email, link);
        System.out.println("RESET LINK: " + link);
    }

    //Reset Password
    public void resetPassword(String token, String newPassword){
        Claims claims = (Claims) Jwts.parserBuilder().setSigningKey(jwtService.getSignKey()).build().parseClaimsJws(token).getBody();
        String email = claims.getSubject();
        String type = (String) claims.get("type", String.class);

        if (!"RESET_PASSWORD".equals(type)){
            throw new RuntimeException("Invalid Token Type");
        }

        if (claims.getExpiration().before(new Date())){
            throw new RuntimeException("Token Het Han !!!");
        }

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User Not Found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private String generateOTP() {
        return String.valueOf((int)(Math.random() * 900000) + 100000);
    }

    //VERIFY EMAIL
    public String verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User Not Found !!!"));

        // 🛡️ ĐÃ SỬA: Nếu trạng thái hiện tại đang bị Admin khóa (BANNED), chặn ngay không cho phép verify đổi lại trạng thái bừa bãi
        if ("BANNED".equalsIgnoreCase(user.getAccountStatus())) {
            throw new RuntimeException("Tài khoản đã bị khóa vĩnh viễn do vi phạm chính sách, không thể xác thực Email.");
        }

        if ("ACTIVE".equals(user.getAccountStatus())) {
            throw new RuntimeException("Account Already Verified");
        }

        if (user.getVerificationCode() == null) {
            throw new RuntimeException("OTP Not Found");
        }

        if (!user.getVerificationCode().equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        if (user.getVerificationExpiry().before(new Date())) {
            throw new RuntimeException("OTP Expired");
        }

        user.setAccountStatus("ACTIVE");
        user.setVerificationCode(null);
        user.setVerificationExpiry(null);

        userRepository.save(user);

        return "Verify Successfully";
    }

    //ResendOTP
    public void resendOtp(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Email not found")
                );
        // Nếu đã verify rồi thì không gửi nữa
        if ("ACTIVE".equalsIgnoreCase(user.getAccountStatus())) {
            throw new RuntimeException(
                    "Account already verified"
            );
        }
        String otp = generateOTP();
        user.setVerificationCode(otp);
        user.setVerificationExpiry(
                new Date(
                        System.currentTimeMillis()
                                + 5 * 60 * 1000
                )
        );
        userRepository.save(user);
        emailService.sendVerificationEmail(
                email,
                otp
        );
        System.out.println(
                "RESEND OTP: " + otp
        );
    }

    //UPDATE AVATAR
    public void updateAvatar(String token, String avatarUrl){
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