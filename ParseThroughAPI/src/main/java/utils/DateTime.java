package utils;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.regex.Pattern;

public final class DateTime {

    private static final Pattern DIGITS = Pattern.compile("^\\d+$");

    private static final DateTimeFormatter[] ISO_VARIANTS = new DateTimeFormatter[]{
            DateTimeFormatter.ISO_OFFSET_DATE_TIME, // 2024-06-01T12:34:56+00:00
            DateTimeFormatter.ISO_INSTANT,         // 2024-06-01T12:34:56Z
            DateTimeFormatter.ISO_LOCAL_DATE_TIME // 2024-06-01T12:34:56 (no offset)
    };

    private static final DateTimeFormatter[] LOCAL_DATE_VARIANTS = new DateTimeFormatter[]{
            DateTimeFormatter.ISO_LOCAL_DATE,      // 2024-06-01
            DateTimeFormatter.ofPattern("MM-dd-yy"), // 06-01-24 (if such appears)
            DateTimeFormatter.ofPattern("MM-dd-yyyy") // 06-01-2024
    };

    private DateTime() {}

    /**
     * Parses a string or numeric string in OffsetDateTime.
     * Supports:
     * - epoch seconds (a string of digits OR Long -> see parseEpochSeconds)
     *  - ISO_OFFSET_DATE_TIME / ISO_INSTANT / ISO_LOCAL_DATE_TIME
     * - RFC_1123_DATE_TIME (as a fallback)
     * Returns null if parsing failed.
     */
    // TODO reduce coomplexity
    public static OffsetDateTime parseToOffsetDateTime(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;

        try {
            if (DIGITS.matcher(s).matches()) {
                long epoch = Long.parseLong(s);
                return parseEpochSeconds(epoch);
            }
        } catch (Exception e) {
            System.out.println("Error parsing date: " + s);
        }

        for (DateTimeFormatter f : ISO_VARIANTS) {
            try {
                TemporalAccessor ta = f.parse(s);
                Instant instant = null;
                if (f == DateTimeFormatter.ISO_LOCAL_DATE_TIME) {
                    LocalDateTime ldt = LocalDateTime.parse(s, f);
                    instant = ldt.toInstant(ZoneOffset.UTC);
                } else {
                    try {
                        instant = Instant.from(ta);
                    } catch (DateTimeException de) {
                        try {
                            return OffsetDateTime.parse(s, f);
                        } catch (DateTimeException e) {
                            System.out.println("Error parsing date: " + s);
                        }
                    }
                }
                if (instant != null) return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {}
        }

        try {
            ZonedDateTime zdt = ZonedDateTime.parse(s, DateTimeFormatter.RFC_1123_DATE_TIME);
            return zdt.toOffsetDateTime();
        } catch (DateTimeParseException ignored) {}

        return null;
    }

    public static OffsetDateTime parseToOffsetDateTime(Long epochSeconds) {
        if (epochSeconds == null) return null;
        return parseEpochSeconds(epochSeconds);
    }

    private static OffsetDateTime parseEpochSeconds(long epochSeconds) {
        Instant ins = Instant.ofEpochSecond(epochSeconds);
        return OffsetDateTime.ofInstant(ins, ZoneOffset.UTC);
    }

    public static LocalDate parseToLocalDate(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;

        if (DIGITS.matcher(s).matches()) {
            try {
                long epoch = Long.parseLong(s);
                return Instant.ofEpochSecond(epoch).atZone(ZoneOffset.UTC).toLocalDate();
            } catch (Exception ignored) {}
        }

        for (DateTimeFormatter f : LOCAL_DATE_VARIANTS) {
            try {
                return LocalDate.parse(s, f);
            } catch (DateTimeParseException ignored) {}
        }

        try {
            OffsetDateTime odt = parseToOffsetDateTime(s);
            if (odt != null) return odt.toLocalDate();
        } catch (Exception ignored) {}

        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException ignored) {}

        return null;
    }
}