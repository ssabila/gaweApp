package id.ac.stis.pbo.demo1.ui;

import id.ac.stis.pbo.demo1.HelloApplication;
import id.ac.stis.pbo.demo1.data.MySQLDataStore;
import id.ac.stis.pbo.demo1.models.Employee;
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
    private static MySQLDataStore dataStore; // Using static to be consistent with HelloApplication's dataStore

    private static final Logger logger = Logger.getLogger(EmployeeDashboard.class.getName());

    public EmployeeDashboard(Employee employee, MySQLDataStore dataStore) {
        this.employee = employee;
        // This constructor assumes dataStore is provided.
        // For consistency with other dashboards, you might also fetch it from HelloApplication if null.
        if (EmployeeDashboard.dataStore == null) {
            EmployeeDashboard.dataStore = dataStore; // Assign if not already assigned
        }
        if (EmployeeDashboard.dataStore == null) {
            EmployeeDashboard.dataStore = HelloApplication.getDataStore(); // Fallback to shared instance
            if (EmployeeDashboard.dataStore == null) {
                logger.severe("MySQLDataStore is null when initializing EmployeeDashboard. Exiting.");
                // This scenario should ideally be handled earlier in HelloApplication's init/start
                // For robustness, adding a check here.
            }
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        if (dataStore == null) {
            // If dataStore is still null (e.g., direct launch of EmployeeDashboard without HelloApplication init)
            showAlert(stage, "Database Error", "Database connection not initialized. Please launch the application from HelloApplication.");
            return;
        }

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("EmployeeDashboardView.fxml"));
        BorderPane root = fxmlLoader.load();

        EmployeeDashboardController controller = fxmlLoader.getController();
        controller.setEmployee(employee);
        controller.setDataStore(dataStore);
        controller.setStage(stage); // Pass the stage to the controller for logout/closing

        Scene scene = new Scene(root, 1600, 900);
        stage.setScene(scene);
        stage.show();
    }

    private void showAlert(Stage ownerStage, String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (ownerStage != null) {
            alert.initOwner(ownerStage);
        }
        alert.showAndWait();
    }

    // This main method is primarily for direct testing.
    // The dashboard should be launched through HelloApplication
    public static void main(String[] args) {
        throw new UnsupportedOperationException(
                "EmployeeDashboard should be launched through HelloApplication");
    }
}