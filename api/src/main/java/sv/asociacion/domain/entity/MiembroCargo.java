package sv.asociacion.domain.entity;

import java.time.LocalDate;

public class MiembroCargo {
    public enum Estado { ACTIVO, REVOCADO, FINALIZADO }

    private Integer idMiembroCargo;
    private Integer idMiembro;
    private Integer idCargo;
    private Integer idPeriodo;
    private LocalDate fechaAsignacion;
    private LocalDate fechaFin;
    private String motivoSalida;
    private Estado estado = Estado.ACTIVO;

    public MiembroCargo() {}

    public MiembroCargo(Integer idMiembroCargo, Integer idMiembro, Integer idCargo,
                        Integer idPeriodo, LocalDate fechaAsignacion) {
        this(idMiembroCargo, idMiembro, idCargo, idPeriodo, fechaAsignacion, null, null, Estado.ACTIVO);
    }

    public MiembroCargo(Integer idMiembroCargo, Integer idMiembro, Integer idCargo,
                        Integer idPeriodo, LocalDate fechaAsignacion, LocalDate fechaFin,
                        String motivoSalida, Estado estado) {
        this.idMiembroCargo = idMiembroCargo;
        this.idMiembro = idMiembro;
        this.idCargo = idCargo;
        this.idPeriodo = idPeriodo;
        this.fechaAsignacion = fechaAsignacion;
        this.fechaFin = fechaFin;
        this.motivoSalida = motivoSalida;
        this.estado = estado != null ? estado : Estado.ACTIVO;
    }

    public Integer getIdMiembroCargo() { return idMiembroCargo; }
    public void setIdMiembroCargo(Integer idMiembroCargo) { this.idMiembroCargo = idMiembroCargo; }

    public Integer getIdMiembro() { return idMiembro; }
    public void setIdMiembro(Integer idMiembro) { this.idMiembro = idMiembro; }

    public Integer getIdCargo() { return idCargo; }
    public void setIdCargo(Integer idCargo) { this.idCargo = idCargo; }

    public Integer getIdPeriodo() { return idPeriodo; }
    public void setIdPeriodo(Integer idPeriodo) { this.idPeriodo = idPeriodo; }

    public LocalDate getFechaAsignacion() { return fechaAsignacion; }
    public void setFechaAsignacion(LocalDate fechaAsignacion) { this.fechaAsignacion = fechaAsignacion; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public String getMotivoSalida() { return motivoSalida; }
    public void setMotivoSalida(String motivoSalida) { this.motivoSalida = motivoSalida; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
}
