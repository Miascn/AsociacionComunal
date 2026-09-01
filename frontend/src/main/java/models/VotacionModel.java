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

        public OpcionModel() {}

        public OpcionModel(Integer idOpcion, String descripcion, int orden, int votos) {
            this.idOpcion = idOpcion;
            this.descripcion = descripcion;
            this.orden = orden;
            this.votos = votos;
        }

        public Integer getIdOpcion() { return idOpcion; }
        public void setIdOpcion(Integer idOpcion) { this.idOpcion = idOpcion; }

        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

        public int getOrden() { return orden; }
        public void setOrden(int orden) { this.orden = orden; }

        public int getVotos() { return votos; }
        public void setVotos(int votos) { this.votos = votos; }
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

    public boolean isBorrador() {
        return "BORRADOR".equalsIgnoreCase(estado) || "PROGRAMADA".equalsIgnoreCase(estado);
    }

    public boolean isCerrada() {
        return "CERRADA".equalsIgnoreCase(estado);
    }

    public String getRangoFechas() {
        String inicio = fechaInicio != null ? fechaInicio.replace("T", " ") : "Sin fecha";
        String fin = fechaFin != null ? fechaFin.replace("T", " ") : "Sin fecha";
        return inicio + "  →  " + fin;
    }
}