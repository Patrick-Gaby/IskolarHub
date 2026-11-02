package ph.iskolarhub.scholarship.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ph.iskolarhub.support.TestData;

class ScholarshipTest {

    @Test
    void newScholarshipIsADraftWithoutDeadline() {
        Scholarship s = TestData.newScholarship("sample-one");
        assertThat(s.getStatus()).isEqualTo(ScholarshipStatus.DRAFT);
        assertThat(s.getDeadlineAt()).isNull();
        assertThat(s.getDeadlineDate()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Upper-Case", "has space", "double--hyphen", "-leading", "trailing-", "under_score"})
    void invalidSlugsAreRejected(String slug) {
        assertThatThrownBy(() -> new Scholarship(slug, "Name", "Provider", ScholarshipDataSource.DEMO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dateOnlyDeadlineStoresTheDateAndTheEndOfThatDayInManila() {
        Scholarship s = TestData.newScholarship("sample-two");
        s.useDateOnlyDeadline(LocalDate.of(2026, 12, 31));
        assertThat(s.getDeadlineDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(s.getDeadlineAt()).isEqualTo(Instant.parse("2026-12-31T15:59:59.999999Z"));
    }

    @Test
    void exactDeadlineClearsTheCalendarDate() {
        Scholarship s = TestData.newScholarship("sample-three");
        s.useDateOnlyDeadline(LocalDate.of(2026, 5, 1));
        s.useExactDeadline(Instant.parse("2026-05-01T04:00:00Z"));
        assertThat(s.getDeadlineDate()).isNull();
        assertThat(s.getDeadlineAt()).isEqualTo(Instant.parse("2026-05-01T04:00:00Z"));
    }

    @Test
    void clearDeadlineRemovesBothValues() {
        Scholarship s = TestData.newScholarship("sample-four");
        s.useDateOnlyDeadline(LocalDate.of(2026, 5, 1));
        s.clearDeadline();
        assertThat(s.getDeadlineAt()).isNull();
        assertThat(s.getDeadlineDate()).isNull();
    }

    @Test
    void infoUrlMustBeHttpOrHttps() {
        Scholarship s = TestData.newScholarship("sample-five");
        s.updateDetails("Name", "Provider", null, null, "https://example.org/info");
        assertThat(s.getInfoUrl()).isEqualTo("https://example.org/info");
        assertThatThrownBy(() -> s.updateDetails("Name", "Provider", null, null, "ftp://example.org"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> s.updateDetails("Name", "Provider", null, null, "javascript:alert(1)"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blankOptionalDetailsBecomeNull() {
        Scholarship s = TestData.newScholarship("sample-six");
        s.updateDetails("Name", "Provider", "  ", "", "   ");
        assertThat(s.getDescription()).isNull();
        assertThat(s.getBenefits()).isNull();
        assertThat(s.getInfoUrl()).isNull();
    }

    @Test
    void publishAndArchiveChangeStatus() {
        Scholarship s = TestData.newScholarship("sample-seven");
        s.publish();
        assertThat(s.getStatus()).isEqualTo(ScholarshipStatus.PUBLISHED);
        s.archive();
        assertThat(s.getStatus()).isEqualTo(ScholarshipStatus.ARCHIVED);
    }
}
