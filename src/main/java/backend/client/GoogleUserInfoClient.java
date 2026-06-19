//package backend.client;
//import backend.dto.response.GoogleExchangeTokenResponse;
//import backend.dto.response.GoogleUserInfoResponse;
//import org.springframework.web.bind.annotation.RequestHeader;
//import org.springframework.web.service.annotation.GetExchange;
//import org.springframework.web.service.annotation.HttpExchange;
//
//@HttpExchange(url = "https://openidconnect.googleapis.com")
//public interface GoogleUserInfoClient {
//    @GetExchange("/v1/userinfo")
//    GoogleUserInfoResponse getUserInfo(@RequestHeader("Authorization") String authorization);
//}
