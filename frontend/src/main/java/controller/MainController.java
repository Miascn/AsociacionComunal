package controller;

import java.io.IOException;
import java.util.Objects;

import javafx.animation.Transition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.util.Duration;
import models.AuthUser;
import org.kordamp.ikonli.javafx.FontIcon;
import security.SessionManager;
import service.ThemeService;
import service.UpdateService;

public class MainController {
    @FXML private VBox sidebar;
    @FXML private ImageView imgSidebarLogo;
    @FXML private Label lblNombreUsuario;
    @FXML private Label lblRolUsuario;
    @FXML private Label lblVersion;
    @FXML private Button btnTema;
    @FXML private FontIcon iconTemaSidebar;

    // Botones de navegación
    @FXML private Button btnNavDashboard;
    @FXML private Button btnNavMiembros;
    @FXML private Button btnUsuarios;
    @FXML private Button btnRoles;
    @FXML private Button btnNavCargos;
    @FXML private Button btnNavPeriodos;
    @FXML private Button btnNavDirectiva;
    @FXML private Button btnBitacora;
    @FXML private Button btnNavViviendas;
    @FXML private Button btnNavAportaciones;
    @FXML private Button btnNavProyectos;
    @FXML private Button btnNavReuniones;
    @FXML private Button btnNavVotaciones;
    @FXML private Button btnNavReportes;

    // Barra superior
    @FXML private HBox topBar;
    @FXML private Button btnToggleSidebar;
    @FXML private Button btnTemaTop;
    @FXML private FontIcon iconTemaTop;
    @FXML private Button btnNotificaciones;
    @FXML private Button btnUsuarioBadge;
    @FXML private Label lblTopAvatarInicial;
    @FXML private Label lblTopNombreUsuario;
    @FXML private Label lblTopEmailUsuario;

    // Área de contenido
    @FXML private StackPane contentArea;

    private Runnable onLogout;
    private Button botonActivo;
    private boolean sidebarFijada = false;
    private Transition animacionSidebar;
    private Popup popupUsuario;
    private static MainController instance;

    public static MainController getInstance() {
        return instance;
    }

    @FXML
    private void initialize() {
        instance = this;
        AuthUser user = SessionManager.getInstance().requireCurrentUser();
        if (lblNombreUsuario != null) lblNombreUsuario.setText(user.getDisplayName());
        if (lblRolUsuario != null) lblRolUsuario.setText(user.getRole());
        if (lblVersion != null) lblVersion.setText("Versión " + UpdateService.currentVersion());

        sidebar.setPrefWidth(68.0);
        sidebar.setMinWidth(68.0);
        sidebar.setMaxWidth(68.0);

        configurarUsuarioTopBar(user);

        boolean administrator = isAdministrator(user.getRole());
        boolean puedeAuditar = administrator || isSindico(user.getRole());
        btnUsuarios.setVisible(administrator);
        btnUsuarios.setManaged(administrator);
        btnRoles.setVisible(administrator);
        btnRoles.setManaged(administrator);
        btnBitacora.setVisible(puedeAuditar);
        btnBitacora.setManaged(puedeAuditar);

        actualizarBotonTema();
        configurarSidebarHover();

        mostrarDashboard();

        sidebar.sceneProperty().addListener((observable, previous, scene) -> {
            if (scene == null) return;
            scene.widthProperty().addListener((obs, oldWidth, width) -> adaptarBarra(width.doubleValue()));
            adaptarBarra(scene.getWidth());
        });
    }

    private void configurarUsuarioTopBar(AuthUser user) {
        String displayName = (user != null && user.getDisplayName() != null && !user.getDisplayName().isBlank())
                ? user.getDisplayName()
                : "Administrador";
        String username = (user != null && user.getUsername() != null && !user.getUsername().isBlank())
                ? user.getUsername()
                : "admin";
        String inicial = displayName.substring(0, 1).toUpperCase();
        String email = username.toLowerCase() + "@sistema.local";

        if (lblTopAvatarInicial != null) lblTopAvatarInicial.setText(inicial);
        if (lblTopNombreUsuario != null) lblTopNombreUsuario.setText(displayName);
        if (lblTopEmailUsuario != null) lblTopEmailUsuario.setText(email);
    }

    private void configurarSidebarHover() {
        sidebar.setOnMouseEntered(e -> {
            if (!sidebarFijada) {
                animarSidebar(true);
            }
        });

        sidebar.setOnMouseExited(e -> {
            // Solo colapsar si el cursor salió realmente hacia fuera del sidebar
            // (no hacia un nodo hijo como un popup o sub-menú)
            if (!sidebarFijada && !sidebar.isHover()) {
                animarSidebar(false);
            }
        });
    }

