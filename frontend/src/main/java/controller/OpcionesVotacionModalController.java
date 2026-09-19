package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.OpcionVotacionModel;
import models.VotacionModel;
import service.VotacionApiClient;

public class OpcionesVotacionModalController {
    @FXML private Label lblModalTitulo;
    @FXML private Label lblSubtitulo;
    @FXML private HBox bannerSoloLectura;
    @FXML private Label lblBannerMensaje;
    @FXML private VBox secAgregar;
    @FXML private TextField txtNuevaOpcion;
    @FXML private Button btnAgregar;
    @FXML private Label lblContadorOpciones;
    @FXML private ListView<OpcionVotacionModel> listOpciones;
    @FXML private Label lblError;

    private final ObservableList<OpcionVotacionModel> opciones = FXCollections.observableArrayList();
    private final VotacionApiClient apiClient = new VotacionApiClient();

    private VotacionModel votacion;
    private Runnable onCloseCallback;

    @FXML
    public void initialize() {
        listOpciones.setItems(opciones);
        listOpciones.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(OpcionVotacionModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(8);
                    box.setAlignment(Pos.CENTER_LEFT);

                    Label badgeOrden = new Label("#" + item.getOrden());
                    badgeOrden.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #1e293b; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-min-width: 32; -fx-alignment: center;");

                    Label lblDesc = new Label(item.getDescripcion());
                    lblDesc.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
                    HBox.setHgrow(lblDesc, Priority.ALWAYS);

                    if (item.getVotos() > 0) {
                        Label badgeVotos = new Label(item.getVotos() + " votos");
                        badgeVotos.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-font-size: 11px; -fx-padding: 2 6 2 6; -fx-background-radius: 4;");
                        box.getChildren().addAll(badgeOrden, lblDesc, badgeVotos);
                    } else {
                        box.getChildren().addAll(badgeOrden, lblDesc);
                    }

                    if (item.isEditable()) {
                        Button btnSubir = new Button("▲");
                        btnSubir.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-padding: 2 6 2 6; -fx-cursor: hand;");
                        btnSubir.setOnAction(e -> subirOpcion(item));

                        Button btnBajar = new Button("▼");
                        btnBajar.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-padding: 2 6 2 6; -fx-cursor: hand;");
                        btnBajar.setOnAction(e -> bajarOpcion(item));

                        Button btnEditar = new Button("✎");
                        btnEditar.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #2563eb; -fx-padding: 2 6 2 6; -fx-cursor: hand;");
                        btnEditar.setOnAction(e -> editarOpcion(item));

                        Button btnEliminar = new Button("✕");
                        btnEliminar.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-cursor: hand;");
                        btnEliminar.setOnAction(e -> eliminarOpcion(item));

                        box.getChildren().addAll(btnSubir, btnBajar, btnEditar, btnEliminar);
                    }

                    setGraphic(box);
                    setText(null);
                }
            }
        });
    }

    public void setVotacion(VotacionModel v) {
        this.votacion = v;
        if (v != null) {
            lblModalTitulo.setText("Opciones: " + v.getTitulo());

            // BORRADOR o PROGRAMADA, igual que OpcionVotacionService.validarVotacionModificable.
            // Antes se usaba isBorrador(), que agrupaba ambos estados; al separarlos hay que
            // nombrar la regla real para no bloquear la edición en una votación programada.
            boolean editable = v.isEditable();
            bannerSoloLectura.setVisible(!editable);
            bannerSoloLectura.setManaged(!editable);
            if (!editable) {
                lblBannerMensaje.setText("🔒 Modo solo lectura: El proceso se encuentra " + v.getEstado() +
                    ". Las alternativas están bloqueadas para proteger la integridad del conteo de votos.");
            }

            secAgregar.setVisible(editable);
            secAgregar.setManaged(editable);

            cargarOpciones();
        }
    }

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    private void cargarOpciones() {
        if (votacion == null) return;
        lblError.setText("");

        Task<List<OpcionVotacionModel>> task = new Task<>() {
            @Override
            protected List<OpcionVotacionModel> call() throws Exception {
                return apiClient.getOpciones(votacion.getId());
            }
        };

        task.setOnSucceeded(e -> {
            opciones.setAll(task.getValue());
            int total = opciones.size();
            lblContadorOpciones.setText(total + (total == 1 ? " opción" : " opciones") +
                (total < 2 ? " (mínimo 2 para abrir)" : ""));
        });

        task.setOnFailed(e -> {
            lblError.setText(task.getException() != null ? task.getException().getMessage() : "Error al cargar opciones.");
        });

        new Thread(task).start();
    }

    @FXML
    public void agregarOpcion() {
        lblError.setText("");
        String texto = txtNuevaOpcion.getText();
        if (texto == null || texto.trim().isBlank()) {
            lblError.setText("La descripción de la opción es obligatoria.");
            return;
        }

        btnAgregar.setDisable(true);

        Task<OpcionVotacionModel> task = new Task<>() {
            @Override
            protected OpcionVotacionModel call() throws Exception {
                return apiClient.addOpcion(votacion.getId(), texto.trim(), null);
            }
        };

        task.setOnSucceeded(e -> {
            btnAgregar.setDisable(false);
            txtNuevaOpcion.clear();
            cargarOpciones();
        });

        task.setOnFailed(e -> {
            btnAgregar.setDisable(false);
            lblError.setText(task.getException() != null ? task.getException().getMessage() : "Error al agregar opción.");
        });

        new Thread(task).start();
    }

    private void subirOpcion(OpcionVotacionModel item) {
        int idx = opciones.indexOf(item);
        if (idx <= 0) return;
        Collections.swap(opciones, idx, idx - 1);
        guardarReordenamiento();
    }

    private void bajarOpcion(OpcionVotacionModel item) {
        int idx = opciones.indexOf(item);
        if (idx < 0 || idx >= opciones.size() - 1) return;
        Collections.swap(opciones, idx, idx + 1);
        guardarReordenamiento();
    }

    private void guardarReordenamiento() {
        List<Integer> ids = new ArrayList<>();
        for (OpcionVotacionModel op : opciones) {
            ids.add(op.getIdOpcion());
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                apiClient.reordenarOpciones(votacion.getId(), ids);
                return null;
            }
        };

        task.setOnSucceeded(e -> cargarOpciones());
        task.setOnFailed(e -> lblError.setText("Error al reordenar: " + task.getException().getMessage()));
        new Thread(task).start();
    }

    private void editarOpcion(OpcionVotacionModel item) {
        TextInputDialog dialog = new TextInputDialog(item.getDescripcion());
        dialog.setTitle("Editar opción de votación");
        dialog.setHeaderText("Modificar descripción de la alternativa #" + item.getOrden());
        dialog.setContentText("Descripción:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(nuevaDesc -> {
            if (nuevaDesc.trim().isBlank()) {
                lblError.setText("La descripción no puede estar vacía.");
                return;
            }

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    apiClient.updateOpcion(item.getIdOpcion(), nuevaDesc.trim(), item.getOrden());
                    return null;
                }
            };

            task.setOnSucceeded(e -> cargarOpciones());
            task.setOnFailed(e -> lblError.setText("Error al actualizar: " + task.getException().getMessage()));
            new Thread(task).start();
        });
    }

    private void eliminarOpcion(OpcionVotacionModel item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar opción");
        confirm.setHeaderText("¿Eliminar la opción \"" + item.getDescripcion() + "\"?");
        confirm.setContentText("Esta acción retirará la opción de la votación.");

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        apiClient.deleteOpcion(item.getIdOpcion());
                        return null;
                    }
                };

                task.setOnSucceeded(e -> cargarOpciones());
                task.setOnFailed(e -> lblError.setText("Error al eliminar: " + task.getException().getMessage()));
                new Thread(task).start();
            }
        });
    }

    @FXML
    public void cerrar() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
        Stage stage = (Stage) lblModalTitulo.getScene().getWindow();
        if (stage != null) stage.close();
    }
}
