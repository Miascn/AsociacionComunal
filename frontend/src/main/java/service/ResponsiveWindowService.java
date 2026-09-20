package service;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 * Ajusta ventanas y modales al área visible de la pantalla según la resolución detectada.
 *
 * <p>El límite físico son siempre los {@code visualBounds} de la pantalla donde está la
 * ventana propietaria. El propietario sirve para elegir esa pantalla y para posicionar,
 * pero nunca para decidir el tamaño máximo: una ventana maximizada no autoriza a un modal
 * a ocupar más de lo que la pantalla muestra.
 *
 * <p>En resoluciones bajas (laptops, 768p, 720p o pantallas con escalado de 125%/150%),
 * el servicio detecta automáticamente el entorno, compacta paddings/espaciados
 * mediante clases CSS responsivas, acota las dimensiones al área visible y asegura que la
 * coordenada vertical {@code Y} jamás quede por encima de la pantalla, manteniendo la barra
 * de título y el botón de cerrar siempre visibles y operables.
 */
public final class ResponsiveWindowService {
    private static final double WINDOW_MARGIN = 24;

    /** Holgura de un modal respecto a la pantalla: deja ver que hay algo detrás. */
    private static final double MODAL_MARGIN = 48;

    public static final double MIN_MODAL_WIDTH = 320;
    public static final double MIN_MODAL_HEIGHT = 240;

    /** Márgenes de seguridad para garantizar que barra de título y botones siempre se vean */
    public static final double TOP_SAFE_MARGIN = 24.0;
    public static final double BOTTOM_SAFE_MARGIN = 20.0;
    public static final double HORIZONTAL_SAFE_MARGIN = 20.0;

    /** Umbrales de detección de resolución */
    public static final double UMBRAL_ALTO_BAJA = 800.0;
    public static final double UMBRAL_ANCHO_BAJA = 1240.0;
    public static final double UMBRAL_ALTO_ALTA = 940.0;
    public static final double UMBRAL_ANCHO_ALTA = 1600.0;

    public static final String CLASE_MODAL_COMPACTO = "modal-compact";
    public static final String CLASE_MODAL_ESPACIOSO = "modal-spacious";

    /** Pulsos que se esperan a que la ventana tenga tamaño real antes de rendirse. */
    private static final int INTENTOS_AJUSTE_INICIAL = 10;

    private static final String SCROLL_STYLE_CLASS = "responsive-scroll";
    private static final String SCROLL_STYLESHEET = "/styles/responsive-scroll.css";

    public enum CategoriaResolucion {
        BAJA,
        MEDIA,
        ALTA
    }

    private ResponsiveWindowService() { }

    // ------------------------------------------------------------------
    // Aritmética. Pura y sin dependencias del entorno gráfico: Rectangle2D es
    // un objeto de valor, de modo que esto se puede probar sin arrancar JavaFX.
    // ------------------------------------------------------------------

    /**
     * Categoriza la resolución de pantalla para aplicar ajustes de escala y estilos.
     */
    public static CategoriaResolucion categoriaResolucion(Rectangle2D bounds) {
        if (bounds == null) return CategoriaResolucion.MEDIA;
        if (bounds.getHeight() < UMBRAL_ALTO_BAJA || bounds.getWidth() < UMBRAL_ANCHO_BAJA) {
            return CategoriaResolucion.BAJA;
        }
        if (bounds.getHeight() >= UMBRAL_ALTO_ALTA && bounds.getWidth() >= UMBRAL_ANCHO_ALTA) {
            return CategoriaResolucion.ALTA;
        }
        return CategoriaResolucion.MEDIA;
    }

    /**
     * Indica si la pantalla actual corresponde a una resolución baja o con espacio vertical reducido.
     */
    public static boolean esResolucionBaja(Rectangle2D bounds) {
        return categoriaResolucion(bounds) == CategoriaResolucion.BAJA;
    }

