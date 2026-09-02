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

    public static String getDescripcionJerarquia(Integer nivel) {
        int n = (nivel != null && nivel > 0) ? nivel : 1;
        return switch (n) {
            case 1 -> "1 - Nivel Máximo (Presidencia)";
            case 2 -> "2 - Nivel Alto (Vicepresidencia)";
            case 3 -> "3 - Nivel Medio-Alto (Secretaría)";
            case 4 -> "4 - Nivel Medio (Tesorería)";
            case 5 -> "5 - Nivel Operativo (Vocalía / Síndico)";
            default -> n + " - Nivel de Apoyo";
        };
    }

    public static String getDescripcionCortaJerarquia(Integer nivel) {
        int n = (nivel != null && nivel > 0) ? nivel : 1;
        return switch (n) {
            case 1 -> "Nivel Máximo";
            case 2 -> "Nivel Alto";
            case 3 -> "Nivel Medio-Alto";
            case 4 -> "Nivel Medio";
            case 5 -> "Nivel Operativo";
            default -> "Nivel " + n;
        };
    }

    public String getNivelDisplay() {
        return getDescripcionJerarquia(nivelJerarquico);
    }

    public String getEstadoDisplay() {
        return isActivo() ? "Activo" : "Inactivo";
    }
}