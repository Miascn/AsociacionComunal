package models;

import java.math.BigDecimal;
import java.util.List;

public class AportacionModel {
    private Long idAportacion;
    private Integer idMiembro;
    private String nombreMiembro;
    private String duiMiembro;
    private Integer idProyecto;
    private String nombreProyecto;
    private String periodoMes;
    private BigDecimal monto;
    private String fechaPago;
    private String metodoPago;
    private String referencia;
    private String estado;

    public AportacionModel() {}

    public AportacionModel(Long idAportacion, Integer idMiembro, String nombreMiembro, String duiMiembro,
                           Integer idProyecto, String nombreProyecto, String periodoMes,
                           BigDecimal monto, String fechaPago, String metodoPago,
                           String referencia, String estado) {
        this.idAportacion = idAportacion;
        this.idMiembro = idMiembro;
        this.nombreMiembro = nombreMiembro;
        this.duiMiembro = duiMiembro;
        this.idProyecto = idProyecto;
        this.nombreProyecto = nombreProyecto;
        this.periodoMes = periodoMes;
        this.monto = monto;
        this.fechaPago = fechaPago;
        this.metodoPago = metodoPago;
        this.referencia = referencia;
        this.estado = estado;
    }

    public Long getIdAportacion() { return idAportacion; }
    public void setIdAportacion(Long idAportacion) { this.idAportacion = idAportacion; }

    public Long getId() { return idAportacion; }
    public void setId(Long id) { this.idAportacion = id; }

    public Integer getIdMiembro() { return idMiembro; }
    public void setIdMiembro(Integer idMiembro) { this.idMiembro = idMiembro; }

    public String getNombreMiembro() { return nombreMiembro; }
    public void setNombreMiembro(String nombreMiembro) { this.nombreMiembro = nombreMiembro; }

    public String getDuiMiembro() { return duiMiembro; }
    public void setDuiMiembro(String duiMiembro) { this.duiMiembro = duiMiembro; }

    public Integer getIdProyecto() { return idProyecto; }
    public void setIdProyecto(Integer idProyecto) { this.idProyecto = idProyecto; }

    public String getNombreProyecto() { return nombreProyecto; }
    public void setNombreProyecto(String nombreProyecto) { this.nombreProyecto = nombreProyecto; }

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

    public String getProyectoDisplay() {
        if (nombreProyecto != null && !nombreProyecto.isBlank()) {
            return nombreProyecto;
        }
        return idProyecto != null ? "Proyecto #" + idProyecto : "General";
    }

    public String getReferenciaDisplay() {
        return referencia != null && !referencia.isBlank() ? referencia : "Sin comprobante";
    }

    public String getMontoFormateado() {
        return monto != null ? String.format("$%.2f", monto) : "$0.00";
    }

    public static class Page {
        private List<AportacionModel> items;
        private int total;
        private BigDecimal totalRecaudado;
        private int page;
        private int size;
        private int totalPages;

        public Page() {}

        public List<AportacionModel> getItems() { return items; }
        public void setItems(List<AportacionModel> items) { this.items = items; }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public BigDecimal getTotalRecaudado() { return totalRecaudado; }
        public void setTotalRecaudado(BigDecimal totalRecaudado) { this.totalRecaudado = totalRecaudado; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    }
}