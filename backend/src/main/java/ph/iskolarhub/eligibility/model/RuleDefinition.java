package ph.iskolarhub.eligibility.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import ph.iskolarhub.scholarship.entity.RuleType;

/**
 * One configured scholarship requirement as handed to the engine: its type plus the raw, NOT yet validated JSON
 * configuration. Validation and parsing belong to the rule configuration validators (Cluster 3.2). The
 * configuration is deep-copied into unmodifiable collections so later changes to the caller's map cannot alter
 * an evaluation. Null values inside the configuration are preserved so that validators can reject them.
 *
 * @param ruleType the kind of requirement
 * @param config   raw configuration; may be empty, never null
 */
public record RuleDefinition(RuleType ruleType, Map<String, Object> config) {

    public RuleDefinition {
        Objects.requireNonNull(ruleType, "ruleType");
        Objects.requireNonNull(config, "config");
        config = copyMap(config);
    }

    private static Map<String, Object> copyMap(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            copy.put((String) entry.getKey(), copyValue(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    private static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return copyMap(map);
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object element : list) {
                copy.add(copyValue(element));
            }
            return Collections.unmodifiableList(copy);
        }
        if (value instanceof Set<?> set) {
            Set<Object> copy = new LinkedHashSet<>();
            for (Object element : set) {
                copy.add(copyValue(element));
            }
            return Collections.unmodifiableSet(copy);
        }
        return value; // String, Number, Boolean or null: immutable
    }
}
