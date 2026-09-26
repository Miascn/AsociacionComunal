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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
     * Muestra un diálogo informativo con MaterialFX.
     */
    public static void info(Window owner, String titulo, String mensaje) {
        MFXGenericDialog dialogContent = new MFXGenericDialog();
        dialogContent.getStyleClass().add("mfx-modern-dialog");
        dialogContent.setPrefWidth(450);

        dialogContent.setHeaderIcon(crearContenedorIcono(HeroIcon.INFO, HeroIcon.BLUE_600, "#dbeafe"));
        dialogContent.setHeaderText(titulo != null ? titulo : "Información");

        Label lblMensaje = new Label(mensaje != null ? mensaje : "");
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
            Alert fallback = new Alert(Alert.AlertType.INFORMATION, mensaje != null ? mensaje : "", ButtonType.OK);
            fallback.setTitle("Información");
            fallback.setHeaderText(titulo != null ? titulo : "Información");
            if (owner != null) fallback.initOwner(owner);
            fallback.showAndWait();
        }
    }

    /**
     * Muestra un diálogo de advertencia con MaterialFX.
     */
    public static void advertencia(Window owner, String titulo, String mensaje) {
        MFXGenericDialog dialogContent = new MFXGenericDialog();
        dialogContent.getStyleClass().add("mfx-modern-dialog");
        dialogContent.setPrefWidth(460);

        dialogContent.setHeaderIcon(crearContenedorIcono(HeroIcon.SHIELD, HeroIcon.AMBER_600, "#fef3c7"));
        dialogContent.setHeaderText(titulo != null ? titulo : "Advertencia");

        Label lblMensaje = new Label(mensaje != null ? mensaje : "");
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
            Alert fallback = new Alert(Alert.AlertType.WARNING, mensaje != null ? mensaje : "", ButtonType.OK);
            fallback.setTitle("Advertencia");
            fallback.setHeaderText(titulo != null ? titulo : "Advertencia");
            if (owner != null) fallback.initOwner(owner);
            fallback.showAndWait();
        }
    }

    /**
     * Muestra la ventana modal de código QR para un miembro con contraseña personalizada.
     */
    public static void mostrarQrAccesoMovil(Window owner, String nombreMiembro, String usuario) {
        MFXGenericDialog dialogContent = new MFXGenericDialog();
        dialogContent.getStyleClass().add("mfx-modern-dialog");
        dialogContent.setPrefWidth(560);

        dialogContent.setHeaderIcon(crearContenedorIcono(HeroIcon.SHIELD, HeroIcon.GREEN_600, "#dcfce7"));
        dialogContent.setHeaderText("Acceso a App Móvil y Código QR");

        VBox content = new VBox(12);
        content.setPadding(new Insets(4, 0, 8, 0));

        Label aviso = new Label("✓ Contraseña Personalizada Configurada");
        aviso.setWrapText(true);
        aviso.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #15803d;");

        Label sub = new Label("El miembro ya actualizó su contraseña personal desde la app. Al escanear este código QR en su celular se abrirá la aplicación móvil con su usuario/DUI colocado automáticamente.");
        sub.setWrapText(true);
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        HBox mainRow = new HBox(16);
        mainRow.setAlignment(Pos.CENTER_LEFT);

        VBox credCard = new VBox(8);
        credCard.setStyle("-fx-background-color: #f0fdf4; -fx-border-color: #bbf7d0; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 14px;");
        HBox.setHgrow(credCard, Priority.ALWAYS);

        if (nombreMiembro != null && !nombreMiembro.isBlank()) {
            HBox mBox = new HBox(8);
            mBox.setAlignment(Pos.CENTER_LEFT);
            Label mTag = new Label("Miembro:");
            mTag.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #166534;");
            Label mVal = new Label(nombreMiembro);
            mVal.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #1e293b;");
            mBox.getChildren().addAll(mTag, mVal);
            credCard.getChildren().add(mBox);
        }

        HBox uBox = new HBox(8);
        uBox.setAlignment(Pos.CENTER_LEFT);
        Label uTag = new Label("Usuario / DUI:");
        uTag.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #166534;");
        Label uVal = new Label(usuario != null ? usuario : "—");
        uVal.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #0f172a; -fx-font-family: 'Consolas', monospace;");
        uBox.getChildren().addAll(uTag, uVal);

        HBox pBox = new HBox(8);
        pBox.setAlignment(Pos.CENTER_LEFT);
        Label pTag = new Label("Contraseña:");
        pTag.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #166534;");
        Label pVal = new Label("•••••••• (Protegida)");
        pVal.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #15803d; -fx-font-family: 'Consolas', monospace;");
        pBox.getChildren().addAll(pTag, pVal);

        credCard.getChildren().addAll(uBox, pBox);

        VBox qrBox = crearTarjetaQr(usuario, "", 145);

        mainRow.getChildren().addAll(credCard, qrBox);
        content.getChildren().addAll(aviso, sub, mainRow);

        dialogContent.setContent(content);

        MFXStageDialog stageDialog = new MFXStageDialog(dialogContent);
        configurarStageDialog(stageDialog, owner, "Código QR de Acceso");

        Button btnCopiarEnlace = new Button("Copiar Enlace QR");
        btnCopiarEnlace.getStyleClass().addAll("mfx-dialog-btn", "mfx-dialog-cancel");
        btnCopiarEnlace.setOnAction(e -> {
            try {
                String uri = QrCodeService.buildLoginUri(usuario, "");
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                cc.putString(uri);
                clipboard.setContent(cc);
                btnCopiarEnlace.setText("¡Enlace copiado!");
            } catch (Exception ignored) {}
        });

        Button btnCerrar = new Button("Entendido / Cerrar");
        btnCerrar.getStyleClass().addAll("mfx-dialog-btn", "mfx-dialog-primary");
        btnCerrar.setOnAction(e -> stageDialog.close());

        dialogContent.addActions(btnCopiarEnlace, btnCerrar);
        stageDialog.showAndWait();
    }

    /**
     * Muestra la ventana modal de entrega única de credenciales para un nuevo miembro.
     */
    public static void credenciales(Window owner, String usuario, String passwordTemporal) {
        MFXGenericDialog dialogContent = new MFXGenericDialog();
        dialogContent.getStyleClass().add("mfx-modern-dialog");
        dialogContent.setPrefWidth(580);

        dialogContent.setHeaderIcon(crearContenedorIcono(HeroIcon.SHIELD, HeroIcon.BLUE_600, "#dbeafe"));
        dialogContent.setHeaderText("Credenciales y Código QR de Acceso Móvil");

        VBox content = new VBox(12);
        content.setPadding(new Insets(4, 0, 8, 0));

        Label sub = new Label("Entregue estas credenciales o permita que el miembro escanee el código QR con su teléfono celular. La app Android se abrirá y pondrá automáticamente el usuario y contraseña.");
        sub.setWrapText(true);
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

        HBox mainRow = new HBox(16);
        mainRow.setAlignment(Pos.CENTER_LEFT);

        VBox credCard = new VBox(10);
        credCard.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 14px;");
        HBox.setHgrow(credCard, Priority.ALWAYS);

        HBox uBox = new HBox(8);
        uBox.setAlignment(Pos.CENTER_LEFT);
        Label uTag = new Label("Usuario / DUI:");
        uTag.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        Label uVal = new Label(usuario != null ? usuario : "—");
        uVal.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #0f172a; -fx-font-family: 'Consolas', monospace;");
        uBox.getChildren().addAll(uTag, uVal);

        HBox pBox = new HBox(8);
        pBox.setAlignment(Pos.CENTER_LEFT);
        Label pTag = new Label("Contraseña:");
        pTag.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        Label pVal = new Label(passwordTemporal != null ? passwordTemporal : "—");
        pVal.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #2563eb; -fx-font-family: 'Consolas', monospace;");
        pBox.getChildren().addAll(pTag, pVal);

        Label aviso = new Label("ℹ️ La contraseña provisional permanecerá guardada y visible en la información del miembro hasta que éste inicie sesión en su teléfono celular y la reemplace por una propia.");
        aviso.setWrapText(true);
        aviso.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-style: italic;");

        credCard.getChildren().addAll(uBox, pBox, aviso);

        VBox qrBox = crearTarjetaQr(usuario, passwordTemporal, 145);

        mainRow.getChildren().addAll(credCard, qrBox);
        content.getChildren().addAll(sub, mainRow);

        dialogContent.setContent(content);

        MFXStageDialog stageDialog = new MFXStageDialog(dialogContent);
        configurarStageDialog(stageDialog, owner, "Credenciales y QR Generado");

        Button btnCopiarEnlace = new Button("Copiar Enlace QR");
        btnCopiarEnlace.getStyleClass().addAll("mfx-dialog-btn", "mfx-dialog-cancel");
        btnCopiarEnlace.setOnAction(e -> {
            try {
                String uri = QrCodeService.buildLoginUri(usuario, passwordTemporal);
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                cc.putString(uri);
                clipboard.setContent(cc);
                btnCopiarEnlace.setText("¡Enlace copiado!");
            } catch (Exception ignored) {}
        });

        Button btnCopiarTexto = new Button("Copiar Datos");
        btnCopiarTexto.getStyleClass().addAll("mfx-dialog-btn", "mfx-dialog-cancel");
        btnCopiarTexto.setOnAction(e -> {
            try {
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                cc.putString("Usuario: " + (usuario != null ? usuario : "") + "\nContraseña: " + (passwordTemporal != null ? passwordTemporal : ""));
                clipboard.setContent(cc);
                btnCopiarTexto.setText("¡Copiado!");
            } catch (Exception ignored) {}
        });

        Button btnCerrar = new Button("Listo, credenciales entregadas");
        btnCerrar.getStyleClass().addAll("mfx-dialog-btn", "mfx-dialog-primary");
        btnCerrar.setOnAction(e -> stageDialog.close());

        dialogContent.addActions(btnCopiarEnlace, btnCopiarTexto, btnCerrar);
        stageDialog.showAndWait();
    }

    /**
     * Muestra la ventana modal para revisar credenciales de un miembro con clave provisional activa.
     */
    public static void revisarCredencialesProvisionales(Window owner, String nombreMiembro, String usuario, String claveTemporal) {
        MFXGenericDialog dialogContent = new MFXGenericDialog();
        dialogContent.getStyleClass().add("mfx-modern-dialog");
        dialogContent.setPrefWidth(580);

        dialogContent.setHeaderIcon(crearContenedorIcono(HeroIcon.SHIELD, "#d97706", "#fef3c7"));
        dialogContent.setHeaderText("Credenciales y Código QR Provisionales");

        VBox content = new VBox(12);
        content.setPadding(new Insets(4, 0, 8, 0));

        Label aviso = new Label("⚠️ Este miembro todavía no ha cambiado su contraseña provisional en la aplicación móvil.");
        aviso.setWrapText(true);
        aviso.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #b45309;");

        Label sub = new Label("Esta alerta, contraseña y código QR permanecerán disponibles hasta que el miembro inicie sesión en su teléfono celular y cambie su contraseña.");
        sub.setWrapText(true);
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        HBox mainRow = new HBox(16);
        mainRow.setAlignment(Pos.CENTER_LEFT);

        VBox credCard = new VBox(8);
        credCard.setStyle("-fx-background-color: #fffbeb; -fx-border-color: #fde68a; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 14px;");
        HBox.setHgrow(credCard, Priority.ALWAYS);

        if (nombreMiembro != null && !nombreMiembro.isBlank()) {
            HBox mBox = new HBox(8);
            mBox.setAlignment(Pos.CENTER_LEFT);
            Label mTag = new Label("Miembro:");
            mTag.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #78350f;");
            Label mVal = new Label(nombreMiembro);
            mVal.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #1e293b;");
            mBox.getChildren().addAll(mTag, mVal);
            credCard.getChildren().add(mBox);
        }

        HBox uBox = new HBox(8);
        uBox.setAlignment(Pos.CENTER_LEFT);
        Label uTag = new Label("Usuario / DUI:");
        uTag.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #78350f;");
        Label uVal = new Label(usuario != null ? usuario : "—");
        uVal.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #0f172a; -fx-font-family: 'Consolas', monospace;");
        uBox.getChildren().addAll(uTag, uVal);

        HBox pBox = new HBox(8);
        pBox.setAlignment(Pos.CENTER_LEFT);
        Label pTag = new Label("Contraseña:");
        pTag.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #78350f;");
        String claveMostrar = (claveTemporal != null && !claveTemporal.isBlank()) ? claveTemporal : "(Generada previamente)";
        Label pVal = new Label(claveMostrar);
        pVal.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #b45309; -fx-font-family: 'Consolas', monospace;");
        pBox.getChildren().addAll(pTag, pVal);

        credCard.getChildren().addAll(uBox, pBox);

        VBox qrBox = crearTarjetaQr(usuario, claveTemporal, 145);

        mainRow.getChildren().addAll(credCard, qrBox);
        content.getChildren().addAll(aviso, sub, mainRow);

        dialogContent.setContent(content);

        MFXStageDialog stageDialog = new MFXStageDialog(dialogContent);
        configurarStageDialog(stageDialog, owner, "Revisar Credenciales Provisionales");

        Button btnCopiarEnlace = new Button("Copiar Enlace QR");
        btnCopiarEnlace.getStyleClass().addAll("mfx-dialog-btn", "mfx-dialog-cancel");
        btnCopiarEnlace.setOnAction(e -> {
            try {
                String uri = QrCodeService.buildLoginUri(usuario, claveTemporal);
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                cc.putString(uri);
                clipboard.setContent(cc);
                btnCopiarEnlace.setText("¡Enlace copiado!");
            } catch (Exception ignored) {}
        });

        Button btnCopiar = new Button("Copiar Datos");
        btnCopiar.getStyleClass().addAll("mfx-dialog-btn", "mfx-dialog-cancel");
        btnCopiar.setOnAction(e -> {
            try {
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                cc.putString("Usuario: " + (usuario != null ? usuario : "") + "\nContraseña: " + (claveTemporal != null ? claveTemporal : ""));
                clipboard.setContent(cc);
                btnCopiar.setText("¡Copiado!");
            } catch (Exception ignored) {}
        });

        Button btnCerrar = new Button("Entendido / Cerrar");
        btnCerrar.getStyleClass().addAll("mfx-dialog-btn", "mfx-dialog-primary");
        btnCerrar.setOnAction(e -> stageDialog.close());

        dialogContent.addActions(btnCopiarEnlace, btnCopiar, btnCerrar);
        stageDialog.showAndWait();
    }

    private static VBox crearTarjetaQr(String username, String password, int qrSize) {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cbd5e1; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 10px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

        String uri = QrCodeService.buildLoginUri(username, password);
        Image qrImage = QrCodeService.generateQr(uri, qrSize, qrSize);
        ImageView imgView = new ImageView(qrImage);
        imgView.setFitWidth(qrSize);
        imgView.setFitHeight(qrSize);
        imgView.setPreserveRatio(true);

        Label lblTitulo = new Label("📱 Escanea con tu celular");
        lblTitulo.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Label lblSub = new Label("Abre la app y autocompleta");
        lblSub.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");

        box.getChildren().addAll(imgView, lblTitulo, lblSub);
        return box;
    }

    /**
     * Muestra diálogo interactivo para actualizar la cuota mensual de mantenimiento de la colonia.
     */
    public static java.math.BigDecimal dialogoModificarCuota(Window owner, java.math.BigDecimal cuotaActual) {
        java.util.concurrent.atomic.AtomicReference<java.math.BigDecimal> resultado = new java.util.concurrent.atomic.AtomicReference<>(null);

        MFXGenericDialog dialogContent = new MFXGenericDialog();
        dialogContent.getStyleClass().add("mfx-modern-dialog");
        dialogContent.setPrefWidth(460);

        dialogContent.setHeaderIcon(crearContenedorIcono(HeroIcon.SPARKLES, HeroIcon.BLUE_600, "#dbeafe"));
        dialogContent.setHeaderText("Cuota de Mantenimiento Mensual");

        VBox content = new VBox(12);
        content.setPadding(new Insets(4, 0, 8, 0));

        Label desc = new Label("Configure el monto oficial de la aportación mensual de mantenimiento y vigilancia comunal:");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");

        VBox inputCard = new VBox(8);
        inputCard.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 14px;");

        Label lblMonto = new Label("Monto mensual por vivienda/miembro ($ USD):");
        lblMonto.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #334155;");

        javafx.scene.control.TextField txtMonto = new javafx.scene.control.TextField(
            cuotaActual != null ? cuotaActual.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "10.00"
        );
        txtMonto.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-border-color: #cbd5e1; -fx-border-radius: 6px; -fx-padding: 8px;");

        Label lblError = new Label();
        lblError.setStyle("-fx-font-size: 12px; -fx-text-fill: #dc2626; -fx-font-weight: 600;");
        lblError.setVisible(false);
        lblError.setManaged(false);

        inputCard.getChildren().addAll(lblMonto, txtMonto, lblError);
        content.getChildren().addAll(desc, inputCard);

        dialogContent.setContent(content);

        MFXStageDialog stageDialog = new MFXStageDialog(dialogContent);
        configurarStageDialog(stageDialog, owner, "Modificar Cuota de Mantenimiento");

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().addAll("mfx-dialog-btn", "mfx-dialog-cancel");
        btnCancelar.setOnAction(e -> stageDialog.close());

        Button btnGuardar = new Button("Guardar Nueva Cuota");
        btnGuardar.getStyleClass().addAll("mfx-dialog-btn", "mfx-dialog-primary");
        btnGuardar.setOnAction(e -> {
            try {
                String str = txtMonto.getText().trim().replace("$", "");
                java.math.BigDecimal nuevo = new java.math.BigDecimal(str);
                if (nuevo.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                    lblError.setText("La cuota debe ser mayor a $0.00");
                    lblError.setVisible(true);
                    lblError.setManaged(true);
                    return;
                }
                resultado.set(nuevo.setScale(2, java.math.RoundingMode.HALF_UP));
                stageDialog.close();
            } catch (Exception ex) {
                lblError.setText("Ingrese un monto numérico válido (ej. 15.00)");
                lblError.setVisible(true);
                lblError.setManaged(true);
            }
        });

        dialogContent.addActions(btnCancelar, btnGuardar);
        stageDialog.showAndWait();
        return resultado.get();
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
