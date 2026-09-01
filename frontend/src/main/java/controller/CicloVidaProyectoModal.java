package controller;

import java.io.IOException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.ProyectoModel;
import org.kordamp.ikonli.javafx.FontIcon;
import service.ProyectoApiClient;

public final class CicloVidaProyectoModal {

    private CicloVidaProyectoModal() {}

    public static void mostrar(ProyectoModel proyecto, Stage owner, boolean puedeGestionar, Runnable onActualizado) {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        if (owner != null) stage.initOwner(owner);
        stage.setTitle("Ciclo de vida - " + proyecto.getNombre());
        stage.setResizable(false);

        VBox root = new VBox(16);
        root.setPadding(new Insets(24, 28, 24, 28));
        root.setPrefWidth(560);
        root.getStyleClass().addAll("form-dialog", "member-form-dialog");

        // Encabezado
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label eyebrow = new Label("GESTIÓN DEL CICLO DE VIDA");
        eyebrow.getStyleClass().add("page-eyebrow");

        Label title = new Label(proyecto.getNombre());
        title.getStyleClass().addAll("page-title", "members-title");
        title.setWrapText(true);

        Label subtitle = new Label("Creado por " + proyecto.getCreadorDisplay() + " el " + proyecto.getFechaCreacion());
        subtitle.getStyleClass().add("page-subtitle");

        titleBox.getChildren().addAll(eyebrow, title, subtitle);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label badgeEstado = new Label(proyecto.getEstado());
        aplicarEstiloBadge(badgeEstado, proyecto.getEstado());
        badgeEstado.setPadding(new Insets(6, 12, 6, 12));

        header.getChildren().addAll(titleBox, badgeEstado);

        // Tarjeta de Presupuesto y Descripción
        VBox descCard = new VBox(8);
        descCard.setPadding(new Insets(14));
        descCard.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 8; -fx-border-color: -color-border-subtle; -fx-border-radius: 8;");

        HBox presRow = new HBox(8);
        presRow.setAlignment(Pos.CENTER_LEFT);
        Label lblPresTitle = new Label("PRESUPUESTO ASIGNADO:");
        lblPresTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: -color-fg-muted;");
        Label lblPresVal = new Label(proyecto.getPresupuestoFormateado());
        lblPresVal.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: -color-success-fg;");
        presRow.getChildren().addAll(lblPresTitle, lblPresVal);

        Label lblDesc = new Label(proyecto.getDescripcion() != null && !proyecto.getDescripcion().isBlank()
            ? proyecto.getDescripcion()
            : "Sin descripción detallada registrada.");
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-fg-default;");

        descCard.getChildren().addAll(presRow, lblDesc);

        // Barra de Fases del Ciclo de Vida
        VBox stepperBox = new VBox(10);
        Label lblFases = new Label("FASES DEL PROYECTO COMUNAL");
        lblFases.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -color-fg-muted;");

        double progress = calcularProgreso(proyecto.getEstado());
        ProgressBar progressBar = new ProgressBar(progress);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(10);

        HBox stepsLabels = new HBox();
        stepsLabels.setAlignment(Pos.CENTER);
        stepsLabels.getChildren().addAll(
            crearStepLabel("1. Borrador", esAlcanzado(proyecto.getEstado(), 1)),
            crearSpacer(),
            crearStepLabel("2. Propuesto", esAlcanzado(proyecto.getEstado(), 2)),
            crearSpacer(),
            crearStepLabel("3. Aprobado", esAlcanzado(proyecto.getEstado(), 3)),
            crearSpacer(),
            crearStepLabel("4. En ejecución", esAlcanzado(proyecto.getEstado(), 4)),
            crearSpacer(),
            crearStepLabel("5. Finalizado", esAlcanzado(proyecto.getEstado(), 5))
        );

        stepperBox.getChildren().addAll(lblFases, progressBar, stepsLabels);

        // Mensaje de estado
        Label lblStatusMsg = new Label();
        lblStatusMsg.setWrapText(true);
        lblStatusMsg.setStyle("-fx-font-size: 11px;");

        // Botones de acciones contextuales
        HBox actionsRow = new HBox(10);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.getStyleClass().add("secondary-action");
        btnCerrar.setOnAction(e -> stage.close());

        actionsRow.getChildren().add(btnCerrar);

        if (puedeGestionar) {
            String estado = proyecto.getEstado() != null ? proyecto.getEstado().toUpperCase() : "BORRADOR";
            switch (estado) {
                case "BORRADOR" -> {
                    Button btnAvanzar = new Button("Presentar propuesta");
                    btnAvanzar.getStyleClass().add("primary-action");
                    btnAvanzar.setGraphic(new FontIcon("fth-arrow-right"));
                    btnAvanzar.setOnAction(e -> ejecutarCambio(proyecto, "PROPUESTO", stage, lblStatusMsg, onActualizado));
                    actionsRow.getChildren().add(btnAvanzar);
                }
                case "PROPUESTO" -> {
                    Button btnRechazar = new Button("Rechazar");
                    btnRechazar.getStyleClass().add("secondary-action");
                    btnRechazar.setStyle("-fx-text-fill: -color-danger-fg;");
                    btnRechazar.setOnAction(e -> ejecutarCambio(proyecto, "RECHAZADO", stage, lblStatusMsg, onActualizado));

                    Button btnAprobar = new Button("Aprobar proyecto");
                    btnAprobar.getStyleClass().add("primary-action");
                    btnAprobar.setGraphic(new FontIcon("fth-check"));
                    btnAprobar.setOnAction(e -> ejecutarCambio(proyecto, "APROBADO", stage, lblStatusMsg, onActualizado));

                    actionsRow.getChildren().addAll(btnRechazar, btnAprobar);
                }
                case "APROBADO" -> {
                    Button btnRechazar = new Button("Cancelar/Rechazar");
                    btnRechazar.getStyleClass().add("secondary-action");
                    btnRechazar.setStyle("-fx-text-fill: -color-danger-fg;");
                    btnRechazar.setOnAction(e -> ejecutarCambio(proyecto, "RECHAZADO", stage, lblStatusMsg, onActualizado));

                    Button btnEjecutar = new Button("Iniciar ejecución");
                    btnEjecutar.getStyleClass().add("primary-action");
                    btnEjecutar.setGraphic(new FontIcon("fth-play"));
                    btnEjecutar.setOnAction(e -> ejecutarCambio(proyecto, "EN_EJECUCION", stage, lblStatusMsg, onActualizado));

                    actionsRow.getChildren().addAll(btnRechazar, btnEjecutar);
                }
                case "EN_EJECUCION" -> {
                    Button btnFinalizar = new Button("Finalizar proyecto");
                    btnFinalizar.getStyleClass().add("primary-action");
                    btnFinalizar.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white;");
                    btnFinalizar.setGraphic(new FontIcon("fth-award"));
                    btnFinalizar.setOnAction(e -> ejecutarCambio(proyecto, "FINALIZADO", stage, lblStatusMsg, onActualizado));

                    actionsRow.getChildren().add(btnFinalizar);
                }
                case "RECHAZADO" -> {
                    Button btnReformular = new Button("Reformular (volver a Borrador)");
                    btnReformular.getStyleClass().add("primary-action");
                    btnReformular.setGraphic(new FontIcon("fth-rotate-ccw"));
                    btnReformular.setOnAction(e -> ejecutarCambio(proyecto, "BORRADOR", stage, lblStatusMsg, onActualizado));

                    actionsRow.getChildren().add(btnReformular);
                }
            }
        }

        root.getChildren().addAll(header, new Separator(), descCard, stepperBox, lblStatusMsg, new Separator(), actionsRow);

        stage.setScene(new Scene(root));
        stage.showAndWait();
    }

