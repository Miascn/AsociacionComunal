package sv.asociacion.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    @Test
    @DisplayName("Fecha: formato yyyy-MM-dd de ida y vuelta")
    void fecha() {
        LocalDate fecha = LocalDate.of(2026, 3, 7);
        assertEquals("2026-03-07", DateUtils.formatDate(fecha));
        assertEquals(fecha, DateUtils.parseDate("2026-03-07"));
    }

    @Test
    @DisplayName("Fecha y hora: formato yyyy-MM-dd HH:mm:ss de ida y vuelta")
    void fechaHora() {
        LocalDateTime momento = LocalDateTime.of(2026, 10, 18, 9, 5, 0);
        assertEquals("2026-10-18 09:05:00", DateUtils.formatDateTime(momento));
        assertEquals(momento, DateUtils.parseDateTime("2026-10-18 09:05:00"));
    }

    @Test
    @DisplayName("Periodo mensual de una aportacion: yyyy-MM")
    void periodoMensual() {
        assertEquals("2026-01", DateUtils.toPeriodMonth(LocalDate.of(2026, 1, 31)));
    }

    @Test
    @DisplayName("Los valores nulos se propagan como nulos en lugar de fallar")
    void nulos() {
        assertNull(DateUtils.formatDate(null));
        assertNull(DateUtils.parseDate(null));
        assertNull(DateUtils.formatDateTime(null));
        assertNull(DateUtils.parseDateTime(null));
        assertNull(DateUtils.toPeriodMonth(null));
    }

    @Test
    @DisplayName("Un texto con formato incorrecto se rechaza")
    void formatoInvalido() {
        assertThrows(DateTimeParseException.class, () -> DateUtils.parseDate("07/03/2026"));
        assertThrows(DateTimeParseException.class, () -> DateUtils.parseDateTime("2026-10-18T09:05:00"));
    }
}