    /**
     * Calcula la geometría segura (X, Y, Ancho, Alto) para una ventana modal dentro de los límites visibles.
     * Garantiza que el título y botón cerrar nunca queden fuera del borde superior (Y >= bounds.getMinY() + TOP_SAFE_MARGIN)
     * y que los botones inferiores queden por encima del borde de la pantalla / barra de tareas.
     */
    public static Rectangle2D calcularGeometriaModal(Rectangle2D bounds, double anchoDeseado, double altoDeseado) {
        if (bounds == null) {
            bounds = new Rectangle2D(0, 0, 1366, 768);
        }

        double maxAncho = Math.max(MIN_MODAL_WIDTH, bounds.getWidth() - HORIZONTAL_SAFE_MARGIN);
        double maxAlto = Math.max(MIN_MODAL_HEIGHT, bounds.getHeight() - (TOP_SAFE_MARGIN + BOTTOM_SAFE_MARGIN));

        double ancho = anchoDeseado > 0 ? Math.min(anchoDeseado, maxAncho) : maxAncho * 0.85;
        ancho = Math.max(MIN_MODAL_WIDTH, Math.min(ancho, maxAncho));

        double alto = altoDeseado > 0 ? Math.min(altoDeseado, maxAlto) : maxAlto * 0.90;
        alto = Math.max(MIN_MODAL_HEIGHT, Math.min(alto, maxAlto));

        // Posición X centrada pero dentro de márgenes
        double x = bounds.getMinX() + (bounds.getWidth() - ancho) / 2.0;
        double minX = bounds.getMinX() + HORIZONTAL_SAFE_MARGIN / 2.0;
        double maxX = bounds.getMaxX() - ancho - HORIZONTAL_SAFE_MARGIN / 2.0;
        x = acotar(x, minX, Math.max(minX, maxX));

        // Posición Y segura: la barra de título NUNCA queda fuera de la pantalla
        double y = bounds.getMinY() + (bounds.getHeight() - alto) / 2.0;
        double minY = bounds.getMinY() + TOP_SAFE_MARGIN;
        double maxY = bounds.getMaxY() - alto - BOTTOM_SAFE_MARGIN;
        y = acotar(y, minY, Math.max(minY, maxY));

        // Si la altura máxima excede el espacio restante hacia abajo, forzar que quepa
        if (y + alto > bounds.getMaxY() - BOTTOM_SAFE_MARGIN) {
            y = minY;
            alto = Math.max(MIN_MODAL_HEIGHT, bounds.getMaxY() - y - BOTTOM_SAFE_MARGIN);
        }

        return new Rectangle2D(x, y, ancho, alto);
    }

    /**
     * Ancho que puede ocupar una ventana: el deseado, recortado a lo que quepa.
     */
    public static double presupuestoAncho(Rectangle2D bounds, double deseado, double margen) {
        return presupuesto(bounds.getWidth(), deseado, margen);
    }

    /** Alto que puede ocupar una ventana. Mismas reglas que {@link #presupuestoAncho}. */
    public static double presupuestoAlto(Rectangle2D bounds, double deseado, double margen) {
        return presupuesto(bounds.getHeight(), deseado, margen);
    }

    private static double presupuesto(double disponibleTotal, double deseado, double margen) {
        double disponible = margen > 0 && disponibleTotal > margen
            ? disponibleTotal - margen
            : disponibleTotal;
        return deseado > 0 ? Math.min(deseado, disponible) : disponible;
    }

    /**
     * Un mínimo nunca puede exceder el espacio del que se dispone.
     */
    public static double minimoSeguro(double minDeseado, double presupuesto) {
        return Math.min(minDeseado, presupuesto);
    }

    /**
     * Centra un rectángulo dentro de otro sin salirse. Si no cabe, se alinea con el
     * origen del área visible en lugar de quedar con coordenadas negativas.
     */
    public static Rectangle2D centrar(Rectangle2D bounds, double ancho, double alto) {
        double x = bounds.getMinX() + (bounds.getWidth() - ancho) / 2;
        double y = bounds.getMinY() + (bounds.getHeight() - alto) / 2;
        return new Rectangle2D(
            acotar(x, bounds.getMinX(), bounds.getMaxX() - ancho),
            acotar(y, bounds.getMinY(), bounds.getMaxY() - alto),
            ancho, alto);
    }

