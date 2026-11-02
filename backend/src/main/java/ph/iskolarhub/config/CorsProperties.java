package ph.iskolarhub.config;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Browser origins allowed to call the API. Bound from {@code iskolarhub.cors.allowed-origins}. */
@Validated
@ConfigurationProperties(prefix = "iskolarhub.cors")
public record CorsProperties(@NotEmpty List<String> allowedOrigins) {
}
