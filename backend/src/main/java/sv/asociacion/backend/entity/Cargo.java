package sv.asociacion.backend.entity;

public class Cargo {
    private Integer idCargo;
    private String nombre;
    private String descripcion;

    public Cargo() {}

    public Cargo(Integer idCargo, String nombre, String descripcion) {
        this.idCargo = idCargo;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public Integer getIdCargo() { return idCargo; }
    public void setIdCargo(Integer idCargo) { this.idCargo = idCargo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