    private static double acotar(double valor, double minimo, double maximo) {
        if (maximo < minimo) return minimo;
        return Math.max(minimo, Math.min(valor, maximo));
    }

    /**
     * Posición corregida de una ventana para que quede dentro del área visible.
     *
     * <p>Las dos invariantes son {@code minY <= y} y {@code y + alto <= maxY}, y sus
     * equivalentes horizontales. Cuando la ventana es más grande que el área visible las
     * dos no pueden cumplirse a la vez; entonces se conserva el borde superior izquierdo,
     * porque perder la barra de título deja la ventana imposible de mover, mientras que
     * perder el borde inferior sólo obliga a desplazar el contenido.
     *
     * <p>Una ventana que ya cumple las invariantes se devuelve sin tocar.
     */
    public static Rectangle2D posicionAnclada(Rectangle2D bounds, double x, double y,
                                              double ancho, double alto) {
        return new Rectangle2D(
            anclar(x, ancho, bounds.getMinX(), bounds.getMaxX()),
            anclar(y, alto, bounds.getMinY(), bounds.getMaxY()),
            ancho, alto);
    }

    private static double anclar(double posicion, double tamano, double minimo, double maximo) {
        if (tamano >= maximo - minimo) return minimo;
        return acotar(posicion, minimo, maximo - tamano);
    }

    // ------------------------------------------------------------------
    // Aplicación a ventanas reales.
    // ------------------------------------------------------------------

    /** Área visible de la pantalla que contiene a la ventana dada. */
    public static Rectangle2D visualBounds(Window window) {
        if (window == null) {
            Screen primary = Screen.getPrimary();
            return primary != null ? primary.getVisualBounds() : new Rectangle2D(0, 0, 1366, 768);
        }
        Window target = window;
        if (target instanceof Stage stage && (Double.isNaN(target.getX()) || Double.isNaN(target.getY())) && stage.getOwner() != null) {
            target = stage.getOwner();
        }
        double x = Double.isNaN(target.getX()) ? 0 : target.getX();
        double y = Double.isNaN(target.getY()) ? 0 : target.getY();
        double w = Double.isNaN(target.getWidth()) || target.getWidth() <= 0 ? 1 : target.getWidth();
        double h = Double.isNaN(target.getHeight()) || target.getHeight() <= 0 ? 1 : target.getHeight();

        var screens = Screen.getScreensForRectangle(x, y, w, h);
        if (!screens.isEmpty()) {
            return screens.getFirst().getVisualBounds();
        }
        Screen primary = Screen.getPrimary();
        return primary != null ? primary.getVisualBounds() : new Rectangle2D(0, 0, 1366, 768);
    }

    public static void fit(Stage stage, double preferredWidth, double preferredHeight, boolean dashboard) {
        Rectangle2D bounds = visualBounds(stage);
        double availableWidth = presupuestoAncho(bounds, 0, WINDOW_MARGIN);
        double availableHeight = presupuestoAlto(bounds, 0, WINDOW_MARGIN);
        boolean userMaximized = stage.isMaximized();

        double width = Math.min(preferredWidth, availableWidth);
        double height = Math.min(preferredHeight, availableHeight);

        stage.setMinWidth(minimoSeguro(960, availableWidth));
        stage.setMinHeight(minimoSeguro(640, availableHeight));
        stage.setMaxWidth(bounds.getWidth());
        stage.setMaxHeight(bounds.getHeight());

        if (!userMaximized) {
            stage.setWidth(width);
            stage.setHeight(height);
            Rectangle2D destino = centrar(bounds, width, height);
            stage.setX(destino.getMinX());
            stage.setY(destino.getMinY());
        }

        if (userMaximized || (dashboard && (bounds.getWidth() <= 1366 || bounds.getHeight() <= 768))) {
            stage.setMaximized(true);
        }
    }

