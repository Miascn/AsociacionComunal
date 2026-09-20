package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.geometry.Rectangle2D;
import org.junit.jupiter.api.Test;

/**
 * Aritmética de ajuste al área visible — SCRUM-159.
 *
 * <p>{@code Rectangle2D} es un objeto de valor, de modo que estas reglas se verifican sin
 * arrancar JavaFX, igual que {@code VotacionEstadoTest} con la lógica de estados.
 *
 * <p>El defecto que motivó el ticket era aritmético: el alto de los diálogos se calculaba
 * contra la pantalla con una holgura fija de 180 px y un suelo de 360 px, y el límite se
 * aplicaba al contenido en lugar de al diálogo completo. Lo que se fija aquí es que ningún
 * cálculo pueda devolver un tamaño mayor que el área visible ni un mínimo que impida
 * encoger. Que los botones se vean en pantalla no es comprobable sin entorno gráfico.
 */
class ResponsiveWindowServiceTest {

    /** Pantalla 1920x1080 con barra de tareas, empezando en el origen. */
    private static final Rectangle2D AMPLIA = new Rectangle2D(0, 0, 1920, 1040);

    /** Portátil 1366x768: el caso donde el defecto se manifestaba. */
    private static final Rectangle2D BAJA = new Rectangle2D(0, 0, 1366, 728);

    /** Segunda pantalla a la derecha: el origen no es cero. */
    private static final Rectangle2D SECUNDARIA = new Rectangle2D(1920, 0, 1280, 1000);

    @Test
    void respetaElTamanoDeseadoCuandoCabeDeSobra() {
        assertEquals(720, ResponsiveWindowService.presupuestoAncho(AMPLIA, 720, 48), 0.001);
        assertEquals(600, ResponsiveWindowService.presupuestoAlto(AMPLIA, 600, 48), 0.001);
    }

    @Test
    void recortaElTamanoDeseadoCuandoNoCabe() {
        // 728 - 48 = 680. Un formulario que pide 900 no puede obtenerlos.
        assertEquals(680, ResponsiveWindowService.presupuestoAlto(BAJA, 900, 48), 0.001);
    }

    @Test
    void sinPreferenciaDevuelveTodoElEspacioDisponible() {
        assertEquals(680, ResponsiveWindowService.presupuestoAlto(BAJA, 0, 48), 0.001);
        assertEquals(1318, ResponsiveWindowService.presupuestoAncho(BAJA, -1, 48), 0.001);
    }

    @Test
    void elPresupuestoJamasSuperaElAreaVisible() {
        // Ésta es la regla que el código anterior violaba al medir contra la pantalla
        // completa y sumar después la cabecera y la barra de botones del diálogo.
        for (Rectangle2D pantalla : new Rectangle2D[] {AMPLIA, BAJA, SECUNDARIA}) {
            for (double deseado : new double[] {0, 100, 5000, Double.MAX_VALUE}) {
                assertTrue(
                    ResponsiveWindowService.presupuestoAlto(pantalla, deseado, 48) <= pantalla.getHeight(),
                    "El alto no puede exceder la pantalla: deseado=" + deseado);
                assertTrue(
                    ResponsiveWindowService.presupuestoAncho(pantalla, deseado, 48) <= pantalla.getWidth(),
                    "El ancho no puede exceder la pantalla: deseado=" + deseado);
            }
        }
    }

    @Test
    void elPresupuestoSiempreEsPositivoAunqueElMargenSeaAbsurdo() {
        Rectangle2D diminuta = new Rectangle2D(0, 0, 200, 150);
        double alto = ResponsiveWindowService.presupuestoAlto(diminuta, 0, 400);
        assertTrue(alto > 0, "Un margen mayor que la pantalla no puede dejar altura cero.");
        assertTrue(alto <= diminuta.getHeight());
    }

    @Test
    void elMinimoNuncaSuperaElPresupuesto() {
        // Un minHeight de 580 en una pantalla que sólo ofrece 400 es lo que impide
        // encoger la ventana y deja los botones fuera.
        assertEquals(400, ResponsiveWindowService.minimoSeguro(580, 400), 0.001);
        assertEquals(580, ResponsiveWindowService.minimoSeguro(580, 900), 0.001);
    }

