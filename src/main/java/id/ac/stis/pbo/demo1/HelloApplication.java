package id.ac.stis.pbo.demo1;

import id.ac.stis.pbo.demo1.data.MySQLDataStore;
import id.ac.stis.pbo.demo1.server.GaweServer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Enhanced HelloApplication with MySQL database integration, now using MVC for login.
 */
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
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Database Connection Error");
                    alert.setHeaderText("Failed to connect to MySQL database");
                    alert.setContentText("Please ensure MySQL is running and the database configuration is correct.\n\nError: " + e.getMessage());
                    alert.showAndWait();
                    Platform.exit();
                });
                return;
            }
        } else {
            System.out.println("Using existing MySQL DataStore instance");
        }

        CompletableFuture.runAsync(() -> {
            try {
                if (server == null) {
                    System.out.println("Starting server...");
                    server = new GaweServer();
                    server.start();
                    System.out.println("Server started successfully");
                }
            } catch (Exception e) {
                String errorMsg = "Failed to start server: " + e.getMessage();
                System.err.println(errorMsg);
                e.printStackTrace();
            }
        });

        System.out.println("Application initialization completed");
    }

    @Override
    public void start(Stage stage) throws IOException {
        if (dataStore == null) {
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Failed to connect to database. Please ensure MySQL is running.");
            Platform.exit();
            return;
        }

        // Corrected FXML loading path
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/id/ac/stis/pbo/demo1/LoginView.fxml"));
        BorderPane root = fxmlLoader.load();

        LoginController loginController = fxmlLoader.getController();
        loginController.setPrimaryStage(stage);
        loginController.setDataStore(dataStore);

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setTitle("GAWE - Employee Management System (MySQL)");
        stage.setOnCloseRequest(e -> {
            stopApplication();
        });
        stage.show();
    }

    private void stopApplication() {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception ex) {
                System.err.println("Error stopping server: " + ex.getMessage());
            }
        }
        if (dataStore != null) {
            dataStore.close();
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
    }

    public static void main(String[] args) {
        launch();
    }
}