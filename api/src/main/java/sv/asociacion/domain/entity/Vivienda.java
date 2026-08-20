package sv.asociacion.domain.entity;

import java.time.LocalDate;

public class Vivienda {
    private Integer idVivienda;
    private String codigo;
    private String sector;
    private String direccion;
    private String referencia;
    private Integer idRepresentante;
    private String representante;
    private LocalDate fechaRegistro;
    private String estado;
    private int adultos;
    private int menores;

    public Vivienda() { }

    public Vivienda(Integer idVivienda, String codigo, String sector, String direccion,
                    String referencia, Integer idRepresentante, String representante,
                    LocalDate fechaRegistro, String estado, int adultos, int menores) {
        this.idVivienda = idVivienda; this.codigo = codigo; this.sector = sector;
        this.direccion = direccion; this.referencia = referencia;
        this.idRepresentante = idRepresentante; this.representante = representante;
        this.fechaRegistro = fechaRegistro; this.estado = estado;
        this.adultos = adultos; this.menores = menores;
    }

    public Integer getIdVivienda() { return idVivienda; }
    public void setIdVivienda(Integer value) { idVivienda = value; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String value) { codigo = value; }
    public String getSector() { return sector; }
    public void setSector(String value) { sector = value; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String value) { direccion = value; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String value) { referencia = value; }
    public Integer getIdRepresentante() { return idRepresentante; }
    public void setIdRepresentante(Integer value) { idRepresentante = value; }
    public String getRepresentante() { return representante; }
    public void setRepresentante(String value) { representante = value; }
    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDate value) { fechaRegistro = value; }
    public String getEstado() { return estado; }
    public void setEstado(String value) { estado = value; }
    public int getAdultos() { return adultos; }
    public void setAdultos(int value) { adultos = value; }
    public int getMenores() { return menores; }
    public void setMenores(int value) { menores = value; }
    public int getTotalResidentes() { return adultos + menores; }
}
