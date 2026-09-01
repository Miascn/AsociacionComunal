package models;

public class AsignacionCargoModel {
    private Integer id;
    private Integer idMiembro;
    private String nombreMiembro;
    private String duiMiembro;
    private String telefonoMiembro;
    private Integer idCargo;
    private String nombreCargo;
    private Integer nivelJerarquico;
    private Integer idPeriodo;
    private String nombrePeriodo;
    private String estadoPeriodo;
    private String fechaAsignacion;
    private String fechaFin;
    private String motivoSalida;
    private String estado;

    public AsignacionCargoModel() {}

    public AsignacionCargoModel(Integer id, Integer idMiembro, String nombreMiembro, String duiMiembro,
                                String telefonoMiembro, Integer idCargo, String nombreCargo,
                                Integer nivelJerarquico, Integer idPeriodo, String nombrePeriodo,
                                String estadoPeriodo, String fechaAsignacion, String fechaFin,
                                String motivoSalida, String estado) {
        this.id = id;
        this.idMiembro = idMiembro;
        this.nombreMiembro = nombreMiembro;
        this.duiMiembro = duiMiembro;
        this.telefonoMiembro = telefonoMiembro;
        this.idCargo = idCargo;
        this.nombreCargo = nombreCargo;
        this.nivelJerarquico = nivelJerarquico;
        this.idPeriodo = idPeriodo;
        this.nombrePeriodo = nombrePeriodo;
        this.estadoPeriodo = estadoPeriodo;
        this.fechaAsignacion = fechaAsignacion;
        this.fechaFin = fechaFin;
        this.motivoSalida = motivoSalida;
        this.estado = estado;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getIdMiembro() { return idMiembro; }
    public void setIdMiembro(Integer idMiembro) { this.idMiembro = idMiembro; }

    public String getNombreMiembro() { return nombreMiembro; }
    public void setNombreMiembro(String nombreMiembro) { this.nombreMiembro = nombreMiembro; }

    public String getDuiMiembro() { return duiMiembro; }
    public void setDuiMiembro(String duiMiembro) { this.duiMiembro = duiMiembro; }

    public String getTelefonoMiembro() { return telefonoMiembro; }
    public void setTelefonoMiembro(String telefonoMiembro) { this.telefonoMiembro = telefonoMiembro; }

    public Integer getIdCargo() { return idCargo; }
    public void setIdCargo(Integer idCargo) { this.idCargo = idCargo; }

    public String getNombreCargo() { return nombreCargo; }
    public void setNombreCargo(String nombreCargo) { this.nombreCargo = nombreCargo; }

    public Integer getNivelJerarquico() { return nivelJerarquico; }
    public void setNivelJerarquico(Integer nivelJerarquico) { this.nivelJerarquico = nivelJerarquico; }

    public String getJerarquiaDisplay() {
        return nivelJerarquico != null ? "Nivel " + nivelJerarquico : "Nivel 1";
    }

    public Integer getIdPeriodo() { return idPeriodo; }
    public void setIdPeriodo(Integer idPeriodo) { this.idPeriodo = idPeriodo; }

    public String getNombrePeriodo() { return nombrePeriodo; }
    public void setNombrePeriodo(String nombrePeriodo) { this.nombrePeriodo = nombrePeriodo; }

    public String getEstadoPeriodo() { return estadoPeriodo; }
    public void setEstadoPeriodo(String estadoPeriodo) { this.estadoPeriodo = estadoPeriodo; }

    public String getFechaAsignacion() { return fechaAsignacion; }
    public void setFechaAsignacion(String fechaAsignacion) { this.fechaAsignacion = fechaAsignacion; }

    public String getFechaFin() { return fechaFin; }
    public void setFechaFin(String fechaFin) { this.fechaFin = fechaFin; }

    public String getMotivoSalida() { return motivoSalida; }
    public void setMotivoSalida(String motivoSalida) { this.motivoSalida = motivoSalida; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public boolean isActivo() {
        return "ACTIVO".equalsIgnoreCase(estado);
    }

    public String getVigenciaDisplay() {
        if (fechaFin != null && !fechaFin.isBlank()) {
            return fechaAsignacion + "  →  " + fechaFin;
        }
        return "Desde " + (fechaAsignacion != null ? fechaAsignacion : "N/D") + " (En funciones)";
    }
}
