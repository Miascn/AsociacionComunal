package models;

import java.util.ArrayList;
import java.util.List;

public class VotacionModel {
    private Integer id;
    private String titulo;
    private String descripcion;
    private Integer idProyecto;
    private String nombreProyecto;
    private String fechaInicio;
    private String fechaFin;
    private String estado;
    private int totalOpciones;
    private int totalVotos;
    private List<OpcionModel> opciones = new ArrayList<>();

    public static class OpcionModel {
        private Integer idOpcion;
        private String descripcion;
        private int orden;
        private int votos;
        private double porcentaje;

        public OpcionModel() {}

        public OpcionModel(Integer idOpcion, String descripcion, int orden, int votos, double porcentaje) {
            this.idOpcion = idOpcion;
            this.descripcion = descripcion;
            this.orden = orden;
            this.votos = votos;
            this.porcentaje = porcentaje;
        }

        public Integer getIdOpcion() { return idOpcion; }
        public void setIdOpcion(Integer idOpcion) { this.idOpcion = idOpcion; }

        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

        public int getOrden() { return orden; }
        public void setOrden(int orden) { this.orden = orden; }

        public int getVotos() { return votos; }
        public void setVotos(int votos) { this.votos = votos; }

        /** Cuota sobre el total, calculada por el backend (SCRUM-260). No se recalcula aquí. */
        public double getPorcentaje() { return porcentaje; }
        public void setPorcentaje(double porcentaje) { this.porcentaje = porcentaje; }

        /** Fracción 0..1 para la barra de progreso. */
        public double getFraccion() {
            return Math.max(0.0, Math.min(1.0, porcentaje / 100.0));
        }
    }

    public VotacionModel() {}

    public VotacionModel(Integer id, String titulo, String descripcion, Integer idProyecto,
                         String nombreProyecto, String fechaInicio, String fechaFin,
                         String estado, int totalOpciones, int totalVotos, List<OpcionModel> opciones) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.idProyecto = idProyecto;
        this.nombreProyecto = nombreProyecto;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
        this.totalOpciones = totalOpciones;
        this.totalVotos = totalVotos;
        this.opciones = opciones != null ? opciones : new ArrayList<>();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getIdProyecto() { return idProyecto; }
    public void setIdProyecto(Integer idProyecto) { this.idProyecto = idProyecto; }

    public String getNombreProyecto() { return nombreProyecto; }
    public void setNombreProyecto(String nombreProyecto) { this.nombreProyecto = nombreProyecto; }

    public String getProyectoDisplay() {
        return nombreProyecto != null && !nombreProyecto.isBlank() ? nombreProyecto : "Consulta General";
    }

    public String getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }

    public String getFechaFin() { return fechaFin; }
    public void setFechaFin(String fechaFin) { this.fechaFin = fechaFin; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public int getTotalOpciones() { return totalOpciones; }
    public void setTotalOpciones(int totalOpciones) { this.totalOpciones = totalOpciones; }

    public int getTotalVotos() { return totalVotos; }
    public void setTotalVotos(int totalVotos) { this.totalVotos = totalVotos; }

    public List<OpcionModel> getOpciones() { return opciones; }
    public void setOpciones(List<OpcionModel> opciones) { this.opciones = opciones; }

    public boolean isAbierta() {
        return "ABIERTA".equalsIgnoreCase(estado);
    }

    /** Solo BORRADOR. PROGRAMADA es un estado distinto: ver {@link #isProgramada()}. */
    public boolean isBorrador() {
        return "BORRADOR".equalsIgnoreCase(estado);
    }

    public boolean isProgramada() {
        return "PROGRAMADA".equalsIgnoreCase(estado);
    }

    public boolean isCerrada() {
        return "CERRADA".equalsIgnoreCase(estado);
    }

    public boolean isCancelada() {
        return "CANCELADA".equalsIgnoreCase(estado);
    }

    // ------------------------------------------------------------------
    // Reglas de negocio del backend, reflejadas para no ofrecer acciones que
    // el servidor va a rechazar. No son restricciones nuevas de la interfaz:
    // cada una espeja una guarda existente en VotacionService.
    // ------------------------------------------------------------------

    /** El cierre es el acto que publica los resultados (SCRUM-262). */
    public boolean isResultadosPublicados() {
        return isCerrada();
    }

    /** {@code VotacionService.update}: solo lo que no ha iniciado. */
    public boolean isEditable() {
        return isBorrador() || isProgramada();
    }

    /** {@code VotacionService.delete}: BORRADOR o CANCELADA, nunca PROGRAMADA. */
    public boolean isEliminable() {
        return isBorrador() || isCancelada();
    }

    /** {@code VotacionService.abrir}: no se reabre una cerrada ni se abre una cancelada. */
    public boolean isAbrible() {
        return isBorrador() || isProgramada();
    }

    /** {@code VotacionService.cerrar}: solo desde ABIERTA. */
    public boolean isCerrable() {
        return isAbierta();
    }

    public String getRangoFechas() {
        String inicio = fechaInicio != null ? fechaInicio.replace("T", " ") : "Sin fecha";
        String fin = fechaFin != null ? fechaFin.replace("T", " ") : "Sin fecha";
        return inicio + "  →  " + fin;
    }
}