    @Test
    void centrarDejaLaVentanaDentroDelAreaVisible() {
        Rectangle2D destino = ResponsiveWindowService.centrar(AMPLIA, 720, 600);
        assertEquals(600, destino.getMinX(), 0.001);
        assertEquals(220, destino.getMinY(), 0.001);
        assertTrue(destino.getMaxX() <= AMPLIA.getMaxX());
        assertTrue(destino.getMaxY() <= AMPLIA.getMaxY());
    }

    @Test
    void centrarRespetaElOrigenDeUnaPantallaSecundaria() {
        Rectangle2D destino = ResponsiveWindowService.centrar(SECUNDARIA, 640, 500);
        assertTrue(destino.getMinX() >= SECUNDARIA.getMinX(),
            "La ventana no puede colocarse a la izquierda de su pantalla.");
        assertEquals(1920 + (1280 - 640) / 2.0, destino.getMinX(), 0.001);
        assertTrue(destino.getMaxX() <= SECUNDARIA.getMaxX());
    }

    @Test
    void centrarNoProduceCoordenadasNegativasSiElContenidoNoCabe() {
        Rectangle2D destino = ResponsiveWindowService.centrar(BAJA, 2000, 1500);
        assertEquals(BAJA.getMinX(), destino.getMinX(), 0.001);
        assertEquals(BAJA.getMinY(), destino.getMinY(), 0.001);
    }

    @Test
    void unModalEnPantallaBajaCabeEnteroConSusBotones() {
        // Reproduce el escenario de QA: pantalla de portátil y un formulario alto.
        double alto = ResponsiveWindowService.presupuestoAlto(BAJA, 0, 48);
        double minimo = ResponsiveWindowService.minimoSeguro(240, alto);

        assertTrue(alto < BAJA.getHeight(),
            "Debe quedar holgura respecto a la pantalla, no ocuparla entera.");
        assertTrue(minimo <= alto, "El mínimo no puede impedir que el diálogo encoja.");

        Rectangle2D destino = ResponsiveWindowService.centrar(BAJA, 720, alto);
        assertTrue(destino.getMaxY() <= BAJA.getMaxY(),
            "El borde inferior, donde viven los botones, debe quedar dentro del área visible.");
    }

    @Test
    void detectaCorrectamenteLasCategoriasDeResolucion() {
        Rectangle2D resolucion720p = new Rectangle2D(0, 0, 1280, 680);
        Rectangle2D resolucion1080pEscalada150 = new Rectangle2D(0, 0, 1280, 680);
        Rectangle2D resolucion1080pEscalada125 = new Rectangle2D(0, 0, 1536, 824);
        Rectangle2D resolucionFullHD = new Rectangle2D(0, 0, 1920, 1040);
        Rectangle2D resolucion2K = new Rectangle2D(0, 0, 2560, 1400);

        assertTrue(ResponsiveWindowService.esResolucionBaja(BAJA));
        assertTrue(ResponsiveWindowService.esResolucionBaja(resolucion720p));
        assertTrue(ResponsiveWindowService.esResolucionBaja(resolucion1080pEscalada150));
        assertEquals(ResponsiveWindowService.CategoriaResolucion.BAJA, ResponsiveWindowService.categoriaResolucion(BAJA));

        assertEquals(ResponsiveWindowService.CategoriaResolucion.MEDIA, ResponsiveWindowService.categoriaResolucion(resolucion1080pEscalada125));

        assertEquals(ResponsiveWindowService.CategoriaResolucion.ALTA, ResponsiveWindowService.categoriaResolucion(resolucionFullHD));
        assertEquals(ResponsiveWindowService.CategoriaResolucion.ALTA, ResponsiveWindowService.categoriaResolucion(resolucion2K));
        assertFalse(ResponsiveWindowService.esResolucionBaja(resolucionFullHD));
    }

