package models;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MantenimientoMiembroModel {
    private Integer idMiembro;
    private String nombreCompleto;
    private String dui;
    private String telefono;
    private String direccion;
    private String periodoMes;
    private BigDecimal cuotaEsperada;
    private boolean pagado;
    private Long idAportacion;
    private BigDecimal montoPagado;
    private LocalDate fechaPago;
    private String metodoPago;
    private String referencia;
    private String estadoAportacion;

    public MantenimientoMiembroModel() {}

    public Integer getIdMiembro() { return idMiembro; }
    public void setIdMiembro(Integer idMiembro) { this.idMiembro = idMiembro; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getDui() { return dui; }
    public void setDui(String dui) { this.dui = dui; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getPeriodoMes() { return periodoMes; }
    public void setPeriodoMes(String periodoMes) { this.periodoMes = periodoMes; }

    public BigDecimal getCuotaEsperada() { return cuotaEsperada; }
    public void setCuotaEsperada(BigDecimal cuotaEsperada) { this.cuotaEsperada = cuotaEsperada; }

    public boolean isPagado() { return pagado; }
    public void setPagado(boolean pagado) { this.pagado = pagado; }

    public Long getIdAportacion() { return idAportacion; }
    public void setIdAportacion(Long idAportacion) { this.idAportacion = idAportacion; }

    public BigDecimal getMontoPagado() { return montoPagado; }
    public void setMontoPagado(BigDecimal montoPagado) { this.montoPagado = montoPagado; }

    public LocalDate getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDate fechaPago) { this.fechaPago = fechaPago; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public String getEstadoAportacion() { return estadoAportacion; }
    public void setEstadoAportacion(String estadoAportacion) { this.estadoAportacion = estadoAportacion; }

    public String getEstadoTexto() {
        return pagado ? "PAGADO" : "PENDIENTE";
    }
}
