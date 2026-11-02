package ph.iskolarhub.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Test-only endpoints that trigger each error path. Lives in src/test; never shipped. */
@RestController
@RequestMapping("/test-probe")
public class ErrorProbeController {

    public record Payload(@NotBlank(message = "must not be blank") String name) {
    }

    @PostMapping("/body")
    public String body(@Valid @RequestBody Payload payload) {
        return payload.name();
    }

    @GetMapping("/boom")
    public String boom() {
        throw new IllegalStateException("secret internal detail jdbc:postgresql://db/prod");
    }

    @GetMapping("/api-error")
    public String apiError() {
        throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "That email is already registered.");
    }

    @GetMapping("/typed")
    public String typed(@RequestParam int n) {
        return "n=" + n;
    }
}
