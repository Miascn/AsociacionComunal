package models;

public class MiembroModel {
    private Integer id;
    private String dui;
    private String tipoDocumento;
    private String paisOrigen;
    private Integer idVivienda;
    private String nombres;
    private String apellidos;
    private String telefono;
    private String correo;
    private String direccion;
    private String fechaIngreso;
    private String estado;

    public MiembroModel() { }

    public MiembroModel(Integer id, String dui, String tipoDocumento, String paisOrigen,
                        Integer idVivienda, String nombres, String apellidos, String telefono,
                        String correo, String direccion, String fechaIngreso, String estado) {
        this.id = id; this.dui = dui; this.tipoDocumento = tipoDocumento; this.paisOrigen = paisOrigen;
        this.idVivienda = idVivienda; this.nombres = nombres; this.apellidos = apellidos;
        this.telefono = telefono; this.correo = correo; this.direccion = direccion;
        this.fechaIngreso = fechaIngreso; this.estado = estado;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getIdMiembro() { return id; }
    public String getDui() { return dui; }
    public void setDui(String dui) { this.dui = dui; }
    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String value) { tipoDocumento = value; }
    public String getPaisOrigen() { return paisOrigen; }
    public void setPaisOrigen(String value) { paisOrigen = value; }
    public Integer getIdVivienda() { return idVivienda; }
    public void setIdVivienda(Integer value) { idVivienda = value; }
    public String getNombres() { return nombres; }
    public String getNombre() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public String getApellido() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(String fechaIngreso) { this.fechaIngreso = fechaIngreso; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getNombreCompleto() {
        return ((nombres != null ? nombres : "") + " " + (apellidos != null ? apellidos : "")).trim();
    }

    public String getDocumento() {
        return dui != null ? dui : "-";
    }
}
