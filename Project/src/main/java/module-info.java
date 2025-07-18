module com.example.project {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;



    opens com.example.project to javafx.fxml;
    exports com.example.project;
}