    private static void ejecutarCambio(ProyectoModel proyecto, String nuevoEstado, Stage stage, Label lblStatus, Runnable onActualizado) {
        lblStatus.setStyle("-fx-text-fill: -color-accent-fg;");
        lblStatus.setText("Actualizando estado a " + nuevoEstado + "...");

        Task<ProyectoModel> task = new Task<>() {
            @Override
            protected ProyectoModel call() throws Exception {
                return new ProyectoApiClient().cambiarEstado(proyecto.getId(), nuevoEstado);
            }
        };

        task.setOnSucceeded(e -> {
            if (onActualizado != null) {
                onActualizado.run();
            }
            stage.close();
        });

        task.setOnFailed(e -> {
            lblStatus.setStyle("-fx-text-fill: -color-danger-fg;");
            Throwable err = task.getException();
            lblStatus.setText("No se pudo cambiar el estado: " + (err != null ? err.getMessage() : "Error de red."));
        });

        Thread thread = new Thread(task, "cambiar-estado-proyecto");
        thread.setDaemon(true);
        thread.start();
    }

    private static double calcularProgreso(String estado) {
        if (estado == null) return 0.2;
        return switch (estado.toUpperCase()) {
            case "BORRADOR" -> 0.2;
            case "PROPUESTO" -> 0.4;
            case "APROBADO" -> 0.6;
            case "EN_EJECUCION" -> 0.8;
            case "FINALIZADO" -> 1.0;
            case "RECHAZADO" -> 0.0;
            default -> 0.2;
        };
    }

    private static boolean esAlcanzado(String estado, int step) {
        if (estado == null) return step == 1;
        int current = switch (estado.toUpperCase()) {
            case "BORRADOR" -> 1;
            case "PROPUESTO" -> 2;
            case "APROBADO" -> 3;
            case "EN_EJECUCION" -> 4;
            case "FINALIZADO" -> 5;
            default -> 0;
        };
        return step <= current;
    }

    private static Label crearStepLabel(String texto, boolean alcanzado) {
        Label lbl = new Label(texto);
        if (alcanzado) {
            lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -color-accent-fg;");
        } else {
            lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-fg-muted;");
        }
        return lbl;
    }

    private static Region crearSpacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private static void aplicarEstiloBadge(Label lbl, String estado) {
        if (estado == null) estado = "BORRADOR";
        lbl.setStyle(switch (estado.toUpperCase()) {
            case "BORRADOR" -> "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 12; -fx-font-weight: bold;";
            case "PROPUESTO" -> "-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; -fx-background-radius: 12; -fx-font-weight: bold;";
            case "APROBADO" -> "-fx-background-color: #e0e7ff; -fx-text-fill: #4338ca; -fx-background-radius: 12; -fx-font-weight: bold;";
            case "EN_EJECUCION" -> "-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-background-radius: 12; -fx-font-weight: bold;";
            case "FINALIZADO" -> "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-background-radius: 12; -fx-font-weight: bold;";
            case "RECHAZADO" -> "-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-background-radius: 12; -fx-font-weight: bold;";
            default -> "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 12;";
        });
    }
}
