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
            @RequestBody LoginRequest request
    ){
        // Nhận Map từ Service chứa cả token và user
        Map<String, Object> authData = userService.login(
                request.getEmail(),
                request.getPassword()
        );

        // Trả về kèm HTTP 200 OK
        return ResponseEntity.ok(authData);
    }
    @PostMapping("/google")
    public ResponseEntity<?> googleAuth(@RequestBody Map<String, String> body) {

        String credential = body.get("credential");

        return ResponseEntity.ok(
                userService.googleAuth(credential)
        );
    }
    //================================================================================================






    //AVATAR ======================================================================
    @GetMapping("/profile")
    public ResponseEntity<User> profile(@RequestHeader("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        String email = jwtService.extractUsername(jwt);

        return ResponseEntity.ok(userService.getByEmail(email));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getProfile(
            @RequestHeader("Authorization") String token
    ) {
        User user = userService.getProfile(token);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/avatar")
    public ResponseEntity<?> updateAvatar(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> body
    ) {
        String jwt = token.replace("Bearer ", "");
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
    public ResponseEntity<?> updateProfile(@RequestHeader("Authorization") String token, @RequestBody UpdateProfileRequest req){
        String jwt = token.replace("Bearer ", "");
        String email = jwtService.extractUsername(jwt);

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User Not Found !!!"));

        user.setFullName(req.getFullName());
        user.setPhone(req.getPhone());
        user.setSchool(req.getSchool());
        user.setBio(req.getBio());

        userRepository.save(user);

        return ResponseEntity.ok(user);
    }
    //============================================================================




    //FORGET - RESET - CHANGE PASSWORD - LOGOUT ======================================================================

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest req, @RequestHeader("Authorization") String token){
        String email = jwtService.extractUsername(token.substring(7)); //Bearer e4 ==> Bo ky tu tu 0 den 6 (tong cong 7 ki tu)
        userService.changePassword(email, req);

        return ResponseEntity.ok("Password Change Successfully!!!");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest req){
        userService.forgotPassword(req.getEmail());
        return ResponseEntity.ok("Check Email Di");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req){
        userService.resetPassword(req.getToken(), req.getNewPassword());
        return ResponseEntity.ok("Password Has Been Updated !!!");
    }

    //========================================================================================================

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody VerifyEmailRequest req){
        String message = userService.verifyEmail(req);

        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(
            @RequestBody Map<String, String> body
    ) {

        String email = body.get("email");

        userService.resendOtp(email);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "OTP has been resent successfully"
                )
        );
    }

    //========================================================================================================
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String token){
        userService.logout(token);
        return ResponseEntity.ok(Map.of("message", "Logout Successfully"));
    }
}