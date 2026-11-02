package ph.iskolarhub.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;

/** The single error body returned by every failing API call. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String code,
        String message,
        List<FieldIssue> fieldErrors,
        String requestId,
        Instant timestamp) {

    public record FieldIssue(String field, String message) {
    }

    public static ApiError of(String code, String message, List<FieldIssue> fieldErrors) {
        return new ApiError(code, message, fieldErrors, MDC.get(RequestIdFilter.MDC_KEY), Instant.now());
    }
}
