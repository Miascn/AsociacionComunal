package service;

import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Sistema de paginación universal para TableView en JavaFX.
 * Permite controlar el tamaño de filas por página (5, 10, 25, 50, Todas),
 * navegar entre páginas y ajustar dinámicamente la altura de la tabla sin barras
 * de desplazamiento internas innecesarias.
 *
 * @param <T> Tipo de datos de la fila
 */
public class TablePaginator<T> {
    private final TableView<T> tableView;
    private ObservableList<T> sourceList;
    private final ObservableList<T> pageData = FXCollections.observableArrayList();

    private int pageSize = 10;
    private int currentPage = 1;
    private int totalPages = 1;
    private String entityName = "registros";

    private boolean autoAdjustHeight = true;
    private double rowHeight = 50.0;
    private double headerHeight = 47.0;

    // Componentes de la interfaz
    private final HBox controlBox = new HBox(8);
    private final Label lblPageSize = new Label("Filas:");
    private final ComboBox<String> comboPageSize = new ComboBox<>();
    private final Label lblInfo = new Label();
    private final Button btnAnterior = new Button("←");
    private final Button btnSiguiente = new Button("→");

    private Consumer<Integer> onPageChanged;

    private final ListChangeListener<T> listChangeListener = change -> {
        Platform.runLater(this::updatePagination);
    };

    public TablePaginator(TableView<T> tableView, ObservableList<T> sourceList) {
        this(tableView, sourceList, "registros", 10);
    }

    public TablePaginator(TableView<T> tableView, ObservableList<T> sourceList, String entityName) {
        this(tableView, sourceList, entityName, 10);
    }

    public TablePaginator(TableView<T> tableView, ObservableList<T> sourceList, String entityName, int initialPageSize) {
        this.tableView = tableView;
        this.entityName = (entityName != null && !entityName.isBlank()) ? entityName : "registros";
        this.pageSize = initialPageSize > 0 ? initialPageSize : 10;

        if (this.tableView != null) {
            this.tableView.setFixedCellSize(rowHeight);
            this.tableView.setItems(pageData);
        }

        buildUI();
        setSourceList(sourceList);
    }

