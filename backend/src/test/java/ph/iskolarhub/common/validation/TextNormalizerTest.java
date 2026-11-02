package ph.iskolarhub.common.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TextNormalizerTest {

    @Test
    void emailIsTrimmedAndLowerCased() {
        assertThat(TextNormalizer.email("  Student@Example.COM \t")).isEqualTo("student@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "no-at-sign", "@example.com"})
    void emailWithoutALocalPartIsRejected(String raw) {
        assertThatThrownBy(() -> TextNormalizer.email(raw)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullEmailIsRejected() {
        assertThatThrownBy(() -> TextNormalizer.email(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emailLengthBoundaryIs254Characters() {
        String atLimit = "a".repeat(254 - "@x.ph".length()) + "@x.ph";
        assertThat(atLimit).hasSize(254);
        assertThat(TextNormalizer.email(atLimit)).isEqualTo(atLimit);
        assertThatThrownBy(() -> TextNormalizer.email("a" + atLimit)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void errorMessagesNeverEchoTheEmail() {
        assertThatThrownBy(() -> TextNormalizer.email("secret-address-without-at"))
                .hasMessageNotContaining("secret-address");
    }

    @Test
    void codesAreUpperCasedWithUnderscores() {
        assertThat(TextNormalizer.code("ncr", "region", 20)).isEqualTo("NCR");
        assertThat(TextNormalizer.code("  iv-a ", "region", 20)).isEqualTo("IV_A");
        assertThat(TextNormalizer.code("bs computer  science", "course", 50)).isEqualTo("BS_COMPUTER_SCIENCE");
    }

    @Test
    void blankOrNullCodeMeansNotProvided() {
        assertThat(TextNormalizer.code(null, "region", 20)).isNull();
        assertThat(TextNormalizer.code("   ", "region", 20)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"A", "BS@CS", "NCR!", "ÑCR"})
    void invalidCodesAreRejected(String raw) {
        assertThatThrownBy(() -> TextNormalizer.code(raw, "region", 20)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void codeLengthBoundary() {
        assertThat(TextNormalizer.code("A".repeat(20), "region", 20)).hasSize(20);
        assertThatThrownBy(() -> TextNormalizer.code("A".repeat(21), "region", 20))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requireTextTrimsAndEnforcesLimits() {
        assertThat(TextNormalizer.requireText("  hello ", "name", 10)).isEqualTo("hello");
        assertThatThrownBy(() -> TextNormalizer.requireText("  ", "name", 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TextNormalizer.requireText(null, "name", 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TextNormalizer.requireText("x".repeat(11), "name", 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void optionalTextTurnsBlankIntoNull() {
        assertThat(TextNormalizer.optionalText(null, "d", 10)).isNull();
        assertThat(TextNormalizer.optionalText("   ", "d", 10)).isNull();
        assertThat(TextNormalizer.optionalText(" ok ", "d", 10)).isEqualTo("ok");
    }
}