    @FXML
    public void onToggleSidebar() {
        sidebarFijada = !sidebarFijada;
        animarSidebar(sidebarFijada);
    }

    private void animarSidebar(boolean expandir) {
        // Parar animación en curso y capturar el ancho actual en ese instante
        if (animacionSidebar != null) {
            animacionSidebar.stop();
            animacionSidebar = null;
        }

        // Ancho de partida = lo que se ve ahora en pantalla
        double inicio = sidebar.getPrefWidth();
        double fin = expandir ? 230.0 : 68.0;

        // Evitar reanimar si ya está en el estado objetivo
        if (Math.abs(inicio - fin) < 1.0) {
            // Asegurar clases CSS correctas
            aplicarClasesSidebar(expandir);
            return;
        }

        // Aplicar clases CSS antes de la animación para que los textos/íconos aparezcan correctamente
        aplicarClasesSidebar(expandir);

        animacionSidebar = new Transition() {
            {
                setCycleDuration(Duration.millis(220));
                setInterpolator(javafx.animation.Interpolator.EASE_OUT);
            }
            @Override
            protected void interpolate(double frac) {
                double ancho = inicio + (fin - inicio) * frac;
                sidebar.setPrefWidth(ancho);
                sidebar.setMinWidth(ancho);
                sidebar.setMaxWidth(ancho);
            }
        };

        animacionSidebar.setOnFinished(e -> {
            animacionSidebar = null;
            sidebar.setPrefWidth(fin);
            sidebar.setMinWidth(fin);
            sidebar.setMaxWidth(fin);
            if (imgSidebarLogo != null) {
                imgSidebarLogo.setFitWidth(expandir ? 48 : 36);
                imgSidebarLogo.setFitHeight(expandir ? 48 : 36);
            }
        });

        animacionSidebar.play();
    }

    /** Aplica sidebar-collapsed / sidebar-expanded sin duplicar clases. */
    private void aplicarClasesSidebar(boolean expandir) {
        if (expandir) {
            sidebar.getStyleClass().remove("sidebar-collapsed");
            if (!sidebar.getStyleClass().contains("sidebar-expanded")) {
                sidebar.getStyleClass().add("sidebar-expanded");
            }
        } else {
            sidebar.getStyleClass().remove("sidebar-expanded");
            if (!sidebar.getStyleClass().contains("sidebar-collapsed")) {
                sidebar.getStyleClass().add("sidebar-collapsed");
            }
        }
        if (imgSidebarLogo != null) {
            imgSidebarLogo.setFitWidth(expandir ? 48 : 36);
            imgSidebarLogo.setFitHeight(expandir ? 48 : 36);
        }
    }

    private void adaptarBarra(double width) {
        double w = sidebarFijada ? 230.0 : 68.0;
        sidebar.setPrefWidth(w);
        sidebar.setMinWidth(w);
        sidebar.setMaxWidth(w);
    }

    @FXML
    public void onAlternarTema() {
        if (btnTema != null && btnTema.getScene() != null) {
            ThemeService.toggle(btnTema.getScene());
        } else if (btnTemaTop != null && btnTemaTop.getScene() != null) {
            ThemeService.toggle(btnTemaTop.getScene());
        }
        actualizarBotonTema();
    }

    private void actualizarBotonTema() {
        boolean dark = ThemeService.isDark();
        if (btnTema != null) {
            btnTema.setText(dark ? "Modo claro" : "Modo oscuro");
        }
        if (iconTemaSidebar != null) {
            iconTemaSidebar.setIconLiteral(dark ? "fth-sun" : "fth-moon");
        }
        if (iconTemaTop != null) {
            iconTemaTop.setIconLiteral(dark ? "fth-sun" : "fth-moon");
        }
    }

