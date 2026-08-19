package models;

public class MiembroCargoModel {
    private Integer id;
    private Integer idMiembro;
    private Integer idCargo;
    private Integer idPeriodo;
    private String fechaAsignacion;

    public MiembroCargoModel() {}

    public MiembroCargoModel(Integer id, Integer idMiembro, Integer idCargo,
                              Integer idPeriodo, String fechaAsignacion) {
        this.id = id;
        this.idMiembro = idMiembro;
        this.idCargo = idCargo;
        this.idPeriodo = idPeriodo;
        this.fechaAsignacion = fechaAsignacion;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getIdMiembro() { return idMiembro; }
    public void setIdMiembro(Integer idMiembro) { this.idMiembro = idMiembro; }

    public Integer getIdCargo() { return idCargo; }
    public void setIdCargo(Integer idCargo) { this.idCargo = idCargo; }

    public Integer getIdPeriodo() { return idPeriodo; }
    public void setIdPeriodo(Integer idPeriodo) { this.idPeriodo = idPeriodo; }

    public String getFechaAsignacion() { return fechaAsignacion; }
    public void setFechaAsignacion(String fechaAsignacion) { this.fechaAsignacion = fechaAsignacion; }
}