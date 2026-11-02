package ph.iskolarhub.common.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Deadline policy: a date-only deadline is the END of that calendar date in Asia/Manila.
 * The end of day is 23:59:59.999999 because PostgreSQL stores microsecond precision; a nanosecond value
 * would be rounded up into the next day on insert.
 */
public final class DeadlinePolicy {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Manila");
    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59, 999_999_000);

    private DeadlinePolicy() {
    }

    public static Instant endOfDay(LocalDate date) {
        return ZonedDateTime.of(date, END_OF_DAY, BUSINESS_ZONE).toInstant();
    }

    /** The Asia/Manila calendar date on which the instant falls. */
    public static LocalDate businessDateOf(Instant instant) {
        return instant.atZone(BUSINESS_ZONE).toLocalDate();
    }
}