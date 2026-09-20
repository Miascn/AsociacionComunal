package service;

import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ScrollPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Ajusta ventanas y modales al área visible de la pantalla.
 *
 * <p>El límite físico son siempre los {@code visualBounds} de la pantalla donde está la
 * ventana propietaria. El propietario sirve para elegir esa pantalla y para posicionar,
 * pero nunca para decidir el tamaño máximo: una ventana maximizada no autoriza a un modal
 * a ocupar más de lo que la pantalla muestra.
 *
 * <p>La aritmética vive en métodos puros y sin estado para poder fijarla con pruebas; la
 * parte que toca nodos de JavaFX no es verificable sin un entorno gráfico.
 */
public final class ResponsiveWindowService {
    private static final double WINDOW_MARGIN = 24;

    /** Holgura de un modal respecto a la pantalla: deja ver que hay algo detrás. */
    private static final double MODAL_MARGIN = 48;

    private static final double MIN_MODAL_WIDTH = 320;
    private static final double MIN_MODAL_HEIGHT = 240;

    private static final String SCROLL_STYLE_CLASS = "responsive-scroll";
    private static final String SCROLL_STYLESHEET = "/styles/responsive-scroll.css";

    private ResponsiveWindowService() { }

    // ------------------------------------------------------------------
    // Aritmética. Pura y sin dependencias del entorno gráfico: Rectangle2D es
    // un objeto de valor, de modo que esto se puede probar sin arrancar JavaFX.
    // ------------------------------------------------------------------

    /**
     * Ancho que puede ocupar una ventana: el deseado, recortado a lo que quepa.
     *
     * <p>Un {@code deseado} de cero o negativo significa «sin preferencia» y devuelve todo
     * el espacio disponible. El resultado nunca supera el ancho de la pantalla, ni siquiera
     * cuando el margen es mayor que ella.
     */
    public static double presupuestoAncho(Rectangle2D bounds, double deseado, double margen) {
        return presupuesto(bounds.getWidth(), deseado, margen);
    }

    /** Alto que puede ocupar una ventana. Mismas reglas que {@link #presupuestoAncho}. */
    public static double presupuestoAlto(Rectangle2D bounds, double deseado, double margen) {
        return presupuesto(bounds.getHeight(), deseado, margen);
    }

    private static double presupuesto(double disponibleTotal, double deseado, double margen) {
        // Si el margen se comiera la pantalla entera, se ignora: más vale un modal a
        // pantalla completa que uno de altura cero.
        double disponible = margen > 0 && disponibleTotal > margen
            ? disponibleTotal - margen
            : disponibleTotal;
        return deseado > 0 ? Math.min(deseado, disponible) : disponible;
    }

    /**
     * Un mínimo nunca puede exceder el espacio del que se dispone. Fijar
     * {@code minHeight} por encima del presupuesto es justamente lo que impide que una
     * ventana encoja y deja sus botones fuera de la pantalla.
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
        if (window == null) return Screen.getPrimary().getVisualBounds();
        var screens = Screen.getScreensForRectangle(
            window.getX(), window.getY(),
            Math.max(window.getWidth(), 1), Math.max(window.getHeight(), 1));
        return (screens.isEmpty() ? Screen.getPrimary() : screens.getFirst()).getVisualBounds();
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
     * Acota un diálogo al área visible y garantiza que su contenido se pueda desplazar.
     *
     * <p>El presupuesto se aplica al {@link DialogPane} completo, no al contenido: la
     * cabecera y la barra de botones son parte del diálogo, y limitar sólo el contenido
     * deja la botonera fuera de la pantalla. Es exactamente el defecto de SCRUM-159.
     *
     * <p>El contenido se envuelve en un {@link ScrollPane} sólo si no lo estaba ya, para
     * no anidar uno dentro de otro.
     *
     * @param anchoPreferido ancho deseado; se recorta si no cabe. Cero significa
     *                       «sin preferencia».
     */
    public static void fitDialog(Dialog<?> dialog, Window owner, double anchoPreferido) {
        if (owner != null && dialog.getOwner() == null) {
            dialog.initOwner(owner);
        }
        Rectangle2D bounds = visualBounds(owner != null ? owner : dialog.getOwner());
        DialogPane pane = dialog.getDialogPane();

        pane.setContent(envolver(pane.getContent(), pane.getStylesheets()));

        double alto = presupuestoAlto(bounds, 0, MODAL_MARGIN);
        double ancho = presupuestoAncho(bounds, anchoPreferido, MODAL_MARGIN);

        pane.setPrefWidth(ancho);
        pane.setMaxWidth(ancho);
        pane.setMaxHeight(alto);
        pane.setMinHeight(minimoSeguro(MIN_MODAL_HEIGHT, alto));
        pane.setMinWidth(minimoSeguro(MIN_MODAL_WIDTH, ancho));
    }

