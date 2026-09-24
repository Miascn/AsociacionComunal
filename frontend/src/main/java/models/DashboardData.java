package models;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardData {

    private int miembros;
    private int miembrosActivos;
    private int familias;
    private int totalViviendas;
    private int viviendasOcupadas;
    private int totalHabitantes;
    private int totalAdultos;
    private int totalMenores;
    private double ingresos;
    private double ingresosMesActual;
    private int proyectos;
    private int totalProyectos;

    private List<ViviendaModel> viviendas = new ArrayList<>();
    private Map<String, Double> aportacionesPorMes = new LinkedHashMap<>();
    private List<ReunionModel> proximasReuniones = new ArrayList<>();
    private List<BitacoraModel> actividadReciente = new ArrayList<>();

    public DashboardData() {
    }

    public DashboardData(int miembros, int familias, double ingresos, int proyectos) {
        this.miembros = miembros;
        this.miembrosActivos = miembros;
        this.familias = familias;
        this.totalViviendas = familias;
        this.viviendasOcupadas = familias;
        this.ingresos = ingresos;
        this.ingresosMesActual = ingresos;
        this.proyectos = proyectos;
        this.totalProyectos = proyectos;
    }

    public int getMiembros() {
        return miembros;
    }

    public void setMiembros(int miembros) {
        this.miembros = miembros;
    }

    public int getMiembrosActivos() {
        return miembrosActivos;
    }

    public void setMiembrosActivos(int miembrosActivos) {
        this.miembrosActivos = miembrosActivos;
    }

    public int getFamilias() {
        return familias;
    }

    public void setFamilias(int familias) {
        this.familias = familias;
    }

    public int getTotalViviendas() {
        return totalViviendas;
    }

    public void setTotalViviendas(int totalViviendas) {
        this.totalViviendas = totalViviendas;
    }

    public int getViviendasOcupadas() {
        return viviendasOcupadas;
    }

    public void setViviendasOcupadas(int viviendasOcupadas) {
        this.viviendasOcupadas = viviendasOcupadas;
    }

    public int getTotalHabitantes() {
        return totalHabitantes;
    }

    public void setTotalHabitantes(int totalHabitantes) {
        this.totalHabitantes = totalHabitantes;
    }

    public int getTotalAdultos() {
        return totalAdultos;
    }

    public void setTotalAdultos(int totalAdultos) {
        this.totalAdultos = totalAdultos;
    }

    public int getTotalMenores() {
        return totalMenores;
    }

    public void setTotalMenores(int totalMenores) {
        this.totalMenores = totalMenores;
    }

    public double getIngresos() {
        return ingresos;
    }

    public void setIngresos(double ingresos) {
        this.ingresos = ingresos;
    }

    public double getIngresosMesActual() {
        return ingresosMesActual;
    }

    public void setIngresosMesActual(double ingresosMesActual) {
        this.ingresosMesActual = ingresosMesActual;
    }

    public int getProyectos() {
        return proyectos;
    }

    public void setProyectos(int proyectos) {
        this.proyectos = proyectos;
    }

    public int getTotalProyectos() {
        return totalProyectos;
    }

    public void setTotalProyectos(int totalProyectos) {
        this.totalProyectos = totalProyectos;
    }

    public List<ViviendaModel> getViviendas() {
        return viviendas;
    }

    public void setViviendas(List<ViviendaModel> viviendas) {
        this.viviendas = (viviendas != null) ? viviendas : new ArrayList<>();
    }

    public Map<String, Double> getAportacionesPorMes() {
        return aportacionesPorMes;
    }

    public void setAportacionesPorMes(Map<String, Double> aportacionesPorMes) {
        this.aportacionesPorMes = (aportacionesPorMes != null) ? aportacionesPorMes : new LinkedHashMap<>();
    }

    public List<ReunionModel> getProximasReuniones() {
        return proximasReuniones;
    }

    public void setProximasReuniones(List<ReunionModel> proximasReuniones) {
        this.proximasReuniones = (proximasReuniones != null) ? proximasReuniones : new ArrayList<>();
    }

    public List<BitacoraModel> getActividadReciente() {
        return actividadReciente;
    }

    public void setActividadReciente(List<BitacoraModel> actividadReciente) {
        this.actividadReciente = (actividadReciente != null) ? actividadReciente : new ArrayList<>();
    }
}