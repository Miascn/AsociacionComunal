package views;


import components.Header;
import components.Sidebar;


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