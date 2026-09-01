package models;

public record RolModel(Integer idRol, String nombre, String descripcion, Integer usuariosAsociados) {
    public RolModel(Integer idRol, String nombre, String descripcion) {
        this(idRol, nombre, descripcion, 0);
    }

    public int getUsuariosAsociadosCount() {
        return usuariosAsociados == null ? 0 : usuariosAsociados;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
