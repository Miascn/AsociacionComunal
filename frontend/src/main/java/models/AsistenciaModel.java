package models;

public class AsistenciaModel {
    private Long id;
    private Integer idReunion;
    private Integer idMiembro;
    private boolean asistio;
    private String observacion;

    public AsistenciaModel() {}

    public AsistenciaModel(Long id, Integer idReunion, Integer idMiembro,
                            boolean asistio, String observacion) {
        this.id = id;
        this.idReunion = idReunion;
        this.idMiembro = idMiembro;
        this.asistio = asistio;
        this.observacion = observacion;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getIdReunion() { return idReunion; }
    public void setIdReunion(Integer idReunion) { this.idReunion = idReunion; }

    public Integer getIdMiembro() { return idMiembro; }
    public void setIdMiembro(Integer idMiembro) { this.idMiembro = idMiembro; }

    public boolean isAsistio() { return asistio; }
    public void setAsistio(boolean asistio) { this.asistio = asistio; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}