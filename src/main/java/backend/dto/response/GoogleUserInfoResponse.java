package backend.dto.response;

public record GoogleUserInfoResponse(
        String sub,
        String name,
        String email,
        String picture
        ) {
}
