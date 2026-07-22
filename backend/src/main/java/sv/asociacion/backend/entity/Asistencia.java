package sv.asociacion.backend.entity;

public class Asistencia {
    private Long idAsistencia;
    private Integer idReunion;
    private Integer idMiembro;
    private boolean asistio;
    private String observacion;

    public Asistencia() {}

    public Asistencia(Long idAsistencia, Integer idReunion, Integer idMiembro,
                     boolean asistio, String observacion) {
        this.idAsistencia = idAsistencia;
        this.idReunion = idReunion;
        this.idMiembro = idMiembro;
        this.asistio = asistio;
        this.observacion = observacion;
    }

    public Long getIdAsistencia() { return idAsistencia; }
    public void setIdAsistencia(Long idAsistencia) { this.idAsistencia = idAsistencia; }

    public Integer getIdReunion() { return idReunion; }
    public void setIdReunion(Integer idReunion) { this.idReunion = idReunion; }

    public Integer getIdMiembro() { return idMiembro; }
    public void setIdMiembro(Integer idMiembro) { this.idMiembro = idMiembro; }

    public boolean isAsistio() { return asistio; }
    public void setAsistio(boolean asistio) { this.asistio = asistio; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