    @FXML
    public void onMostrarNotificaciones() {
        if (btnNotificaciones == null) return;
        Tooltip tip = new Tooltip("No tienes notificaciones pendientes");
        javafx.geometry.Point2D pt = btnNotificaciones.localToScreen(0, btnNotificaciones.getHeight() + 4);
        if (pt != null) {
            tip.show(btnNotificaciones, pt.getX(), pt.getY());
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.seconds(2.5));
            pause.setOnFinished(e -> tip.hide());
            pause.play();
        }
    }

    @FXML
    public void onMostrarMenuUsuario() {
        if (popupUsuario != null && popupUsuario.isShowing()) {
            popupUsuario.hide();
            return;
        }

        AuthUser user = SessionManager.getInstance().getCurrentUser().orElse(null);
        String displayName = (user != null && user.getDisplayName() != null && !user.getDisplayName().isBlank())
                ? user.getDisplayName()
                : "Administrador";
        String username = (user != null && user.getUsername() != null && !user.getUsername().isBlank())
                ? user.getUsername()
                : "admin";
        String email = username.toLowerCase() + "@sistema.local";
        String inicial = displayName.substring(0, 1).toUpperCase();

        VBox card = new VBox(12);
        card.getStyleClass().add("user-profile-popup-card");
        card.setAlignment(Pos.TOP_CENTER);

        // Cabecera: email + botón cerrar (✕)
        Label lblHeaderEmail = new Label(email);
        lblHeaderEmail.getStyleClass().add("user-popup-header-email");
        Region spacerHeader = new Region();
        HBox.setHgrow(spacerHeader, Priority.ALWAYS);
        Button btnCerrarPopup = new Button();
        btnCerrarPopup.getStyleClass().add("user-popup-close-btn");
        FontIcon closeIcon = new FontIcon("fth-x");
        closeIcon.setIconSize(14);
        btnCerrarPopup.setGraphic(closeIcon);
        btnCerrarPopup.setOnAction(e -> popupUsuario.hide());
        HBox headerBox = new HBox(8, lblHeaderEmail, spacerHeader, btnCerrarPopup);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        // Avatar grande
        StackPane avatarLarge = new StackPane();
        avatarLarge.getStyleClass().add("user-popup-avatar-large");
        Label lblInicialLarge = new Label(inicial);
        lblInicialLarge.getStyleClass().add("user-popup-avatar-text");
        avatarLarge.getChildren().add(lblInicialLarge);

        // Saludo y rol
        Label lblGreeting = new Label("¡Hola, " + displayName + "!");
        lblGreeting.getStyleClass().add("user-popup-greeting");
        Label lblSubtitulo = new Label(user != null ? user.getRole() : "Usuario");
        lblSubtitulo.getStyleClass().add("user-badge-email");

        // Botón "Gestionar tu cuenta"
        Button btnGestionar = new Button("Gestionar tu cuenta");
        btnGestionar.getStyleClass().add("btn-gestionar-cuenta");
        btnGestionar.setMaxWidth(Double.MAX_VALUE);
        btnGestionar.setOnAction(e -> {
            popupUsuario.hide();
            if (isAdministrator(user != null ? user.getRole() : null)) {
                mostrarUsuarios();
            }
        });

        Separator sep1 = new Separator();

        // Acciones secundarias: + Añadir otra cuenta, Cerrar sesión
        Button btnAddAccount = new Button("Añadir otra cuenta");
        btnAddAccount.getStyleClass().add("btn-user-action-secondary");
        btnAddAccount.setMaxWidth(Double.MAX_VALUE);
        FontIcon userPlusIcon = new FontIcon("fth-user-plus");
        userPlusIcon.setIconSize(14);
        btnAddAccount.setGraphic(userPlusIcon);
        btnAddAccount.setOnAction(e -> {
            popupUsuario.hide();
            Alert alert = new Alert(
                Alert.AlertType.INFORMATION,
                "La funcionalidad de múltiples cuentas estará disponible próximamente.",
                ButtonType.OK
            );
            alert.setHeaderText("Gestión de cuentas");
            alert.showAndWait();
        });

        Button btnLogoutPopup = new Button("Cerrar sesión");
        btnLogoutPopup.getStyleClass().add("btn-user-action-secondary");
        btnLogoutPopup.setMaxWidth(Double.MAX_VALUE);
        FontIcon logoutIcon = new FontIcon("fth-log-out");
        logoutIcon.setIconSize(14);
        btnLogoutPopup.setGraphic(logoutIcon);
        btnLogoutPopup.setOnAction(e -> {
            popupUsuario.hide();
            onCerrarSesion();
        });

        Separator sep2 = new Separator();

        // Pie de popup con enlaces legales
        Label lblFooter = new Label("Política de Privacidad  •  Condiciones");
        lblFooter.getStyleClass().add("user-popup-footer-text");

        card.getChildren().addAll(
            headerBox,
            avatarLarge,
            lblGreeting,
            lblSubtitulo,
            btnGestionar,
            sep1,
            btnAddAccount,
            btnLogoutPopup,
            sep2,
            lblFooter
        );

        if (popupUsuario == null) {
            popupUsuario = new Popup();
            popupUsuario.setAutoHide(true);
        }
        popupUsuario.getContent().setAll(card);

        if (btnUsuarioBadge != null && btnUsuarioBadge.getScene() != null) {
            javafx.geometry.Point2D pt = btnUsuarioBadge.localToScreen(0, btnUsuarioBadge.getHeight() + 6);
            if (pt != null) {
                double popupX = pt.getX() - 120;
                if (popupX < 10) popupX = 10;
                popupUsuario.show(btnUsuarioBadge.getScene().getWindow(), popupX, pt.getY());
            }
        }
    }

    private void marcarBotonActivo(Button boton) {
        if (botonActivo != null) {
            botonActivo.getStyleClass().remove("active-nav-pill");
        }
        if (boton != null) {
            if (!boton.getStyleClass().contains("active-nav-pill")) {
                boton.getStyleClass().add("active-nav-pill");
            }
            botonActivo = boton;
        }
    }

    public void setOnLogout(Runnable onLogout) {
        this.onLogout = Objects.requireNonNull(onLogout);
    }

    @FXML
    private void mostrarDashboard() {
        marcarBotonActivo(btnNavDashboard);
        cargarVista("/fxml/views/dashboard.fxml");
    }

    /**
     * Navega al dashboard y activa directamente el modo de mover y rotar casas en el plano interactivo.
     */
    public void mostrarDashboardYActivarEdicionMapa() {
        marcarBotonActivo(btnNavDashboard);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/dashboard.fxml"));
            Parent view = loader.load();
            DashboardController dashboardCtrl = loader.getController();
            contentArea.getChildren().setAll(view);
            if (dashboardCtrl != null) {
                dashboardCtrl.activarEdicionMapaDirecta();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible cargar el dashboard.", exception);
        }
    }

    @FXML
    private void mostrarMiembros() {
        marcarBotonActivo(btnNavMiembros);
        cargarVista("/fxml/views/miembros.fxml");
    }

    @FXML
    private void mostrarUsuarios() {
        marcarBotonActivo(btnUsuarios);
        cargarVista("/fxml/views/usuarios.fxml");
    }

    @FXML
    private void mostrarRoles() {
        marcarBotonActivo(btnRoles);
        cargarVista("/fxml/views/roles.fxml");
    }

    @FXML
    private void mostrarCargos() {
        marcarBotonActivo(btnNavCargos);
        cargarVista("/fxml/views/cargos.fxml");
    }

    @FXML
    private void mostrarPeriodos() {
        marcarBotonActivo(btnNavPeriodos);
        cargarVista("/fxml/views/periodos.fxml");
    }

    @FXML
    private void mostrarDirectiva() {
        marcarBotonActivo(btnNavDirectiva);
        cargarVista("/fxml/views/directiva.fxml");
    }

    @FXML
    private void mostrarBitacora() {
        marcarBotonActivo(btnBitacora);
        cargarVista("/fxml/views/bitacora.fxml");
    }

    static boolean isAdministrator(String role) {
        return role != null && ("ADMIN".equalsIgnoreCase(role) || "ADMINISTRADOR".equalsIgnoreCase(role));
    }

    static boolean isSindico(String role) {
        return role != null && "SINDICO".equalsIgnoreCase(role.trim());
    }

    @FXML
    private void mostrarViviendas() {
        marcarBotonActivo(btnNavViviendas);
        cargarVista("/fxml/views/viviendas.fxml");
    }

    @FXML
    private void mostrarAportaciones() {
        marcarBotonActivo(btnNavAportaciones);
        cargarVista("/fxml/views/aportaciones.fxml");
    }

    @FXML
    private void mostrarProyectos() {
        marcarBotonActivo(btnNavProyectos);
        cargarVista("/fxml/views/proyectos.fxml");
    }

    @FXML
    private void mostrarReuniones() {
        marcarBotonActivo(btnNavReuniones);
        cargarVista("/fxml/views/reuniones.fxml");
    }

    @FXML
    private void mostrarVotaciones() {
        marcarBotonActivo(btnNavVotaciones);
        cargarVista("/fxml/views/votaciones.fxml");
    }

    @FXML
    private void mostrarReportes() {
        marcarBotonActivo(btnNavReportes);
        mostrarPlaceholder("Reportes");
    }

    @FXML
    private void onCerrarSesion() {
        SessionManager.getInstance().clear();
        if (onLogout != null) {
            onLogout.run();
        }
    }

    private void mostrarPlaceholder(String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/placeholder.fxml"));
            Parent view = loader.load();
            PlaceholderController controller = loader.getController();
            controller.setTitulo(titulo + " - módulo en construcción");
            contentArea.getChildren().setAll(view);
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible abrir el módulo " + titulo + ".", exception);
        }
    }

    private void cargarVista(String resourcePath) {
        try {
            Parent view = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(resourcePath)));
            contentArea.getChildren().setAll(view);
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible cargar la vista " + resourcePath + ".", exception);
        }
    }
}
