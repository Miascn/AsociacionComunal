package models;

import java.math.BigDecimal;

public class ProyectoModel {
    private Integer id;
    private Integer creadoPor;
    private String nombreCreador;
    private String nombre;
    private String descripcion;
    private BigDecimal presupuesto;
    private String fechaCreacion;
    private String estado;

    public ProyectoModel() {}

    public ProyectoModel(Integer id, Integer creadoPor, String nombreCreador, String nombre,
                         String descripcion, BigDecimal presupuesto, String fechaCreacion, String estado) {
        this.id = id;
        this.creadoPor = creadoPor;
        this.nombreCreador = nombreCreador;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.presupuesto = presupuesto;
        this.fechaCreacion = fechaCreacion;
        this.estado = estado;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getIdProyecto() { return id; }
    public void setIdProyecto(Integer id) { this.id = id; }

    public Integer getCreadoPor() { return creadoPor; }
    public void setCreadoPor(Integer creadoPor) { this.creadoPor = creadoPor; }

    public String getNombreCreador() { return nombreCreador; }
    public void setNombreCreador(String nombreCreador) { this.nombreCreador = nombreCreador; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPresupuesto() { return presupuesto; }
    public void setPresupuesto(BigDecimal presupuesto) { this.presupuesto = presupuesto; }

    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getPresupuestoFormateado() {
        return presupuesto != null ? String.format("$%.2f", presupuesto) : "$0.00";
    }

    public String getCreadorDisplay() {
        if (nombreCreador != null && !nombreCreador.isBlank()) {
            return nombreCreador;
        }
        return creadoPor != null ? "Usuario #" + creadoPor : "Sistema";
    }
}