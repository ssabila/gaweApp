module gawe {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires com.google.gson;

    // MySQL JDBC driver
    requires java.sql.rowset;
    requires com.zaxxer.hikari;

    // Export packages for reflection access
    exports app;
    exports ui;
    exports models;
    exports data;
    exports database;
    exports server;

    // Open packages for FXML loading and reflection
    opens app to javafx.fxml, com.google.gson;
    opens ui to javafx.fxml, com.google.gson;
    opens models to com.google.gson, javafx.base;
    opens data to com.google.gson;
}
