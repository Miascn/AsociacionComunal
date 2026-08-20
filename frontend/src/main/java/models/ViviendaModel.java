package models;

import java.time.LocalDate;

public class ViviendaModel {
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

    public ViviendaModel(Integer idVivienda, String codigo, String sector, String direccion,
                         String referencia, Integer idRepresentante, String representante,
                         LocalDate fechaRegistro, String estado, int adultos, int menores) {
        this.idVivienda=idVivienda; this.codigo=codigo; this.sector=sector; this.direccion=direccion;
        this.referencia=referencia; this.idRepresentante=idRepresentante; this.representante=representante;
        this.fechaRegistro=fechaRegistro; this.estado=estado; this.adultos=adultos; this.menores=menores;
    }
    public Integer getIdVivienda(){return idVivienda;}
    public String getCodigo(){return codigo;}
    public String getSector(){return sector;}
    public String getDireccion(){return direccion;}
    public String getReferencia(){return referencia;}
    public Integer getIdRepresentante(){return idRepresentante;}
    public String getRepresentante(){return representante;}
    public LocalDate getFechaRegistro(){return fechaRegistro;}
    public String getEstado(){return estado;}
    public void setEstado(String value){estado=value;}
    public int getAdultos(){return adultos;}
    public int getMenores(){return menores;}
    public int getTotalResidentes(){return adultos+menores;}
}
