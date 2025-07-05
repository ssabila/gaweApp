package app;

import data.MySQLDataStore;
import models.Employee;
import ui.EmployeeDashboard;
import ui.ManagerDashboard;
import ui.SupervisorDashboard;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;

public class LoginController {

    @FXML
    private TextField employeeIdField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField passwordTextField;
    @FXML
    private Button togglePasswordVisibility;
    @FXML
    private SVGPath eyeIcon;

    private Stage primaryStage;
    private MySQLDataStore dataStore;
    private boolean isPasswordVisible = false;

    // SVG paths for eye icons
    private static final String EYE_OPEN_PATH = "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z";
    private static final String EYE_CLOSED_PATH = "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z";

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setDataStore(MySQLDataStore dataStore) {
        this.dataStore = dataStore;
    }

    @FXML
    private void initialize() {
        // Sync password fields
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!passwordTextField.getText().equals(newValue)) {
                passwordTextField.setText(newValue);
            }
        });

        passwordTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!passwordField.getText().equals(newValue)) {
                passwordField.setText(newValue);
            }
        });

        // Add focus styling
        employeeIdField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                employeeIdField.setStyle(employeeIdField.getStyle() +
                        "-fx-border-color: #2B7583; -fx-effect: dropshadow(gaussian, rgba(43, 117, 131, 0.3), 4, 0, 0, 0);");
            } else {
                employeeIdField.setStyle(employeeIdField.getStyle().replace(
                        "-fx-border-color: #2B7583; -fx-effect: dropshadow(gaussian, rgba(43, 117, 131, 0.3), 4, 0, 0, 0);",
                        "-fx-border-color: #B5F3EE;"));
            }
        });

        passwordField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            updatePasswordFieldStyle(isNowFocused);
        });

        passwordTextField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            updatePasswordFieldStyle(isNowFocused);
        });

        // Add hover effect to toggle button
        togglePasswordVisibility.setOnMouseEntered(e -> {
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(100), togglePasswordVisibility);
            scaleIn.setToX(1.1);
            scaleIn.setToY(1.1);
            scaleIn.play();
        });

        togglePasswordVisibility.setOnMouseExited(e -> {
            ScaleTransition scaleOut = new ScaleTransition(Duration.millis(100), togglePasswordVisibility);
            scaleOut.setToX(1.0);
            scaleOut.setToY(1.0);
            scaleOut.play();
        });
    }

    private void updatePasswordFieldStyle(boolean isFocused) {
        String focusStyle = "-fx-border-color: #2B7583; -fx-effect: dropshadow(gaussian, rgba(43, 117, 131, 0.3), 4, 0, 0, 0);";
        String normalStyle = "-fx-border-color: #B5F3EE;";

        if (isPasswordVisible) {
            if (isFocused) {
                passwordTextField.setStyle(passwordTextField.getStyle().replace(normalStyle, focusStyle));
            } else {
                passwordTextField.setStyle(passwordTextField.getStyle().replace(focusStyle, normalStyle));
            }
        } else {
            if (isFocused) {
                passwordField.setStyle(passwordField.getStyle().replace(normalStyle, focusStyle));
            } else {
                passwordField.setStyle(passwordField.getStyle().replace(focusStyle, normalStyle));
            }
        }
    }

    @FXML
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;

        if (isPasswordVisible) {
            // Show password as text
            passwordTextField.setText(passwordField.getText());
            passwordField.setVisible(false);
            passwordTextField.setVisible(true);
            passwordTextField.requestFocus();

            // Change icon to eye-slash
            eyeIcon.setContent(EYE_CLOSED_PATH);
        } else {
            // Hide password
            passwordField.setText(passwordTextField.getText());
            passwordTextField.setVisible(false);
            passwordField.setVisible(true);
            passwordField.requestFocus();

            // Change icon to eye
            eyeIcon.setContent(EYE_OPEN_PATH);
        }

        // Add button press animation
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(50), togglePasswordVisibility);
        scaleDown.setToX(0.95);
        scaleDown.setToY(0.95);

        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(50), togglePasswordVisibility);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);

        scaleDown.setOnFinished(e -> scaleUp.play());
        scaleDown.play();
    }

    @FXML
    private void handleLogin() {
        String employeeId = employeeIdField.getText().trim();
        String password = isPasswordVisible ? passwordTextField.getText() : passwordField.getText();

        if (employeeId.isEmpty() || password.isEmpty()) {
            showStyledAlert(Alert.AlertType.WARNING, "Login Failed",
                    "Please enter both Employee ID and Password.");
            return;
        }

        // Add loading animation to login button
        Button loginButton = (Button) employeeIdField.getScene().lookup(".button");
        if (loginButton != null) {
            loginButton.setText("Signing In...");
            loginButton.setDisable(true);
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
                showStyledAlert(Alert.AlertType.ERROR, "Login Failed",
                        "Invalid Employee ID or Password.\n\nPlease check your credentials and try again.");

                // Reset login button
                if (loginButton != null) {
                    loginButton.setText("Sign In");
                    loginButton.setDisable(false);
                }

                // Add shake animation to form
                addShakeAnimation();
            }
        } catch (Exception e) {
            System.err.println("❌ Authentication error: " + e.getMessage());
            e.printStackTrace();
            showStyledAlert(Alert.AlertType.ERROR, "Database Error",
                    "Failed to authenticate user: " + e.getMessage());

            // Reset login button
            if (loginButton != null) {
                loginButton.setText("Sign In");
                loginButton.setDisable(false);
            }
        }
    }

    private void addShakeAnimation() {
        // Add shake animation to the form
        javafx.animation.TranslateTransition shake = new javafx.animation.TranslateTransition(Duration.millis(50), employeeIdField.getParent());
        shake.setFromX(0);
        shake.setToX(10);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.play();
    }

    private void showStyledAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Style the alert dialog
        alert.getDialogPane().setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-border-color: #B5F3EE;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 15;" +
                        "-fx-background-radius: 15;" +
                        "-fx-effect: dropshadow(gaussian, rgba(33, 97, 99, 0.3), 15, 0, 0, 5);"
        );

        alert.getDialogPane().lookup(".content.label").setStyle(
                "-fx-font-family: 'Poppins';" +
                        "-fx-font-size: 14;" +
                        "-fx-text-fill: #216163;"
        );

        alert.showAndWait();
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
                    showStyledAlert(Alert.AlertType.ERROR, "Error", "Unknown user role: " + employee.getRole());
                    return;
            }

            System.out.println("✅ Dashboard opened successfully");

        } catch (Exception e) {
            System.err.println("❌ Failed to open dashboard: " + e.getMessage());
            e.printStackTrace();

            // Show detailed error to user
            showStyledAlert(Alert.AlertType.ERROR, "Dashboard Error",
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
}