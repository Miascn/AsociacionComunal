package models;

public class DashboardData {

    private int miembros;
    private int familias;
    private double ingresos;
    private int proyectos;

    public DashboardData(int miembros, int familias, double ingresos, int proyectos) {
        this.miembros = miembros;
        this.familias = familias;
        this.ingresos = ingresos;
        this.proyectos = proyectos;
    }

    public int getMiembros() {
        return miembros;
    }

    public int getFamilias() {
        return familias;
    }

    public double getIngresos() {
        return ingresos;
    }

    public int getProyectos() {
        return proyectos;
    }
}