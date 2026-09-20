package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    /**
     * Portátil 1366x768 con barra de tareas: el entorno donde QA reproduce el defecto.
     * Es la pantalla de referencia del ticket, no un caso extremo.
     */
    private static final Rectangle2D BAJA = new Rectangle2D(0, 0, 1366, 728);

    /** 1366x768 sin barra de tareas, para separar el efecto de una cosa y de la otra. */
    private static final Rectangle2D BAJA_COMPLETA = new Rectangle2D(0, 0, 1366, 768);

    /**
     * 1366x768 al 125 % de escalado, menos la barra de tareas. JavaFX trabaja en
     * coordenadas lógicas, de modo que el escalado encoge el área disponible: es el caso
     * más estrecho verticalmente y donde un modal alto deja de caber antes.
     */
    private static final Rectangle2D ESCALADA_125 = new Rectangle2D(0, 0, 1093, 574);

    /** Segunda pantalla a la derecha: el origen no es cero. */
    private static final Rectangle2D SECUNDARIA = new Rectangle2D(1920, 0, 1280, 1000);

    /** Las pantallas con poco espacio vertical, que son las que importan en este ticket. */
    private static final Rectangle2D[] PANTALLAS_ESTRECHAS = {BAJA, BAJA_COMPLETA, ESCALADA_125};

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

    // ------------------------------------------------------------------
    // posicionAnclada — invariantes sobre la ventana real.
    //
    // La primera corrección de SCRUM-159 arregló el corte inferior pero destapó el
    // contrario: al volverse alto, el diálogo se centraba sobre su propietario y su borde
    // superior salía de la pantalla. Lo que se fija aquí es que, sea cual sea la posición
    // de partida, la ventana acabe dentro del área visible.
    // ------------------------------------------------------------------

    /** Comprueba las cuatro desigualdades sobre el resultado. */
    private static void assertDentro(Rectangle2D bounds, Rectangle2D ventana) {
        assertTrue(ventana.getMinY() >= bounds.getMinY(),
            "El borde superior queda fuera: y=" + ventana.getMinY() + " < minY=" + bounds.getMinY());
        assertTrue(ventana.getMinX() >= bounds.getMinX(),
            "El borde izquierdo queda fuera: x=" + ventana.getMinX());
        assertTrue(ventana.getMaxY() <= bounds.getMaxY(),
            "El borde inferior queda fuera: " + ventana.getMaxY() + " > maxY=" + bounds.getMaxY());
        assertTrue(ventana.getMaxX() <= bounds.getMaxX(),
            "El borde derecho queda fuera: " + ventana.getMaxX());
    }

    @Test
    void unaVentanaYaValidaNoSeMueve() {
        Rectangle2D resultado = ResponsiveWindowService.posicionAnclada(AMPLIA, 300, 200, 720, 600);
        assertEquals(300, resultado.getMinX(), 0.001);
        assertEquals(200, resultado.getMinY(), 0.001);
        assertDentro(AMPLIA, resultado);
    }

    @Test
    void corrigeElBordeSuperiorFueraDePantalla() {
        // El caso que reportó QA: el diálogo, ya alto, centrado por JavaFX sobre su
        // propietario, acaba con la barra de título por encima del área visible.
        Rectangle2D resultado = ResponsiveWindowService.posicionAnclada(BAJA, 300, -120, 680, 650);
        assertEquals(BAJA.getMinY(), resultado.getMinY(), 0.001);
        assertDentro(BAJA, resultado);
    }

    @Test
    void corrigeElBordeInferiorFueraDePantalla() {
        Rectangle2D resultado = ResponsiveWindowService.posicionAnclada(BAJA, 100, 600, 500, 400);
        assertEquals(BAJA.getMaxY() - 400, resultado.getMinY(), 0.001);
        assertDentro(BAJA, resultado);
    }

    @Test
    void corrigeLosBordesLateralesFueraDePantalla() {
        assertDentro(AMPLIA, ResponsiveWindowService.posicionAnclada(AMPLIA, -400, 100, 720, 600));
        assertEquals(0, ResponsiveWindowService.posicionAnclada(AMPLIA, -400, 100, 720, 600).getMinX(), 0.001);

        Rectangle2D derecha = ResponsiveWindowService.posicionAnclada(AMPLIA, 1800, 100, 720, 600);
        assertEquals(AMPLIA.getMaxX() - 720, derecha.getMinX(), 0.001);
        assertDentro(AMPLIA, derecha);
    }

    @Test
    void anclaDentroDeUnaPantallaSecundaria() {
        // Una ventana en el monitor derecho no puede empujarse al origen global: su área
        // visible empieza en x=1920.
        Rectangle2D resultado = ResponsiveWindowService.posicionAnclada(SECUNDARIA, 1900, -50, 600, 500);
        assertEquals(SECUNDARIA.getMinX(), resultado.getMinX(), 0.001);
        assertEquals(SECUNDARIA.getMinY(), resultado.getMinY(), 0.001);
        assertDentro(SECUNDARIA, resultado);
    }

    @Test
    void unaVentanaMayorQueLaPantallaConservaElBordeSuperior() {
        // Las dos invariantes verticales no pueden cumplirse a la vez. Se conserva la
        // barra de título: sin ella la ventana no se puede mover ni cerrar.
        Rectangle2D resultado = ResponsiveWindowService.posicionAnclada(BAJA, 50, -200, 1500, 900);
        assertEquals(BAJA.getMinX(), resultado.getMinX(), 0.001);
        assertEquals(BAJA.getMinY(), resultado.getMinY(), 0.001);
    }

    @Test
    void laInvarianteSeCumpleParaCualquierPosicionDePartida() {
        for (Rectangle2D pantalla : new Rectangle2D[] {AMPLIA, BAJA, SECUNDARIA}) {
            for (double x : new double[] {-5000, -1, 0, 500, 5000}) {
                for (double y : new double[] {-5000, -1, 0, 400, 5000}) {
                    assertDentro(pantalla,
                        ResponsiveWindowService.posicionAnclada(pantalla, x, y, 600, 500));
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Resoluciones bajas. QA reproduce el defecto en 1366x768; que un modal quepa en 4K
    // no dice nada sobre si cabe ahí. El criterio es que la ventana real siga accesible
    // por completo con un área vertical de unos 700 px o menos.
    // ------------------------------------------------------------------

    /** Decoración típica de una ventana en Windows. No se usa en producción —allí se mide—
     *  pero aquí sirve para comprobar que el presupuesto la admite. */
    private static final double DECORACION = 31;

    private static final double MARGEN_MODAL = 48;

    @Test
    void enResolucionesBajasLaVentanaConDecoracionSigueCabiendo() {
        for (Rectangle2D pantalla : PANTALLAS_ESTRECHAS) {
            double contenido = ResponsiveWindowService.presupuestoAlto(pantalla, 0, MARGEN_MODAL)
                - DECORACION;
            assertTrue(contenido > 0,
                "No queda alto de contenido en " + descripcion(pantalla));

            double altoVentana = contenido + DECORACION;
            Rectangle2D ventana = ResponsiveWindowService.posicionAnclada(
                pantalla, 0, 0, 600, altoVentana);

            assertDentro(pantalla, ventana);
            assertTrue(altoVentana <= pantalla.getHeight(),
                "La ventana con decoración no cabe en " + descripcion(pantalla));
        }
    }

    @Test
    void enResolucionesBajasUnModalDemasiadoAltoQuedaAncladoArriba() {
        // Un formulario que pide más de lo que hay: lo que no puede pasar es que la barra
        // de título acabe fuera, porque entonces la ventana no se puede ni mover.
        for (Rectangle2D pantalla : PANTALLAS_ESTRECHAS) {
            Rectangle2D ventana = ResponsiveWindowService.posicionAnclada(
                pantalla, 40, -90, 700, pantalla.getHeight() + 300);

            assertEquals(pantalla.getMinY(), ventana.getMinY(), 0.001,
                "El borde superior debe quedar anclado en " + descripcion(pantalla));
        }
    }

    @Test
    void elAreaVerticalDeUnos700PxSigueDejandoUnModalUtilizable() {
        // BAJA tiene 728 px de alto visible; ESCALADA_125 baja hasta 574.
        for (Rectangle2D pantalla : PANTALLAS_ESTRECHAS) {
            double alto = ResponsiveWindowService.presupuestoAlto(pantalla, 0, MARGEN_MODAL);
            double minimo = ResponsiveWindowService.minimoSeguro(240, alto);

            assertTrue(alto >= minimo,
                "El mínimo no puede superar el presupuesto en " + descripcion(pantalla));
            assertTrue(alto < pantalla.getHeight(),
                "Debe quedar margen visual en " + descripcion(pantalla));

            Rectangle2D ventana = ResponsiveWindowService.posicionAnclada(pantalla, 0, 0, 680, alto);
            assertDentro(pantalla, ventana);
        }
    }

    @Test
    void laInvarianteSeMantieneEnResolucionesBajasSeaCualSeaLaPosicion() {
        for (Rectangle2D pantalla : PANTALLAS_ESTRECHAS) {
            for (double y : new double[] {-400, -31, 0, 300, 2000}) {
                for (double alto : new double[] {200, 500, 720, 1200}) {
                    Rectangle2D ventana =
                        ResponsiveWindowService.posicionAnclada(pantalla, 10, y, 500, alto);
                    assertTrue(ventana.getMinY() >= pantalla.getMinY(),
                        "Borde superior fuera en " + descripcion(pantalla)
                            + " con alto=" + alto + " e y=" + y);
                }
            }
        }
    }

    private static String descripcion(Rectangle2D pantalla) {
        return ((int) pantalla.getWidth()) + "x" + ((int) pantalla.getHeight());
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
}
