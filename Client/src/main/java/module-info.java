module com.Client.Client {
    requires java.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.google.gson;

    opens com.client to javafx.fxml;
    exports com.client;
}