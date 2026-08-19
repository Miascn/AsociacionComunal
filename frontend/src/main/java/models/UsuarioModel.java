package models;

public class UsuarioModel {
    private Integer idUsuario;
    private Integer idRol;
    private Integer idMiembro;
    private String nombreUsuario;
    private String estado;
    private String ultimoAcceso;

    public UsuarioModel() {}

    public UsuarioModel(Integer idUsuario, Integer idRol, Integer idMiembro,
                        String nombreUsuario, String estado, String ultimoAcceso) {
        this.idUsuario = idUsuario;
        this.idRol = idRol;
        this.idMiembro = idMiembro;
        this.nombreUsuario = nombreUsuario;
        this.estado = estado;
        this.ultimoAcceso = ultimoAcceso;
    }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public Integer getIdRol() { return idRol; }
    public void setIdRol(Integer idRol) { this.idRol = idRol; }

    public Integer getIdMiembro() { return idMiembro; }
    public void setIdMiembro(Integer idMiembro) { this.idMiembro = idMiembro; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getUltimoAcceso() { return ultimoAcceso; }
    public void setUltimoAcceso(String ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; }
}