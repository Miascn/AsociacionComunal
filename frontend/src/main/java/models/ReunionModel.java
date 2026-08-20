package models;

public class ReunionModel {
    private Integer id;
    private String titulo;
    private String fechaHora;
    private String lugar;
    private String tipo;
    private String estado;

    public ReunionModel() {}

    public ReunionModel(Integer id, String titulo, String fechaHora,
                         String lugar, String tipo, String estado) {
        this.id = id;
        this.titulo = titulo;
        this.fechaHora = fechaHora;
        this.lugar = lugar;
        this.tipo = tipo;
        this.estado = estado;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getFechaHora() { return fechaHora; }
    public void setFechaHora(String fechaHora) { this.fechaHora = fechaHora; }

    public String getLugar() { return lugar; }
    public void setLugar(String lugar) { this.lugar = lugar; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}