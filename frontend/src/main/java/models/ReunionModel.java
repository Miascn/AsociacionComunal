package models;

import javafx.beans.property.*;

public class ReunionModel {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty titulo = new SimpleStringProperty();
    private final StringProperty fechaHora = new SimpleStringProperty();
    private final StringProperty lugar = new SimpleStringProperty();
    private final StringProperty tipo = new SimpleStringProperty();
    private final StringProperty estado = new SimpleStringProperty();
    private final IntegerProperty totalConvocados = new SimpleIntegerProperty();
    private final IntegerProperty totalAsistentes = new SimpleIntegerProperty();
    private final DoubleProperty porcentajeAsistencia = new SimpleDoubleProperty();
    private final BooleanProperty editable = new SimpleBooleanProperty();

    public ReunionModel() {}

    public ReunionModel(Integer id, String titulo, String fechaHora, String lugar,
                        String tipo, String estado, int totalConvocados, int totalAsistentes,
                        double porcentajeAsistencia, boolean editable) {
        this.id.set(id != null ? id : 0);
        this.titulo.set(titulo != null ? titulo : "");
        this.fechaHora.set(fechaHora != null ? fechaHora : "");
        this.lugar.set(lugar != null ? lugar : "");
        this.tipo.set(tipo != null ? tipo : "ORDINARIA");
        this.estado.set(estado != null ? estado : "PROGRAMADA");
        this.totalConvocados.set(totalConvocados);
        this.totalAsistentes.set(totalAsistentes);
        this.porcentajeAsistencia.set(porcentajeAsistencia);
        this.editable.set(editable);
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }
    public void setId(int id) { this.id.set(id); }

    public String getTitulo() { return titulo.get(); }
    public StringProperty tituloProperty() { return titulo; }
    public void setTitulo(String titulo) { this.titulo.set(titulo); }

    public String getFechaHora() { return fechaHora.get(); }
    public StringProperty fechaHoraProperty() { return fechaHora; }
    public void setFechaHora(String fechaHora) { this.fechaHora.set(fechaHora); }

    public String getLugar() { return lugar.get(); }
    public StringProperty lugarProperty() { return lugar; }
    public void setLugar(String lugar) { this.lugar.set(lugar); }

    public String getTipo() { return tipo.get(); }
    public StringProperty tipoProperty() { return tipo; }
    public void setTipo(String tipo) { this.tipo.set(tipo); }

    public String getEstado() { return estado.get(); }
    public StringProperty estadoProperty() { return estado; }
    public void setEstado(String estado) { this.estado.set(estado); }

    public int getTotalConvocados() { return totalConvocados.get(); }
    public IntegerProperty totalConvocadosProperty() { return totalConvocados; }

    public int getTotalAsistentes() { return totalAsistentes.get(); }
    public IntegerProperty totalAsistentesProperty() { return totalAsistentes; }

    public double getPorcentajeAsistencia() { return porcentajeAsistencia.get(); }
    public DoubleProperty porcentajeAsistenciaProperty() { return porcentajeAsistencia; }

    public boolean isEditable() { return editable.get(); }
    public BooleanProperty editableProperty() { return editable; }

    public boolean isProgramada() { return "PROGRAMADA".equalsIgnoreCase(getEstado()); }
    public boolean isRealizada() { return "REALIZADA".equalsIgnoreCase(getEstado()); }
    public boolean isCancelada() { return "CANCELADA".equalsIgnoreCase(getEstado()); }

    public String getFechaDisplay() {
        String fh = getFechaHora();
        if (fh == null || fh.isBlank()) return "-";
        return fh.replace("T", " ");
    }

    public String getQuorumDisplay() {
        if (isProgramada()) {
            return totalConvocados.get() > 0 ? totalConvocados.get() + " convocados" : "Sin convocatoria";
        }
        if (totalConvocados.get() == 0) return "0 asistencias";
        return totalAsistentes.get() + " / " + totalConvocados.get() + " (" + porcentajeAsistencia.get() + "%)";
    }

    public String getLugarDisplay() {
        String l = getLugar();
        return (l == null || l.isBlank()) ? "Lugar por definir" : l;
    }
}