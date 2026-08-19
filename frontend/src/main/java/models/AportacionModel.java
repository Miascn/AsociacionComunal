package models;

import java.math.BigDecimal;

public class AportacionModel {
    private Long id;
    private Integer idMiembro;
    private String periodoMes;
    private BigDecimal monto;
    private String fechaPago;
    private String metodoPago;
    private String referencia;
    private String estado;

    public AportacionModel() {}

    public AportacionModel(Long id, Integer idMiembro, String periodoMes, BigDecimal monto,
                            String fechaPago, String metodoPago, String referencia, String estado) {
        this.id = id;
        this.idMiembro = idMiembro;
        this.periodoMes = periodoMes;
        this.monto = monto;
        this.fechaPago = fechaPago;
        this.metodoPago = metodoPago;
        this.referencia = referencia;
        this.estado = estado;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getIdMiembro() { return idMiembro; }
    public void setIdMiembro(Integer idMiembro) { this.idMiembro = idMiembro; }

    public String getPeriodoMes() { return periodoMes; }
    public void setPeriodoMes(String periodoMes) { this.periodoMes = periodoMes; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public String getFechaPago() { return fechaPago; }
    public void setFechaPago(String fechaPago) { this.fechaPago = fechaPago; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}