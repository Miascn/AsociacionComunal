package sv.asociacion.backend.entity;

import java.time.LocalDateTime;

public class Voto {
    private Long idVoto;
    private Integer idVotacion;
    private Integer idOpcion;
    private Integer idMiembro;
    private LocalDateTime fechaHora;

    public Voto() {}

    public Voto(Long idVoto, Integer idVotacion, Integer idOpcion,
                Integer idMiembro, LocalDateTime fechaHora) {
        this.idVoto = idVoto;
        this.idVotacion = idVotacion;
        this.idOpcion = idOpcion;
        this.idMiembro = idMiembro;
        this.fechaHora = fechaHora;
    }

    public Long getIdVoto() { return idVoto; }
    public void setIdVoto(Long idVoto) { this.idVoto = idVoto; }

    public Integer getIdVotacion() { return idVotacion; }
    public void setIdVotacion(Integer idVotacion) { this.idVotacion = idVotacion; }

    public Integer getIdOpcion() { return idOpcion; }
    public void setIdOpcion(Integer idOpcion) { this.idOpcion = idOpcion; }

    public Integer getIdMiembro() { return idMiembro; }
    public void setIdMiembro(Integer idMiembro) { this.idMiembro = idMiembro; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
}
