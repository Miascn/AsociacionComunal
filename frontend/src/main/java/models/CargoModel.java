package models;

public class CargoModel {
    private Integer id;
    private String nombre;
    private String descripcion;
    private Integer nivelJerarquico;
    private Boolean activo;
    private Integer totalAsignaciones;

    public CargoModel() {}

    public CargoModel(Integer id, String nombre, String descripcion) {
        this(id, nombre, descripcion, 1, true, 0);
    }

    public CargoModel(Integer id, String nombre, String descripcion, Integer nivelJerarquico, Boolean activo, Integer totalAsignaciones) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.nivelJerarquico = nivelJerarquico != null ? nivelJerarquico : 1;
        this.activo = activo != null ? activo : true;
        this.totalAsignaciones = totalAsignaciones != null ? totalAsignaciones : 0;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getIdCargo() { return id; }
    public void setIdCargo(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getNivelJerarquico() { return nivelJerarquico; }
    public void setNivelJerarquico(Integer nivelJerarquico) { this.nivelJerarquico = nivelJerarquico; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public boolean isActivo() { return Boolean.TRUE.equals(activo); }

    public Integer getTotalAsignaciones() { return totalAsignaciones; }
    public void setTotalAsignaciones(Integer totalAsignaciones) { this.totalAsignaciones = totalAsignaciones; }

    public String getNivelDisplay() {
        return "Nivel " + (nivelJerarquico != null ? nivelJerarquico : 1);
    }

    public String getEstadoDisplay() {
        return isActivo() ? "Activo" : "Inactivo";
    }
}