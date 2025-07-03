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
import java.net.URL;

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
            System.out.println("=== LOGIN ATTEMPT ===");
            System.out.println("Employee ID: " + employeeId);
            System.out.println("Attempting authentication...");

            Employee employee = dataStore.authenticateUser(employeeId, password);

            if (employee != null) {
                System.out.println("✅ Authentication successful for: " + employee.getNama());
                System.out.println("Role: " + employee.getRole());
                System.out.println("Division: " + employee.getDivisi());

                primaryStage.close();
                openDashboard(employee);
            } else {
                System.err.println("❌ Authentication failed - Invalid credentials");
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid Employee ID or Password.");
            }
        } catch (Exception e) {
            System.err.println("❌ Authentication error: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to authenticate user: " + e.getMessage());
        }
    }

    private void openDashboard(Employee employee) {
        System.out.println("=== OPENING DASHBOARD ===");
        System.out.println("Employee: " + employee.getNama());
        System.out.println("Role: " + employee.getRole());

        try {
            Stage dashboardStage = new Stage();

            switch (employee.getRole().toLowerCase()) {
                case "manajer":
                    System.out.println("Opening Manager Dashboard...");
                    openManagerDashboard(employee, dashboardStage);
                    break;
                case "supervisor":
                    System.out.println("Opening Supervisor Dashboard...");
                    openSupervisorDashboard(employee, dashboardStage);
                    break;
                case "pegawai":
                    System.out.println("Opening Employee Dashboard...");
                    openEmployeeDashboard(employee, dashboardStage);
                    break;
                default:
                    System.err.println("❌ Unknown user role: " + employee.getRole());
                    showAlert(Alert.AlertType.ERROR, "Error", "Unknown user role: " + employee.getRole());
                    return;
            }

            System.out.println("✅ Dashboard opened successfully");

        } catch (Exception e) {
            System.err.println("❌ Failed to open dashboard: " + e.getMessage());
            e.printStackTrace();

            // Show detailed error to user
            showAlert(Alert.AlertType.ERROR, "Dashboard Error",
                    "Failed to open dashboard.\n\n" +
                            "Error: " + e.getClass().getSimpleName() + "\n" +
                            "Message: " + e.getMessage() + "\n\n" +
                            "Please check:\n" +
                            "1. FXML files exist in src/main/resources/ui/\n" +
                            "2. Controller classes are properly configured\n" +
                            "3. Check console for detailed error information");
        }
    }

    private void openManagerDashboard(Employee employee, Stage dashboardStage) throws IOException {
        System.out.println("Creating ManagerDashboard instance...");

        // Check if FXML file exists
        URL fxmlUrl = getClass().getResource("/ui/ManagerDashboardView.fxml");
        if (fxmlUrl == null) {
            System.err.println("❌ ManagerDashboardView.fxml not found at /ui/ManagerDashboardView.fxml");
            throw new IOException("FXML file not found: /ui/ManagerDashboardView.fxml\n" +
                    "Please ensure the file exists at: src/main/resources/ui/ManagerDashboardView.fxml");
        }

        System.out.println("✅ ManagerDashboardView.fxml found at: " + fxmlUrl);

        // Check if Controller class exists
        try {
            Class.forName("ui.ManagerDashboardController");
            System.out.println("✅ ManagerDashboardController class found");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ ManagerDashboardController class not found");
            throw new IOException("Controller class not found: ui.ManagerDashboardController");
        }

        new ManagerDashboard(employee).start(dashboardStage);
    }

    private void openSupervisorDashboard(Employee employee, Stage dashboardStage) throws IOException {
        System.out.println("Creating SupervisorDashboard instance...");

        // Check if FXML file exists
        URL fxmlUrl = getClass().getResource("/ui/SupervisorDashboardView.fxml");
        if (fxmlUrl == null) {
            System.err.println("❌ SupervisorDashboardView.fxml not found at /ui/SupervisorDashboardView.fxml");
            throw new IOException("FXML file not found: /ui/SupervisorDashboardView.fxml\n" +
                    "Please ensure the file exists at: src/main/resources/ui/SupervisorDashboardView.fxml");
        }

        System.out.println("✅ SupervisorDashboardView.fxml found at: " + fxmlUrl);

        try {
            Class.forName("ui.SupervisorDashboardController");
            System.out.println("✅ SupervisorDashboardController class found");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ SupervisorDashboardController class not found");
            throw new IOException("Controller class not found: ui.SupervisorDashboardController");
        }

        new SupervisorDashboard(employee).start(dashboardStage);
    }

    private void openEmployeeDashboard(Employee employee, Stage dashboardStage) throws IOException {
        System.out.println("Creating EmployeeDashboard instance...");

        // Check if FXML file exists
        URL fxmlUrl = getClass().getResource("/ui/EmployeeDashboardView.fxml");
        if (fxmlUrl == null) {
            System.err.println("❌ EmployeeDashboardView.fxml not found at /ui/EmployeeDashboardView.fxml");
            throw new IOException("FXML file not found: /ui/EmployeeDashboardView.fxml\n" +
                    "Please ensure the file exists at: src/main/resources/ui/EmployeeDashboardView.fxml");
        }

        System.out.println("✅ EmployeeDashboardView.fxml found at: " + fxmlUrl);

        try {
            Class.forName("ui.EmployeeDashboardController");
            System.out.println("✅ EmployeeDashboardController class found");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ EmployeeDashboardController class not found");
            throw new IOException("Controller class not found: ui.EmployeeDashboardController");
        }

        new EmployeeDashboard(employee, dataStore).start(dashboardStage);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}