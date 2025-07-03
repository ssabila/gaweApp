package id.ac.stis.pbo.demo1; // Corrected package declaration

import id.ac.stis.pbo.demo1.data.MySQLDataStore;
import id.ac.stis.pbo.demo1.models.Employee;
import id.ac.stis.pbo.demo1.ui.SupervisorDashboard;
import id.ac.stis.pbo.demo1.ui.ManagerDashboard;
import id.ac.stis.pbo.demo1.ui.EmployeeDashboard;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.logging.Logger;
import java.io.IOException; // Added import for IOException

public class LoginController {

    @FXML
    private TextField employeeIdField;

    @FXML
    private PasswordField passwordField;

    private Stage primaryStage;
    private MySQLDataStore dataStore;

    private static final Logger logger = Logger.getLogger(LoginController.class.getName());

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setDataStore(MySQLDataStore dataStore) {
        this.dataStore = dataStore;
    }

    @FXML
    private void handleLogin() {
        String employeeId = employeeIdField.getText();
        String password = passwordField.getText();

        if (employeeId.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Login Failed", "Please enter both Employee ID and Password.");
            return;
        }

        try {
            // Authenticate user using MySQL DataStore
            Employee employee = dataStore.authenticateUser(employeeId, password);

            if (employee != null) {
                // Close login window
                primaryStage.close();

                // Open appropriate dashboard based on role
                openDashboard(employee);
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid Employee ID or Password.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to authenticate user: " + e.getMessage());
            logger.severe("Authentication error: " + e.getMessage());
        }
    }

    private void openDashboard(Employee employee) {
        try {
            Stage dashboardStage = new Stage();

            switch (employee.getRole().toLowerCase()) {
                case "manajer":
                    // Open Manager Dashboard
                    ManagerDashboard managerDashboard = new ManagerDashboard(employee);
                    managerDashboard.start(dashboardStage);
                    break;

                case "supervisor":
                    // Open Full-Featured Supervisor Dashboard
                    SupervisorDashboard supervisorDashboard = new SupervisorDashboard(employee);
                    supervisorDashboard.start(dashboardStage);
                    break;

                case "pegawai":
                    // Open Employee Dashboard
                    EmployeeDashboard employeeDashboard = new EmployeeDashboard(employee, dataStore);
                    employeeDashboard.start(dashboardStage);
                    break;

                default:
                    showAlert(Alert.AlertType.ERROR, "Error", "Unknown user role: " + employee.getRole());
                    return;
            }

        } catch (IOException e) { // Catch IOException specifically for start() methods of dashboards
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open dashboard: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) { // Catch other potential exceptions during dashboard opening
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}