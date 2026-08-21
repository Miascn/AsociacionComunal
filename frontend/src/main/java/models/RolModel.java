package models;

public record RolModel(Integer idRol, String nombre, String descripcion) {
    @Override public String toString() { return nombre; }
}