    private void buildUI() {
        controlBox.setAlignment(Pos.CENTER_RIGHT);
        controlBox.getStyleClass().add("pagination-box");

        lblPageSize.getStyleClass().addAll("footer-summary-text", "pagination-label");
        lblInfo.getStyleClass().addAll("footer-summary-text", "pagination-info");

        comboPageSize.getItems().setAll("5", "10", "25", "50", "Todas");
        String initialStr = (pageSize == Integer.MAX_VALUE) ? "Todas" : String.valueOf(pageSize);
        if (comboPageSize.getItems().contains(initialStr)) {
            comboPageSize.setValue(initialStr);
        } else {
            comboPageSize.setValue("10");
        }

        comboPageSize.getStyleClass().add("combo-pagination");
        comboPageSize.setPrefWidth(85);
        comboPageSize.setMinWidth(75);
        comboPageSize.setMaxWidth(95);

        comboPageSize.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            if ("Todas".equalsIgnoreCase(newVal.trim())) {
                pageSize = Integer.MAX_VALUE;
            } else {
                try {
                    pageSize = Integer.parseInt(newVal.trim());
                } catch (NumberFormatException ignored) {
                    pageSize = 10;
                }
            }
            currentPage = 1;
            updatePagination();
            if (onPageChanged != null) {
                onPageChanged.accept(currentPage);
            }
        });

        btnAnterior.getStyleClass().add("btn-pagination");
        btnAnterior.setTooltip(new Tooltip("Página anterior"));
        btnAnterior.setOnAction(e -> previousPage());

        btnSiguiente.getStyleClass().add("btn-pagination");
        btnSiguiente.setTooltip(new Tooltip("Página siguiente"));
        btnSiguiente.setOnAction(e -> nextPage());

        controlBox.getChildren().addAll(lblPageSize, comboPageSize, lblInfo, btnAnterior, btnSiguiente);
    }

    public void setSourceList(ObservableList<T> newSource) {
        if (this.sourceList != null) {
            this.sourceList.removeListener(this.listChangeListener);
        }
        this.sourceList = newSource;
        if (this.sourceList != null) {
            this.sourceList.addListener(this.listChangeListener);
        }
        currentPage = 1;
        updatePagination();
    }

    public void updatePagination() {
        int total = (sourceList != null) ? sourceList.size() : 0;
        if (pageSize <= 0 || pageSize == Integer.MAX_VALUE) {
            totalPages = 1;
            currentPage = 1;
        } else {
            totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        }

        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        if (currentPage < 1) {
            currentPage = 1;
        }

        if (total == 0 || sourceList == null) {
            pageData.clear();
            lblInfo.setText("0 " + entityName);
            btnAnterior.setDisable(true);
            btnSiguiente.setDisable(true);
        } else {
            int fromIndex;
            int toIndex;
            if (pageSize <= 0 || pageSize == Integer.MAX_VALUE) {
                fromIndex = 0;
                toIndex = total;
            } else {
                fromIndex = (currentPage - 1) * pageSize;
                toIndex = Math.min(fromIndex + pageSize, total);
            }

            List<T> subList = sourceList.subList(fromIndex, toIndex);
            pageData.setAll(subList);

            if (pageSize >= total || pageSize == Integer.MAX_VALUE) {
                lblInfo.setText(String.format("Mostrando %d de %d %s", total, total, entityName));
            } else {
                lblInfo.setText(String.format("%d–%d de %d  •  Pág. %d de %d",
                    fromIndex + 1, toIndex, total, currentPage, totalPages));
            }

            btnAnterior.setDisable(currentPage <= 1);
            btnSiguiente.setDisable(currentPage >= totalPages);
        }

        if (autoAdjustHeight && tableView != null) {
            adjustTableHeight();
        }
    }

    public void adjustTableHeight() {
        if (tableView == null) return;
        int count = pageData.size();
        double height;
        if (count == 0) {
            height = headerHeight + 85.0;
        } else {
            height = Math.max(130.0, headerHeight + (count * rowHeight) + 3.0);
        }
        tableView.setPrefHeight(height);
        tableView.setMinHeight(height);
        tableView.setMaxHeight(height);
    }

    public void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            updatePagination();
            if (onPageChanged != null) {
                onPageChanged.accept(currentPage);
            }
        }
    }

    public void nextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            updatePagination();
            if (onPageChanged != null) {
                onPageChanged.accept(currentPage);
            }
        }
    }

    /**
     * Inyecta los controles de paginación automáticamente en una barra de pie HBox existente.
     */
    public void attachTo(HBox footerBar) {
        if (footerBar == null) return;
        if (footerBar.getChildren().contains(controlBox)) return;

        int insertIndex = -1;
        for (int i = 0; i < footerBar.getChildren().size(); i++) {
            Node child = footerBar.getChildren().get(i);
            if (child instanceof Region && HBox.getHgrow(child) == Priority.ALWAYS) {
                insertIndex = i + 1;
                break;
            }
        }

        if (insertIndex >= 0) {
            footerBar.getChildren().add(insertIndex, controlBox);
        } else {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            footerBar.getChildren().addAll(spacer, controlBox);
        }
    }

    // Getters y Setters
    public HBox getControlBox() { return controlBox; }
    public ObservableList<T> getPageData() { return pageData; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
        String val = (pageSize == Integer.MAX_VALUE) ? "Todas" : String.valueOf(pageSize);
        comboPageSize.setValue(val);
    }
    public int getCurrentPage() { return currentPage; }
    public int getTotalPages() { return totalPages; }
    public int getTotalItems() { return sourceList != null ? sourceList.size() : 0; }
    public ComboBox<String> getComboPageSize() { return comboPageSize; }
    public Label getLblInfo() { return lblInfo; }
    public Button getBtnAnterior() { return btnAnterior; }
    public Button getBtnSiguiente() { return btnSiguiente; }
    public void setAutoAdjustHeight(boolean autoAdjustHeight) { this.autoAdjustHeight = autoAdjustHeight; }
    public void setRowHeight(double rowHeight) { this.rowHeight = rowHeight; }
    public void setOnPageChanged(Consumer<Integer> listener) { this.onPageChanged = listener; }
}
