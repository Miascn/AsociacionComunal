package service;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

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

    /** Pulsos que se esperan a que la ventana tenga tamaño real antes de rendirse. */
    private static final int INTENTOS_AJUSTE_INICIAL = 10;

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
