package backend.client;
import backend.dto.response.GoogleExchangeTokenResponse;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "https://oauth2.googleapis.com")
public interface GoogleTokenClient {
    @PostExchange(value = "/token", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    GoogleExchangeTokenResponse exchangeToken(@RequestBody MultiValueMap<String, String> fromData);
}
