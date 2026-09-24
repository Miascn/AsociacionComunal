package service;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Proveedor de iconos vectoriales SVG de Heroicons (Outline)
 * renderizados nativamente con trazos y colores de Tailwind CSS.
 */
public final class HeroIcon {

    // Heroicons Outline (v2 / 24x24)
    public static final String EYE =
        "M2.036 12.322a1.012 1.012 0 0 1 0-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178Z M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z";

    public static final String PENCIL =
        "M16.862 4.487l1.687-1.688a1.875 1.875 0 1 1 2.652 2.652L10.582 16.07a4.5 4.5 0 0 1-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 0 1 1.13-1.897l8.932-8.931Zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0 1 15.75 21H5.25A2.25 2.25 0 0 1 3 18.75V8.25A2.25 2.25 0 0 1 5.25 6H10";

    public static final String TRASH =
        "M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0";

    public static final String ARCHIVE =
        "M20.25 7.5l-.625 10.632a2.25 2.25 0 0 1-2.247 2.118H6.622a2.25 2.25 0 0 1-2.247-2.118L3.75 7.5M10 11.25h4M3.375 7.5h17.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125Z";

    public static final String PLUS =
        "M12 4.5v15m7.5-7.5h-15";

    public static final String SEARCH =
        "M21 21l-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z";

    public static final String REFRESH =
        "M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0 3.181 3.183a8.25 8.25 0 0 0 13.803-3.7M4.031 9.865a8.25 8.25 0 0 1 13.803-3.7l3.181 3.182m0-4.991v4.99";

    // Colores estándar Tailwind CSS
    public static final String BLUE_600 = "#2563eb";
    public static final String AMBER_600 = "#d97706";
    public static final String RED_600 = "#dc2626";
    public static final String GREEN_600 = "#16a34a";
    public static final String SLATE_700 = "#334155";
    public static final String WHITE = "#ffffff";

    private HeroIcon() {}

    /**
     * Construye un nodo gráfico con el icono SVG de Heroicons en modo outline.
     */
    public static Node create(String pathContent, String strokeHex, double size) {
        SVGPath path = new SVGPath();
        path.setContent(pathContent);
        path.setFill(Color.TRANSPARENT);
        path.setStroke(Color.web(strokeHex));
        path.setStrokeWidth(1.85);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);

        Group group = new Group(path);
        double scale = size / 24.0;
        group.setScaleX(scale);
        group.setScaleY(scale);

        StackPane container = new StackPane(group);
        container.setPrefSize(size, size);
        container.setMinSize(size, size);
        container.setMaxSize(size, size);
        return container;
    }
}
