package models;

public class VotacionModel {
    private Integer id;
    private Integer idProyecto;
    private String titulo;
    private String fechaInicio;
    private String fechaFin;
    private String estado;

    public VotacionModel() {}

    public VotacionModel(Integer id, Integer idProyecto, String titulo,
                          String fechaInicio, String fechaFin, String estado) {
        this.id = id;
        this.idProyecto = idProyecto;
        this.titulo = titulo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getIdProyecto() { return idProyecto; }
    public void setIdProyecto(Integer idProyecto) { this.idProyecto = idProyecto; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }

    public String getFechaFin() { return fechaFin; }
    public void setFechaFin(String fechaFin) { this.fechaFin = fechaFin; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}