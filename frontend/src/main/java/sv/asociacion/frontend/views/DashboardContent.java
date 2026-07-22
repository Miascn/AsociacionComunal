package sv.asociacion.frontend.views;


import sv.asociacion.frontend.components.DashboardCard;
import sv.asociacion.frontend.models.DashboardData;
import sv.asociacion.frontend.services.MockDashboardService;


import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;


public class DashboardContent extends VBox {


    public DashboardContent(){


        MockDashboardService service =
                new MockDashboardService();


        DashboardData data =
                service.getDashboard();



        HBox cards = new HBox(20);


        cards.getChildren().addAll(

                new DashboardCard(
                        "Miembros",
                        ""+data.getMiembros()
                ),

                new DashboardCard(
                        "Familias",
                        ""+data.getFamilias()
                ),

                new DashboardCard(
                        "Ingresos",
                        "$"+data.getIngresos()
                ),

                new DashboardCard(
                        "Proyectos",
                        ""+data.getProyectos()
                )
        );


        setPadding(
                new Insets(30)
        );


        getChildren().add(cards);

    }
}