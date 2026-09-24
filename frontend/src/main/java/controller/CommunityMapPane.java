package controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;
import models.ViviendaModel;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Componente interactivo del Plano Comunal estilo Google Maps con panel de herramientas lateral.
 *
 * Características clave:
 * 1. Panel de herramientas a la par del mapa (lateral): no se corta ningún texto ni botón.
 * 2. Visualización 100% íntegra: a escala mínima muestra todo el plano (de extremo a extremo).
 * 3. Inmovilidad en escala mínima: cuando el mapa está al 100%, queda bloqueado y no se puede arrastrar.
 *    Al hacer zoom in, se desbloquea el arrastre fluido con límites de mapa.
 * 4. Correspondencia estricta 1-a-1: tantas casas en el plano como viviendas reales en la BD.
 * 5. Colocación interactiva: clic en el plano para ubicar nuevas viviendas detectando su sector.
 * 6. Giro angular libre: rueda del ratón o clic derecho para alinear casas con calles curvas/diagonales.
 */
public class CommunityMapPane extends HBox {

    public static final double MAP_WIDTH = 945;
    public static final double MAP_HEIGHT = 1024;
    public static final double VIEWPORT_SIZE = 720;
    // Escala mínima exacta para que la imagen de 945x1024 quepa completa en un viewport cuadrado de 720x720:
    public static final double MIN_SCALE = VIEWPORT_SIZE / MAP_HEIGHT; // 720 / 1024 ≈ 0.703125
    public static final double MAX_SCALE = 3.2; // Zoom hasta 320%

    private static final String POSICIONES_FILENAME = "mapa_lotes_posiciones.json";
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    // Contenedores principales
    private final VBox sideToolPanel = new VBox(10);
    private final ScrollPane sideToolScrollPane = new ScrollPane();
    private final StackPane viewportWrapper = new StackPane();

    // Capas del mapa
    private final Pane viewportPane = new Pane();
    private final Pane mapCanvas = new Pane();
    private final Group mapGroup = new Group();
    private final Translate mapTranslateTransform = new Translate(0, 0);
    private final Scale mapScaleTransform = new Scale(MIN_SCALE, MIN_SCALE, 0, 0);
    private ImageView mapImageView;
    private Pane layerPuntosInteres;
    private Pane layerLotes;
    private VBox bannerSinViviendas;
    private HBox bannerModoFlotante;
    private Label lblBannerModoFlotante;
    private Circle marcadorColocacionPreview;
    private VBox calloutCard;

    // Controles de navegación y zoom
    private final DoubleProperty scaleFactor = new SimpleDoubleProperty(MIN_SCALE);
    private double dragStartX, dragStartY;
    private double mapTranslateX = 0, mapTranslateY = 0;

    // Estados de modo
    private boolean modoEdicion = false;
    private boolean modoColocacion = false;

    // Controles del panel lateral
    private Label lblContadorCasas;
    private Button btnPonerVivienda;
    private Button btnNuevaVivienda;
    private Button btnModoEdicion;
    private VBox boxEstadoEdicion;
    private VBox boxEstadoColocacion;
    private Button btnGuardarCoordenadas;
    private Button btnResetearCoordenadas;
    private Button btnTerminarEdicion;
    private TextField txtBuscarCasa;
    private ComboBox<String> cbSectores;
    private Button btnModoMapa, btnModoSatelite;
    private Button btnZoomIn, btnZoomOut, btnReset;

    // Lotes y datos
    private final List<LotSlot> lotes = new ArrayList<>();
    private final Map<String, ViviendaModel> viviendasMap = new HashMap<>();
    private final Map<Integer, LotSlot> lotesPorNumero = new HashMap<>();
    private final Map<Integer, double[]> posicionesPorDefecto = new HashMap<>();
    private ViviendaModel viviendaSeleccionada;
    private LotSlot loteSeleccionado;

    // Gestión de viviendas pendientes y edición exclusiva de casa nueva
    private final List<ViviendaModel> viviendasPendientes = new ArrayList<>();
    private ViviendaModel viviendaEnColocacion = null;
    private LotSlot loteEnEdicionExclusiva = null;

    // Componentes del panel lateral para la alerta de viviendas pendientes
    private VBox boxAlertaPendientes;
    private Label lblTituloAlerta;
    private Label lblSubAlerta;
    private ComboBox<String> cbPendientes;
    private Button btnColocarSeleccionada;

    // Callbacks
    @FunctionalInterface
    public interface OnColocarViviendaListener {
        void onColocarVivienda(double x, double y, String sector);
    }

    private java.util.function.Consumer<ViviendaModel> onViviendaSelected;
    private Runnable onNuevaViviendaAction;
    private OnColocarViviendaListener onColocarViviendaListener;

    public CommunityMapPane() {
        getStyleClass().add("community-map-container");
        setAlignment(Pos.CENTER);
        setSpacing(14);
        setPadding(new Insets(4));
        setStyle("-fx-background-color: transparent;");

        inicializarPosicionesBase();
        inicializarMapa();
        inicializarPanelHerramientasLateral();
        inicializarInteraccion();

        // Envolver el panel lateral en un ScrollPane transparente
        sideToolScrollPane.setContent(sideToolPanel);
        sideToolScrollPane.setFitToWidth(true);
        sideToolScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sideToolScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sideToolScrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-width: 0;");
        sideToolScrollPane.setPrefWidth(285);
        sideToolScrollPane.setMinWidth(270);
        sideToolScrollPane.setMaxWidth(295);
        sideToolScrollPane.setPrefHeight(VIEWPORT_SIZE);
        sideToolScrollPane.setMaxHeight(VIEWPORT_SIZE);

        getChildren().addAll(sideToolScrollPane, viewportWrapper);

        // Restablecer vista inicial con la imagen 100% visible y centrada
        resetVista();
    }

    private void inicializarMapa() {
        // Lienzo del mapa maestro
        mapCanvas.setPrefSize(MAP_WIDTH, MAP_HEIGHT);
        mapCanvas.setMinSize(MAP_WIDTH, MAP_HEIGHT);
        mapCanvas.setMaxSize(MAP_WIDTH, MAP_HEIGHT);
        mapCanvas.setStyle("-fx-background-color: #dcd7ce;");

        // 1. Imagen oficial del plano maestro de la colonia
        try {
            var resourceStream = getClass().getResourceAsStream("/images/mapa-colonia-masterplan.jpg");
            if (resourceStream != null) {
                Image img = new Image(resourceStream);
                mapImageView = new ImageView(img);
                mapImageView.setFitWidth(MAP_WIDTH);
                mapImageView.setFitHeight(MAP_HEIGHT);
                mapImageView.setPreserveRatio(true);
                mapImageView.setSmooth(true);
                mapImageView.setCache(true);
                mapCanvas.getChildren().add(mapImageView);
            } else {
                dibujarFondoFallback();
            }
        } catch (Exception ex) {
            dibujarFondoFallback();
        }

        // 2. Capa de puntos de interés y hitos urbanísticos
        layerPuntosInteres = new Pane();
        layerPuntosInteres.setPickOnBounds(false);
        crearPuntosDeInteres();
        mapCanvas.getChildren().add(layerPuntosInteres);

        // 3. Capa dinámica de viviendas reales
        layerLotes = new Pane();
        layerLotes.setPickOnBounds(false);
        mapCanvas.getChildren().add(layerLotes);

        // 4. Cartel vacío amigable cuando aún no hay viviendas creadas
        crearBannerSinViviendas();

        // 5. Marcador de previsualización al colocar vivienda
        marcadorColocacionPreview = new Circle(13);
        marcadorColocacionPreview.setFill(Color.rgb(16, 185, 129, 0.35));
        marcadorColocacionPreview.setStroke(Color.web("#10b981"));
        marcadorColocacionPreview.setStrokeWidth(2.0);
        marcadorColocacionPreview.getStrokeDashArray().setAll(4.0, 4.0);
        marcadorColocacionPreview.setVisible(false);
        marcadorColocacionPreview.setMouseTransparent(true);
        mapCanvas.getChildren().add(marcadorColocacionPreview);

        // 6. Tarjeta Callout flotante de detalle de casa
        crearCalloutFlotante();

        mapGroup.getTransforms().setAll(mapTranslateTransform, mapScaleTransform);
        mapGroup.getChildren().add(mapCanvas);

        // 7. Configurar viewport cuadrado perfectamente recortado
        viewportPane.setPrefSize(VIEWPORT_SIZE, VIEWPORT_SIZE);
        viewportPane.setMinSize(VIEWPORT_SIZE, VIEWPORT_SIZE);
        viewportPane.setMaxSize(VIEWPORT_SIZE, VIEWPORT_SIZE);
        viewportPane.setStyle("-fx-background-color: #dcd7ce;");
        viewportPane.setPickOnBounds(true);

        Rectangle clipRect = new Rectangle(0, 0, VIEWPORT_SIZE, VIEWPORT_SIZE);
        clipRect.setArcWidth(16);
        clipRect.setArcHeight(16);
        viewportPane.setClip(clipRect);
        viewportPane.getChildren().add(mapGroup);

        viewportWrapper.setPrefSize(VIEWPORT_SIZE, VIEWPORT_SIZE);
        viewportWrapper.setMinSize(VIEWPORT_SIZE, VIEWPORT_SIZE);
        viewportWrapper.setMaxSize(VIEWPORT_SIZE, VIEWPORT_SIZE);
        viewportWrapper.setStyle("-fx-background-color: transparent; " +
                "-fx-background-radius: 12px; " +
                "-fx-border-color: #cbd5e1; " +
                "-fx-border-radius: 12px; " +
                "-fx-border-width: 1.5px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.14), 14, 0, 0, 4);");

        // Banner flotante superior para indicaciones cuando se coloca o mueve
        crearBannerModoFlotante();

        viewportWrapper.getChildren().addAll(viewportPane, bannerSinViviendas, bannerModoFlotante);
        StackPane.setAlignment(viewportPane, Pos.CENTER);
        StackPane.setAlignment(bannerSinViviendas, Pos.CENTER);
        StackPane.setAlignment(bannerModoFlotante, Pos.TOP_CENTER);
    }

    private void dibujarFondoFallback() {
        Rectangle bg = new Rectangle(0, 0, MAP_WIDTH, MAP_HEIGHT);
        bg.setFill(Color.web("#e8ece9"));
        mapCanvas.getChildren().add(bg);
    }

    private void crearPuntosDeInteres() {
        layerPuntosInteres.getChildren().addAll(
                crearBadgePOI("🌊 Laguna Norte", 160, 160),
                crearBadgePOI("🧭 Acceso Principal", 85, 440),
                crearBadgePOI("⛲ Glorieta Central", 505, 520),
                crearBadgePOI("🏞️ Parque Los Almendros", 205, 640),
                crearBadgePOI("🌅 Laguna Sur y Mirador", 575, 935),
                crearBadgePOI("🌿 Bulevar Los Próceres", 590, 215)
        );
    }

