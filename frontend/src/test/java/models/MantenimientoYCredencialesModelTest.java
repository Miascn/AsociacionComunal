package models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MantenimientoYCredencialesModelTest {

    @Test
    void testMiembroModelTieneClaveProvisional() {
        MiembroModel m = new MiembroModel();
        assertFalse(m.tieneClaveProvisional());

        m.setRequiereCambioClave(true);
        assertTrue(m.tieneClaveProvisional());

        m.setClaveTemporal("Asoc2026#abc");
        assertTrue(m.tieneClaveProvisional());

        // Al cambiar clave en celular, se limpian ambos campos
        m.setRequiereCambioClave(false);
        m.setClaveTemporal(null);
        assertFalse(m.tieneClaveProvisional());
    }

    @Test
    void testUsuarioModelTieneClaveProvisional() {
        UsuarioModel u = new UsuarioModel();
        assertFalse(u.tieneClaveProvisional());

        u.setRequiereCambioClave(true);
        assertTrue(u.tieneClaveProvisional());

        u.setClaveTemporal("Temporal123!");
        assertTrue(u.tieneClaveProvisional());

        u.setRequiereCambioClave(false);
        u.setClaveTemporal(null);
        assertFalse(u.tieneClaveProvisional());
    }

    @Test
    void testMantenimientoMiembroModelPagado() {
        MantenimientoMiembroModel model = new MantenimientoMiembroModel();
        model.setIdMiembro(1);
        model.setDui("01234567-8");
        model.setNombreCompleto("Carlos Martínez");
        model.setDireccion("Vivienda #5");
        model.setPeriodoMes("2026-03");
        model.setCuotaEsperada(new BigDecimal("10.00"));
        model.setPagado(true);
        model.setIdAportacion(100L);
        model.setMontoPagado(new BigDecimal("10.00"));
        model.setFechaPago(LocalDate.of(2026, 3, 15));
        model.setMetodoPago("EFECTIVO");
        model.setReferencia("REC-001");
        model.setEstadoAportacion("REGISTRADA");

        assertEquals(1, model.getIdMiembro());
        assertEquals("01234567-8", model.getDui());
        assertEquals("Carlos Martínez", model.getNombreCompleto());
        assertTrue(model.isPagado());
        assertEquals("PAGADO", model.getEstadoTexto());
        assertEquals(new BigDecimal("10.00"), model.getMontoPagado());
        assertEquals(LocalDate.of(2026, 3, 15), model.getFechaPago());
    }

    @Test
    void testMantenimientoMiembroModelPendiente() {
        MantenimientoMiembroModel model = new MantenimientoMiembroModel();
        model.setIdMiembro(2);
        model.setDui("87654321-0");
        model.setNombreCompleto("Ana Gómez");
        model.setPeriodoMes("2026-03");
        model.setCuotaEsperada(new BigDecimal("10.00"));
        model.setPagado(false);

        assertEquals(2, model.getIdMiembro());
        assertFalse(model.isPagado());
        assertEquals("PENDIENTE", model.getEstadoTexto());
        assertEquals(new BigDecimal("10.00"), model.getCuotaEsperada());
    }

    @Test
    void testCredencialesMiembroRecord() {
        service.MiembroApiClient.CredencialesMiembro creds = new service.MiembroApiClient.CredencialesMiembro(
            5, 10, "01234567-8", true, "Tmp!abc123xyz#", "ACTIVO"
        );
        assertEquals(5, creds.idMiembro());
        assertEquals(10, creds.idUsuario());
        assertEquals("01234567-8", creds.nombreUsuario());
        assertTrue(creds.requiereCambioClave());
        assertEquals("Tmp!abc123xyz#", creds.claveTemporal());
        assertEquals("ACTIVO", creds.estadoUsuario());
    }
}
