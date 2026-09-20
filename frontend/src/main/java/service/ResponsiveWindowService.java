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
     * Calcula la geometría segura (x, y, ancho, alto) de un modal adaptada a la resolución.
     * Garantiza que:
     * 1. {@code y >= bounds.getMinY() + TOP_SAFE_MARGIN}: el título y botón [X] NUNCA se ocultan arriba.
     * 2. {@code y + alto <= bounds.getMaxY() - BOTTOM_SAFE_MARGIN}: los botones inferiores NUNCA se ocultan abajo.
     * 3. En resolución baja achica dimensiones según el espacio real disponible.
     * 4. En resolución alta permite expandir ligeramente el ancho preferido para mejor visualización.
     */
    public static Rectangle2D calcularGeometriaModal(Rectangle2D bounds, double anchoDeseado, double altoDeseado) {
        if (bounds == null) {
            bounds = new Rectangle2D(0, 0, 1366, 768);
        }
        CategoriaResolucion cat = categoriaResolucion(bounds);

        double maxAncho = Math.max(MIN_MODAL_WIDTH, bounds.getWidth() - HORIZONTAL_SAFE_MARGIN * 2);
        double maxAlto = Math.max(MIN_MODAL_HEIGHT, bounds.getHeight() - (TOP_SAFE_MARGIN + BOTTOM_SAFE_MARGIN));

        double ancho;
        if (anchoDeseado <= 0) {
            ancho = maxAncho;
        } else if (cat == CategoriaResolucion.BAJA) {
            ancho = Math.min(anchoDeseado, maxAncho);
        } else if (cat == CategoriaResolucion.ALTA) {
            double expandido = Math.max(anchoDeseado, anchoDeseado * 1.05);
            ancho = Math.min(expandido, maxAncho);
        } else {
            ancho = Math.min(anchoDeseado, maxAncho);
        }

        double alto;
        if (altoDeseado <= 0) {
            alto = maxAlto;
        } else {
            alto = Math.min(altoDeseado, maxAlto);
        }

        // Posición X
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

        stage.setMinWidth(minimoSeguro(820, availableWidth));
        stage.setMinHeight(minimoSeguro(580, availableHeight));
        if (!userMaximized) {
            double ancho = presupuestoAncho(bounds, preferredWidth, WINDOW_MARGIN);
            double alto = presupuestoAlto(bounds, preferredHeight, WINDOW_MARGIN);
            Rectangle2D destino = centrar(bounds, ancho, alto);
            stage.setWidth(destino.getWidth());
            stage.setHeight(destino.getHeight());
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

        Rectangle2D geom = calcularGeometriaModal(bounds, anchoPreferido, 0);

        pane.setPrefWidth(geom.getWidth());
        pane.setMaxWidth(geom.getWidth());
        pane.setMaxHeight(geom.getHeight());
        pane.setMinHeight(minimoSeguro(MIN_MODAL_HEIGHT, geom.getHeight()));
        pane.setMinWidth(minimoSeguro(MIN_MODAL_WIDTH, geom.getWidth()));

        // Atajo de cierre con tecla ESC
        configurarEscapeEnDialogo(dialog);

        // Control y ajuste seguro del Stage subyacente
        configurarPosicionDialogo(dialog, bounds, geom);
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

        Rectangle2D geom = calcularGeometriaModal(bounds, deseadoAncho, deseadoAlto);

        stage.setMinWidth(minimoSeguro(MIN_MODAL_WIDTH, geom.getWidth()));
        stage.setMinHeight(minimoSeguro(MIN_MODAL_HEIGHT, geom.getHeight()));
        stage.setMaxWidth(bounds.getWidth() - 20);
        stage.setMaxHeight(bounds.getHeight() - 20);

        // Pre-establecer geometría antes de mostrar para evitar salto o Y negativo inicial
        stage.setWidth(geom.getWidth());
        stage.setHeight(geom.getHeight());
        stage.setX(geom.getMinX());
        stage.setY(geom.getMinY());

        // Manejo universal de ESC para cerrar
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stage.close();
                event.consume();
            }
        });

        // Asegurar ajuste y posicionamiento seguro al mostrarse
        stage.addEventHandler(WindowEvent.WINDOW_SHOWING, event -> {
            ajustarStageDirecto(stage, bounds, geom.getWidth(), geom.getHeight());
        });

        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, event -> {
            ajustarStageDirecto(stage, bounds, geom.getWidth(), geom.getHeight());
            Platform.runLater(() -> ajustarStageDirecto(stage, bounds, geom.getWidth(), geom.getHeight()));
        });
    }

    private static void ajustarStageDirecto(Stage stage, Rectangle2D bounds, double preferidoAncho, double preferidoAlto) {
        double w = stage.getWidth() > 0 ? stage.getWidth() : preferidoAncho;
        double h = stage.getHeight() > 0 ? stage.getHeight() : preferidoAlto;
        Rectangle2D finalGeom = calcularGeometriaModal(bounds, w, h);
        stage.setWidth(finalGeom.getWidth());
        stage.setHeight(finalGeom.getHeight());
        stage.setX(finalGeom.getMinX());
        stage.setY(finalGeom.getMinY());
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

    private static void configurarPosicionDialogo(Dialog<?> dialog, Rectangle2D bounds, Rectangle2D geom) {
        dialog.setOnShowing(event -> {
            ajustarStageDialogo(dialog, bounds, geom);
        });

        dialog.setOnShown(event -> {
            ajustarStageDialogo(dialog, bounds, geom);
            Platform.runLater(() -> ajustarStageDialogo(dialog, bounds, geom));
        });
    }

    private static void ajustarStageDialogo(Dialog<?> dialog, Rectangle2D bounds, Rectangle2D geom) {
        Scene scene = dialog.getDialogPane().getScene();
        if (scene == null || !(scene.getWindow() instanceof Stage stage)) return;

        stage.setResizable(true);
        stage.setMinWidth(minimoSeguro(MIN_MODAL_WIDTH, geom.getWidth()));
        stage.setMinHeight(minimoSeguro(MIN_MODAL_HEIGHT, geom.getHeight()));
        stage.setMaxWidth(bounds.getWidth() - 20);
        stage.setMaxHeight(bounds.getHeight() - 20);

        if (stage.getWidth() <= 0 || stage.getWidth() > geom.getWidth()) {
            stage.setWidth(geom.getWidth());
        }
        if (stage.getHeight() <= 0 || stage.getHeight() > geom.getHeight()) {
            stage.setHeight(geom.getHeight());
        }

        double curW = stage.getWidth() > 0 ? stage.getWidth() : geom.getWidth();
        double curH = stage.getHeight() > 0 ? stage.getHeight() : geom.getHeight();
        Rectangle2D finalGeom = calcularGeometriaModal(bounds, curW, curH);

        stage.setX(finalGeom.getMinX());
        stage.setY(finalGeom.getMinY());
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
