module id.ac.stis.pbo.demo1 {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.zaxxer.hikari;
    requires com.google.gson;
    requires java.sql; // Add this if not present

    opens id.ac.stis.pbo.demo1 to javafx.fxml;
    opens id.ac.stis.pbo.demo1.ui to javafx.fxml; // Ensure this is open
    exports id.ac.stis.pbo.demo1;
    exports id.ac.stis.pbo.demo1.data;
    exports id.ac.stis.pbo.demo1.database;
    exports id.ac.stis.pbo.demo1.models;
    exports id.ac.stis.pbo.demo1.server;
    exports id.ac.stis.pbo.demo1.ui; // Export the UI package
}