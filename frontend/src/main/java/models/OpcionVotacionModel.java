package models;

public class OpcionVotacionModel {
    private Integer id;
    private Integer idVotacion;
    private String descripcion;
    private Short orden;

    public OpcionVotacionModel() {}

    public OpcionVotacionModel(Integer id, Integer idVotacion, String descripcion, Short orden) {
        this.id = id;
        this.idVotacion = idVotacion;
        this.descripcion = descripcion;
        this.orden = orden;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getIdVotacion() { return idVotacion; }
    public void setIdVotacion(Integer idVotacion) { this.idVotacion = idVotacion; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Short getOrden() { return orden; }
    public void setOrden(Short orden) { this.orden = orden; }
}