    /**
     * Acota un diálogo al área visible, aplica estilos responsivos y garantiza
     * que su contenido sea desplazable y que siempre se pueda cerrar.
     */
    public static void fitDialog(Dialog<?> dialog, Window owner, double anchoPreferido) {
        if (dialog == null) return;
        if (owner != null && dialog.getOwner() == null) {
            dialog.initOwner(owner);
        }
        Window referencia = owner != null ? owner : dialog.getOwner();
        Rectangle2D bounds = visualBounds(referencia);
        DialogPane pane = dialog.getDialogPane();

        dialog.setResizable(true);

        CategoriaResolucion cat = categoriaResolucion(bounds);
        aplicarClasesResolucion(pane, cat);

        pane.setContent(envolver(pane.getContent(), pane.getStylesheets()));

        double ancho = presupuestoAncho(bounds, anchoPreferido, MODAL_MARGIN);
        double alto = presupuestoAlto(bounds, 0, MODAL_MARGIN);

        pane.setPrefWidth(ancho);
        pane.setMaxWidth(ancho);
        pane.setMaxHeight(alto);
        pane.setMinHeight(minimoSeguro(MIN_MODAL_HEIGHT, alto));
        pane.setMinWidth(minimoSeguro(MIN_MODAL_WIDTH, ancho));

        // Atajo de cierre con tecla ESC
        configurarEscapeEnDialogo(dialog);

        // Un diálogo se posiciona solo, centrándose sobre su propietario, y eso es lo que
        // dejaba su borde superior fuera de la pantalla cuando se volvía alto. El presupuesto
        // de contenido no basta: hace falta la misma invariante sobre la ventana real que
        // aplican los Stage, y sólo puede comprobarse cuando la ventana existe.
        anclarCuandoExistaVentana(pane);
    }

    /**
     * Engancha el anclaje a la ventana del diálogo en cuanto exista.
     *
     * <p>Un diálogo no tiene escena hasta que se muestra, y su evento
     * {@code DIALOG_SHOWING} llega antes de que la tenga. Condicionar el enganche a ese
     * evento dejaba a los diálogos sin anclar: la comprobación fallaba en silencio y no
     * se registraba ningún listener. Se observa la aparición de la escena y, dentro de
     * ella, la de la ventana, de modo que el enganche ocurre cuando de verdad hay algo
     * que anclar, sin depender de ningún manejador que el llamador pueda sustituir.
     */
    private static void anclarCuandoExistaVentana(DialogPane pane) {
        Scene scene = pane.getScene();
        if (scene == null) {
            pane.sceneProperty().addListener((obs, anterior, nueva) -> {
                if (nueva != null) anclarCuandoExistaVentana(pane);
            });
            return;
        }
        if (scene.getWindow() != null) {
            anclarEnAreaVisible(scene.getWindow());
        } else {
            scene.windowProperty().addListener((obs, anterior, ventana) -> {
                if (ventana != null) anclarEnAreaVisible(ventana);
            });
        }
    }

    /**
     * Acota un {@link Stage} modal al área visible, hace desplazable su contenido,
     * aplica estilos responsivos y garantiza que la barra de título y botón [X] queden visibles.
     */
    public static void fitModalStage(Stage stage) {
        fitModalStage(stage, null);
    }

