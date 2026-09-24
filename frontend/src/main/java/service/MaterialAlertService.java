package service;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import io.github.palexdev.materialfx.dialogs.MFXStageDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Servicio de alertas y cuadros de confirmación modernos construidos con MaterialFX y Heroicons.
 */
public final class MaterialAlertService {

    private MaterialAlertService() {}

    /**
     * Muestra un diálogo de confirmación moderno con MaterialFX y fondo atenuado (Scrim).
     */
    public static boolean confirmacion(
            Window owner,
            String titulo,
            String mensaje,
            String textoAccion,
            boolean esPeligroso,
            Runnable alConfirmar
    ) {
        AtomicBoolean confirmado = new AtomicBoolean(false);

        MFXGenericDialog dialogContent = new MFXGenericDialog();
        dialogContent.getStyleClass().add("mfx-modern-dialog");
        dialogContent.setPrefWidth(460);
        dialogContent.setMaxWidth(500);

        // Icono de cabecera temático
        String iconoHero = esPeligroso ? HeroIcon.TRASH : HeroIcon.REFRESH;
        String colorIcono = esPeligroso ? HeroIcon.RED_600 : HeroIcon.BLUE_600;
        dialogContent.setHeaderIcon(crearContenedorIcono(iconoHero, colorIcono, esPeligroso ? "#fee2e2" : "#dbeafe"));
        dialogContent.setHeaderText(titulo != null ? titulo : "Confirmar acción");

        // Cuerpo del mensaje
        Label lblMensaje = new Label(mensaje);
        lblMensaje.setWrapText(true);
        lblMensaje.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #475569; -fx-line-spacing: 3px;");
        dialogContent.setContent(lblMensaje);

        MFXStageDialog stageDialog = new MFXStageDialog(dialogContent);
        configurarStageDialog(stageDialog, owner, titulo);

        // Botones de acción
        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().addAll("mfx-dialog-btn", "mfx-dialog-cancel");
        btnCancelar.setOnAction(e -> stageDialog.close());

        Button btnAccion = new Button(textoAccion != null ? textoAccion : "Confirmar");
        btnAccion.getStyleClass().addAll("mfx-dialog-btn", esPeligroso ? "mfx-dialog-danger" : "mfx-dialog-primary");
        btnAccion.setOnAction(e -> {
            confirmado.set(true);
            stageDialog.close();
            if (alConfirmar != null) {
                alConfirmar.run();
            }
        });

        HBox actions = new HBox(10, btnCancelar, btnAccion);
        actions.setAlignment(Pos.CENTER_RIGHT);
        dialogContent.addActions(btnCancelar, btnAccion);

        stageDialog.showAndWait();
        return confirmado.get();
    }

    /**
     * Muestra un diálogo de éxito con MaterialFX.
     */
    public static void exito(Window owner, String titulo, String mensaje) {
        MFXGenericDialog dialogContent = new MFXGenericDialog();
        dialogContent.getStyleClass().add("mfx-modern-dialog");
        dialogContent.setPrefWidth(440);

        dialogContent.setHeaderIcon(crearContenedorIcono(HeroIcon.EYE, HeroIcon.GREEN_600, "#dcfce7"));
        dialogContent.setHeaderText(titulo != null ? titulo : "Operación exitosa");

        Label lblMensaje = new Label(mensaje);
        lblMensaje.setWrapText(true);
        lblMensaje.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #334155;");
        dialogContent.setContent(lblMensaje);

        MFXStageDialog stageDialog = new MFXStageDialog(dialogContent);
        configurarStageDialog(stageDialog, owner, titulo);

        Button btnAceptar = new Button("Aceptar");
        btnAceptar.getStyleClass().addAll("mfx-dialog-btn", "mfx-dialog-primary");
        btnAceptar.setOnAction(e -> stageDialog.close());
        dialogContent.addActions(btnAceptar);

        stageDialog.showAndWait();
    }

    /**
     * Muestra un diálogo de error con MaterialFX.
     */
    public static void error(Window owner, String titulo, String mensaje) {
        MFXGenericDialog dialogContent = new MFXGenericDialog();
        dialogContent.getStyleClass().add("mfx-modern-dialog");
        dialogContent.setPrefWidth(460);

        dialogContent.setHeaderIcon(crearContenedorIcono(HeroIcon.TRASH, HeroIcon.RED_600, "#fee2e2"));
        dialogContent.setHeaderText(titulo != null ? titulo : "Ha ocurrido un problema");

        Label lblMensaje = new Label(mensaje != null ? mensaje : "Ocurrió un error inesperado.");
        lblMensaje.setWrapText(true);
        lblMensaje.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #334155;");
        dialogContent.setContent(lblMensaje);

        MFXStageDialog stageDialog = new MFXStageDialog(dialogContent);
        configurarStageDialog(stageDialog, owner, titulo);

        Button btnEntendido = new Button("Entendido");
        btnEntendido.getStyleClass().addAll("mfx-dialog-btn", "mfx-dialog-primary");
        btnEntendido.setOnAction(e -> stageDialog.close());
        dialogContent.addActions(btnEntendido);

        try {
            stageDialog.showAndWait();
        } catch (Throwable t) {
            Alert fallback = new Alert(Alert.AlertType.ERROR, mensaje != null ? mensaje : "Ocurrió un error inesperado.", ButtonType.OK);
            fallback.setTitle("Error");
            fallback.setHeaderText(titulo != null ? titulo : "Ha ocurrido un problema");
            if (owner != null) fallback.initOwner(owner);
            fallback.showAndWait();
        }
    }

