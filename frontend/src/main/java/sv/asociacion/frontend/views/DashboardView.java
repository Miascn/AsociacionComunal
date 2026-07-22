package sv.asociacion.frontend.views;


import sv.asociacion.frontend.components.Header;
import sv.asociacion.frontend.components.Sidebar;


import javafx.scene.layout.BorderPane;


public class DashboardView extends BorderPane {


    public DashboardView(){


        Sidebar sidebar =
                new Sidebar();


        Header header =
                new Header();



        setLeft(sidebar);

        setTop(header);



        setCenter(
                new DashboardContent()
        );


    }
}