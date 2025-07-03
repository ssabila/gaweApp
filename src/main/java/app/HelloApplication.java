package app;

import data.MySQLDataStore;
import server.GaweServer;
import javafx.application.Application;
import javafx.application.Platform; // <-- PERBAIKAN: Import yang hilang ditambahkan di sini
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;


public class HelloApplication extends Application {
    private GaweServer server;
    private static MySQLDataStore dataStore;
    private static final Logger logger = Logger.getLogger(HelloApplication.class.getName());

    public static MySQLDataStore getDataStore() {
        return dataStore;
    }

    @Override
    public void init() {
        System.out.println("Initializing application...");
        if (dataStore == null) {
            try {
                System.out.println("Creating new MySQL DataStore instance...");
                dataStore = new MySQLDataStore();
                logger.info("MySQL DataStore initialized successfully");
            } catch (Exception e) {
                String errorMsg = "Failed to initialize MySQL DataStore: " + e.getMessage();
                logger.severe(errorMsg);
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Database Connection Error", "Could not connect to database.");
                    Platform.exit();
                });
                return;
            }
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        if (dataStore == null) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Database connection is not available.");
            Platform.exit();
            return;
        }

        // Menggunakan path relatif dari root resources ke folder 'app'
        URL fxmlLocation = getClass().getResource("/app/LoginView.fxml");

        if (fxmlLocation == null) {
            showAlert(Alert.AlertType.ERROR, "Critical Error", "Could not find /app/LoginView.fxml in resources folder.");
            Platform.exit();
            return;
        }

        FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
        BorderPane root = fxmlLoader.load();

        LoginController loginController = fxmlLoader.getController();
        loginController.setPrimaryStage(stage);
        loginController.setDataStore(dataStore);

        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("GAWE - Employee Management System (MySQL)");
        stage.setOnCloseRequest(e -> stopApplication());
        stage.show();
    }

    private void stopApplication() {
        if (dataStore != null) {
            dataStore.close();
        }
        if (server != null) {
            try {
                server.stop();
            } catch (Exception ex) {
                System.err.println("Error stopping server: " + ex.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void stop() {
        stopApplication();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}