    /**
     * Acota un {@link Stage} modal al área visible, hace desplazable su contenido y lo
     * deja redimensionable.
     *
     * <p>Se llama con la escena ya asignada. Un modal que no se puede encoger es un modal
     * del que no se puede rescatar el contenido cuando no cabe, así que aquí
     * {@code resizable} pasa a verdadero de forma deliberada.
     */
    public static void fitModalStage(Stage stage) {
        fitModalStage(stage, null);
    }

    public static void fitModalStage(Stage stage, Window owner) {
        if (owner != null && stage.getOwner() == null) {
            stage.initOwner(owner);
        }
        // Si el llamador ya asignó propietario con initOwner, esa es la referencia:
        // indica en qué pantalla debe medirse el modal.
        Window referencia = owner != null ? owner
            : stage.getOwner() != null ? stage.getOwner() : stage;
        Rectangle2D bounds = visualBounds(referencia);
        Scene scene = stage.getScene();
        if (scene == null) return;

        Node envuelto = envolver(scene.getRoot(), scene.getStylesheets());
        if (envuelto instanceof Parent nuevaRaiz && envuelto != scene.getRoot()) {
            scene.setRoot(nuevaRaiz);
        }

        double maxAncho = presupuestoAncho(bounds, 0, MODAL_MARGIN);
        double maxAlto = presupuestoAlto(bounds, 0, MODAL_MARGIN);

        stage.setResizable(true);
        stage.setMaxWidth(maxAncho);
        stage.setMaxHeight(maxAlto);
        stage.setMinWidth(minimoSeguro(MIN_MODAL_WIDTH, maxAncho));
        stage.setMinHeight(minimoSeguro(MIN_MODAL_HEIGHT, maxAlto));

        // El tamaño real sólo se conoce tras medir el contenido; al mostrarse se recorta
        // lo que sobre y se recoloca dentro del área visible.
        stage.setOnShown(event -> {
            double ancho = presupuestoAncho(bounds, stage.getWidth(), MODAL_MARGIN);
            double alto = presupuestoAlto(bounds, stage.getHeight(), MODAL_MARGIN);
            stage.setWidth(ancho);
            stage.setHeight(alto);
            Rectangle2D destino = centrar(bounds, ancho, alto);
            stage.setX(destino.getMinX());
            stage.setY(destino.getMinY());
        });
    }

    /**
     * Devuelve el nodo dentro de un {@link ScrollPane}, o tal cual si ya lo estaba.
     * Registra además la hoja de estilo que mantiene el envoltorio transparente.
     */
    private static Node envolver(Node contenido, javafx.collections.ObservableList<String> stylesheets) {
        if (contenido == null || contenido instanceof ScrollPane) return contenido;

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add(SCROLL_STYLE_CLASS);

        var hoja = ResponsiveWindowService.class.getResource(SCROLL_STYLESHEET);
        if (hoja != null && !stylesheets.contains(hoja.toExternalForm())) {
            stylesheets.add(hoja.toExternalForm());
        }
        return scroll;
    }
}
