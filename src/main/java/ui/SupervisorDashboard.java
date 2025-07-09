package ui;

import app.HelloApplication;
import data.MySQLDataStore;
import models.Employee;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Supervisor Dashboard Application - now acts as a View Loader following MVC pattern.
 */
public class SupervisorDashboard extends Application {
    private final Employee supervisor;
    private static MySQLDataStore dataStore;

    private static final Logger logger = Logger.getLogger(SupervisorDashboard.class.getName());

    public SupervisorDashboard(Employee supervisor) {
        this.supervisor = supervisor;
        // Ensure dataStore is initialized, similar to HelloApplication
        if (dataStore == null) {
            dataStore = HelloApplication.getDataStore();
            if (dataStore == null) {
                logger.severe("MySQLDataStore is null when initializing SupervisorDashboard");
            }
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        System.out.println("=== STARTING SUPERVISOR DASHBOARD ===");
        System.out.println("Supervisor: " + supervisor.getNama());
        System.out.println("DataStore available: " + (dataStore != null ? "Yes" : "No"));

        if (dataStore == null) {
            showAlert(stage, "Database Error", "Database connection not initialized. Please launch the application from HelloApplication.");
            return;
        }

        try {
            System.out.println("Loading SupervisorDashboardView.fxml...");
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ui/SupervisorDashboardView.fxml"));

            if (fxmlLoader.getLocation() == null) {
                throw new IOException("SupervisorDashboardView.fxml not found at /ui/SupervisorDashboardView.fxml");
            }

            System.out.println("FXML file found, loading...");
            BorderPane root = fxmlLoader.load();
            System.out.println("✅ FXML loaded successfully");

            Object controllerObj = fxmlLoader.getController();
            if (controllerObj == null) {
                throw new IOException("Controller is null - check fx:controller in FXML");
            }

            if (!(controllerObj instanceof SupervisorDashboardController)) {
                throw new IOException("Controller is not SupervisorDashboardController instance: " + controllerObj.getClass());
            }

            SupervisorDashboardController controller = (SupervisorDashboardController) controllerObj;
            System.out.println("✅ Controller obtained successfully");

            // CRITICAL FIX: Set dataStore and stage before supervisor
            System.out.println("Setting DataStore...");
            controller.setDataStore(dataStore);

            System.out.println("Setting Stage...");
            controller.setStage(stage);

            System.out.println("Setting Supervisor...");
            controller.setSupervisor(supervisor); // This will call initializeContent() internally

            System.out.println("✅ Controller configured successfully");

            // Create scene and show
            Scene scene = new Scene(root, 1366, 700);
            stage.setScene(scene);
            stage.setTitle("GAWE - Supervisor Dashboard - " + supervisor.getNama());
            stage.show();

            System.out.println("✅ Supervisor Dashboard displayed successfully");

        } catch (Exception e) {
            System.err.println("❌ Error loading Supervisor Dashboard: " + e.getMessage());
            e.printStackTrace();

            showAlert(stage, "Dashboard Error",
                    "Failed to load Supervisor Dashboard.\n\n" +
                            "Error: " + e.getMessage() + "\n\n" +
                            "Please ensure:\n" +
                            "1. SupervisorDashboardView.fxml exists in src/main/resources/ui/\n" +
                            "2. SupervisorDashboardController.java exists in src/main/java/ui/\n" +
                            "3. fx:controller=\"ui.SupervisorDashboardController\" is set in FXML");

            throw new IOException("Failed to load Supervisor Dashboard", e);
        }
    }

    private void showAlert(Stage ownerStage, String title, String message) {
        try {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            if (ownerStage != null) {
                alert.initOwner(ownerStage);
            }
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("Error showing alert: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        throw new UnsupportedOperationException(
                "SupervisorDashboard should be launched through HelloApplication");
    }
}