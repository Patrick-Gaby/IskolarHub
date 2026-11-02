package ph.iskolarhub.common.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ph.iskolarhub.common.exception.ApiError.FieldIssue;

/**
 * Maps every failure to {@link ApiError}. Exception messages and stack traces are never sent to clients;
 * unexpected errors are logged server-side with the request id.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiError.of(ex.getCode(), ex.getMessage(), null));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldIssue> issues = ex.getConstraintViolations().stream()
                .map(v -> new FieldIssue(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiError.of("VALIDATION_FAILED", "One or more fields are invalid.", issues));
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of("UNAUTHENTICATED", "Authentication is required to access this resource.", null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of("FORBIDDEN", "You do not have permission to access this resource.", null));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("INTERNAL_ERROR", "An unexpected error occurred.", null));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldIssue> issues = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldIssue(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ApiError body = ApiError.of("VALIDATION_FAILED", "One or more fields are invalid.", issues);
        return ResponseEntity.status(status).headers(headers).body(body);
    }

    /** Fallback for every other Spring MVC exception (malformed JSON, 404, 405, 415, type mismatch, ...). */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        int value = statusCode.value();
        String code;
        String message;
        switch (value) {
            case 400 -> {
                code = "BAD_REQUEST";
                message = "The request is malformed or contains invalid values.";
            }
            case 404 -> {
                code = "NOT_FOUND";
                message = "The requested resource was not found.";
            }
            case 405 -> {
                code = "METHOD_NOT_ALLOWED";
                message = "This HTTP method is not supported for the resource.";
            }
            case 406 -> {
                code = "NOT_ACCEPTABLE";
                message = "The requested response format is not supported.";
            }
            case 415 -> {
                code = "UNSUPPORTED_MEDIA_TYPE";
                message = "The request content type is not supported.";
            }
            default -> {
                code = value >= 500 ? "INTERNAL_ERROR" : "REQUEST_FAILED";
                message = value >= 500 ? "An unexpected error occurred." : "The request could not be processed.";
            }
        }
        return ResponseEntity.status(statusCode).headers(headers).body(ApiError.of(code, message, null));
    }
}
