package controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.AportacionModel;
import org.kordamp.ikonli.javafx.FontIcon;

public final class ReciboAportacionModal {

    public static void mostrar(AportacionModel aportacion, Stage owner) {
        Stage dialog = new Stage();
        dialog.setTitle("Recibo de aportación #" + aportacion.getIdAportacion());
        dialog.initModality(Modality.WINDOW_MODAL);
        if (owner != null) dialog.initOwner(owner);

        VBox container = new VBox(16);
        container.setPadding(new Insets(24));
        container.setPrefWidth(540);
        container.setStyle("-fx-background-color: -color-bg-default;");

        // Documento imprimible del recibo
        VBox reciboCard = new VBox(14);
        reciboCard.setPadding(new Insets(22));
        reciboCard.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #cbd5e1; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 8px; " +
            "-fx-background-radius: 8px; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 8, 0, 0, 2);"
        );

        // Encabezado institucional
        VBox header = new VBox(4);
        header.setAlignment(Pos.CENTER);
        Label lblOrg = new Label("SISTEMA DE ASOCIACIÓN COMUNAL (SAMA)");
        lblOrg.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748b; -fx-letter-spacing: 1px;");
        Label lblTitulo = new Label("RECIBO OFICIAL DE APORTACIÓN");
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");
        Label lblReciboNum = new Label("N° REC-" + String.format("%06d", aportacion.getIdAportacion()));
        lblReciboNum.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2563eb;");
        header.getChildren().addAll(lblOrg, lblTitulo, lblReciboNum);

        Separator sep1 = new Separator();

        // Datos del aporte
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(8);

        addFila(grid, 0, "Miembro aportante:", aportacion.getNombreMiembro());
        addFila(grid, 1, "DUI del miembro:", aportacion.getDuiMiembro());
        addFila(grid, 2, "Período mensual:", aportacion.getPeriodoMes());
        addFila(grid, 3, "Proyecto comunal:", aportacion.getProyectoDisplay());
        addFila(grid, 4, "Fecha de pago:", aportacion.getFechaPago());
        addFila(grid, 5, "Método de pago:", aportacion.getMetodoPago());
        addFila(grid, 6, "Comprobante / Ref:", aportacion.getReferenciaDisplay());
        addFila(grid, 7, "Estado:", aportacion.getEstado());

        Separator sep2 = new Separator();

        // Total destacado
        HBox boxTotal = new HBox(12);
        boxTotal.setAlignment(Pos.CENTER_RIGHT);
        boxTotal.setPadding(new Insets(6, 10, 6, 10));
        boxTotal.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 6px;");

        Label lblMontoTitulo = new Label("MONTO RECIBIDO:");
        lblMontoTitulo.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #334155;");
        Label lblMontoValor = new Label(aportacion.getMontoFormateado() + " USD");
        lblMontoValor.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: #16a34a;");
        boxTotal.getChildren().addAll(lblMontoTitulo, lblMontoValor);

        // Pie de firma
        HBox boxFirmas = new HBox(30);
        boxFirmas.setAlignment(Pos.CENTER);
        boxFirmas.setPadding(new Insets(16, 0, 4, 0));

        VBox firmaTesoreria = new VBox(4);
        firmaTesoreria.setAlignment(Pos.CENTER);
        Label lineaFirma = new Label("____________________________________");
        lineaFirma.setStyle("-fx-text-fill: #94a3b8;");
        Label lblFirma = new Label("Firma y sello de Tesorería Comunal");
        lblFirma.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");
        firmaTesoreria.getChildren().addAll(lineaFirma, lblFirma);
        boxFirmas.getChildren().add(firmaTesoreria);

        reciboCard.getChildren().addAll(header, sep1, grid, sep2, boxTotal, boxFirmas);

        // Barra inferior de botones
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnImprimir = new Button("Imprimir comprobante");
        btnImprimir.getStyleClass().add("primary-action");
        btnImprimir.setGraphic(new FontIcon("fth-printer"));
        btnImprimir.setOnAction(e -> {
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job != null && job.showPrintDialog(dialog)) {
                boolean success = job.printPage(reciboCard);
                if (success) {
                    job.endJob();
                    new Alert(Alert.AlertType.INFORMATION, "Comprobante impreso con éxito.", ButtonType.OK).showAndWait();
                } else {
                    new Alert(Alert.AlertType.ERROR, "No se pudo imprimir el comprobante.", ButtonType.OK).showAndWait();
                }
            } else {
                new Alert(Alert.AlertType.INFORMATION, "La vista del comprobante oficial está lista para impresión.", ButtonType.OK).showAndWait();
            }
        });

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.getStyleClass().add("secondary-action");
        btnCerrar.setOnAction(e -> dialog.close());

        actions.getChildren().addAll(btnImprimir, btnCerrar);

        container.getChildren().addAll(reciboCard, actions);

        Scene scene = new Scene(container);
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();
    }

    private static void addFila(GridPane grid, int row, String label, String value) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        Label val = new Label(value != null ? value : "-");
        val.setStyle("-fx-font-size: 12px; -fx-text-fill: #0f172a;");
        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
    }
}
