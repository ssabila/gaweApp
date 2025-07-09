package app;

import data.MySQLDataStore;
import server.GaweServer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

public class HelloApplication extends Application {
    private GaweServer server;
    private static MySQLDataStore dataStore;
    private static final Logger logger = Logger.getLogger(HelloApplication.class.getName());

    public static MySQLDataStore getDataStore() {
        return dataStore;
    }

    @Override
    public void init() {
        System.out.println("=== GAWE APPLICATION INITIALIZATION ===");
        try {
            if (dataStore == null) {
                System.out.println("Step 1: Creating MySQL DataStore instance...");
                dataStore = new MySQLDataStore();
                System.out.println("✅ Step 1: MySQL DataStore initialized successfully");
            }
        } catch (Exception e) {
            System.err.println("❌ Step 1 FAILED: MySQL DataStore initialization failed");
            System.err.println("Error details: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("=== INITIALIZATION COMPLETE ===\n");
    }

    @Override
    public void start(Stage stage) throws IOException {
        System.out.println("=== GAWE APPLICATION START ===");

        // Step 1: Check DataStore
        System.out.println("Step 2: Checking DataStore availability...");
        if (dataStore == null) {
            System.err.println("❌ Step 2: DataStore is null, attempting recovery...");
            try {
                dataStore = new MySQLDataStore();
                System.out.println("✅ Step 2: DataStore recovery successful");
            } catch (Exception e) {
                System.err.println("❌ Step 2 CRITICAL FAILURE: Cannot create DataStore");
                System.err.println("Error details: " + e.getMessage());
                e.printStackTrace();

                // Show fallback UI instead of exiting
                showFallbackUI(stage, "Database Connection Failed",
                        "Could not connect to MySQL database.\n\n" +
                                "Please ensure:\n" +
                                "1. MySQL server is running\n" +
                                "2. Database 'gawe_db' exists\n" +
                                "3. Username 'root' with empty password has access\n\n" +
                                "Error: " + e.getMessage());
                return;
            }
        } else {
            System.out.println("✅ Step 2: DataStore is available");
        }

        // Step 2: Check FXML file
        System.out.println("Step 3: Locating FXML file...");
        URL fxmlLocation = getClass().getResource("/app/LoginView.fxml");
        if (fxmlLocation == null) {
            System.err.println("❌ Step 3 FAILED: Cannot find /app/LoginView.fxml");

            // Try alternative paths
            System.out.println("Trying alternative FXML paths...");
            String[] altPaths = {
                    "LoginView.fxml",
                    "/LoginView.fxml",
                    "/ui/LoginView.fxml",
                    "/resources/app/LoginView.fxml"
            };

            for (String path : altPaths) {
                System.out.println("Checking: " + path);
                URL altLocation = getClass().getResource(path);
                if (altLocation != null) {
                    System.out.println("✅ Found FXML at alternative path: " + path);
                    fxmlLocation = altLocation;
                    break;
                } else {
                    System.out.println("❌ Not found: " + path);
                }
            }

            if (fxmlLocation == null) {
                System.err.println("❌ Step 3 CRITICAL FAILURE: FXML file not found in any location");
                showFallbackUI(stage, "FXML File Not Found",
                        "Could not find LoginView.fxml file.\n" +
                                "Please ensure the file exists at: src/main/resources/app/LoginView.fxml");
                return;
            }
        } else {
            System.out.println("✅ Step 3: Found FXML file at: " + fxmlLocation);
        }

        // Step 3: Load FXML
        System.out.println("Step 4: Loading FXML content...");
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
            System.out.println("FXMLLoader created successfully");

            // FIX: Changed BorderPane to Parent to avoid ClassCastException
            Parent root = fxmlLoader.load();
            System.out.println("✅ Step 4: FXML loaded successfully");

            // Step 4: Get Controller
            System.out.println("Step 5: Getting LoginController...");
            Object controller = fxmlLoader.getController();
            if (controller == null) {
                System.err.println("❌ Step 5 FAILED: LoginController is null");
                throw new RuntimeException("LoginController is null - check fx:controller in FXML");
            }

            if (!(controller instanceof app.LoginController)) {
                System.err.println("❌ Step 5 FAILED: Controller is not LoginController instance");
                System.err.println("Controller class: " + controller.getClass().getName());
                throw new RuntimeException("Wrong controller type");
            }

            app.LoginController loginController = (app.LoginController) controller;
            System.out.println("✅ Step 5: LoginController obtained successfully");

            // Step 5: Configure Controller
            System.out.println("Step 6: Configuring LoginController...");
            loginController.setPrimaryStage(stage);
            loginController.setDataStore(dataStore);
            System.out.println("✅ Step 6: LoginController configured successfully");

            // Step 6: Create Scene
            System.out.println("Step 7: Creating Scene...");
            Scene scene = new Scene(root, 1366, 700);
            System.out.println("✅ Step 7: Scene created successfully");

            // Step 7: Configure Stage
            System.out.println("Step 8: Configuring Stage...");
            stage.setTitle("GAWE - Employee Management System (MySQL)");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setOnCloseRequest(e -> stopApplication());
            System.out.println("✅ Step 8: Stage configured successfully");

            // Step 8: Show Stage
            System.out.println("Step 9: Showing Stage...");
            stage.show();
            System.out.println("✅ Step 9: Stage shown successfully");

            System.out.println("=== APPLICATION START COMPLETE ===");

        } catch (Exception e) {
            System.err.println("❌ CRITICAL ERROR during FXML loading or scene creation");
            System.err.println("Error type: " + e.getClass().getSimpleName());
            System.err.println("Error message: " + e.getMessage());
            e.printStackTrace();

            showFallbackUI(stage, "Application Error",
                    "Failed to load the login interface.\n\n" +
                            "Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void showFallbackUI(Stage stage, String title, String message) {
        System.out.println("Showing fallback UI due to error: " + title);

        try {
            VBox fallbackRoot = new VBox(20);
            fallbackRoot.setStyle("-fx-padding: 50; -fx-alignment: center; -fx-background-color: #f0f0f0;");

            Label titleLabel = new Label("GAWE - Application Error");
            titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #d32f2f;");

            Label messageLabel = new Label(message);
            messageLabel.setStyle("-fx-font-size: 14px; -fx-wrap-text: true; -fx-text-alignment: center;");
            messageLabel.setMaxWidth(600);

            Label instructionLabel = new Label("Please check the console for detailed error information.");
            instructionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

            fallbackRoot.getChildren().addAll(titleLabel, messageLabel, instructionLabel);

            Scene fallbackScene = new Scene(fallbackRoot, 800, 600);
            stage.setTitle("GAWE - Error");
            stage.setScene(fallbackScene);
            stage.show();

            System.out.println("✅ Fallback UI displayed successfully");

        } catch (Exception e) {
            System.err.println("❌ CRITICAL: Even fallback UI failed to load");
            e.printStackTrace();
            Platform.exit();
        }
    }

    private void stopApplication() {
        System.out.println("=== STOPPING APPLICATION ===");
        try {
            if (dataStore != null) {
                dataStore.close();
                System.out.println("✅ DataStore closed");
            }
            if (server != null) {
                server.stop();
                System.out.println("✅ Server stopped");
            }
        } catch (Exception ex) {
            System.err.println("❌ Error stopping application: " + ex.getMessage());
        }
        System.out.println("=== APPLICATION STOPPED ===");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    @Override
    public void stop() {
        stopApplication();
    }

    public static void main(String[] args) {
        System.out.println("🚀 Starting GAWE Employee Management System...");
        System.out.println("Database: MySQL");
        System.out.println("JavaFX Version: " + System.getProperty("javafx.runtime.version", "Unknown"));
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("Working Directory: " + System.getProperty("user.dir"));
        System.out.println("");

        // Check JavaFX availability
        try {
            Class.forName("javafx.application.Application");
            System.out.println("✅ JavaFX classes are available");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ JavaFX classes not found - check module path");
        }

        launch(args);
    }
}
