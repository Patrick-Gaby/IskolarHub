package ph.iskolarhub.common.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Web-layer slice: no database and no Docker needed. */
@WebMvcTest(ErrorProbeController.class)
@ActiveProfiles("test")
@Import({GlobalExceptionHandler.class, RequestIdFilter.class})
class ErrorHandlingTest {

    @TestConfiguration
    static class OpenSecurity {
        @Bean
        SecurityFilterChain open(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(a -> a.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired
    MockMvc mvc;

    @Test
    void validationFailureListsFieldsWithStableCode() throws Exception {
        mvc.perform(post("/test-probe/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        mvc.perform(post("/test-probe/body").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void typeMismatchIsBadRequest() throws Exception {
        mvc.perform(get("/test-probe/typed").param("n", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void unknownPathIsNotFound() throws Exception {
        mvc.perform(get("/test-probe/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void wrongMethodIsMethodNotAllowed() throws Exception {
        mvc.perform(post("/test-probe/boom"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void apiExceptionKeepsItsOwnStatusAndCode() throws Exception {
        mvc.perform(get("/test-probe/api-error"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"))
                .andExpect(jsonPath("$.message").value("That email is already registered."));
    }

    @Test
    void unexpectedExceptionNeverLeaksInternals() throws Exception {
        mvc.perform(get("/test-probe/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred."))
                .andExpect(content().string(not(containsString("secret internal detail"))))
                .andExpect(content().string(not(containsString("jdbc:"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void wellFormedRequestIdIsEchoedAndAppearsInErrorBody() throws Exception {
        mvc.perform(get("/test-probe/api-error").header("X-Request-Id", "abc-12345678"))
                .andExpect(header().string("X-Request-Id", "abc-12345678"))
                .andExpect(jsonPath("$.requestId").value("abc-12345678"));
    }

    @Test
    void malformedRequestIdIsReplacedWithGeneratedOne() throws Exception {
        mvc.perform(get("/test-probe/api-error").header("X-Request-Id", "bad id with spaces!"))
                .andExpect(header().string("X-Request-Id", not("bad id with spaces!")))
                .andExpect(header().exists("X-Request-Id"));
    }

    private static org.springframework.test.web.servlet.result.ContentResultMatchers content() {
        return org.springframework.test.web.servlet.result.MockMvcResultMatchers.content();
    }
}