    /**
     * Muestra la ventana modal de entrega única de credenciales para un nuevo miembro.
     */
    public static void credenciales(Window owner, String usuario, String passwordTemporal) {
        MFXGenericDialog dialogContent = new MFXGenericDialog();
        dialogContent.getStyleClass().add("mfx-modern-dialog");
        dialogContent.setPrefWidth(500);

        dialogContent.setHeaderIcon(crearContenedorIcono(HeroIcon.EYE, HeroIcon.BLUE_600, "#dbeafe"));
        dialogContent.setHeaderText("Cuenta creada exitosamente");

        VBox content = new VBox(12);
        content.setPadding(new Insets(4, 0, 8, 0));

        Label sub = new Label("Entregue estas credenciales de acceso al miembro. La contraseña temporal solo se muestra una vez.");
        sub.setWrapText(true);
        sub.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #64748b;");

        VBox credCard = new VBox(8);
        credCard.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 14px;");

        HBox uBox = new HBox(8);
        uBox.setAlignment(Pos.CENTER_LEFT);
        Label uTag = new Label("Usuario / DUI:");
        uTag.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        Label uVal = new Label(usuario != null ? usuario : "—");
        uVal.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #0f172a; -fx-font-family: 'Consolas', monospace;");
        uBox.getChildren().addAll(uTag, uVal);

        HBox pBox = new HBox(8);
        pBox.setAlignment(Pos.CENTER_LEFT);
        Label pTag = new Label("Contraseña:");
        pTag.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        Label pVal = new Label(passwordTemporal != null ? passwordTemporal : "—");
        pVal.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #2563eb; -fx-font-family: 'Consolas', monospace;");
        pBox.getChildren().addAll(pTag, pVal);

        credCard.getChildren().addAll(uBox, pBox);
        content.getChildren().addAll(sub, credCard);

        dialogContent.setContent(content);

        MFXStageDialog stageDialog = new MFXStageDialog(dialogContent);
        configurarStageDialog(stageDialog, owner, "Credenciales generadas");

        Button btnCopiarCerrar = new Button("Listo, credenciales entregadas");
        btnCopiarCerrar.getStyleClass().addAll("mfx-dialog-btn", "mfx-dialog-primary");
        btnCopiarCerrar.setOnAction(e -> stageDialog.close());
        dialogContent.addActions(btnCopiarCerrar);

        stageDialog.showAndWait();
    }

    private static Node crearContenedorIcono(String svgPath, String strokeColor, String bgHex) {
        StackPane container = new StackPane();
        container.setPrefSize(42, 42);
        container.setMinSize(42, 42);
        container.setMaxSize(42, 42);
        container.setStyle("-fx-background-color: " + bgHex + "; -fx-background-radius: 10px;");
        container.getChildren().add(HeroIcon.create(svgPath, strokeColor, 20));
        return container;
    }

    private static void configurarStageDialog(MFXStageDialog stageDialog, Window owner, String title) {
        stageDialog.setTitle(title != null ? title : "Aviso");
        stageDialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            stageDialog.initOwner(owner);
            if (owner.getScene() != null && owner.getScene().getRoot() instanceof Pane rootPane) {
                stageDialog.setOwnerNode(rootPane);
                stageDialog.setScrimOwner(true);
            } else {
                stageDialog.setScrimOwner(false);
            }
        } else {
            stageDialog.setScrimOwner(false);
        }

        // Inyectar hojas de estilo para botones y contenedor MaterialFX
        var cssDialog = MaterialAlertService.class.getResource("/styles/member-dialog.css");
        if (cssDialog != null && stageDialog.getScene() != null) {
            stageDialog.getScene().getStylesheets().add(cssDialog.toExternalForm());
        }
        var appCss = MaterialAlertService.class.getResource("/css/app.css");
        if (appCss != null && stageDialog.getScene() != null) {
            stageDialog.getScene().getStylesheets().add(appCss.toExternalForm());
        }
    }
}
