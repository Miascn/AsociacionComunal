package sv.asociacion.domain.entity;

import java.time.LocalDate;

public class PeriodoDirectiva {
    public enum Estado { PLANIFICADO, ACTIVO, FINALIZADO }

    private Integer idPeriodo;
    private String nombre;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Estado estado;

    public PeriodoDirectiva() {}

    public PeriodoDirectiva(Integer idPeriodo, String nombre, LocalDate fechaInicio,
                            LocalDate fechaFin, Estado estado) {
        this.idPeriodo = idPeriodo;
        this.nombre = nombre;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
    }

    public Integer getIdPeriodo() { return idPeriodo; }
    public void setIdPeriodo(Integer idPeriodo) { this.idPeriodo = idPeriodo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
}
