package ph.iskolarhub.eligibility.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ph.iskolarhub.scholarship.entity.RuleType;

class RuleDefinitionTest {

    @Test
    void copiesTopLevelConfigAndIsUnmodifiable() {
        Map<String, Object> source = new HashMap<>();
        source.put("min_year", 1);
        RuleDefinition rule = new RuleDefinition(RuleType.YEAR_LEVEL, source);

        source.put("max_year", 2);

        assertThat(rule.config()).containsOnlyKeys("min_year");
        assertThatThrownBy(() -> rule.config().put("x", 1)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void deepCopiesNestedListsAndMaps() {
        List<Object> codes = new ArrayList<>(List.of("NCR", "IV_A"));
        Map<String, Object> nested = new HashMap<>(Map.of("k", "v"));
        Map<String, Object> source = new HashMap<>();
        source.put("allowed_region_codes", codes);
        source.put("nested", nested);
        RuleDefinition rule = new RuleDefinition(RuleType.REGION, source);

        codes.add("REGION_V");
        nested.put("k2", "v2");

        assertThat(rule.config().get("allowed_region_codes"))
                .isEqualTo(List.of("NCR", "IV_A"));
        assertThat(rule.config().get("nested"))
                .isEqualTo(Map.of("k", "v"));
        assertThatThrownBy(() -> ((List<Object>) rule.config().get("allowed_region_codes")).add("X"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> ((Map<String, Object>) rule.config().get("nested")).put("a", "b"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void preservesNullValuesAndAcceptsEmptyConfigForLaterValidation() {
        Map<String, Object> source = new HashMap<>();
        source.put("value", null);

        assertThat(new RuleDefinition(RuleType.GWA, source).config()).containsEntry("value", null);
        assertThat(new RuleDefinition(RuleType.GWA, Map.of()).config()).isEmpty();
    }

    @Test
    void rejectsNullTypeAndNullConfig() {
        assertThatThrownBy(() -> new RuleDefinition(null, Map.of())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RuleDefinition(RuleType.GWA, null)).isInstanceOf(NullPointerException.class);
    }
}
