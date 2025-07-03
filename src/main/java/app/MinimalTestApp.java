package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;

public class MinimalTestApp extends Application {

    @Override
    public void start(Stage stage) {
        System.out.println("=== MINIMAL TEST APP ===");

        try {
            // Test 1: Simple JavaFX UI (no FXML)
            System.out.println("Test 1: Creating simple UI without FXML...");
            VBox simpleRoot = new VBox(20);
            simpleRoot.setStyle("-fx-padding: 50; -fx-alignment: center; -fx-background-color: #f0f0f0;");

            Label titleLabel = new Label("GAWE - JavaFX Test");
            titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

            Label statusLabel = new Label("✅ JavaFX is working!");
            statusLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: green;");

            Button testFxmlButton = new Button("Test FXML Loading");
            testFxmlButton.setStyle("-fx-font-size: 14px; -fx-padding: 10;");
            testFxmlButton.setOnAction(e -> testFxmlLoading(stage));

            simpleRoot.getChildren().addAll(titleLabel, statusLabel, testFxmlButton);

            Scene scene = new Scene(simpleRoot, 600, 400);
            stage.setTitle("GAWE - Minimal Test");
            stage.setScene(scene);
            stage.show();

            System.out.println("✅ Simple UI displayed successfully");

        } catch (Exception e) {
            System.err.println("❌ Even simple UI failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void testFxmlLoading(Stage stage) {
        System.out.println("=== TESTING FXML LOADING ===");

        try {
            // Test FXML loading
            System.out.println("Looking for LoginView.fxml...");
            URL fxmlLocation = getClass().getResource("/app/LoginView.fxml");

            if (fxmlLocation == null) {
                System.err.println("❌ FXML file not found at /app/LoginView.fxml");

                // Show error in UI
                VBox errorRoot = new VBox(20);
                errorRoot.setStyle("-fx-padding: 50; -fx-alignment: center; -fx-background-color: #ffebee;");

                Label errorLabel = new Label("❌ FXML File Not Found");
                errorLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #d32f2f;");

                Label pathLabel = new Label("Expected: src/main/resources/app/LoginView.fxml");
                pathLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

                Button backButton = new Button("Back to Simple UI");
                backButton.setOnAction(e -> start(stage));

                errorRoot.getChildren().addAll(errorLabel, pathLabel, backButton);
                Scene errorScene = new Scene(errorRoot, 600, 400);
                stage.setScene(errorScene);
                return;
            }

            System.out.println("✅ FXML file found: " + fxmlLocation);

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            BorderPane fxmlRoot = loader.load();

            System.out.println("✅ FXML loaded successfully");

            // Check controller
            Object controller = loader.getController();
            if (controller != null) {
                System.out.println("✅ Controller loaded: " + controller.getClass().getName());
            } else {
                System.err.println("⚠️ Controller is null");
            }

            Scene fxmlScene = new Scene(fxmlRoot, 800, 600);
            stage.setScene(fxmlScene);
            stage.setTitle("GAWE - FXML Test Success");

            System.out.println("✅ FXML UI displayed successfully");

        } catch (Exception e) {
            System.err.println("❌ FXML loading failed: " + e.getMessage());
            e.printStackTrace();

            // Show error details in UI
            VBox errorRoot = new VBox(20);
            errorRoot.setStyle("-fx-padding: 50; -fx-alignment: center; -fx-background-color: #ffebee;");

            Label errorLabel = new Label("❌ FXML Loading Failed");
            errorLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #d32f2f;");

            Label detailLabel = new Label(e.getClass().getSimpleName() + ": " + e.getMessage());
            detailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666; -fx-wrap-text: true;");
            detailLabel.setMaxWidth(500);

            Button backButton = new Button("Back to Simple UI");
            backButton.setOnAction(ev -> start(stage));

            errorRoot.getChildren().addAll(errorLabel, detailLabel, backButton);
            Scene errorScene = new Scene(errorRoot, 600, 400);
            stage.setScene(errorScene);
        }
    }

    public static void main(String[] args) {
        System.out.println("🧪 Running Minimal Test App...");
        System.out.println("This will test JavaFX and FXML loading without database dependencies");
        launch(args);
    }
}