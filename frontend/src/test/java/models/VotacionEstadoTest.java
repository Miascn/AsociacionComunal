package models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Lógica de estado de una votación — SCRUM-279.
 *
 * <p>Sigue el patrón de {@code MiembroPermissionTest}: reglas puras, sin arrancar JavaFX.
 * Cada predicado refleja una guarda que el backend ya aplica, de modo que la interfaz no
 * ofrezca acciones que el servidor va a rechazar ni imponga restricciones propias.
 *
 * <p>Lo visual —que el botón aparezca, el color de la etiqueta, el aviso en pantalla— no
 * se puede comprobar aquí y queda para la validación funcional.
 */
class VotacionEstadoTest {

    private static VotacionModel con(String estado) {
        VotacionModel v = new VotacionModel();
        v.setEstado(estado);
        return v;
    }

    @Test
    void programadaYaNoSeConfundeConBorrador() {
        assertTrue(con("PROGRAMADA").isProgramada());
        assertFalse(con("PROGRAMADA").isBorrador(),
            "PROGRAMADA dejó de agruparse con BORRADOR: son estados distintos.");
        assertTrue(con("BORRADOR").isBorrador());
        assertFalse(con("BORRADOR").isProgramada());
    }

    @Test
    void cadaEstadoSeReconoceASiMismo() {
        assertTrue(con("ABIERTA").isAbierta());
        assertTrue(con("CERRADA").isCerrada());
        assertTrue(con("CANCELADA").isCancelada());
    }

    @Test
    void elEstadoNoDistingueMayusculas() {
        assertTrue(con("programada").isProgramada());
        assertTrue(con("Abierta").isAbierta());
        assertTrue(con("cerrada").isCerrada());
    }

    @Test
    void unEstadoNuloNoActivaNingunPredicado() {
        VotacionModel v = con(null);
        assertFalse(v.isBorrador());
        assertFalse(v.isProgramada());
        assertFalse(v.isAbierta());
        assertFalse(v.isCerrada());
        assertFalse(v.isCancelada());
        assertFalse(v.isResultadosPublicados());
    }

    @Test
    void soloLaCerradaPublicaResultados() {
        assertTrue(con("CERRADA").isResultadosPublicados());
        assertFalse(con("ABIERTA").isResultadosPublicados(),
            "Una votación en curso no publica: mostrar sus ceros se leería como que nadie votó.");
        assertFalse(con("PROGRAMADA").isResultadosPublicados());
        assertFalse(con("BORRADOR").isResultadosPublicados());
        assertFalse(con("CANCELADA").isResultadosPublicados());
    }

    @Test
    void soloSeEditaLoQueNoHaIniciado() {
        // Espeja VotacionService.update y OpcionVotacionService.validarVotacionModificable.
        assertTrue(con("BORRADOR").isEditable());
        assertTrue(con("PROGRAMADA").isEditable());
        assertFalse(con("ABIERTA").isEditable());
        assertFalse(con("CERRADA").isEditable());
        assertFalse(con("CANCELADA").isEditable());
    }

    @Test
    void laProgramadaNoSePuedeEliminar() {
        // VotacionService.delete admite BORRADOR y CANCELADA. Ofrecer "Eliminar" sobre una
        // PROGRAMADA producía un error del servidor por una acción que la interfaz ofrecía.
        assertTrue(con("BORRADOR").isEliminable());
        assertTrue(con("CANCELADA").isEliminable());
        assertFalse(con("PROGRAMADA").isEliminable());
        assertFalse(con("ABIERTA").isEliminable());
        assertFalse(con("CERRADA").isEliminable());
    }

    @Test
    void soloSeAbreLoNoIniciadoYSoloSeCierraLoAbierto() {
        assertTrue(con("BORRADOR").isAbrible());
        assertTrue(con("PROGRAMADA").isAbrible());
        assertFalse(con("CERRADA").isAbrible(), "Una votación cerrada no se reabre.");
        assertFalse(con("CANCELADA").isAbrible());

        assertTrue(con("ABIERTA").isCerrable());
        assertFalse(con("BORRADOR").isCerrable());
        assertFalse(con("PROGRAMADA").isCerrable());
        assertFalse(con("CANCELADA").isCerrable());
    }

    @Test
    void elPorcentajeVieneDelBackendYNoSeRecalcula() {
        VotacionModel.OpcionModel op = new VotacionModel.OpcionModel(1, "A", 1, 3, 75.0);
        assertEquals(75.0, op.getPorcentaje(), 0.001);
        assertEquals(0.75, op.getFraccion(), 0.001);
    }

    @Test
    void laFraccionSeMantieneEnRangoAunqueElValorLlegueFueraDeEl() {
        assertEquals(0.0, new VotacionModel.OpcionModel(1, "A", 1, 0, -5.0).getFraccion(), 0.001);
        assertEquals(1.0, new VotacionModel.OpcionModel(1, "A", 1, 9, 140.0).getFraccion(), 0.001);
        assertEquals(0.0, new VotacionModel.OpcionModel(1, "A", 1, 0, 0.0).getFraccion(), 0.001);
    }
}
