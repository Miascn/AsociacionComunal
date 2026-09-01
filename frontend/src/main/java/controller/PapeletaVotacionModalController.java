package controller;

import java.util.HashMap;
import java.util.Map;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.VotacionModel;
import service.VotacionApiClient;

public class PapeletaVotacionModalController {
    @FXML private Label lblEstado;
    @FXML private Label lblTitulo;
    @FXML private Label lblDescripcion;
    @FXML private Label lblFechas;
    @FXML private HBox bannerYaVoto;
    @FXML private Label lblFechaHoraVoto;
    @FXML private VBox secOpciones;
    @FXML private VBox boxOpciones;
    @FXML private Label lblError;
    @FXML private Button btnEmitirVoto;

    private final VotacionApiClient apiClient = new VotacionApiClient();
    private final ToggleGroup grupoOpciones = new ToggleGroup();
    private final Map<RadioButton, VotacionModel.OpcionModel> mapaOpciones = new HashMap<>();

    private VotacionModel votacion;
    private Runnable onVotedCallback;

    public void setVotacion(VotacionModel v) {
        this.votacion = v;
        if (v == null) return;

        lblTitulo.setText(v.getTitulo());
        lblDescripcion.setText(v.getDescripcion() != null && !v.getDescripcion().isBlank() ? v.getDescripcion() : "Sin descripción adicional.");
        lblFechas.setText("Válida: " + v.getRangoFechas() + " (" + v.getProyectoDisplay() + ")");
        lblEstado.setText(v.getEstado());

        verificarEstadoParticipacion();
    }

    public void setOnVotedCallback(Runnable callback) {
        this.onVotedCallback = callback;
    }

    private void verificarEstadoParticipacion() {
        Task<VotacionApiClient.ParticipacionDto> task = new Task<>() {
            @Override
            protected VotacionApiClient.ParticipacionDto call() throws Exception {
                return apiClient.verificarParticipacion(votacion.getId(), null);
            }
        };

        task.setOnSucceeded(e -> {
            VotacionApiClient.ParticipacionDto part = task.getValue();
            if (part != null && part.yaVoto()) {
                mostrarEstadoYaVoto(part.fechaHoraVoto());
            } else {
                mostrarPapeletaActiva();
            }
        });

        task.setOnFailed(e -> {
            // Si falla verificación, permitimos renderizar opciones y el backend validará
            mostrarPapeletaActiva();
        });

        new Thread(task).start();
    }

    private void mostrarEstadoYaVoto(String fechaHora) {
        bannerYaVoto.setVisible(true);
        bannerYaVoto.setManaged(true);
        if (fechaHora != null && !fechaHora.isBlank()) {
            lblFechaHoraVoto.setText("Tu voto fue registrado el " + fechaHora.replace("T", " ") + " de forma secreta e inmutable.");
        }

        secOpciones.setVisible(false);
        secOpciones.setManaged(false);
        btnEmitirVoto.setVisible(false);
        btnEmitirVoto.setManaged(false);
    }

    private void mostrarPapeletaActiva() {
        bannerYaVoto.setVisible(false);
        bannerYaVoto.setManaged(false);

        secOpciones.setVisible(true);
        secOpciones.setManaged(true);
        btnEmitirVoto.setVisible(true);
        btnEmitirVoto.setManaged(true);

        boxOpciones.getChildren().clear();
        mapaOpciones.clear();

        for (VotacionModel.OpcionModel op : votacion.getOpciones()) {
            HBox card = new HBox(12);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 12 16 12 16; -fx-cursor: hand;");

            RadioButton rb = new RadioButton(op.getDescripcion());
            rb.setToggleGroup(grupoOpciones);
            rb.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            card.setOnMouseClicked(evt -> rb.setSelected(true));

            card.getChildren().add(rb);
            boxOpciones.getChildren().add(card);
            mapaOpciones.put(rb, op);
        }
    }

    @FXML
    public void emitirVoto() {
        lblError.setText("");
        RadioButton seleccionado = (RadioButton) grupoOpciones.getSelectedToggle();
        if (seleccionado == null) {
            lblError.setText("Por favor, selecciona una opción de la papeleta.");
            return;
        }

        VotacionModel.OpcionModel op = mapaOpciones.get(seleccionado);
        if (op == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar emisión de voto");
        confirm.setHeaderText("¿Confirmas tu voto por: \"" + op.getDescripcion() + "\"?");
        confirm.setContentText("ATENCIÓN: Tu voto será registrado de forma secreta e inmutable. Esta acción no se puede deshacer ni modificar.");

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                btnEmitirVoto.setDisable(true);

                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        apiClient.emitirVoto(votacion.getId(), op.getIdOpcion(), null);
                        return null;
                    }
                };

                task.setOnSucceeded(e -> {
                    btnEmitirVoto.setDisable(false);
                    if (onVotedCallback != null) onVotedCallback.run();
                    mostrarEstadoYaVoto("hace un momento");

                    Alert exito = new Alert(Alert.AlertType.INFORMATION);
                    exito.setTitle("Voto emitido");
                    exito.setHeaderText("¡Tu voto ha sido registrado exitosamente!");
                    exito.setContentText("Gracias por participar en este proceso democrático comunal.");
                    exito.showAndWait();
                });

                task.setOnFailed(e -> {
                    btnEmitirVoto.setDisable(false);
                    Throwable ex = task.getException();
                    lblError.setText(ex != null ? ex.getMessage() : "Error al registrar el voto.");
                });

                new Thread(task).start();
            }
        });
    }

    @FXML
    public void cancelar() {
        Stage stage = (Stage) btnEmitirVoto.getScene().getWindow();
        if (stage != null) stage.close();
    }
}
