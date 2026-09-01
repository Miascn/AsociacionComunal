package sv.asociacion.domain.entity;

public class Cargo {
    private Integer idCargo;
    private String nombre;
    private String descripcion;
    private Integer nivelJerarquico;
    private Boolean activo;

    public Cargo() {}

    public Cargo(Integer idCargo, String nombre, String descripcion) {
        this(idCargo, nombre, descripcion, 1, true);
    }

    public Cargo(Integer idCargo, String nombre, String descripcion, Integer nivelJerarquico, Boolean activo) {
        this.idCargo = idCargo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.nivelJerarquico = nivelJerarquico != null ? nivelJerarquico : 1;
        this.activo = activo != null ? activo : true;
    }

    public Integer getIdCargo() { return idCargo; }
    public void setIdCargo(Integer idCargo) { this.idCargo = idCargo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getNivelJerarquico() { return nivelJerarquico; }
    public void setNivelJerarquico(Integer nivelJerarquico) { this.nivelJerarquico = nivelJerarquico; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
