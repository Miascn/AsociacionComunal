package service;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

public final class ResponsiveWindowService {
    private static final double WINDOW_MARGIN = 24;

    private ResponsiveWindowService() { }

    public static Rectangle2D visualBounds(Stage stage) {
        var screens = Screen.getScreensForRectangle(
            stage.getX(), stage.getY(), Math.max(stage.getWidth(), 1), Math.max(stage.getHeight(), 1)
        );
        return (screens.isEmpty() ? Screen.getPrimary() : screens.getFirst()).getVisualBounds();
    }

    public static void fit(Stage stage, double preferredWidth, double preferredHeight, boolean dashboard) {
        Rectangle2D bounds = visualBounds(stage);
        double availableWidth = Math.max(640, bounds.getWidth() - WINDOW_MARGIN);
        double availableHeight = Math.max(480, bounds.getHeight() - WINDOW_MARGIN);
        boolean userMaximized = stage.isMaximized();

        stage.setMinWidth(Math.min(820, availableWidth));
        stage.setMinHeight(Math.min(580, availableHeight));
        if (!userMaximized) {
            stage.setWidth(Math.min(preferredWidth, availableWidth));
            stage.setHeight(Math.min(preferredHeight, availableHeight));
            stage.setX(bounds.getMinX() + (bounds.getWidth() - stage.getWidth()) / 2);
            stage.setY(bounds.getMinY() + (bounds.getHeight() - stage.getHeight()) / 2);
        }

        if (userMaximized || (dashboard && (bounds.getWidth() <= 1366 || bounds.getHeight() <= 768))) {
            stage.setMaximized(true);
        }
    }
}
