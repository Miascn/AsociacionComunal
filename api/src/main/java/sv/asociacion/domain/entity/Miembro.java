package sv.asociacion.domain.entity;

import java.time.LocalDate;

public class Miembro {
    public enum Estado { ACTIVO, INACTIVO }

    private Integer idMiembro;
    private String dui;
    private String tipoDocumento;
    private String paisOrigen;
    private Integer idVivienda;
    private String nombres;
    private String apellidos;
    private String telefono;
    private String correo;
    private String direccion;
    private LocalDate fechaIngreso;
    private Estado estado;

    public Miembro() {}

    public Miembro(Integer idMiembro, String dui, String nombres, String apellidos,
                   String telefono, String correo, String direccion,
                   LocalDate fechaIngreso, Estado estado) {
        this.idMiembro = idMiembro;
        this.dui = dui;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.correo = correo;
        this.direccion = direccion;
        this.fechaIngreso = fechaIngreso;
        this.estado = estado;
    }

    public Miembro(Integer idMiembro, String dui, String tipoDocumento, String paisOrigen,
                   String nombres, String apellidos, String telefono, String correo,
                   String direccion, LocalDate fechaIngreso, Estado estado) {
        this(idMiembro, dui, nombres, apellidos, telefono, correo, direccion, fechaIngreso, estado);
        this.tipoDocumento = tipoDocumento;
        this.paisOrigen = paisOrigen;
    }

    public Miembro(Integer idMiembro, String dui, String tipoDocumento, String paisOrigen,
                   Integer idVivienda, String nombres, String apellidos, String telefono,
                   String correo, String direccion, LocalDate fechaIngreso, Estado estado) {
        this(idMiembro, dui, tipoDocumento, paisOrigen, nombres, apellidos, telefono, correo, direccion, fechaIngreso, estado);
        this.idVivienda = idVivienda;
    }

    public Integer getIdMiembro() { return idMiembro; }
    public void setIdMiembro(Integer idMiembro) { this.idMiembro = idMiembro; }

    public String getDui() { return dui; }
    public void setDui(String dui) { this.dui = dui; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getPaisOrigen() { return paisOrigen; }
    public void setPaisOrigen(String paisOrigen) { this.paisOrigen = paisOrigen; }

    public Integer getIdVivienda() { return idVivienda; }
    public void setIdVivienda(Integer idVivienda) { this.idVivienda = idVivienda; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public LocalDate getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(LocalDate fechaIngreso) { this.fechaIngreso = fechaIngreso; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
}
