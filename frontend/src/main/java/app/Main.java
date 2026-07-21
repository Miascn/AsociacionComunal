package app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import views.DashboardView;


public class Main extends Application {


    @Override
    public void start(Stage stage) {


        DashboardView dashboard =
                new DashboardView();


        Scene scene =
                new Scene(
                        dashboard,
                        1200,
                        700
                );


        stage.setTitle(
                "Asociación Comunal ERP"
        );


        stage.setScene(scene);

        stage.show();

    }


    public static void main(String[] args) {

        launch(args);

    }
}