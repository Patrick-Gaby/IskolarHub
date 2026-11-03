package ph.iskolarhub.eligibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Architecture guard: the eligibility package must stay plain Java. It may use only the two existing plain enums
 * GwaScale and RuleType from other ph.iskolarhub packages, and nothing from Spring, JPA or Hibernate. Maven runs
 * tests from the backend directory, which is where the relative source path below resolves.
 */
class EngineIndependenceTest {

    private static final Path SOURCE_ROOT = Paths.get("src", "main", "java", "ph", "iskolarhub", "eligibility");

    private static final List<String> FORBIDDEN_TEXT = List.of(
            "org.springframework", "jakarta.", "javax.persistence", "org.hibernate");

    private static final Set<String> ALLOWED_EXTERNAL_PROJECT_IMPORTS = Set.of(
            "ph.iskolarhub.profile.entity.GwaScale",
            "ph.iskolarhub.scholarship.entity.RuleType");

    @Test
    void eligibilitySourcesDoNotDependOnSpringJpaOrOtherProjectClasses() throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(SOURCE_ROOT)) {
            files = walk.filter(p -> p.toString().endsWith(".java")).toList();
        }
        assertThat(files).as("eligibility sources found under " + SOURCE_ROOT.toAbsolutePath()).isNotEmpty();

        for (Path file : files) {
            String text = Files.readString(file);
            for (String forbidden : FORBIDDEN_TEXT) {
                assertThat(text).as(file + " must not reference " + forbidden).doesNotContain(forbidden);
            }
            for (String line : text.lines().map(String::strip).toList()) {
                if (line.startsWith("import ph.iskolarhub.") && !line.startsWith("import ph.iskolarhub.eligibility.")) {
                    String imported = line.substring("import ".length(), line.length() - 1);
                    assertThat(ALLOWED_EXTERNAL_PROJECT_IMPORTS)
                            .as(file + " imports " + imported)
                            .contains(imported);
                }
            }
        }
    }
}
