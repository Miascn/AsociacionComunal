package sv.asociacion.backend.entity;

public class OpcionVotacion {
    private Integer idOpcion;
    private Integer idVotacion;
    private String descripcion;
    private Short orden;

    public OpcionVotacion() {}

    public OpcionVotacion(Integer idOpcion, Integer idVotacion, String descripcion, Short orden) {
        this.idOpcion = idOpcion;
        this.idVotacion = idVotacion;
        this.descripcion = descripcion;
        this.orden = orden;
    }

    public Integer getIdOpcion() { return idOpcion; }
    public void setIdOpcion(Integer idOpcion) { this.idOpcion = idOpcion; }

    public Integer getIdVotacion() { return idVotacion; }
    public void setIdVotacion(Integer idVotacion) { this.idVotacion = idVotacion; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Short getOrden() { return orden; }
    public void setOrden(Short orden) { this.orden = orden; }
}
