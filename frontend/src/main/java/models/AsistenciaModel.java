package models;

import javafx.beans.property.*;

public class AsistenciaModel {
    private final LongProperty idAsistencia = new SimpleLongProperty();
    private final IntegerProperty idReunion = new SimpleIntegerProperty();
    private final IntegerProperty idMiembro = new SimpleIntegerProperty();
    private final StringProperty nombreMiembro = new SimpleStringProperty();
    private final StringProperty duiMiembro = new SimpleStringProperty();
    private final StringProperty telefonoMiembro = new SimpleStringProperty();
    private final BooleanProperty asistio = new SimpleBooleanProperty();
    private final StringProperty observacion = new SimpleStringProperty();

    public AsistenciaModel() {}

    public AsistenciaModel(Long idAsistencia, Integer idReunion, Integer idMiembro,
                           String nombreMiembro, String duiMiembro, String telefonoMiembro,
                           boolean asistio, String observacion) {
        this.idAsistencia.set(idAsistencia != null ? idAsistencia : 0L);
        this.idReunion.set(idReunion != null ? idReunion : 0);
        this.idMiembro.set(idMiembro != null ? idMiembro : 0);
        this.nombreMiembro.set(nombreMiembro != null ? nombreMiembro : "");
        this.duiMiembro.set(duiMiembro != null ? duiMiembro : "-");
        this.telefonoMiembro.set(telefonoMiembro != null ? telefonoMiembro : "-");
        this.asistio.set(asistio);
        this.observacion.set(observacion != null ? observacion : "");
    }

    public long getIdAsistencia() { return idAsistencia.get(); }
    public LongProperty idAsistenciaProperty() { return idAsistencia; }
    public void setIdAsistencia(long idAsistencia) { this.idAsistencia.set(idAsistencia); }

    public int getIdReunion() { return idReunion.get(); }
    public IntegerProperty idReunionProperty() { return idReunion; }
    public void setIdReunion(int idReunion) { this.idReunion.set(idReunion); }

    public int getIdMiembro() { return idMiembro.get(); }
    public IntegerProperty idMiembroProperty() { return idMiembro; }
    public void setIdMiembro(int idMiembro) { this.idMiembro.set(idMiembro); }

    public String getNombreMiembro() { return nombreMiembro.get(); }
    public StringProperty nombreMiembroProperty() { return nombreMiembro; }
    public void setNombreMiembro(String nombreMiembro) { this.nombreMiembro.set(nombreMiembro); }

    public String getDuiMiembro() { return duiMiembro.get(); }
    public StringProperty duiMiembroProperty() { return duiMiembro; }
    public void setDuiMiembro(String duiMiembro) { this.duiMiembro.set(duiMiembro); }

    public String getTelefonoMiembro() { return telefonoMiembro.get(); }
    public StringProperty telefonoMiembroProperty() { return telefonoMiembro; }
    public void setTelefonoMiembro(String telefonoMiembro) { this.telefonoMiembro.set(telefonoMiembro); }

    public boolean isAsistio() { return asistio.get(); }
    public BooleanProperty asistioProperty() { return asistio; }
    public void setAsistio(boolean asistio) { this.asistio.set(asistio); }

    public String getObservacion() { return observacion.get(); }
    public StringProperty observacionProperty() { return observacion; }
    public void setObservacion(String observacion) { this.observacion.set(observacion); }

    private final StringProperty tituloReunion = new SimpleStringProperty();
    private final StringProperty fechaHoraReunion = new SimpleStringProperty();
    private final StringProperty tipoReunion = new SimpleStringProperty();

    public String getTituloReunion() { return tituloReunion.get(); }
    public StringProperty tituloReunionProperty() { return tituloReunion; }
    public void setTituloReunion(String tituloReunion) { this.tituloReunion.set(tituloReunion != null ? tituloReunion : ""); }

    public String getFechaHoraReunion() { return fechaHoraReunion.get(); }
    public StringProperty fechaHoraReunionProperty() { return fechaHoraReunion; }
    public void setFechaHoraReunion(String fechaHoraReunion) { this.fechaHoraReunion.set(fechaHoraReunion != null ? fechaHoraReunion : ""); }

    public String getTipoReunion() { return tipoReunion.get(); }
    public StringProperty tipoReunionProperty() { return tipoReunion; }
    public void setTipoReunion(String tipoReunion) { this.tipoReunion.set(tipoReunion != null ? tipoReunion : ""); }
}