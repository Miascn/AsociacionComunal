package sv.asociacion.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

public class Usuario {
    public enum Estado { ACTIVO, BLOQUEADO, INACTIVO }

    private Integer idUsuario;
    private Integer idRol;
    private Integer idMiembro;
    private String nombreUsuario;
    private String claveHash;
    private Estado estado;
    private Boolean requiereCambioClave;
    private String claveTemporal;
    private LocalDateTime ultimoAcceso;

    public Usuario() {}

    public Usuario(Integer idUsuario, Integer idRol, Integer idMiembro,
                   String nombreUsuario, String claveHash, Estado estado,
                   LocalDateTime ultimoAcceso) {
        this(idUsuario, idRol, idMiembro, nombreUsuario, claveHash, estado, false, null, ultimoAcceso);
    }

    public Usuario(Integer idUsuario, Integer idRol, Integer idMiembro,
                   String nombreUsuario, String claveHash, Estado estado,
                   Boolean requiereCambioClave, String claveTemporal,
                   LocalDateTime ultimoAcceso) {
        this.idUsuario = idUsuario;
        this.idRol = idRol;
        this.idMiembro = idMiembro;
        this.nombreUsuario = nombreUsuario;
        this.claveHash = claveHash;
        this.estado = estado;
        this.requiereCambioClave = requiereCambioClave != null ? requiereCambioClave : false;
        this.claveTemporal = claveTemporal;
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

    @JsonIgnore
    public String getClaveHash() { return claveHash; }
    public void setClaveHash(String claveHash) { this.claveHash = claveHash; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public Boolean getRequiereCambioClave() { return requiereCambioClave; }
    public void setRequiereCambioClave(Boolean requiereCambioClave) { this.requiereCambioClave = requiereCambioClave; }

    public String getClaveTemporal() { return claveTemporal; }
    public void setClaveTemporal(String claveTemporal) { this.claveTemporal = claveTemporal; }

    public LocalDateTime getUltimoAcceso() { return ultimoAcceso; }
    public void setUltimoAcceso(LocalDateTime ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; }
}
