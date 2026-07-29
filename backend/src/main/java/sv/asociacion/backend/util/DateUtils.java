package sv.asociacion.backend.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateUtils {
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter PERIOD_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private DateUtils() {}

    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMAT) : null;
    }

    public static LocalDate parseDate(String date) {
        return date != null ? LocalDate.parse(date, DATE_FORMAT) : null;
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMAT) : null;
    }

    public static LocalDateTime parseDateTime(String dateTime) {
        return dateTime != null ? LocalDateTime.parse(dateTime, DATETIME_FORMAT) : null;
    }

    public static String toPeriodMonth(LocalDate date) {
        return date != null ? date.format(PERIOD_MONTH_FORMAT) : null;
    }
}