    public static void fitModalStage(Stage stage, Window owner) {
        if (stage == null) return;
        if (owner != null && stage.getOwner() == null) {
            stage.initOwner(owner);
        }
        Window referencia = owner != null ? owner
            : stage.getOwner() != null ? stage.getOwner() : stage;
        Rectangle2D bounds = visualBounds(referencia);
        Scene scene = stage.getScene();
        if (scene == null) return;

        stage.setResizable(true);

        CategoriaResolucion cat = categoriaResolucion(bounds);
        if (scene.getRoot() != null) {
            aplicarClasesResolucion(scene.getRoot(), cat);
        }

        Node envuelto = envolver(scene.getRoot(), scene.getStylesheets());
        if (envuelto instanceof Parent nuevaRaiz && envuelto != scene.getRoot()) {
            scene.setRoot(nuevaRaiz);
        }

        double deseadoAncho = stage.getWidth() > 0 ? stage.getWidth()
            : scene.getRoot() != null ? scene.getRoot().prefWidth(-1) : 0;
        double deseadoAlto = stage.getHeight() > 0 ? stage.getHeight()
            : scene.getRoot() != null ? scene.getRoot().prefHeight(-1) : 0;

        double ancho = presupuestoAncho(bounds, deseadoAncho, MODAL_MARGIN);
        double alto = presupuestoAlto(bounds, deseadoAlto, MODAL_MARGIN);

        stage.setMinWidth(minimoSeguro(MIN_MODAL_WIDTH, ancho));
        stage.setMinHeight(minimoSeguro(MIN_MODAL_HEIGHT, alto));
        stage.setMaxWidth(bounds.getWidth() - 20);
        stage.setMaxHeight(bounds.getHeight() - 20);

        if (stage.getWidth() <= 0 || stage.getWidth() > ancho) {
            stage.setWidth(ancho);
        }
        if (stage.getHeight() <= 0 || stage.getHeight() > alto) {
            stage.setHeight(alto);
        }

        // Manejo universal de ESC para cerrar
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stage.close();
                event.consume();
            }
        });

        anclarEnAreaVisible(stage);
    }

    /**
     * Mantiene una ventana dentro del área visible durante toda su vida.
     *
     * <p>Sustituye al ajuste de una sola vez que se hacía en {@code setOnShown}: el
     * tamaño real no existe hasta que JavaFX ha medido el contenido, y puede cambiar
     * después. Además, aquel enfoque pisaba el manejador del llamador.
     *
     * <p>Escucha el alto y el ancho, no la posición. Corregir cada vez que cambian
     * {@code x} o {@code y} devolvería la ventana a su sitio mientras el usuario la
     * arrastra y le impediría llevarla a otro monitor. La pantalla de referencia se
     * recalcula en cada ajuste a partir de dónde esté la ventana en ese momento, de modo
     * que tras moverla a otro monitor el siguiente ajuste ya usa el correcto.
     */
    public static void anclarEnAreaVisible(Window window) {
        if (window == null) return;

        // Corregir el tamaño vuelve a disparar los listeners; la guarda evita que el
        // ajuste se reentre a sí mismo.
        boolean[] ajustando = {false};
        Runnable ajustar = () -> {
            if (ajustando[0]) return;
            ajustando[0] = true;
            try {
                ajustarAAreaVisible(window);
            } finally {
                ajustando[0] = false;
            }
        };

        window.widthProperty().addListener((obs, anterior, actual) -> ajustar.run());
        window.heightProperty().addListener((obs, anterior, actual) -> ajustar.run());

        // Corrección explícita en la primera apertura. WINDOW_SHOWN se emite cuando la
        // ventana ya está en pantalla; se usa addEventHandler y no setOnShown para no
        // sustituir el manejador que pudiera tener el llamador. El runLater añade un
        // pulso más, porque al recibir el evento el layout puede no haber terminado.
        window.addEventHandler(WindowEvent.WINDOW_SHOWN, event ->
            Platform.runLater(() -> ajustarCuandoTengaTamano(window, ajustar, INTENTOS_AJUSTE_INICIAL)));
        if (window.isShowing()) {
            Platform.runLater(() -> ajustarCuandoTengaTamano(window, ajustar, INTENTOS_AJUSTE_INICIAL));
        }
    }

    /**
     * Ejecuta el ajuste inicial en cuanto la ventana tenga tamaño real.
     *
     * <p>Al abrirse, {@code getWidth()} y {@code getHeight()} valen {@code NaN} hasta que
     * JavaFX mide el contenido. Abandonar en ese caso dejaba el modal sin colocar hasta
     * que algo cambiara su tamaño —minimizar y restaurar, por ejemplo—, que es justo lo
     * que QA tenía que hacer para que se acomodara. Por eso se reintenta en el pulso
     * siguiente en lugar de darse por vencido.
     */
    private static void ajustarCuandoTengaTamano(Window window, Runnable ajustar, int intentosRestantes) {
        if (intentosRestantes <= 0) return;
        if (tieneTamanoReal(window)) {
            ajustar.run();
        } else {
            Platform.runLater(() -> ajustarCuandoTengaTamano(window, ajustar, intentosRestantes - 1));
        }
    }

    private static boolean tieneTamanoReal(Window window) {
        return !Double.isNaN(window.getWidth()) && !Double.isNaN(window.getHeight())
            && window.getWidth() > 0 && window.getHeight() > 0;
    }

    private static void ajustarAAreaVisible(Window window) {
        double ancho = window.getWidth();
        double alto = window.getHeight();
        if (Double.isNaN(ancho) || Double.isNaN(alto) || ancho <= 0 || alto <= 0) return;

        Rectangle2D bounds = visualBounds(window);
        double maxAncho = presupuestoAncho(bounds, 0, MODAL_MARGIN);
        double maxAlto = presupuestoAlto(bounds, 0, MODAL_MARGIN);

        // La decoración se mide, no se supone: es la diferencia entre la ventana y su
        // escena, y sólo existe una vez que JavaFX ha hecho el layout. Restarla del
        // presupuesto es lo que impide que el contenido reclame el alto completo y
        // empuje la barra de título fuera de la pantalla.
        Scene scene = window.getScene();
        if (scene != null && scene.getRoot() instanceof Region raiz) {
            double decoracionAlto = Math.max(0, alto - scene.getHeight());
            double decoracionAncho = Math.max(0, ancho - scene.getWidth());
            raiz.setMaxHeight(Math.max(1, maxAlto - decoracionAlto));
            raiz.setMaxWidth(Math.max(1, maxAncho - decoracionAncho));
        }

        if (ancho > maxAncho) {
            window.setWidth(maxAncho);
            ancho = maxAncho;
        }
        if (alto > maxAlto) {
            window.setHeight(maxAlto);
            alto = maxAlto;
        }

        Rectangle2D destino = posicionAnclada(bounds, window.getX(), window.getY(), ancho, alto);
        if (destino.getMinX() != window.getX()) window.setX(destino.getMinX());
        if (destino.getMinY() != window.getY()) window.setY(destino.getMinY());
    }

    private static void aplicarClasesResolucion(Node nodo, CategoriaResolucion cat) {
        if (nodo == null) return;
        ObservableList<String> styleClasses = nodo.getStyleClass();
        styleClasses.removeAll(CLASE_MODAL_COMPACTO, CLASE_MODAL_ESPACIOSO);
        if (cat == CategoriaResolucion.BAJA) {
            styleClasses.add(CLASE_MODAL_COMPACTO);
        } else if (cat == CategoriaResolucion.ALTA) {
            styleClasses.add(CLASE_MODAL_ESPACIOSO);
        }
    }

    private static void configurarEscapeEnDialogo(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        pane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                dialog.close();
                event.consume();
            }
        });
        pane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        dialog.close();
                        event.consume();
                    }
                });
            }
        });
    }

    /**
     * Devuelve el nodo dentro de un {@link ScrollPane}, o tal cual si ya lo estaba.
     * Registra además la hoja de estilo que mantiene el envoltorio transparente y responsivo.
     */
    private static Node envolver(Node contenido, ObservableList<String> stylesheets) {
        if (contenido == null || contenido instanceof ScrollPane) {
            var hoja = ResponsiveWindowService.class.getResource(SCROLL_STYLESHEET);
            if (hoja != null && stylesheets != null && !stylesheets.contains(hoja.toExternalForm())) {
                stylesheets.add(hoja.toExternalForm());
            }
            return contenido;
        }

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add(SCROLL_STYLE_CLASS);

        var hoja = ResponsiveWindowService.class.getResource(SCROLL_STYLESHEET);
        if (hoja != null && stylesheets != null && !stylesheets.contains(hoja.toExternalForm())) {
            stylesheets.add(hoja.toExternalForm());
        }
        return scroll;
    }
}
