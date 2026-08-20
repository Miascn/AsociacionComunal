package models;

public class PeriodoDirectivaModel {
    private Integer id;
    private String nombre;
    private String fechaInicio;
    private String fechaFin;
    private String estado;

    public PeriodoDirectivaModel() {}

    public PeriodoDirectivaModel(Integer id, String nombre, String fechaInicio,
                                  String fechaFin, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }

    public String getFechaFin() { return fechaFin; }
    public void setFechaFin(String fechaFin) { this.fechaFin = fechaFin; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}