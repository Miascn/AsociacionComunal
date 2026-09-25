package sv.asociacion.domain.entity;

import java.time.LocalDateTime;

public class Reunion {
    public enum Tipo { ORDINARIA, EXTRAORDINARIA }
    public enum Estado { PROGRAMADA, REALIZADA, CANCELADA }

    private Integer idReunion;
    private Integer idProyecto;
    private String titulo;
    private String descripcion;
    private LocalDateTime fechaHora;
    private String lugar;
    private Tipo tipo;
    private Estado estado;

    public Reunion() {}

    public Reunion(Integer idReunion, String titulo, LocalDateTime fechaHora,
                   String lugar, Tipo tipo, Estado estado) {
        this(idReunion, null, titulo, null, fechaHora, lugar, tipo, estado);
    }

    public Reunion(Integer idReunion, Integer idProyecto, String titulo, String descripcion,
                   LocalDateTime fechaHora, String lugar, Tipo tipo, Estado estado) {
        this.idReunion = idReunion;
        this.idProyecto = idProyecto;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fechaHora = fechaHora;
        this.lugar = lugar;
        this.tipo = tipo;
        this.estado = estado;
    }

    public Integer getIdReunion() { return idReunion; }
    public void setIdReunion(Integer idReunion) { this.idReunion = idReunion; }

    public Integer getIdProyecto() { return idProyecto; }
    public void setIdProyecto(Integer idProyecto) { this.idProyecto = idProyecto; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public String getLugar() { return lugar; }
    public void setLugar(String lugar) { this.lugar = lugar; }

    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
}
