package models;

public class VotoModel {
    private Long id;
    private Integer idVotacion;
    private Integer idOpcion;
    private Integer idMiembro;
    private String fechaHora;

    public VotoModel() {}

    public VotoModel(Long id, Integer idVotacion, Integer idOpcion,
                      Integer idMiembro, String fechaHora) {
        this.id = id;
        this.idVotacion = idVotacion;
        this.idOpcion = idOpcion;
        this.idMiembro = idMiembro;
        this.fechaHora = fechaHora;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getIdVotacion() { return idVotacion; }
    public void setIdVotacion(Integer idVotacion) { this.idVotacion = idVotacion; }

    public Integer getIdOpcion() { return idOpcion; }
    public void setIdOpcion(Integer idOpcion) { this.idOpcion = idOpcion; }

    public Integer getIdMiembro() { return idMiembro; }
    public void setIdMiembro(Integer idMiembro) { this.idMiembro = idMiembro; }

    public String getFechaHora() { return fechaHora; }
    public void setFechaHora(String fechaHora) { this.fechaHora = fechaHora; }
}