//package backend.service;
//
//import backend.client.GoogleTokenClient;
//import backend.client.GoogleUserInfoClient;
//import backend.dto.response.GoogleExchangeTokenResponse;
//import backend.dto.response.GoogleUserInfoResponse;
//import backend.entity.User;
//import backend.repository.UserRepository;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//
//@Service
//@RequiredArgsConstructor
//public class OAuthService {
//    private final GoogleTokenClient googleTokenClient;
//    private final GoogleUserInfoClient googleUserInfoClient;
//    private final UserRepository userRepository;
//    private final JwtService jwtService;
//
//    @Value("${google.client-id}")
//    private String clientId;
//
//    @Value("${google.client-secret}")
//    private String clientSecret;
//
//    @Value("${google.redirect-url}")
//    private String redirectUrl;
//
//    public String loginWithGoogle(String code){
//        /*
//         * STEP 1:
//         * Exchange authorization_code -> access_token
//        */
//
//        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
//        formData.add("client_id", clientId);
//        formData.add("client_secret", clientSecret);
//        formData.add("code", code);
//        formData.add("grant_type", "authorization_code");
//        formData.add("redirect_url", redirectUrl);
//
//        GoogleExchangeTokenResponse tokenResponse = googleTokenClient.exchangeToken(formData);
//
//        /*
//         * STEP 2:
//         * Use access_token to get Google user info
//        */
//
//        GoogleUserInfoResponse userInfo = googleUserInfoClient.getUserInfo("Bearer " + tokenResponse.accessToken());
//
//        /*
//         * STEP 3:
//         * Check if user already exists
//        */
//        User user = userRepository.findByEmail(userInfo.email()).orElseGet(() -> {
//            User newUser = new User();
//            newUser.setFullName(userInfo.name());
//            newUser.setEmail(userInfo.email());
//
//            /*
//             * Google account
//             * no password needed
//            */
//            newUser.setPasswordHash(null);
//
//            /*
//             * Google email already verified
//            */
//            newUser.setVerified(true);
//
//            return userRepository.save(newUser);
//        });
//
//        /*
//         * STEP 4:
//         * Generate JWT of YOUR SYSTEM
//        */
//        return jwtService.generateToken(user.getEmail());
//    }
//}
