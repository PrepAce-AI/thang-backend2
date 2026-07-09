package backend.exceptions;

public class GeminiException extends RuntimeException{
    private final int statusCode;

    public GeminiException(int statusCode) {
        this.statusCode = statusCode;
    }

    public GeminiException( int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
