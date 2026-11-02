package ph.iskolarhub.common.exception;

import org.springframework.http.HttpStatus;

/** Business-level failure with a stable, client-facing error code. The message must be safe to show. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
