package sv.asociacion.domain.entity;

import java.time.LocalDateTime;

public class Votacion {
    public enum Estado { PROGRAMADA, ABIERTA, CERRADA, CANCELADA }

    private Integer idVotacion;
    private Integer idProyecto;
    private String titulo;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Estado estado;

    public Votacion() {}

    public Votacion(Integer idVotacion, Integer idProyecto, String titulo,
                    LocalDateTime fechaInicio, LocalDateTime fechaFin, Estado estado) {
        this.idVotacion = idVotacion;
        this.idProyecto = idProyecto;
        this.titulo = titulo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
    }

    public Integer getIdVotacion() { return idVotacion; }
    public void setIdVotacion(Integer idVotacion) { this.idVotacion = idVotacion; }

    public Integer getIdProyecto() { return idProyecto; }
    public void setIdProyecto(Integer idProyecto) { this.idProyecto = idProyecto; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
}