    private HBox crearBadgePOI(String texto, double x, double y) {
        HBox badge = new HBox(4);
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(3, 8, 3, 8));
        badge.setStyle("-fx-background-color: rgba(255, 255, 255, 0.92); " +
                "-fx-background-radius: 999; " +
                "-fx-border-color: rgba(148, 163, 184, 0.50); " +
                "-fx-border-radius: 999; " +
                "-fx-border-width: 1px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 5, 0, 0, 1);");

        Label label = new Label(texto);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9.5));
        label.setTextFill(Color.web("#1e293b"));
        badge.getChildren().add(label);

        badge.setLayoutX(x);
        badge.setLayoutY(y);
        badge.setCursor(Cursor.DEFAULT);
        return badge;
    }

    private void crearBannerSinViviendas() {
        bannerSinViviendas = new VBox(8);
        bannerSinViviendas.setAlignment(Pos.CENTER);
        bannerSinViviendas.setPadding(new Insets(16, 20, 16, 20));
        bannerSinViviendas.setPrefWidth(340);
        bannerSinViviendas.setLayoutX((MAP_WIDTH - 340) / 2.0);
        bannerSinViviendas.setLayoutY((MAP_HEIGHT - 120) / 2.0);
        bannerSinViviendas.setStyle("-fx-background-color: rgba(255, 255, 255, 0.96); " +
                "-fx-background-radius: 12px; " +
                "-fx-border-color: #cbd5e1; " +
                "-fx-border-radius: 12px; " +
                "-fx-border-width: 1.5px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 12, 0, 0, 3);");

        FontIcon icon = new FontIcon("fth-home");
        icon.setIconSize(24);
        icon.setIconColor(Color.web("#2563eb"));

        Label lblTitulo = new Label("No hay viviendas en el plano");
        lblTitulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblTitulo.setTextFill(Color.web("#1e293b"));

        Label lblDesc = new Label("El plano mostrará únicamente las viviendas reales registradas en la comunidad. Registra una vivienda para ubicarla aquí.");
        lblDesc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10.5));
        lblDesc.setTextFill(Color.web("#64748b"));
        lblDesc.setWrapText(true);
        lblDesc.setTextAlignment(TextAlignment.CENTER);

        HBox btnBox = new HBox(8);
        btnBox.setAlignment(Pos.CENTER);

        Button btnPonerPrimera = new Button("📍 Poner Casa en el Plano");
        btnPonerPrimera.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 6px; -fx-cursor: hand;");
        btnPonerPrimera.setOnAction(e -> alternarModoColocacion());

        Button btnRegistrarPrimera = new Button("+ Formulario");
        btnRegistrarPrimera.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 6px; -fx-cursor: hand;");
        btnRegistrarPrimera.setOnAction(e -> {
            if (onNuevaViviendaAction != null) onNuevaViviendaAction.run();
        });

        btnBox.getChildren().addAll(btnPonerPrimera, btnRegistrarPrimera);
        bannerSinViviendas.getChildren().addAll(icon, lblTitulo, lblDesc, btnBox);
        bannerSinViviendas.setVisible(false);
    }

    private void crearBannerModoFlotante() {
        bannerModoFlotante = new HBox(8);
        bannerModoFlotante.setAlignment(Pos.CENTER);
        bannerModoFlotante.setPadding(new Insets(6, 14, 6, 14));
        bannerModoFlotante.setMaxWidth(Region.USE_PREF_SIZE);
        bannerModoFlotante.setMaxHeight(Region.USE_PREF_SIZE);
        bannerModoFlotante.setVisible(false);
        bannerModoFlotante.setManaged(false);
        bannerModoFlotante.setStyle("-fx-background-color: rgba(30, 41, 59, 0.92); " +
                "-fx-background-radius: 999; " +
                "-fx-border-color: rgba(255, 255, 255, 0.25); " +
                "-fx-border-radius: 999; " +
                "-fx-border-width: 1px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 8, 0, 0, 2);");

        lblBannerModoFlotante = new Label("");
        lblBannerModoFlotante.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10.5));
        lblBannerModoFlotante.setTextFill(Color.WHITE);
        bannerModoFlotante.getChildren().add(lblBannerModoFlotante);

        StackPane.setAlignment(bannerModoFlotante, Pos.TOP_CENTER);
        StackPane.setMargin(bannerModoFlotante, new Insets(12, 0, 0, 0));
    }

    /**
     * Construye el panel lateral de herramientas ("a la par del mapa").
     * Contiene todos los botones con texto completo sin cortes y organizados por secciones.
     */
    private void inicializarPanelHerramientasLateral() {
        sideToolPanel.setAlignment(Pos.TOP_LEFT);
        sideToolPanel.setPadding(new Insets(14, 14, 14, 14));
        sideToolPanel.setSpacing(10);
        sideToolPanel.setStyle("-fx-background-color: #ffffff; " +
                "-fx-background-radius: 12px; " +
                "-fx-border-color: #cbd5e1; " +
                "-fx-border-radius: 12px; " +
                "-fx-border-width: 1px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10, 0, 0, 3);");

        // 1. Encabezado con título e insignia de conteo de viviendas
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        FontIcon mapIcon = new FontIcon("fth-map-pin");
        mapIcon.setIconSize(15);
        mapIcon.setIconColor(Color.web("#ea4335"));

        Label lblTitulo = new Label("Herramientas");
        lblTitulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblTitulo.setTextFill(Color.web("#1e293b"));

        Region spH = new Region();
        HBox.setHgrow(spH, Priority.ALWAYS);

        lblContadorCasas = new Label("0 viviendas");
        lblContadorCasas.setStyle("-fx-background-color: rgba(37, 99, 235, 0.12); " +
                "-fx-text-fill: #2563eb; -fx-font-weight: bold; -fx-font-size: 10px; " +
                "-fx-background-radius: 999; -fx-padding: 3 8;");

        header.getChildren().addAll(mapIcon, lblTitulo, spH, lblContadorCasas);

        Label lblSubtitulo = new Label("Gestión y navegación del plano comunal");
        lblSubtitulo.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
        lblSubtitulo.setTextFill(Color.web("#64748b"));

        // Alerta de viviendas pendientes por colocar en el plano
        boxAlertaPendientes = new VBox(6);
        boxAlertaPendientes.setPadding(new Insets(8, 10, 8, 10));
        boxAlertaPendientes.setStyle("-fx-background-color: #fffbeb; " +
                "-fx-background-radius: 8px; " +
                "-fx-border-color: #f59e0b; " +
                "-fx-border-radius: 8px; " +
                "-fx-border-width: 1.5px;");
        boxAlertaPendientes.setVisible(false);
        boxAlertaPendientes.setManaged(false);

        HBox headerAlerta = new HBox(6);
        headerAlerta.setAlignment(Pos.CENTER_LEFT);
        FontIcon warnIcon = new FontIcon("fth-alert-triangle");
        warnIcon.setIconSize(13);
        warnIcon.setIconColor(Color.web("#d97706"));

        lblTituloAlerta = new Label("Faltan casas por colocar");
        lblTituloAlerta.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10.5));
        lblTituloAlerta.setTextFill(Color.web("#b45309"));
        headerAlerta.getChildren().addAll(warnIcon, lblTituloAlerta);

        lblSubAlerta = new Label("Selecciona una vivienda registrada para ubicarla en el plano:");
        lblSubAlerta.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 9.5));
        lblSubAlerta.setTextFill(Color.web("#78350f"));
        lblSubAlerta.setWrapText(true);

        cbPendientes = new ComboBox<>();
        cbPendientes.setMaxWidth(Double.MAX_VALUE);
        cbPendientes.setStyle("-fx-font-size: 10px; -fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 5px;");

        btnColocarSeleccionada = new Button("📍 Colocar en el Plano");
        btnColocarSeleccionada.setMaxWidth(Double.MAX_VALUE);
        btnColocarSeleccionada.setStyle("-fx-background-color: #d97706; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10.5px; -fx-background-radius: 5px; -fx-padding: 5 8; -fx-cursor: hand;");
        btnColocarSeleccionada.setOnAction(e -> {
            int idx = cbPendientes.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < viviendasPendientes.size()) {
                iniciarColocacionVivienda(viviendasPendientes.get(idx));
            }
        });

        boxAlertaPendientes.setVisible(false);
        boxAlertaPendientes.setManaged(false);
        boxAlertaPendientes.getChildren().addAll(headerAlerta, lblSubAlerta, cbPendientes, btnColocarSeleccionada);

        // 2. Botones de acción principales (Gestión de Casas)
        btnPonerVivienda = new Button("📍 Poner Casa en el Plano");
        btnPonerVivienda.setMaxWidth(Double.MAX_VALUE);
        btnPonerVivienda.setVisible(false);
        btnPonerVivienda.setManaged(false);
        btnPonerVivienda.setTooltip(new Tooltip("Haz clic en cualquier calle o lote del plano para colocar una nueva vivienda"));
        btnPonerVivienda.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-font-size: 11.5px; -fx-background-radius: 7px; " +
                "-fx-padding: 8 12; -fx-cursor: hand;");
        btnPonerVivienda.setOnAction(e -> {
            if (modoColocacion) {
                alternarModoColocacion();
            } else {
                if (!viviendasPendientes.isEmpty()) {
                    int idx = cbPendientes.getSelectionModel().getSelectedIndex();
                    if (idx < 0 || idx >= viviendasPendientes.size()) idx = 0;
                    iniciarColocacionVivienda(viviendasPendientes.get(idx));
                } else {
                    alternarModoColocacion();
                }
            }
        });

        btnNuevaVivienda = new Button("+ Formulario Vivienda");
        btnNuevaVivienda.setMaxWidth(Double.MAX_VALUE);
        btnNuevaVivienda.setTooltip(new Tooltip("Abrir el formulario de registro de vivienda"));
        btnNuevaVivienda.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-font-size: 11.5px; -fx-background-radius: 7px; " +
                "-fx-padding: 8 12; -fx-cursor: hand;");
        btnNuevaVivienda.setOnAction(e -> {
            if (onNuevaViviendaAction != null) onNuevaViviendaAction.run();
        });

        btnModoEdicion = new Button("🛠️ Mover / Girar Casas");
        btnModoEdicion.setMaxWidth(Double.MAX_VALUE);
        btnModoEdicion.setVisible(true);
        btnModoEdicion.setManaged(true);
        btnModoEdicion.setTooltip(new Tooltip("Activar modo para arrastrar y rotar casas libremente"));
        btnModoEdicion.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: #d97706; " +
                "-fx-font-weight: bold; -fx-font-size: 11.5px; -fx-border-color: #f59e0b; " +
                "-fx-border-radius: 7px; -fx-background-radius: 7px; -fx-padding: 7 12; -fx-cursor: hand;");
        btnModoEdicion.setOnAction(e -> alternarModoEdicion());

        // 3. Cajas dinámicas de estado cuando un modo especial está activo
        crearCajasEstadoModos();

        // 4. Sección de Búsqueda y Filtros
        VBox secBusqueda = new VBox(6);
        Label lblSecBusqueda = new Label("BUSCAR Y FILTRAR");
        lblSecBusqueda.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        lblSecBusqueda.setTextFill(Color.web("#94a3b8"));

        HBox searchBox = new HBox(6);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(4, 8, 4, 8));
        searchBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 6px; -fx-border-color: #cbd5e1; -fx-border-radius: 6px;");
        FontIcon searchIcon = new FontIcon("fth-search");
        searchIcon.setIconSize(12);
        searchIcon.setIconColor(Color.web("#64748b"));

        txtBuscarCasa = new TextField();
        txtBuscarCasa.setPromptText("Buscar código o familia...");
        txtBuscarCasa.setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-font-size: 11px; -fx-text-fill: #1e293b;");
        HBox.setHgrow(txtBuscarCasa, Priority.ALWAYS);
        txtBuscarCasa.textProperty().addListener((obs, old, val) -> buscarCasa(val));
        searchBox.getChildren().addAll(searchIcon, txtBuscarCasa);

        cbSectores = new ComboBox<>(FXCollections.observableArrayList(
                "Todos los sectores",
                "Sector A (Noroeste)",
                "Sector B (Noreste)",
                "Sector C (Suroeste)",
                "Sector D (Sureste)"
        ));
        cbSectores.setValue("Todos los sectores");
        cbSectores.setMaxWidth(Double.MAX_VALUE);
        cbSectores.setStyle("-fx-font-size: 11px; -fx-pref-height: 28px;");
        cbSectores.setOnAction(e -> filtrarPorSector(cbSectores.getValue()));

        secBusqueda.getChildren().addAll(lblSecBusqueda, searchBox, cbSectores);

        // 5. Sección de Visualización y Zoom
        VBox secZoom = new VBox(6);
        Label lblSecZoom = new Label("VISTA Y ZOOM");
        lblSecZoom.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        lblSecZoom.setTextFill(Color.web("#94a3b8"));

        HBox viewToggle = new HBox(2);
        viewToggle.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 6px; -fx-padding: 2px;");
        btnModoMapa = new Button("Plano Maestro");
        btnModoMapa.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnModoMapa, Priority.ALWAYS);
        btnModoMapa.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #1e293b; -fx-font-weight: bold; -fx-font-size: 10.5px; -fx-background-radius: 4px; -fx-padding: 3 8; -fx-cursor: hand;");

        btnModoSatelite = new Button("Satélite");
        btnModoSatelite.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnModoSatelite, Priority.ALWAYS);
        btnModoSatelite.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-weight: 600; -fx-font-size: 10.5px; -fx-padding: 3 8; -fx-cursor: hand;");

        btnModoMapa.setOnAction(e -> cambiarEstilo(false));
        btnModoSatelite.setOnAction(e -> cambiarEstilo(true));
        viewToggle.getChildren().addAll(btnModoMapa, btnModoSatelite);

        HBox zoomRow = new HBox(6);
        zoomRow.setAlignment(Pos.CENTER);

        btnZoomIn = new Button("+ Acercar");
        btnZoomIn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnZoomIn, Priority.ALWAYS);
        btnZoomIn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #1e293b; -fx-font-weight: bold; -fx-font-size: 10.5px; -fx-background-radius: 6px; -fx-padding: 4 6; -fx-cursor: hand;");
        btnZoomIn.setOnAction(e -> zoom(0.20));

        btnZoomOut = new Button("- Alejar");
        btnZoomOut.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnZoomOut, Priority.ALWAYS);
        btnZoomOut.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #1e293b; -fx-font-weight: bold; -fx-font-size: 10.5px; -fx-background-radius: 6px; -fx-padding: 4 6; -fx-cursor: hand;");
        btnZoomOut.setOnAction(e -> zoom(-0.20));

        btnReset = new Button("⛶ Ver Todo");
        btnReset.setTooltip(new Tooltip("Mostrar el 100% del plano fijo y completo"));
        btnReset.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnReset, Priority.ALWAYS);
        btnReset.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #1e293b; -fx-font-weight: bold; -fx-font-size: 10.5px; -fx-background-radius: 6px; -fx-padding: 4 6; -fx-cursor: hand;");
        btnReset.setOnAction(e -> resetVista());

        zoomRow.getChildren().addAll(btnZoomIn, btnZoomOut, btnReset);

        Label lblZoomHelp = new Label("ℹ️ A escala 100%, el plano se muestra completo y fijo. Acércate para navegar entre calles.");
        lblZoomHelp.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 9.5));
        lblZoomHelp.setTextFill(Color.web("#94a3b8"));
        lblZoomHelp.setWrapText(true);

        secZoom.getChildren().addAll(lblSecZoom, viewToggle, zoomRow, lblZoomHelp);

        // 6. Leyenda de Estados
        VBox secLeyenda = new VBox(5);
        Label lblSecLeyenda = new Label("ESTADO DE VIVIENDAS");
        lblSecLeyenda.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        lblSecLeyenda.setTextFill(Color.web("#94a3b8"));

        HBox leg1 = crearItemLeyenda(Color.web("#10b981"), "Habitada (con rep.)");
        HBox leg2 = crearItemLeyenda(Color.web("#2563eb"), "Habitada (sin rep.)");
        HBox leg3 = crearItemLeyenda(Color.web("#f59e0b"), "Deshabitada");

        secLeyenda.getChildren().addAll(lblSecLeyenda, leg1, leg2, leg3);

        // Ensamblar panel lateral
        sideToolPanel.getChildren().addAll(
                header, lblSubtitulo,
                boxAlertaPendientes,
                btnPonerVivienda, btnNuevaVivienda, btnModoEdicion,
                boxEstadoColocacion, boxEstadoEdicion,
                new Separator(),
                secBusqueda,
                new Separator(),
                secZoom,
                new Separator(),
                secLeyenda
        );
    }

    private void crearCajasEstadoModos() {
        // Caja cuando Modo Colocación está activo
        boxEstadoColocacion = new VBox(6);
        boxEstadoColocacion.setPadding(new Insets(8, 10, 8, 10));
        boxEstadoColocacion.setVisible(false);
        boxEstadoColocacion.setManaged(false);
        boxEstadoColocacion.setStyle("-fx-background-color: #ecfdf5; -fx-background-radius: 8px; " +
                "-fx-border-color: #10b981; -fx-border-radius: 8px; -fx-border-width: 1.2px;");

        Label lblTitCol = new Label("📍 Modo Colocación Activo");
        lblTitCol.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10.5));
        lblTitCol.setTextFill(Color.web("#065f46"));

        Label lblMsgCol = new Label("Haz clic en cualquier calle o lote del plano para ubicar la nueva vivienda.");
        lblMsgCol.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 9.5));
        lblMsgCol.setTextFill(Color.web("#047857"));
        lblMsgCol.setWrapText(true);

        Button btnCancelarCol = new Button("✕ Cancelar Colocación");
        btnCancelarCol.setMaxWidth(Double.MAX_VALUE);
        btnCancelarCol.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 10px; -fx-border-color: #fca5a5; -fx-border-radius: 5px; -fx-background-radius: 5px; -fx-padding: 3 6; -fx-cursor: hand;");
        btnCancelarCol.setOnAction(e -> alternarModoColocacion());

        boxEstadoColocacion.getChildren().addAll(lblTitCol, lblMsgCol, btnCancelarCol);

        // Caja cuando Modo Mover/Girar está activo
        boxEstadoEdicion = new VBox(6);
        boxEstadoEdicion.setPadding(new Insets(8, 10, 8, 10));
        boxEstadoEdicion.setVisible(false);
        boxEstadoEdicion.setManaged(false);
        boxEstadoEdicion.setStyle("-fx-background-color: #fefce8; -fx-background-radius: 8px; " +
                "-fx-border-color: #f59e0b; -fx-border-radius: 8px; -fx-border-width: 1.2px;");

        Label lblTitEd = new Label("🛠️ Modo Mover y Girar Activo");
        lblTitEd.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10.5));
        lblTitEd.setTextFill(Color.web("#92400e"));

        Label lblMsgEd = new Label("Arrastra casas con el ratón. Usa la rueda o clic derecho para rotarlas.");
        lblMsgEd.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 9.5));
        lblMsgEd.setTextFill(Color.web("#b45309"));
        lblMsgEd.setWrapText(true);

        HBox edButtons = new HBox(4);
        btnGuardarCoordenadas = new Button("💾 Guardar");
        btnGuardarCoordenadas.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnGuardarCoordenadas, Priority.ALWAYS);
        btnGuardarCoordenadas.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-background-radius: 5px; -fx-padding: 3 6; -fx-cursor: hand;");
        btnGuardarCoordenadas.setOnAction(e -> guardarCoordenadas(true));

        btnResetearCoordenadas = new Button("↺ Restablecer");
        btnResetearCoordenadas.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnResetearCoordenadas, Priority.ALWAYS);
        btnResetearCoordenadas.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #475569; -fx-font-weight: 600; -fx-font-size: 10px; -fx-border-color: #cbd5e1; -fx-border-radius: 5px; -fx-background-radius: 5px; -fx-padding: 3 6; -fx-cursor: hand;");
        btnResetearCoordenadas.setOnAction(e -> resetearPosicionesIniciales());

        edButtons.getChildren().addAll(btnGuardarCoordenadas, btnResetearCoordenadas);

        btnTerminarEdicion = new Button("✓ Terminar Mover");
        btnTerminarEdicion.setMaxWidth(Double.MAX_VALUE);
        btnTerminarEdicion.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10.5px; -fx-background-radius: 5px; -fx-padding: 4 6; -fx-cursor: hand;");
        btnTerminarEdicion.setOnAction(e -> alternarModoEdicion());

        boxEstadoEdicion.getChildren().addAll(lblTitEd, lblMsgEd, edButtons, btnTerminarEdicion);
    }

    private void inicializarPosicionesBase() {
        posicionesPorDefecto.clear();

        // SECTOR A: Noroeste
        posicionesPorDefecto.put(1, new double[]{ 365, 320, 0 });
        posicionesPorDefecto.put(2, new double[]{ 425, 320, 0 });
        posicionesPorDefecto.put(3, new double[]{ 365, 380, 0 });
        posicionesPorDefecto.put(4, new double[]{ 425, 380, 0 });
        posicionesPorDefecto.put(5, new double[]{ 365, 440, 5 });
        posicionesPorDefecto.put(6, new double[]{ 425, 440, 5 });
        posicionesPorDefecto.put(7, new double[]{ 505, 340, 12 });
        posicionesPorDefecto.put(8, new double[]{ 505, 430, 12 });

        // SECTOR B: Noreste
        posicionesPorDefecto.put(9,  new double[]{ 645, 270, -10 });
        posicionesPorDefecto.put(10, new double[]{ 700, 330, -5 });
        posicionesPorDefecto.put(11, new double[]{ 645, 390, -10 });
        posicionesPorDefecto.put(12, new double[]{ 700, 450, -5 });
        posicionesPorDefecto.put(13, new double[]{ 645, 500, -8 });
        posicionesPorDefecto.put(14, new double[]{ 785, 340, 22 });
        posicionesPorDefecto.put(15, new double[]{ 835, 415, 25 });
        posicionesPorDefecto.put(16, new double[]{ 785, 495, 25 });

        // SECTOR C: Suroeste
        posicionesPorDefecto.put(17, new double[]{ 435, 565, 0 });
        posicionesPorDefecto.put(18, new double[]{ 490, 625, -6 });
        posicionesPorDefecto.put(19, new double[]{ 435, 685, 0 });
        posicionesPorDefecto.put(20, new double[]{ 490, 745, -6 });
        posicionesPorDefecto.put(21, new double[]{ 435, 805, 0 });
        posicionesPorDefecto.put(22, new double[]{ 345, 595, 28 });
        posicionesPorDefecto.put(23, new double[]{ 355, 675, 28 });

        // SECTOR D: Sureste
        posicionesPorDefecto.put(24, new double[]{ 610, 605, 6 });
        posicionesPorDefecto.put(25, new double[]{ 670, 665, 8 });
        posicionesPorDefecto.put(26, new double[]{ 615, 730, 6 });
        posicionesPorDefecto.put(27, new double[]{ 675, 790, 8 });
        posicionesPorDefecto.put(28, new double[]{ 760, 645, -24 });
        posicionesPorDefecto.put(29, new double[]{ 800, 720, -28 });
        posicionesPorDefecto.put(30, new double[]{ 820, 830, -32 });
    }

    /**
     * Vincula las viviendas reales de la base de datos de manera estrictamente 1-a-1.
     */
    /**
     * Vincula las viviendas de la base de datos:
     * - Las viviendas inactivas NO aparecen en el plano ni en pendientes.
     * - Las viviendas con coordenadas guardadas se colocan en el plano.
     * - Las viviendas sin coordenadas guardadas quedan PENDIENTES y activan la alerta.
     */
    public void setViviendas(List<ViviendaModel> listaViviendas) {
        lotes.clear();
        lotesPorNumero.clear();
        layerLotes.getChildren().clear();
        viviendasMap.clear();
        viviendasPendientes.clear();

        // 1. Filtrar casas inactivas (cuando la casa esté inactiva NO debe aparecer)
        List<ViviendaModel> viviendasActivas = new ArrayList<>();
        if (listaViviendas != null) {
            for (ViviendaModel v : listaViviendas) {
                if (v == null) continue;
                if (v.getEstado() != null && "INACTIVA".equalsIgnoreCase(v.getEstado().trim())) {
                    continue; // Las casas inactivas no aparecen en el mapa ni en pendientes
                }
                viviendasActivas.add(v);
            }
        }

        if (viviendasActivas.isEmpty()) {
            if (lblContadorCasas != null) {
                lblContadorCasas.setText("0 viviendas");
            }
            if (bannerSinViviendas != null) {
                bannerSinViviendas.setVisible(true);
            }
            actualizarAlertaPendientes();
            actualizarContadorCasas();
            ocultarCallout();
            return;
        }

        if (bannerSinViviendas != null) {
            bannerSinViviendas.setVisible(false);
        }

        Map<String, double[]> posicionesGuardadas = leerCoordenadasGuardadas();

        int index = 0;
        for (ViviendaModel v : viviendasActivas) {
            index++;
            String codigo = v.getCodigo() != null && !v.getCodigo().isBlank()
                    ? v.getCodigo().trim().toUpperCase(Locale.ROOT)
                    : String.format("VIV-%02d", index);

            viviendasMap.put(codigo, v);

            // Verificar si la vivienda ya tiene ubicación asignada en el plano
            double[] pos = buscarPosicionGuardada(posicionesGuardadas, codigo);
            if (pos != null) {
                int numeroPin = extraerNumero(codigo, index);
                String sector = v.getSector() != null && !v.getSector().isBlank() ? v.getSector() : "Sector A";
                String direccion = v.getDireccion() != null && !v.getDireccion().isBlank() ? v.getDireccion() : ("Casa " + codigo);
                double posX = pos[0];
                double posY = pos[1];
                double rotacion = pos.length > 2 ? pos[2] : 0.0;

                LotSlot lot = new LotSlot(numeroPin, index, codigo, sector, direccion, posX, posY, rotacion);
                lot.asignarVivienda(v);
                lot.actualizarModoEdicion(modoEdicion || (loteEnEdicionExclusiva == lot));
                lotes.add(lot);
                lotesPorNumero.put(numeroPin, lot);
                layerLotes.getChildren().add(lot.getNode());
            } else {
                // Aún NO tiene ubicación en el plano -> pasa a PENDIENTE
                viviendasPendientes.add(v);
            }
        }

        actualizarAlertaPendientes();
        actualizarContadorCasas();
    }

    private void actualizarAlertaPendientes() {
        boolean hayPendientes = !viviendasPendientes.isEmpty();

        if (boxAlertaPendientes != null) {
            boxAlertaPendientes.setVisible(hayPendientes);
            boxAlertaPendientes.setManaged(hayPendientes);
        }

        // El botón para colocar casas en el plano SOLO aparece a menos que haya una casa nueva por colocar
        if (btnPonerVivienda != null) {
            btnPonerVivienda.setVisible(hayPendientes);
            btnPonerVivienda.setManaged(hayPendientes);
        }

        if (btnModoEdicion != null) {
            btnModoEdicion.setVisible(true);
            btnModoEdicion.setManaged(true);
            if (modoEdicion) {
                btnModoEdicion.setText("✓ Terminar Mover");
                btnModoEdicion.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11.5px; -fx-background-radius: 7px; -fx-padding: 7 12; -fx-cursor: hand;");
            } else {
                btnModoEdicion.setText("🛠️ Mover / Girar Casas");
                btnModoEdicion.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: #d97706; -fx-font-weight: bold; -fx-font-size: 11.5px; -fx-border-color: #f59e0b; -fx-border-radius: 7px; -fx-background-radius: 7px; -fx-padding: 7 12; -fx-cursor: hand;");
            }
        }

        if (hayPendientes) {
            int cant = viviendasPendientes.size();
            lblTituloAlerta.setText(cant == 1 ? "⚠️ 1 casa por colocar" : String.format("⚠️ Faltan %d casas por colocar", cant));
            lblSubAlerta.setText(String.format("Hay %d %s sin ubicación en el plano. Selecciona una y colócala:",
                    cant, cant == 1 ? "vivienda registrada" : "viviendas registradas"));

            ObservableList<String> items = FXCollections.observableArrayList();
            for (ViviendaModel v : viviendasPendientes) {
                String dir = v.getDireccion() != null && !v.getDireccion().isBlank() ? (" • " + v.getDireccion()) : "";
                items.add(v.getCodigo() + " (" + (v.getSector() != null ? v.getSector() : "Sector") + ")" + dir);
            }
            cbPendientes.setItems(items);
            cbPendientes.getSelectionModel().selectFirst();
        }
    }

    private void actualizarContadorCasas() {
        if (lblContadorCasas == null) return;
        int colocadas = lotes.size();
        int pendientes = viviendasPendientes.size();
        if (pendientes > 0) {
            lblContadorCasas.setText(String.format("%d colocadas (%d pend.)", colocadas, pendientes));
            lblContadorCasas.setStyle("-fx-background-color: rgba(245, 158, 11, 0.15); " +
                    "-fx-text-fill: #b45309; -fx-font-weight: bold; -fx-font-size: 10px; " +
                    "-fx-background-radius: 999; -fx-padding: 3 8;");
        } else {
            lblContadorCasas.setText(colocadas == 1 ? "1 vivienda" : String.format("%d viviendas", colocadas));
            lblContadorCasas.setStyle("-fx-background-color: rgba(37, 99, 235, 0.12); " +
                    "-fx-text-fill: #2563eb; -fx-font-weight: bold; -fx-font-size: 10px; " +
                    "-fx-background-radius: 999; -fx-padding: 3 8;");
        }
    }

    public int getCantidadPendientes() {
        return viviendasPendientes.size();
    }

    public void iniciarColocacionVivienda(ViviendaModel v) {
        if (v == null) return;
        this.viviendaEnColocacion = v;
        if (modoEdicion) {
            alternarModoEdicion();
        }
        modoColocacion = true;
        boxEstadoColocacion.setVisible(true);
        boxEstadoColocacion.setManaged(true);

        btnPonerVivienda.setText("✕ Cancelar Colocación");
        btnPonerVivienda.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11.5px; -fx-background-radius: 7px; -fx-padding: 8 12; -fx-cursor: hand;");

        viewportPane.setCursor(Cursor.CROSSHAIR);
        mapCanvas.setCursor(Cursor.CROSSHAIR);
        if (marcadorColocacionPreview != null) {
            marcadorColocacionPreview.setVisible(true);
        }

        mostrarNotificacionFlotante("📍 Haz clic en el plano para ubicar la casa " + v.getCodigo() + " (" + (v.getSector() != null ? v.getSector() : "Sector") + ")");
    }

    private void colocarViviendaEnPosicion(double clickX, double clickY) {
        String sector = detectarSectorPorCoordenada(clickX, clickY);

        if (viviendaEnColocacion != null) {
            ViviendaModel v = viviendaEnColocacion;
            viviendaEnColocacion = null;
            modoColocacion = false;
            boxEstadoColocacion.setVisible(false);
            boxEstadoColocacion.setManaged(false);
            btnPonerVivienda.setText("📍 Poner Casa en el Plano");
            btnPonerVivienda.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11.5px; -fx-background-radius: 7px; -fx-padding: 8 12; -fx-cursor: hand;");
            Cursor cur = isAtMinZoom() ? Cursor.DEFAULT : Cursor.OPEN_HAND;
            viewportPane.setCursor(cur);
            mapCanvas.setCursor(cur);
            if (marcadorColocacionPreview != null) {
                marcadorColocacionPreview.setVisible(false);
            }

            String codigo = v.getCodigo() != null && !v.getCodigo().isBlank()
                    ? v.getCodigo().trim().toUpperCase(Locale.ROOT)
                    : "CASA";
            int index = lotes.size() + 1;
            int numeroPin = extraerNumero(codigo, index);
            String sec = (v.getSector() != null && !v.getSector().isBlank()) ? v.getSector() : sector;
            String direccion = (v.getDireccion() != null && !v.getDireccion().isBlank()) ? v.getDireccion() : ("Casa " + codigo);

            // Registrar posición en JSON persistente
            registrarPosicionVivienda(codigo, clickX, clickY, 0.0);

            // Crear y agregar el lote al plano
            LotSlot nuevoLote = new LotSlot(numeroPin, index, codigo, sec, direccion, clickX, clickY, 0.0);
            nuevoLote.asignarVivienda(v);
            lotes.add(nuevoLote);
            lotesPorNumero.put(numeroPin, nuevoLote);
            layerLotes.getChildren().add(nuevoLote.getNode());

            // Remover de pendientes y actualizar alerta
            viviendasPendientes.remove(v);
            actualizarAlertaPendientes();
            actualizarContadorCasas();

            // Activar edición EXCLUSIVA para la nueva casa (solo esta casa se puede mover y girar)
            activarEdicionExclusivaCasaNueva(nuevoLote);
        } else {
            alternarModoColocacion();
            if (onColocarViviendaListener != null) {
                onColocarViviendaListener.onColocarVivienda(clickX, clickY, sector);
            }
        }
    }

    private double[] buscarPosicionGuardada(Map<String, double[]> posicionesGuardadas, String codigo) {
        if (posicionesGuardadas == null || codigo == null || codigo.isBlank()) return null;
        String cod = codigo.trim().toUpperCase(Locale.ROOT);
        if (posicionesGuardadas.containsKey(cod)) {
            return posicionesGuardadas.get(cod);
        }
        for (Map.Entry<String, double[]> entry : posicionesGuardadas.entrySet()) {
            if (entry.getKey().trim().equalsIgnoreCase(cod)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private int extraerNumero(String codigo, int defaultIndex) {
        if (codigo == null || codigo.isBlank()) return defaultIndex;
        try {
            String digits = codigo.replaceAll("\\D+", "");
            if (!digits.isBlank()) {
                return Integer.parseInt(digits);
            }
        } catch (Exception ignored) {}
        return defaultIndex;
    }

    private double[] calcularPosicionNueva(int index, String sector) {
        double baseX = 470;
        double baseY = 520;
        double rot = 0;
        if (sector != null) {
            String s = sector.toUpperCase(Locale.ROOT);
            if (s.contains("A")) { baseX = 390; baseY = 360; rot = 5; }
            else if (s.contains("B")) { baseX = 730; baseY = 360; rot = 15; }
            else if (s.contains("C")) { baseX = 390; baseY = 690; rot = 10; }
            else if (s.contains("D")) { baseX = 730; baseY = 690; rot = -15; }
        }
        double offsetX = ((index * 36) % 150) - 75;
        double offsetY = ((index * 28) % 120) - 60;
        return new double[]{ baseX + offsetX, baseY + offsetY, rot };
    }

    private Map<String, double[]> leerCoordenadasGuardadas() {
        Map<String, double[]> map = new HashMap<>();
        File archivo = buscarArchivoPosiciones();
        if (archivo == null || !archivo.exists()) return map;

        try {
            String content = Files.readString(archivo.toPath());
            JsonNode root = jsonMapper.readTree(content);
            if (root != null && root.isObject()) {
                root.fields().forEachRemaining(entry -> {
                    String codigo = entry.getKey().trim().toUpperCase(Locale.ROOT);
                    JsonNode node = entry.getValue();
                    if (node.has("x") && node.has("y")) {
                        double x = node.get("x").asDouble();
                        double y = node.get("y").asDouble();
                        double rot = node.has("rotacion") ? node.get("rotacion").asDouble() : 0.0;
                        map.put(codigo, new double[]{ x, y, rot });
                    }
                });
            }
        } catch (Exception ignored) {}
        return map;
    }

    private void guardarCoordenadas(boolean copiarAlPortapapeles) {
        try {
            ObjectNode root = jsonMapper.createObjectNode();

            for (LotSlot lot : lotes) {
                ObjectNode lotNode = root.putObject(lot.codigo);
                lotNode.put("numero", lot.numero);
                lotNode.put("sector", lot.sector);
                lotNode.put("x", Math.round(lot.posX));
                lotNode.put("y", Math.round(lot.posY));
                lotNode.put("rotacion", Math.round(lot.rotacion));
            }

            String jsonString = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);

            List<File> destinos = List.of(
                    new File("frontend/src/main/resources", POSICIONES_FILENAME),
                    new File("src/main/resources", POSICIONES_FILENAME),
                    new File("frontend", POSICIONES_FILENAME),
                    new File(System.getProperty("user.home"), ".asociacioncomunal/" + POSICIONES_FILENAME),
                    new File(POSICIONES_FILENAME)
            );

            for (File f : destinos) {
                try {
                    if (f.getParentFile() != null && !f.getParentFile().exists()) {
                        f.getParentFile().mkdirs();
                    }
                    Files.writeString(f.toPath(), jsonString);
                } catch (Exception ignored) {}
            }

            if (copiarAlPortapapeles) {
                ClipboardContent content = new ClipboardContent();
                content.putString(jsonString);
                Clipboard.getSystemClipboard().setContent(content);

                mostrarNotificacionFlotante("✓ ¡Coordenadas y giros guardados en JSON y copiados al portapapeles!");
            }
        } catch (Exception ex) {
            mostrarNotificacionFlotante("Error al guardar coordenadas: " + ex.getMessage());
        }
    }

    private void resetearPosicionesIniciales() {
        for (LotSlot lot : lotes) {
            double[] pos = posicionesPorDefecto.get(lot.numero);
            if (pos != null) {
                lot.setCoordenadas(pos[0], pos[1]);
                lot.setRotacion(pos.length > 2 ? pos[2] : 0.0);
            }
        }
        guardarCoordenadas(false);
        mostrarNotificacionFlotante("↺ Posiciones y giros restablecidos a los valores por defecto.");
    }

    public void registrarPosicionVivienda(String codigo, double x, double y, double rotacion) {
        if (codigo == null || codigo.isBlank()) return;
        String cod = codigo.trim().toUpperCase(Locale.ROOT);
        Map<String, double[]> map = leerCoordenadasGuardadas();
        map.put(cod, new double[]{ Math.round(x), Math.round(y), Math.round(rotacion) });
        escribirCoordenadasJson(map);
    }

    private void escribirCoordenadasJson(Map<String, double[]> map) {
        try {
            ObjectNode root = jsonMapper.createObjectNode();
            for (Map.Entry<String, double[]> entry : map.entrySet()) {
                String cod = entry.getKey();
                double[] p = entry.getValue();
                ObjectNode lotNode = root.putObject(cod);
                lotNode.put("numero", extraerNumero(cod, 1));
                lotNode.put("x", Math.round(p[0]));
                lotNode.put("y", Math.round(p[1]));
                lotNode.put("rotacion", p.length > 2 ? Math.round(p[2]) : 0);
            }
            String jsonString = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            List<File> destinos = List.of(
                    new File("frontend/src/main/resources", POSICIONES_FILENAME),
                    new File("src/main/resources", POSICIONES_FILENAME),
                    new File("frontend", POSICIONES_FILENAME),
                    new File(System.getProperty("user.home"), ".asociacioncomunal/" + POSICIONES_FILENAME),
                    new File(POSICIONES_FILENAME)
            );
            for (File f : destinos) {
                try {
                    if (f.getParentFile() != null && !f.getParentFile().exists()) {
                        f.getParentFile().mkdirs();
                    }
                    Files.writeString(f.toPath(), jsonString);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    public static String detectarSectorPorCoordenada(double x, double y) {
        if (x < 540) {
            return y < 530 ? "Sector A" : "Sector C";
        } else {
            return y < 530 ? "Sector B" : "Sector D";
        }
    }

    public int getCantidadViviendas() {
        return lotes.size();
    }

    public void enfocarYEditarVivienda(String codigo, String mensaje) {
        if (codigo == null || codigo.isBlank()) return;
        String cod = codigo.trim().toUpperCase(Locale.ROOT);
        LotSlot match = null;
        for (LotSlot l : lotes) {
            if (l.codigo.equalsIgnoreCase(cod)) {
                match = l;
                break;
            }
        }
        if (match != null) {
            centrarEnPunto(match.posX, match.posY, 1.45);
            seleccionarLote(match);
            activarEdicionExclusivaCasaNueva(match);
            if (mensaje != null) {
                mostrarNotificacionFlotante(mensaje);
            }
        }
    }

    private File buscarArchivoPosiciones() {
        List<File> rutas = List.of(
                new File("frontend/src/main/resources", POSICIONES_FILENAME),
                new File("src/main/resources", POSICIONES_FILENAME),
                new File("frontend", POSICIONES_FILENAME),
                new File(System.getProperty("user.home"), ".asociacioncomunal/" + POSICIONES_FILENAME),
                new File(POSICIONES_FILENAME)
        );
        for (File f : rutas) {
            if (f.exists()) return f;
        }
        return null;
    }

    public void activarModoEdicionConMensaje(String mensaje) {
        if (!modoEdicion) {
            alternarModoEdicion();
        }
        if (mensaje != null) {
            mostrarNotificacionFlotante(mensaje);
        }
    }

    public void alternarModoEdicion() {
        modoEdicion = !modoEdicion;
        if (modoEdicion && modoColocacion) {
            alternarModoColocacion();
        }

        boxEstadoEdicion.setVisible(modoEdicion);
        boxEstadoEdicion.setManaged(modoEdicion);

        if (modoEdicion) {
            btnModoEdicion.setText("✓ Terminar Mover");
            btnModoEdicion.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11.5px; -fx-background-radius: 7px; -fx-padding: 7 12; -fx-cursor: hand;");
            mostrarNotificacionFlotante("🛠️ Arrastra las casas para moverlas • Rueda del ratón o clic derecho para girar.");
        } else {
            btnModoEdicion.setText("🛠️ Mover / Girar Casas");
            btnModoEdicion.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: #d97706; -fx-font-weight: bold; -fx-font-size: 11.5px; -fx-border-color: #f59e0b; -fx-border-radius: 7px; -fx-background-radius: 7px; -fx-padding: 7 12; -fx-cursor: hand;");
            ocultarNotificacionFlotante();
            guardarCoordenadas(false);
            ocultarCallout();
        }

        for (LotSlot lot : lotes) {
            lot.actualizarModoEdicion(modoEdicion);
        }
    }

    public void alternarModoColocacion() {
        modoColocacion = !modoColocacion;
        if (!modoColocacion) {
            viviendaEnColocacion = null;
        }
        if (modoColocacion && modoEdicion) {
            alternarModoEdicion();
        }

        boxEstadoColocacion.setVisible(modoColocacion);
        boxEstadoColocacion.setManaged(modoColocacion);

        if (modoColocacion) {
            btnPonerVivienda.setText("✕ Cancelar Colocación");
            btnPonerVivienda.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11.5px; -fx-background-radius: 7px; -fx-padding: 8 12; -fx-cursor: hand;");
            viewportPane.setCursor(Cursor.CROSSHAIR);
            mapCanvas.setCursor(Cursor.CROSSHAIR);
            if (marcadorColocacionPreview != null) {
                marcadorColocacionPreview.setVisible(true);
            }
            mostrarNotificacionFlotante("📍 Haz clic en cualquier lugar del plano para colocar la nueva vivienda.");
        } else {
            btnPonerVivienda.setText("📍 Poner Casa en el Plano");
            btnPonerVivienda.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11.5px; -fx-background-radius: 7px; -fx-padding: 8 12; -fx-cursor: hand;");
            Cursor cur = isAtMinZoom() ? Cursor.DEFAULT : Cursor.OPEN_HAND;
            viewportPane.setCursor(cur);
            mapCanvas.setCursor(cur);
            if (marcadorColocacionPreview != null) {
                marcadorColocacionPreview.setVisible(false);
            }
            ocultarNotificacionFlotante();
        }
    }

    private void mostrarNotificacionFlotante(String mensaje) {
        if (bannerModoFlotante != null && lblBannerModoFlotante != null) {
            lblBannerModoFlotante.setText(mensaje);
            bannerModoFlotante.setVisible(true);
            bannerModoFlotante.setManaged(true);
        }
    }

    private void ocultarNotificacionFlotante() {
        if (bannerModoFlotante != null) {
            bannerModoFlotante.setVisible(false);
            bannerModoFlotante.setManaged(false);
        }
    }

    private HBox crearItemLeyenda(Color color, String texto) {
        Circle circle = new Circle(4.5, color);
        Label label = new Label(texto);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        label.setTextFill(Color.web("#334155"));
        HBox item = new HBox(6, circle, label);
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private void crearCalloutFlotante() {
        calloutCard = new VBox(4);
        calloutCard.setPadding(new Insets(8, 12, 8, 12));
        calloutCard.setPrefWidth(220);
        calloutCard.setMinWidth(220);
        calloutCard.setMaxWidth(220);
        calloutCard.setVisible(false);
        calloutCard.setStyle("-fx-background-color: #ffffff; " +
                "-fx-background-radius: 8px; " +
                "-fx-border-radius: 8px; " +
                "-fx-border-color: #cbd5e1; " +
                "-fx-border-width: 1px;");

        DropShadow shadow = new DropShadow(10, Color.rgb(0, 0, 0, 0.24));
        shadow.setOffsetY(3);
        calloutCard.setEffect(shadow);

        mapCanvas.getChildren().add(calloutCard);
    }

    private void mostrarCalloutParaLote(LotSlot lot) {
        if (modoEdicion) {
            mostrarCalloutEdicion(lot);
            return;
        }

        calloutCard.getChildren().clear();

        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblCode = new Label("📍 Casa " + lot.codigo);
        lblCode.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblCode.setTextFill(Color.web("#1e293b"));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        String estado = (lot.vivienda != null && lot.vivienda.getEstado() != null)
                ? lot.vivienda.getEstado()
                : (lot.vivienda != null ? "ACTIVA" : "DISPONIBLE");

        Label badge = new Label(estado);
        badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        if ("ACTIVA".equalsIgnoreCase(estado) || "OCUPADA".equalsIgnoreCase(estado)) {
            badge.setStyle("-fx-background-color: rgba(16, 185, 129, 0.15); -fx-text-fill: #059669; -fx-padding: 1 6; -fx-background-radius: 999;");
        } else {
            badge.setStyle("-fx-background-color: rgba(245, 158, 11, 0.15); -fx-text-fill: #d97706; -fx-padding: 1 6; -fx-background-radius: 999;");
        }
        header.getChildren().addAll(lblCode, sp, badge);

        Label lblDir = new Label(lot.sector + " • " + lot.direccion);
        lblDir.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
        lblDir.setTextFill(Color.web("#64748b"));

        VBox infoBox = new VBox(2);
        if (lot.vivienda != null) {
            String rep = lot.vivienda.getRepresentante() != null && !lot.vivienda.getRepresentante().isBlank()
                    ? lot.vivienda.getRepresentante()
                    : "Sin representante asignado";
            Label lblRep = new Label("👤 " + rep);
            lblRep.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            lblRep.setTextFill(Color.web("#334155"));

            int total = lot.vivienda.getTotalResidentes();
            Label lblHab = new Label(String.format("👨‍👩‍👧 %d hab. (%d adultos, %d menores)",
                    total, lot.vivienda.getAdultos(), lot.vivienda.getMenores()));
            lblHab.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
            lblHab.setTextFill(Color.web("#64748b"));
            infoBox.getChildren().addAll(lblRep, lblHab);
        } else {
            Label lblVacio = new Label("ℹ️ Vivienda en censo.");
            lblVacio.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
            lblVacio.setTextFill(Color.web("#94a3b8"));
            infoBox.getChildren().add(lblVacio);
        }

        Label lblTip = new Label("💡 Clic para ver expediente completo");
        lblTip.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 9));
        lblTip.setTextFill(Color.web("#2563eb"));

        calloutCard.getChildren().addAll(header, lblDir, infoBox, lblTip);
        calloutCard.applyCss();
        calloutCard.layout();

        posicionarCalloutEnLote(lot);
    }

    private void mostrarCalloutEdicion(LotSlot lot) {
        calloutCard.getChildren().clear();

        HBox headerEd = new HBox(6);
        headerEd.setAlignment(Pos.CENTER_LEFT);
        Label lblTitulo = new Label("📍 Casa " + lot.codigo + " (" + lot.sector + ")");
        lblTitulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        lblTitulo.setTextFill(Color.web("#92400e"));
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label badgeAngulo = new Label(String.format("%.0f°", lot.rotacion));
        badgeAngulo.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-font-weight: bold; -fx-padding: 1 6; -fx-background-radius: 999; -fx-font-size: 10px;");
        headerEd.getChildren().addAll(lblTitulo, sp, badgeAngulo);

        Label lblCoord = new Label(String.format("Posición: X: %.0f, Y: %.0f", lot.posX, lot.posY));
        lblCoord.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
        lblCoord.setTextFill(Color.web("#1e293b"));

        HBox rotateBar = new HBox(4);
        rotateBar.setAlignment(Pos.CENTER_LEFT);

        Label lblGirar = new Label("Girar:");
        lblGirar.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9.5));
        lblGirar.setTextFill(Color.web("#64748b"));

        Button btnGirarIzq = new Button("↺ -1°");
        btnGirarIzq.setTooltip(new Tooltip("Girar 1° a la izquierda"));
        btnGirarIzq.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-font-size: 9.5px; -fx-padding: 2 6; -fx-background-radius: 4px; -fx-cursor: hand;");
        btnGirarIzq.setOnAction(e -> {
            lot.girar(-1.0);
            mostrarCalloutEdicion(lot);
        });

        Button btnGirarDer = new Button("↻ +1°");
        btnGirarDer.setTooltip(new Tooltip("Girar 1° a la derecha"));
        btnGirarDer.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-font-size: 9.5px; -fx-padding: 2 6; -fx-background-radius: 4px; -fx-cursor: hand;");
        btnGirarDer.setOnAction(e -> {
            lot.girar(1.0);
            mostrarCalloutEdicion(lot);
        });

        Button btnGirarReset = new Button("0°");
        btnGirarReset.setTooltip(new Tooltip("Restablecer a 0° horizontal"));
        btnGirarReset.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; -fx-font-weight: bold; -fx-font-size: 9.5px; -fx-padding: 2 6; -fx-background-radius: 4px; -fx-cursor: hand;");
        btnGirarReset.setOnAction(e -> {
            lot.setRotacion(0.0);
            guardarCoordenadas(false);
            mostrarCalloutEdicion(lot);
        });

        rotateBar.getChildren().addAll(lblGirar, btnGirarIzq, btnGirarDer, btnGirarReset);

        Label lblHelp = new Label("💡 Rueda del ratón o botones para girar grado a grado (1° en 1°).");
        lblHelp.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 9));
        lblHelp.setTextFill(Color.web("#b45309"));

        calloutCard.getChildren().addAll(headerEd, lblCoord, rotateBar, lblHelp);
        calloutCard.applyCss();
        calloutCard.layout();

        posicionarCalloutEnLote(lot);
    }

    public void activarEdicionExclusivaCasaNueva(LotSlot nuevoLote) {
        if (nuevoLote == null) return;
        this.loteEnEdicionExclusiva = nuevoLote;

        // Solo esta casa nueva entra en modo editable, las demás quedan firmemente bloqueadas
        for (LotSlot l : lotes) {
            l.actualizarModoEdicion(l == nuevoLote);
        }

        centrarEnPunto(nuevoLote.posX, nuevoLote.posY, 1.45);
        seleccionarLote(nuevoLote);

        mostrarNotificacionFlotante("🛠️ Casa " + nuevoLote.codigo + " ubicada. Arrástrala para afinar o gira con la rueda del ratón • Clic en 'Fijar' al terminar.");
        mostrarCalloutEdicionCasaNueva(nuevoLote);
    }

    private void mostrarCalloutEdicionCasaNueva(LotSlot lot) {
        calloutCard.getChildren().clear();

        HBox headerEd = new HBox(6);
        headerEd.setAlignment(Pos.CENTER_LEFT);
        Label lblTitulo = new Label("📍 Casa " + lot.codigo + " (Nueva)");
        lblTitulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        lblTitulo.setTextFill(Color.web("#065f46"));
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label badgeAngulo = new Label(String.format("%.0f°", lot.rotacion));
        badgeAngulo.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-font-weight: bold; -fx-padding: 1 6; -fx-background-radius: 999; -fx-font-size: 10px;");
        headerEd.getChildren().addAll(lblTitulo, sp, badgeAngulo);

        Label lblCoord = new Label(String.format("Ubicación: X: %.0f, Y: %.0f", lot.posX, lot.posY));
        lblCoord.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
        lblCoord.setTextFill(Color.web("#1e293b"));

        HBox rotateBar = new HBox(4);
        rotateBar.setAlignment(Pos.CENTER_LEFT);

        Label lblGirar = new Label("Girar:");
        lblGirar.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9.5));
        lblGirar.setTextFill(Color.web("#64748b"));

        Button btnGirarIzq = new Button("↺ -1°");
        btnGirarIzq.setTooltip(new Tooltip("Girar 1° a la izquierda"));
        btnGirarIzq.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-font-size: 9.5px; -fx-padding: 2 6; -fx-background-radius: 4px; -fx-cursor: hand;");
        btnGirarIzq.setOnAction(e -> {
            lot.girar(-1.0);
            mostrarCalloutEdicionCasaNueva(lot);
        });

        Button btnGirarDer = new Button("↻ +1°");
        btnGirarDer.setTooltip(new Tooltip("Girar 1° a la derecha"));
        btnGirarDer.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-font-size: 9.5px; -fx-padding: 2 6; -fx-background-radius: 4px; -fx-cursor: hand;");
        btnGirarDer.setOnAction(e -> {
            lot.girar(1.0);
            mostrarCalloutEdicionCasaNueva(lot);
        });

        Button btnGirarReset = new Button("0°");
        btnGirarReset.setTooltip(new Tooltip("Restablecer a 0° horizontal"));
        btnGirarReset.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; -fx-font-weight: bold; -fx-font-size: 9.5px; -fx-padding: 2 6; -fx-background-radius: 4px; -fx-cursor: hand;");
        btnGirarReset.setOnAction(e -> {
            lot.setRotacion(0.0);
            guardarCoordenadas(false);
            mostrarCalloutEdicionCasaNueva(lot);
        });

        rotateBar.getChildren().addAll(lblGirar, btnGirarIzq, btnGirarDer, btnGirarReset);

        Button btnFijar = new Button("✓ Fijar Casa en el Plano");
        btnFijar.setMaxWidth(Double.MAX_VALUE);
        btnFijar.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 6px; -fx-cursor: hand;");
        btnFijar.setOnAction(e -> fijarCasaNueva(lot));

        Label lblHelp = new Label("💡 Arrastra la casa para afinar su posición. Gira grado a grado (1° en 1°) con la rueda del ratón o botones.");
        lblHelp.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 9));
        lblHelp.setTextFill(Color.web("#047857"));
        lblHelp.setWrapText(true);

        calloutCard.getChildren().addAll(headerEd, lblCoord, rotateBar, btnFijar, lblHelp);
        calloutCard.applyCss();
        calloutCard.layout();

        posicionarCalloutEnLote(lot);
    }

    public void fijarCasaNueva(LotSlot lot) {
        if (lot != null) {
            lot.actualizarModoEdicion(false);
        }
        loteEnEdicionExclusiva = null;
        guardarCoordenadas(false);
        ocultarCallout();
        ocultarNotificacionFlotante();
        mostrarNotificacionFlotante("✓ Casa " + (lot != null ? lot.codigo : "") + " fijada correctamente en el plano.");
        PauseTransition pt = new PauseTransition(Duration.seconds(3));
        pt.setOnFinished(e -> ocultarNotificacionFlotante());
        pt.play();
    }

    private void posicionarCalloutEnLote(LotSlot lot) {
        double posX = Math.max(10, Math.min(MAP_WIDTH - 230, lot.posX - 90));
        double posY = lot.posY > 110 ? (lot.posY - 92) : (lot.posY + 44);
        calloutCard.setLayoutX(posX);
        calloutCard.setLayoutY(posY);
        calloutCard.toFront();
        calloutCard.setVisible(true);
    }

    private void ocultarCallout() {
        if (calloutCard != null) calloutCard.setVisible(false);
    }

    /**
     * Comprueba si el plano está en el nivel de zoom mínimo (escala 100% que muestra todo el plano).
     */
    public boolean isAtMinZoom() {
        return scaleFactor.get() <= MIN_SCALE + 0.005;
    }

    private void inicializarInteraccion() {
        viewportPane.setOnMouseMoved(e -> {
            if (modoColocacion) {
                Point2D p = mapCanvas.sceneToLocal(e.getSceneX(), e.getSceneY());
                if (marcadorColocacionPreview != null) {
                    marcadorColocacionPreview.setCenterX(p.getX());
                    marcadorColocacionPreview.setCenterY(p.getY());
                }
            } else if (isAtMinZoom()) {
                viewportPane.setCursor(Cursor.DEFAULT);
                mapCanvas.setCursor(Cursor.DEFAULT);
            } else {
                viewportPane.setCursor(Cursor.OPEN_HAND);
                mapCanvas.setCursor(Cursor.OPEN_HAND);
            }
        });

        viewportPane.setOnMouseClicked(e -> {
            if (modoColocacion && e.getButton() == MouseButton.PRIMARY) {
                e.consume();
                Point2D p = mapCanvas.sceneToLocal(e.getSceneX(), e.getSceneY());
                double clickX = Math.round(Math.max(25, Math.min(MAP_WIDTH - 25, p.getX())));
                double clickY = Math.round(Math.max(20, Math.min(MAP_HEIGHT - 20, p.getY())));
                colocarViviendaEnPosicion(clickX, clickY);
            } else if (!modoColocacion && loteEnEdicionExclusiva == null && e.getButton() == MouseButton.PRIMARY) {
                deseleccionarLote();
                ocultarCallout();
                if (onViviendaSelected != null) {
                    onViviendaSelected.accept(null);
                }
            }
        });

        viewportPane.setOnMousePressed(e -> {
            // Cuando está al mínimo de zoom, NO se puede mover (bloqueado para mostrar todo el plano fijo)
            if (modoColocacion || isAtMinZoom()) {
                return;
            }
            if (e.getButton() == MouseButton.PRIMARY) {
                dragStartX = e.getSceneX() - mapTranslateX;
                dragStartY = e.getSceneY() - mapTranslateY;
                viewportPane.setCursor(Cursor.CLOSED_HAND);
                mapCanvas.setCursor(Cursor.CLOSED_HAND);
            }
        });

        viewportPane.setOnMouseDragged(e -> {
            // Cuando está al mínimo de zoom, NO se puede mover
            if (modoColocacion || isAtMinZoom()) {
                return;
            }
            if (e.getButton() == MouseButton.PRIMARY) {
                mapTranslateX = e.getSceneX() - dragStartX;
                mapTranslateY = e.getSceneY() - dragStartY;
                actualizarTransformacion();
            }
        });

        viewportPane.setOnMouseReleased(e -> {
            if (modoColocacion) {
                viewportPane.setCursor(Cursor.CROSSHAIR);
                mapCanvas.setCursor(Cursor.CROSSHAIR);
            } else if (isAtMinZoom()) {
                viewportPane.setCursor(Cursor.DEFAULT);
                mapCanvas.setCursor(Cursor.DEFAULT);
            } else {
                viewportPane.setCursor(Cursor.OPEN_HAND);
                mapCanvas.setCursor(Cursor.OPEN_HAND);
            }
        });

        // Rueda de ratón en el viewport del mapa: zoom in permitido, des-zoom bloqueado al 100% de la imagen
        viewportPane.setOnScroll((ScrollEvent event) -> {
            double delta = event.getDeltaY() > 0 ? 0.15 : -0.15;
            if (delta < 0 && isAtMinZoom()) {
                event.consume();
                return;
            }
            zoom(delta);
            event.consume();
        });
    }

    private void zoom(double delta) {
        double oldScale = scaleFactor.get();
        double nuevoScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, oldScale + delta));
        if (Math.abs(nuevoScale - oldScale) < 0.0001) {
            return;
        }

        if (nuevoScale <= MIN_SCALE + 0.005) {
            scaleFactor.set(MIN_SCALE);
            mapTranslateX = (VIEWPORT_SIZE - (MAP_WIDTH * MIN_SCALE)) / 2.0;
            mapTranslateY = (VIEWPORT_SIZE - (MAP_HEIGHT * MIN_SCALE)) / 2.0;
        } else {
            // Zoom centrado en el centro del viewport (360, 360)
            double cx = VIEWPORT_SIZE / 2.0;
            double cy = VIEWPORT_SIZE / 2.0;
            double mapX = (cx - mapTranslateX) / oldScale;
            double mapY = (cy - mapTranslateY) / oldScale;

            scaleFactor.set(nuevoScale);
            mapTranslateX = cx - (mapX * nuevoScale);
            mapTranslateY = cy - (mapY * nuevoScale);
        }
        actualizarTransformacion();
    }

    private void resetVista() {
        scaleFactor.set(MIN_SCALE);
        mapTranslateX = (VIEWPORT_SIZE - (MAP_WIDTH * MIN_SCALE)) / 2.0;
        mapTranslateY = (VIEWPORT_SIZE - (MAP_HEIGHT * MIN_SCALE)) / 2.0;
        actualizarTransformacion();
        if (cbSectores != null) cbSectores.setValue("Todos los sectores");
        filtrarPorSector("Todos los sectores");
        deseleccionarLote();
        ocultarCallout();
    }

    private void centrarEnPunto(double x, double y, double targetZoom) {
        scaleFactor.set(Math.max(MIN_SCALE, Math.min(MAX_SCALE, targetZoom)));
        double scale = scaleFactor.get();
        mapTranslateX = (VIEWPORT_SIZE / 2.0) - (x * scale);
        mapTranslateY = (VIEWPORT_SIZE / 2.0) - (y * scale);
        actualizarTransformacion();
    }

    private void actualizarTransformacion() {
        double scale = scaleFactor.get();
        double currentW = MAP_WIDTH * scale;
        double currentH = MAP_HEIGHT * scale;

        if (isAtMinZoom()) {
            // Bloqueado y centrado matemáticamente cuando está al mínimo (muestra 100% de la imagen sin cortes)
            mapTranslateX = (VIEWPORT_SIZE - currentW) / 2.0;
            mapTranslateY = (VIEWPORT_SIZE - currentH) / 2.0;
            Cursor cur = modoColocacion ? Cursor.CROSSHAIR : Cursor.DEFAULT;
            viewportPane.setCursor(cur);
            mapCanvas.setCursor(cur);
        } else {
            // Limitar paneo para no salir del mapa
            double minX = VIEWPORT_SIZE - currentW;
            double maxX = 0;
            double minY = VIEWPORT_SIZE - currentH;
            double maxY = 0;

            if (currentW <= VIEWPORT_SIZE) {
                mapTranslateX = (VIEWPORT_SIZE - currentW) / 2.0;
            } else {
                mapTranslateX = Math.min(maxX, Math.max(minX, mapTranslateX));
            }

            if (currentH <= VIEWPORT_SIZE) {
                mapTranslateY = (VIEWPORT_SIZE - currentH) / 2.0;
            } else {
                mapTranslateY = Math.min(maxY, Math.max(minY, mapTranslateY));
            }
        }

        mapTranslateTransform.setX(mapTranslateX);
        mapTranslateTransform.setY(mapTranslateY);
        mapScaleTransform.setX(scale);
        mapScaleTransform.setY(scale);

        if (btnZoomOut != null) {
            btnZoomOut.setDisable(isAtMinZoom());
        }
    }

    private void cambiarEstilo(boolean satelite) {
        if (satelite) {
            btnModoMapa.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-weight: 600; -fx-font-size: 10.5px; -fx-padding: 3 8; -fx-cursor: hand;");
            btnModoSatelite.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 10.5px; -fx-background-radius: 4px; -fx-padding: 3 8; -fx-cursor: hand;");

            if (mapImageView != null) {
                ColorAdjust darkEffect = new ColorAdjust();
                darkEffect.setBrightness(-0.35);
                darkEffect.setContrast(0.25);
                darkEffect.setSaturation(-0.15);
                mapImageView.setEffect(darkEffect);
            }
        } else {
            btnModoSatelite.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-weight: 600; -fx-font-size: 10.5px; -fx-padding: 3 8; -fx-cursor: hand;");
            btnModoMapa.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #1e293b; -fx-font-weight: bold; -fx-font-size: 10.5px; -fx-background-radius: 4px; -fx-padding: 3 8; -fx-cursor: hand;");

            if (mapImageView != null) {
                mapImageView.setEffect(null);
            }
        }
    }

    private void filtrarPorSector(String sector) {
        if (sector == null || "Todos los sectores".equals(sector)) {
            for (LotSlot lot : lotes) {
                lot.getNode().setOpacity(1.0);
                lot.getNode().setDisable(false);
            }
            return;
        }

        String[] partes = sector.split(" ");
        String sectorKey = partes.length >= 2 ? (partes[0] + " " + partes[1]) : sector;

        double sumX = 0, sumY = 0;
        int count = 0;

        for (LotSlot lot : lotes) {
            boolean coincide = lot.sector.equalsIgnoreCase(sectorKey);
            if (coincide) {
                lot.getNode().setOpacity(1.0);
                lot.getNode().setDisable(false);
                sumX += lot.posX;
                sumY += lot.posY;
                count++;
            } else {
                lot.getNode().setOpacity(0.18);
                lot.getNode().setDisable(true);
            }
        }

        if (count > 0) {
            centrarEnPunto(sumX / count, sumY / count, 1.45);
        }
    }

    private void buscarCasa(String query) {
        if (query == null || query.isBlank()) {
            for (LotSlot lot : lotes) lot.setResaltado(false);
            ocultarCallout();
            return;
        }
        String q = query.trim().toUpperCase(Locale.ROOT);
        LotSlot primeraCoincidencia = null;

        for (LotSlot lot : lotes) {
            boolean coincideCodigo = lot.codigo.toUpperCase(Locale.ROOT).contains(q);
            boolean coincideRepresentante = lot.vivienda != null &&
                    lot.vivienda.getRepresentante() != null &&
                    lot.vivienda.getRepresentante().toUpperCase(Locale.ROOT).contains(q);

            if (coincideCodigo || coincideRepresentante) {
                lot.setResaltado(true);
                if (primeraCoincidencia == null) primeraCoincidencia = lot;
            } else {
                lot.setResaltado(false);
            }
        }

        if (primeraCoincidencia != null) {
            mostrarCalloutParaLote(primeraCoincidencia);
            centrarEnPunto(primeraCoincidencia.posX, primeraCoincidencia.posY, 1.6);
        }
    }

    public void setOnViviendaSelected(java.util.function.Consumer<ViviendaModel> onViviendaSelected) {
        this.onViviendaSelected = onViviendaSelected;
    }

    public void setOnNuevaViviendaAction(Runnable onNuevaViviendaAction) {
        this.onNuevaViviendaAction = onNuevaViviendaAction;
    }

    public void setOnColocarViviendaListener(OnColocarViviendaListener onColocarViviendaListener) {
        this.onColocarViviendaListener = onColocarViviendaListener;
    }

    // =========================================================================
    // Lote residencial interactivo, arrastrable y giratorio
    // =========================================================================
    private class LotSlot {
        final int numero;
        final int index;
        final String codigo;
        final String sector;
        final String direccion;
        double posX;
        double posY;
        double rotacion;
        ViviendaModel vivienda;

        private final StackPane lotNode = new StackPane();
        private final StackPane cuerpoCasa = new StackPane();
        private final Rectangle parcela = new Rectangle(44, 34);
        private final Rectangle techoCasa = new Rectangle(30, 20);
        private final VBox pinContainer = new VBox(-3);
        private final Circle pinCircle = new Circle(10.5);
        private final Label lblNumero = new Label();
        private final Polygon pinPointer = new Polygon(0.0, 0.0, 6.0, 0.0, 3.0, 4.0);

        LotSlot(int numero, int index, String codigo, String sector, String direccion, double x, double y, double rotacion) {
            this.numero = numero;
            this.index = index;
            this.codigo = codigo;
            this.sector = sector;
            this.direccion = direccion;
            this.posX = x;
            this.posY = y;
            this.rotacion = rotacion;

            construirNodo();
        }

        private void construirNodo() {
            lotNode.setLayoutX(posX - 22);
            lotNode.setLayoutY(posY - 17);
            lotNode.setPrefSize(44, 34);
            lotNode.setCursor(Cursor.HAND);

            parcela.setArcWidth(6);
            parcela.setArcHeight(6);
            parcela.setFill(Color.rgb(255, 255, 255, 0.88));
            parcela.setStroke(Color.web("#64748b"));
            parcela.setStrokeWidth(1.2);
            DropShadow shadowParcela = new DropShadow(4, Color.rgb(0, 0, 0, 0.16));
            shadowParcela.setOffsetY(1.5);
            parcela.setEffect(shadowParcela);

            techoCasa.setArcWidth(4);
            techoCasa.setArcHeight(4);
            techoCasa.setFill(Color.web("#f1f5f9"));
            techoCasa.setStroke(Color.web("#cbd5e1"));
            techoCasa.setStrokeWidth(0.8);

            cuerpoCasa.getChildren().addAll(parcela, techoCasa);
            cuerpoCasa.setAlignment(Pos.CENTER);
            cuerpoCasa.setPickOnBounds(false);
            cuerpoCasa.setRotate(rotacion);

            pinContainer.setAlignment(Pos.CENTER);
            pinContainer.setPickOnBounds(false);

            // Numeración limpia: si el número extraído es corto (<= 99), se muestra; si no, el índice secuencial
            int pinDisplayNum = (numero >= 1 && numero <= 99) ? numero : index;
            lblNumero.setText(String.valueOf(pinDisplayNum));
            lblNumero.setFont(Font.font("Segoe UI", FontWeight.BOLD, pinDisplayNum > 9 ? 8.5 : 9.5));
            lblNumero.setTextFill(Color.WHITE);

            StackPane pinHead = new StackPane(pinCircle, lblNumero);
            pinHead.setPrefSize(21, 21);

            pinContainer.getChildren().addAll(pinHead, pinPointer);

            DropShadow shadowPin = new DropShadow(5, Color.rgb(0, 0, 0, 0.30));
            shadowPin.setOffsetY(1.5);
            pinContainer.setEffect(shadowPin);

            lotNode.getChildren().addAll(cuerpoCasa, pinContainer);

            lotNode.setOnMouseEntered(e -> {
                if (modoColocacion) return;
                animarHover(true);
                if (isEditable()) {
                    if (loteEnEdicionExclusiva == this) {
                        mostrarCalloutEdicionCasaNueva(this);
                    } else {
                        mostrarCalloutEdicion(this);
                    }
                }
            });

            lotNode.setOnMouseExited(e -> {
                animarHover(false);
                if (!isEditable() && loteSeleccionado != this) {
                    ocultarCallout();
                }
            });

            lotNode.setOnMousePressed(e -> {
                if (modoColocacion) return;
                if (isEditable()) {
                    e.consume();
                    lotNode.toFront();
                    animarHover(true);

                    if (e.getButton() == MouseButton.SECONDARY) {
                        girar(1.0);
                    }
                    if (loteEnEdicionExclusiva == this) {
                        mostrarCalloutEdicionCasaNueva(this);
                    } else {
                        mostrarCalloutEdicion(this);
                    }
                }
            });

            lotNode.setOnMouseDragged(e -> {
                if (isEditable() && e.getButton() == MouseButton.PRIMARY) {
                    e.consume();
                    Point2D p = mapCanvas.sceneToLocal(e.getSceneX(), e.getSceneY());
                    double newX = Math.round(Math.max(22, Math.min(MAP_WIDTH - 22, p.getX())));
                    double newY = Math.round(Math.max(17, Math.min(MAP_HEIGHT - 17, p.getY())));
                    setCoordenadas(newX, newY);
                    if (loteEnEdicionExclusiva == this) {
                        mostrarCalloutEdicionCasaNueva(this);
                    } else {
                        mostrarCalloutEdicion(this);
                    }
                }
            });

            lotNode.setOnMouseReleased(e -> {
                if (isEditable()) {
                    e.consume();
                    guardarCoordenadas(false);
                }
            });

            lotNode.setOnScroll((ScrollEvent event) -> {
                if (isEditable()) {
                    event.consume();
                    double delta = event.getDeltaY() > 0 ? 1.0 : -1.0;
                    girar(delta);
                    if (loteEnEdicionExclusiva == this) {
                        mostrarCalloutEdicionCasaNueva(this);
                    } else {
                        mostrarCalloutEdicion(this);
                    }
                }
            });

            lotNode.setOnMouseClicked(e -> {
                e.consume();
                if (modoColocacion && e.getButton() == MouseButton.PRIMARY) {
                    Point2D p = mapCanvas.sceneToLocal(e.getSceneX(), e.getSceneY());
                    double clickX = Math.round(Math.max(25, Math.min(MAP_WIDTH - 25, p.getX())));
                    double clickY = Math.round(Math.max(20, Math.min(MAP_HEIGHT - 20, p.getY())));
                    colocarViviendaEnPosicion(clickX, clickY);
                    return;
                }
                if (isEditable()) {
                    return;
                }
                seleccionarLote(this);
                mostrarCalloutParaLote(this);
                if (onViviendaSelected != null && vivienda != null) {
                    onViviendaSelected.accept(vivienda);
                }
            });

            actualizarEstadoVisual();
        }

        private boolean isEditable() {
            return modoEdicion || (loteEnEdicionExclusiva == this);
        }

        void setCoordenadas(double x, double y) {
            this.posX = x;
            this.posY = y;
            lotNode.setLayoutX(x - 22);
            lotNode.setLayoutY(y - 17);
        }

        void setRotacion(double grados) {
            double angulo = grados % 360.0;
            if (angulo < -180.0) angulo += 360.0;
            if (angulo > 180.0) angulo -= 360.0;
            this.rotacion = angulo;
            cuerpoCasa.setRotate(rotacion);
        }

        void girar(double delta) {
            setRotacion(Math.round(this.rotacion + delta));
            guardarCoordenadas(false);
        }

        void actualizarModoEdicion(boolean activo) {
            if (activo) {
                lotNode.setCursor(Cursor.MOVE);
                parcela.setStroke(Color.web("#f59e0b"));
                parcela.setStrokeWidth(2.0);
                parcela.getStrokeDashArray().setAll(4.0, 4.0);
            } else {
                lotNode.setCursor(Cursor.HAND);
                parcela.getStrokeDashArray().clear();
                actualizarEstadoVisual();
            }
        }

        void asignarVivienda(ViviendaModel vivienda) {
            this.vivienda = vivienda;
            actualizarEstadoVisual();
        }

        void setResaltado(boolean resaltado) {
            if (resaltado) {
                parcela.setStroke(Color.web("#f59e0b"));
                parcela.setStrokeWidth(2.5);
                animarHover(true);
            } else {
                actualizarEstadoVisual();
                animarHover(false);
            }
        }

        private void animarHover(boolean expandir) {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), lotNode);
            st.setToX(expandir ? 1.25 : 1.0);
            st.setToY(expandir ? 1.25 : 1.0);
            st.play();
            if (expandir) {
                lotNode.toFront();
            }
        }

        void actualizarEstadoVisual() {
            if (isEditable()) return;

            parcela.setStrokeWidth(1.2);
            if (vivienda != null) {
                if (vivienda.getTotalResidentes() > 0) {
                    pinCircle.setFill(Color.web("#10b981"));
                    pinPointer.setFill(Color.web("#10b981"));
                    parcela.setStroke(Color.web("#059669"));
                    parcela.setFill(Color.rgb(240, 253, 244, 0.92));
                    techoCasa.setFill(Color.web("#d1fae5"));
                } else {
                    pinCircle.setFill(Color.web("#2563eb"));
                    pinPointer.setFill(Color.web("#2563eb"));
                    parcela.setStroke(Color.web("#1d4ed8"));
                    parcela.setFill(Color.rgb(239, 246, 255, 0.92));
                    techoCasa.setFill(Color.web("#dbeafe"));
                }
            } else {
                pinCircle.setFill(Color.web("#64748b"));
                pinPointer.setFill(Color.web("#64748b"));
                parcela.setStroke(Color.web("#94a3b8"));
                parcela.setFill(Color.rgb(255, 255, 255, 0.88));
                techoCasa.setFill(Color.web("#f1f5f9"));
            }
        }

        Node getNode() {
            return lotNode;
        }
    }

    private void seleccionarLote(LotSlot lot) {
        if (loteSeleccionado != null) {
            loteSeleccionado.actualizarEstadoVisual();
        }
        loteSeleccionado = lot;
        viviendaSeleccionada = lot.vivienda;
        lot.parcela.setStroke(Color.web("#2563eb"));
        lot.parcela.setStrokeWidth(2.5);
    }

    public void deseleccionarLote() {
        if (loteSeleccionado != null) {
            loteSeleccionado.actualizarEstadoVisual();
            loteSeleccionado = null;
        }
        viviendaSeleccionada = null;
    }
}
