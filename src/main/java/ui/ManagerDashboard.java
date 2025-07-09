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

public class ManagerDashboard extends Application {
    private final Employee manager;
    private static MySQLDataStore dataStore;

    private static final Logger logger = Logger.getLogger(ManagerDashboard.class.getName());

    public ManagerDashboard(Employee manager) {
        this.manager = manager;
        // Get dataStore from HelloApplication
        if (dataStore == null) {
            dataStore = HelloApplication.getDataStore();
            if (dataStore == null) {
                logger.severe("MySQLDataStore is null when initializing ManagerDashboard");
            }
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        System.out.println("=== STARTING MANAGER DASHBOARD ===");
        System.out.println("Manager: " + manager.getNama());
        System.out.println("DataStore available: " + (dataStore != null ? "Yes" : "No"));

        if (dataStore == null) {
            showAlert(stage, "Database Error", "Database connection not available. Please restart the application.");
            return;
        }

        try {
            System.out.println("Loading ManagerDashboardView.fxml...");
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ui/ManagerDashboardView.fxml"));

            if (fxmlLoader.getLocation() == null) {
                throw new IOException("ManagerDashboardView.fxml not found at /ui/ManagerDashboardView.fxml");
            }

            System.out.println("FXML file found, loading...");
            BorderPane root = fxmlLoader.load();
            System.out.println("✅ FXML loaded successfully");

            Object controllerObj = fxmlLoader.getController();
            if (controllerObj == null) {
                throw new IOException("Controller is null - check fx:controller in FXML");
            }

            if (!(controllerObj instanceof ManagerDashboardController)) {
                throw new IOException("Controller is not ManagerDashboardController instance: " + controllerObj.getClass());
            }

            ManagerDashboardController controller = (ManagerDashboardController) controllerObj;
            System.out.println("✅ Controller obtained successfully");

            // CRITICAL FIX: Set manager and dataStore in the correct order
            // DataStore must be set before manager to avoid issues in initializeContent()
            System.out.println("Setting DataStore...");
            controller.setDataStore(dataStore);

            System.out.println("Setting Stage...");
            controller.setStage(stage);

            System.out.println("Setting Manager...");
            controller.setManager(manager); // This will call initializeContent() internally

            System.out.println("✅ Controller configured successfully");

            // Create scene and show
            Scene scene = new Scene(root, 1366, 700);
            stage.setScene(scene);
            stage.setTitle("GAWE - Manager Dashboard - " + manager.getNama());
            stage.show();

            System.out.println("✅ Manager Dashboard displayed successfully");

        } catch (Exception e) {
            System.err.println("❌ Error loading Manager Dashboard: " + e.getMessage());
            e.printStackTrace();

            showAlert(stage, "Dashboard Error",
                    "Failed to load Manager Dashboard.\n\n" +
                            "Error: " + e.getMessage() + "\n\n" +
                            "Please ensure:\n" +
                            "1. ManagerDashboardView.fxml exists in src/main/resources/ui/\n" +
                            "2. ManagerDashboardController.java exists in src/main/java/ui/\n" +
                            "3. fx:controller=\"ui.ManagerDashboardController\" is set in FXML");

            throw new IOException("Failed to load Manager Dashboard", e);
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
        // For testing - should not be called directly in normal flow
        Employee testManager = new Employee("MNG001", "Test Manager", "password123", "manajer", "HR", "General Manager", new java.util.Date());

        if (dataStore == null) {
            try {
                dataStore = new MySQLDataStore();
                System.out.println("MySQL DataStore initialized for direct ManagerDashboard launch.");
            } catch (Exception e) {
                System.err.println("Failed to initialize MySQL DataStore: " + e.getMessage());
                return;
            }
        }

        new ManagerDashboard(testManager).launch(args);
    }
}