package app;

/**
 * Punto de entrada neutral para ejecutables nativos. Evita que el lanzador de
 * Java trate de resolver JavaFX antes de cargar las dependencias empaquetadas.
 */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        Main.main(args);
    }
}
