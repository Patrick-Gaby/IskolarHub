package ph.iskolarhub.common.validation;

import java.util.Locale;
import java.util.regex.Pattern;

/** Single place where emails, codes and free text are normalized before they reach the database. */
public final class TextNormalizer {

    private static final Pattern SEPARATORS = Pattern.compile("[\\s\\-]+");
    private static final Pattern CODE = Pattern.compile("^[A-Z0-9_]+$");

    private TextNormalizer() {
    }

    /** Trim and lower-case. Messages never echo the value (it may be personal data). */
    public static String email(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("email must not be null");
        }
        String value = raw.strip().toLowerCase(Locale.ROOT);
        if (value.indexOf('@') < 1 || value.length() > 254) {
            throw new IllegalArgumentException("email is not valid");
        }
        return value;
    }

    /**
     * Normalizes a region or course code: trim, spaces and hyphens become underscores, upper-case.
     * Returns null for null or blank input ("not provided"). Rejects anything else that is not 2..maxLength
     * characters of A-Z, 0-9 or underscore.
     */
    public static String code(String raw, String field, int maxLength) {
        if (raw == null) {
            return null;
        }
        String stripped = raw.strip();
        if (stripped.isEmpty()) {
            return null;
        }
        String value = SEPARATORS.matcher(stripped).replaceAll("_").toUpperCase(Locale.ROOT);
        if (value.length() < 2 || value.length() > maxLength || !CODE.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " is not a valid code");
        }
        return value;
    }

    public static String requireText(String raw, String field, int maxLength) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        String value = raw.strip();
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(field + " is too long");
        }
        return value;
    }

    /** Returns null for null or blank input. */
    public static String optionalText(String raw, String field, int maxLength) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return requireText(raw, field, maxLength);
    }
}