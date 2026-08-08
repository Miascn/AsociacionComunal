package service;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.Scene;

public final class ThemeService {
    private static boolean dark;

    private ThemeService() { }

    public static void apply(Scene scene) {
        Application.setUserAgentStylesheet(dark ? new PrimerDark().getUserAgentStylesheet()
                                                : new PrimerLight().getUserAgentStylesheet());
        scene.getRoot().getStyleClass().removeAll("light-theme", "dark-theme");
        scene.getRoot().getStyleClass().add(dark ? "dark-theme" : "light-theme");
    }

    public static boolean toggle(Scene scene) {
        dark = !dark;
        apply(scene);
        return dark;
    }

    public static boolean isDark() { return dark; }
}
