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
 * Manager Dashboard Application - now acts as a View Loader following MVC pattern.
 */
public class ManagerDashboard extends Application {
    private final Employee manager;
    private static MySQLDataStore dataStore; // Using static to be consistent with HelloApplication's dataStore

    private static final Logger logger = Logger.getLogger(ManagerDashboard.class.getName());

    public ManagerDashboard(Employee manager) {
        this.manager = manager;
        // Ensure dataStore is initialized, similar to HelloApplication
        if (dataStore == null) {
            dataStore = HelloApplication.getDataStore(); // Get shared DataStore instance
            if (dataStore == null) {
                logger.severe("MySQLDataStore is null when initializing ManagerDashboard. Exiting.");
                // This scenario should ideally be handled earlier in HelloApplication's init/start
                // For robustness, adding a check here.
            }
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        if (dataStore == null) {
            // If dataStore is still null (e.g., direct launch of ManagerDashboard without HelloApplication init)
            showAlert(stage, "Database Error", "Database connection not initialized. Please launch the application from HelloApplication.");
            return;
        }

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ManagerDashboardView.fxml"));
        BorderPane root = fxmlLoader.load();

        ManagerDashboardController controller = fxmlLoader.getController();
        controller.setManager(manager);
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
    // In a full application, ManagerDashboard would typically be launched from HelloApplication.
    public static void main(String[] args) {
        // Initialize a dummy manager for direct testing if needed
        Employee manager = new Employee("MNG001", "John Manager", "password123",
                "manajer", "HR", "General Manager", new java.util.Date());

        // For direct testing, you might need to initialize MySQLDataStore manually
        // if not starting from HelloApplication
        if (dataStore == null) {
            try {
                dataStore = new MySQLDataStore();
                System.out.println("MySQL DataStore initialized for direct ManagerDashboard launch.");
            } catch (Exception e) {
                System.err.println("Failed to initialize MySQL DataStore for direct launch: " + e.getMessage());
                // Handle error appropriately, e.g., exit
                return;
            }
        }
        launch(args);
    }
}