    @Test
    void calcularGeometriaModalGarantizaQueElTituloYBotonesSiempreEstenVisiblesEnBajaResolucion() {
        // Formulario muy alto (900 px) en pantalla de portátil 1366x768 (visual 728 px)
        Rectangle2D geom = ResponsiveWindowService.calcularGeometriaModal(BAJA, 720, 900);

        // 1. Coordenada Y nunca puede quedar arriba fuera de pantalla (margen seguro mínimo de 24px)
        assertTrue(geom.getMinY() >= BAJA.getMinY() + ResponsiveWindowService.TOP_SAFE_MARGIN,
            "El título y botón [X] deben quedar visibles, no pegados ni fuera de la pantalla. Y=" + geom.getMinY());

        // 2. Coordenada inferior nunca puede quedar tapada por la barra de tareas
        assertTrue(geom.getMaxY() <= BAJA.getMaxY() - ResponsiveWindowService.BOTTOM_SAFE_MARGIN,
            "Los botones inferiores (Guardar/Cancelar) deben quedar sobre la barra de tareas. MaxY=" + geom.getMaxY());

        // 3. El modal se achica para caber en el área disponible
        assertTrue(geom.getHeight() < BAJA.getHeight(),
            "En baja resolución el modal debe achicarse para adaptarse al espacio disponible");
        assertTrue(geom.getWidth() <= 720,
            "El ancho respeta el límite deseado o se acota a la pantalla");
    }

    @Test
    void calcularGeometriaModalEnPantalla720pConEspacioReducido() {
        Rectangle2D pantalla720p = new Rectangle2D(0, 0, 1280, 680);
        Rectangle2D geom = ResponsiveWindowService.calcularGeometriaModal(pantalla720p, 680, 800);

        assertTrue(geom.getMinY() >= pantalla720p.getMinY() + ResponsiveWindowService.TOP_SAFE_MARGIN);
        assertTrue(geom.getMaxY() <= pantalla720p.getMaxY() - ResponsiveWindowService.BOTTOM_SAFE_MARGIN);
        assertTrue(geom.getMinX() >= pantalla720p.getMinX() + 10);
        assertTrue(geom.getMaxX() <= pantalla720p.getMaxX() - 10);
    }

    @Test
    void calcularGeometriaModalRespetaMonitorSecundarioConOrigenNoCero() {
        Rectangle2D geom = ResponsiveWindowService.calcularGeometriaModal(SECUNDARIA, 600, 500);

        assertTrue(geom.getMinX() >= SECUNDARIA.getMinX(), "No debe salirse hacia la izquierda del monitor");
        assertTrue(geom.getMaxX() <= SECUNDARIA.getMaxX(), "No debe salirse hacia la derecha del monitor");
        assertTrue(geom.getMinY() >= SECUNDARIA.getMinY() + ResponsiveWindowService.TOP_SAFE_MARGIN);
        assertTrue(geom.getMaxY() <= SECUNDARIA.getMaxY() - ResponsiveWindowService.BOTTOM_SAFE_MARGIN);
    }

    @Test
    void calcularGeometriaModalRespetaBarraDeTareasSuperior() {
        Rectangle2D pantallaConBarraSuperior = new Rectangle2D(0, 40, 1920, 1000);
        Rectangle2D geom = ResponsiveWindowService.calcularGeometriaModal(pantallaConBarraSuperior, 700, 800);

        assertTrue(geom.getMinY() >= 40 + ResponsiveWindowService.TOP_SAFE_MARGIN,
            "La ventana debe respetar el origen Y cuando la barra de tareas está arriba");
        assertTrue(geom.getMaxY() <= pantallaConBarraSuperior.getMaxY() - ResponsiveWindowService.BOTTOM_SAFE_MARGIN);
    }

    @Test
    void calcularGeometriaModalEnAltaResolucionPermiteAgrandarYComodidad() {
        Rectangle2D geom = ResponsiveWindowService.calcularGeometriaModal(AMPLIA, 680, 600);

        // En pantalla amplia el ancho puede mantenerse o expandirse ligeramente de forma cómoda
        assertTrue(geom.getWidth() >= 680);
        assertTrue(geom.getHeight() <= 600);
        assertTrue(geom.getMinY() >= AMPLIA.getMinY() + ResponsiveWindowService.TOP_SAFE_MARGIN);
        assertTrue(geom.getMaxY() <= AMPLIA.getMaxY() - ResponsiveWindowService.BOTTOM_SAFE_MARGIN);
    }
}

