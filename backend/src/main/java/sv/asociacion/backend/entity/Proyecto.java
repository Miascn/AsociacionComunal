package sv.asociacion.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Proyecto {
    public enum Estado { BORRADOR, PROPUESTO, APROBADO, RECHAZADO, EN_EJECUCION, FINALIZADO }

    private Integer idProyecto;
    private Integer creadoPor;
    private String nombre;
    private String descripcion;
    private BigDecimal presupuesto;
    private LocalDate fechaCreacion;
    private Estado estado;

    public Proyecto() {}

    public Proyecto(Integer idProyecto, Integer creadoPor, String nombre,
                    String descripcion, BigDecimal presupuesto, LocalDate fechaCreacion,
                    Estado estado) {
        this.idProyecto = idProyecto;
        this.creadoPor = creadoPor;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.presupuesto = presupuesto;
        this.fechaCreacion = fechaCreacion;
        this.estado = estado;
    }

    public Integer getIdProyecto() { return idProyecto; }
    public void setIdProyecto(Integer idProyecto) { this.idProyecto = idProyecto; }

    public Integer getCreadoPor() { return creadoPor; }
    public void setCreadoPor(Integer creadoPor) { this.creadoPor = creadoPor; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPresupuesto() { return presupuesto; }
    public void setPresupuesto(BigDecimal presupuesto) { this.presupuesto = presupuesto; }

    public LocalDate getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDate fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
}
