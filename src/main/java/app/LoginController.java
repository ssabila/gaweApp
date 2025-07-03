package app;

import data.MySQLDataStore;
import models.Employee;
import ui.EmployeeDashboard;
import ui.ManagerDashboard;
import ui.SupervisorDashboard;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField employeeIdField;
    @FXML
    private PasswordField passwordField;

    private Stage primaryStage;
    private MySQLDataStore dataStore;

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
            Employee employee = dataStore.authenticateUser(employeeId, password);

            if (employee != null) {
                primaryStage.close();
                openDashboard(employee);
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid Employee ID or Password.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to authenticate user: " + e.getMessage());
        }
    }

    private void openDashboard(Employee employee) {
        try {
            Stage dashboardStage = new Stage();
            switch (employee.getRole().toLowerCase()) {
                case "manajer":
                    new ManagerDashboard(employee).start(dashboardStage);
                    break;
                case "supervisor":
                    new SupervisorDashboard(employee).start(dashboardStage);
                    break;
                case "pegawai":
                    new EmployeeDashboard(employee, dataStore).start(dashboardStage);
                    break;
                default:
                    showAlert(Alert.AlertType.ERROR, "Error", "Unknown user role: " + employee.getRole());
            }
        } catch (IOException e) {
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