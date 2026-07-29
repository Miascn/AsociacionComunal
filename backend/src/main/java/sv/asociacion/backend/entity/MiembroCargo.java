package sv.asociacion.backend.entity;

import java.time.LocalDate;

public class MiembroCargo {
    private Integer idMiembroCargo;
    private Integer idMiembro;
    private Integer idCargo;
    private Integer idPeriodo;
    private LocalDate fechaAsignacion;

    public MiembroCargo() {}

    public MiembroCargo(Integer idMiembroCargo, Integer idMiembro, Integer idCargo,
                        Integer idPeriodo, LocalDate fechaAsignacion) {
        this.idMiembroCargo = idMiembroCargo;
        this.idMiembro = idMiembro;
        this.idCargo = idCargo;
        this.idPeriodo = idPeriodo;
        this.fechaAsignacion = fechaAsignacion;
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
}
