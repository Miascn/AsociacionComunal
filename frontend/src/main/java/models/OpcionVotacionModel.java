package models;

public class OpcionVotacionModel {
    private Integer idOpcion;
    private Integer idVotacion;
    private String tituloVotacion;
    private String estadoVotacion;
    private String descripcion;
    private short orden;
    private int votos;
    private boolean editable;

    public OpcionVotacionModel() {}

    public OpcionVotacionModel(Integer idOpcion, Integer idVotacion, String tituloVotacion,
                               String estadoVotacion, String descripcion, short orden,
                               int votos, boolean editable) {
        this.idOpcion = idOpcion;
        this.idVotacion = idVotacion;
        this.tituloVotacion = tituloVotacion;
        this.estadoVotacion = estadoVotacion;
        this.descripcion = descripcion;
        this.orden = orden;
        this.votos = votos;
        this.editable = editable;
    }

    public Integer getIdOpcion() { return idOpcion; }
    public void setIdOpcion(Integer idOpcion) { this.idOpcion = idOpcion; }

    public Integer getIdVotacion() { return idVotacion; }
    public void setIdVotacion(Integer idVotacion) { this.idVotacion = idVotacion; }

    public String getTituloVotacion() { return tituloVotacion; }
    public void setTituloVotacion(String tituloVotacion) { this.tituloVotacion = tituloVotacion; }

    public String getEstadoVotacion() { return estadoVotacion; }
    public void setEstadoVotacion(String estadoVotacion) { this.estadoVotacion = estadoVotacion; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public short getOrden() { return orden; }
    public void setOrden(short orden) { this.orden = orden; }

    public int getVotos() { return votos; }
    public void setVotos(int votos) { this.votos = votos; }

    public boolean isEditable() { return editable; }
    public void setEditable(boolean editable) { this.editable = editable; }
}