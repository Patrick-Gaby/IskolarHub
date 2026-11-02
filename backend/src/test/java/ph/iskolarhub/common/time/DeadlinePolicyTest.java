package ph.iskolarhub.common.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DeadlinePolicyTest {

    @Test
    void businessZoneIsAsiaManila() {
        assertThat(DeadlinePolicy.BUSINESS_ZONE.getId()).isEqualTo("Asia/Manila");
    }

    @Test
    void manilaIsAlwaysUtcPlusEight() {
        assertThat(DeadlinePolicy.BUSINESS_ZONE.getRules().getOffset(Instant.parse("2026-01-15T00:00:00Z")))
                .isEqualTo(ZoneOffset.ofHours(8));
        assertThat(DeadlinePolicy.BUSINESS_ZONE.getRules().getOffset(Instant.parse("2026-07-15T00:00:00Z")))
                .isEqualTo(ZoneOffset.ofHours(8));
    }

    @Test
    void endOfDayIsEightHoursBeforeUtcMidnight() {
        assertThat(DeadlinePolicy.endOfDay(LocalDate.of(2026, 6, 15)))
                .isEqualTo(Instant.parse("2026-06-15T15:59:59.999999Z"));
    }

    @Test
    void endOfDayAtTheEndOfTheUtcYear() {
        assertThat(DeadlinePolicy.endOfDay(LocalDate.of(2026, 12, 31)))
                .isEqualTo(Instant.parse("2026-12-31T15:59:59.999999Z"));
    }

    @Test
    void endOfDayAtTheStartOfTheUtcYear() {
        assertThat(DeadlinePolicy.endOfDay(LocalDate.of(2026, 1, 1)))
                .isEqualTo(Instant.parse("2026-01-01T15:59:59.999999Z"));
    }

    @Test
    void instantJustBeforeManilaMidnightIsStillTheSameManilaDate() {
        assertThat(DeadlinePolicy.businessDateOf(Instant.parse("2026-12-31T15:59:59.999999Z")))
                .isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    void instantAtManilaMidnightIsTheNextManilaDate() {
        assertThat(DeadlinePolicy.businessDateOf(Instant.parse("2026-12-31T16:00:00Z")))
                .isEqualTo(LocalDate.of(2027, 1, 1));
    }

    @Test
    void lateUtcEveningIsAlreadyTomorrowInManila() {
        // 23:30 UTC on 14 June is 07:30 on 15 June in Manila.
        assertThat(DeadlinePolicy.businessDateOf(Instant.parse("2026-06-14T23:30:00Z")))
                .isEqualTo(LocalDate.of(2026, 6, 15));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-01-01", "2026-12-31", "2028-02-29", "2027-03-01", "2030-07-04"})
    void endOfDayRoundTripsToTheSameManilaDate(String iso) {
        LocalDate date = LocalDate.parse(iso);
        assertThat(DeadlinePolicy.businessDateOf(DeadlinePolicy.endOfDay(date))).isEqualTo(date);
    }

    @Test
    void endOfDayHasMicrosecondPrecisionSoPostgresDoesNotRoundIntoTheNextDay() {
        Instant end = DeadlinePolicy.endOfDay(LocalDate.of(2026, 5, 5));
        assertThat(end.getNano() % 1000).isZero();
        assertThat(end.getNano()).isEqualTo(999_999_000);
    }
}
