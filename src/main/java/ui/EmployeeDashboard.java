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
 * Employee Dashboard Application - now acts as a View Loader following MVC pattern.
 */
public class EmployeeDashboard extends Application {
    private final Employee employee;
    private static MySQLDataStore dataStore;

    private static final Logger logger = Logger.getLogger(EmployeeDashboard.class.getName());

    public EmployeeDashboard(Employee employee, MySQLDataStore dataStore) {
        this.employee = employee;
        // For consistency with other dashboards, you might also fetch it from HelloApplication if null.
        if (EmployeeDashboard.dataStore == null) {
            EmployeeDashboard.dataStore = dataStore; // Assign if not already assigned
        }
        if (EmployeeDashboard.dataStore == null) {
            EmployeeDashboard.dataStore = HelloApplication.getDataStore(); // Fallback to shared instance
            if (EmployeeDashboard.dataStore == null) {
                logger.severe("MySQLDataStore is null when initializing EmployeeDashboard");
            }
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        System.out.println("=== STARTING EMPLOYEE DASHBOARD ===");
        System.out.println("Employee: " + employee.getNama());
        System.out.println("DataStore available: " + (dataStore != null ? "Yes" : "No"));

        if (dataStore == null) {
            showAlert(stage, "Database Error", "Database connection not initialized. Please launch the application from HelloApplication.");
            return;
        }

        try {
            System.out.println("Loading EmployeeDashboardView.fxml...");
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ui/EmployeeDashboardView.fxml"));

            if (fxmlLoader.getLocation() == null) {
                throw new IOException("EmployeeDashboardView.fxml not found at /ui/EmployeeDashboardView.fxml");
            }

            System.out.println("FXML file found, loading...");
            BorderPane root = fxmlLoader.load();
            System.out.println("✅ FXML loaded successfully");

            Object controllerObj = fxmlLoader.getController();
            if (controllerObj == null) {
                throw new IOException("Controller is null - check fx:controller in FXML");
            }

            if (!(controllerObj instanceof EmployeeDashboardController)) {
                throw new IOException("Controller is not EmployeeDashboardController instance: " + controllerObj.getClass());
            }

            EmployeeDashboardController controller = (EmployeeDashboardController) controllerObj;
            System.out.println("✅ Controller obtained successfully");

            // CRITICAL FIX: Set dataStore and stage before employee
            System.out.println("Setting DataStore...");
            controller.setDataStore(dataStore);

            System.out.println("Setting Stage...");
            controller.setStage(stage);

            System.out.println("Setting Employee...");
            controller.setEmployee(employee); // This will call initializeContent() internally

            System.out.println("✅ Controller configured successfully");

            // Create scene and show
            Scene scene = new Scene(root, 1366, 700);
            stage.setScene(scene);
            stage.setTitle("GAWE - Employee Dashboard - " + employee.getNama());
            stage.show();

            System.out.println("✅ Employee Dashboard displayed successfully");

        } catch (Exception e) {
            System.err.println("❌ Error loading Employee Dashboard: " + e.getMessage());
            e.printStackTrace();

            showAlert(stage, "Dashboard Error",
                    "Failed to load Employee Dashboard.\n\n" +
                            "Error: " + e.getMessage() + "\n\n" +
                            "Please ensure:\n" +
                            "1. EmployeeDashboardView.fxml exists in src/main/resources/ui/\n" +
                            "2. EmployeeDashboardController.java exists in src/main/java/ui/\n" +
                            "3. fx:controller=\"ui.EmployeeDashboardController\" is set in FXML");

            throw new IOException("Failed to load Employee Dashboard", e);
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
                "EmployeeDashboard should be launched through HelloApplication");
    }
}