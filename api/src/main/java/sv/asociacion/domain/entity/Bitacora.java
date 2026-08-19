package sv.asociacion.domain.entity;

import java.time.LocalDateTime;

public class Bitacora {
    private Long idBitacora;
    private Integer idUsuario;
    private String accion;
    private String entidad;
    private String idRegistro;
    private LocalDateTime fechaHora;
    private String detalle;

    public Bitacora() {}

    public Bitacora(Long idBitacora, Integer idUsuario, String accion,
                    String entidad, String idRegistro, LocalDateTime fechaHora,
                    String detalle) {
        this.idBitacora = idBitacora;
        this.idUsuario = idUsuario;
        this.accion = accion;
        this.entidad = entidad;
        this.idRegistro = idRegistro;
        this.fechaHora = fechaHora;
        this.detalle = detalle;
    }

    public Long getIdBitacora() { return idBitacora; }
    public void setIdBitacora(Long idBitacora) { this.idBitacora = idBitacora; }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }

    public String getIdRegistro() { return idRegistro; }
    public void setIdRegistro(String idRegistro) { this.idRegistro = idRegistro; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
}
