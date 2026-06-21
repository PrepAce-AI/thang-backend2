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
        //  HASH PASSWORD
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRoleId(3); // STUDENT default
        user.setAccountStatus("PENDING"); //Chua ACTIVE
        user.setCreatedAt(new Date());

        String otp = generateOTP(); //OTP
        user.setVerificationCode(otp);
        user.setVerificationExpiry(new Date(System.currentTimeMillis() + 5 * 60 * 1000));

        User savedUser = userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), otp);

        System.out.println("OTP Code: " +otp);

        return savedUser;
    }



    //Normal Login
    // Đổi kiểu trả về từ String thành Map<String, Object>
    public Map<String, Object> login(String email, String password){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        boolean isMatch = password.equals(user.getPasswordHash()) || passwordEncoder.matches(password, user.getPasswordHash());

        if (!user.getAccountStatus().equals("ACTIVE")){
            throw new RuntimeException("Please verify your email first !!!");
        }
        if (!isMatch) {
            throw new RuntimeException("Wrong password");
        }

        // Tạo token mã hóa
        String token = jwtService.generateToken(user.getEmail());

        // Gộp cả token và thông tin user trả về y hệt luồng Google Auth
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
            System.out.println("TOKEN RAW: " + idTokenString);
            System.out.println("TOKEN LENGTH: " + (idTokenString == null ? "NULL" : idTokenString.length()));

            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");

            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setEmail(email);
                        newUser.setFullName(name);
                        newUser.setAvatarUrl(picture);
                        newUser.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString())); // google user --“đánh dấu account này không dùng password thường”
                        newUser.setRoleId(3);
                        newUser.setAccountStatus("ACTIVE");
                        newUser.setCreatedAt(new Date());
                        return userRepository.save(newUser);
                    });

            user.setAccountStatus("ACTIVE");
            userRepository.save(user);

            String token = jwtService.generateToken(user);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", user);

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Google Auth Failed");
        }
    }

    //-----------------------------------------------------------------------------------------------------------

    //Change Password
    public void changePassword(String email, ChangePasswordRequest req){
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User Not Found!!!"));
        //Check Old Password
        if(!passwordEncoder.matches(req.getOldPassword(), user.getPasswordHash())){
            throw new RuntimeException("Old Password Is Incorrect!!!");
        }
        //Set new password
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));

        userRepository.save(user);
    }

    //Forgot Password
    public void forgotPassword(String email){
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Email Not Found"));
        String resetToken = jwtService.generateResetToken(email); //Cap ma token TAM THOI
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

    //-----------------------------------------------------------------------------------------------------------

    //GENERATE OTP
    private String generateOTP() {
        return String.valueOf((int)(Math.random() * 900000) + 100000);
    }

    //VERIFY EMAIL
    public String verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("User Not Found !!!")
                );
        // Check account already verified
        if ("ACTIVE".equals(user.getAccountStatus())) {
            throw new RuntimeException("Account Already Verified");
        }

        // Check OTP null
        if (user.getVerificationCode() == null) {
            throw new RuntimeException("OTP Not Found");
        }

        // Check OTP
        if (!user.getVerificationCode().equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        // Check OTP expired
        if (user.getVerificationExpiry().before(new Date())) {
            throw new RuntimeException("OTP Expired");
        }

        // ACTIVE ACCOUNT
        user.setAccountStatus("ACTIVE");

        // Clear OTP
        user.setVerificationCode(null);
        user.setVerificationExpiry(null);

        userRepository.save(user);

        return "Verify Successfully";
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
