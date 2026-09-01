package sv.asociacion.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Aportacion {
    public enum MetodoPago { EFECTIVO, TRANSFERENCIA, OTRO }
    public enum Estado { REGISTRADA, ANULADA }

    private Long idAportacion;
    private Integer idMiembro;
    private Integer idProyecto;
    private String periodoMes;
    private BigDecimal monto;
    private LocalDate fechaPago;
    private MetodoPago metodoPago;
    private String referencia;
    private Estado estado;

    public Aportacion() {}

    public Aportacion(Long idAportacion, Integer idMiembro, String periodoMes,
                      BigDecimal monto, LocalDate fechaPago, MetodoPago metodoPago,
                      String referencia, Estado estado) {
        this(idAportacion, idMiembro, null, periodoMes, monto, fechaPago, metodoPago, referencia, estado);
    }

    public Aportacion(Long idAportacion, Integer idMiembro, Integer idProyecto, String periodoMes,
                      BigDecimal monto, LocalDate fechaPago, MetodoPago metodoPago,
                      String referencia, Estado estado) {
        this.idAportacion = idAportacion;
        this.idMiembro = idMiembro;
        this.idProyecto = idProyecto;
        this.periodoMes = periodoMes;
        this.monto = monto;
        this.fechaPago = fechaPago;
        this.metodoPago = metodoPago;
        this.referencia = referencia;
        this.estado = estado;
    }

    public Long getIdAportacion() { return idAportacion; }
    public void setIdAportacion(Long idAportacion) { this.idAportacion = idAportacion; }

    public Integer getIdMiembro() { return idMiembro; }
    public void setIdMiembro(Integer idMiembro) { this.idMiembro = idMiembro; }

    public Integer getIdProyecto() { return idProyecto; }
    public void setIdProyecto(Integer idProyecto) { this.idProyecto = idProyecto; }

    public String getPeriodoMes() { return periodoMes; }
    public void setPeriodoMes(String periodoMes) { this.periodoMes = periodoMes; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public LocalDate getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDate fechaPago) { this.fechaPago = fechaPago; }

    public MetodoPago getMetodoPago() { return metodoPago; }
    public void setMetodoPago(MetodoPago metodoPago) { this.metodoPago = metodoPago; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
}
