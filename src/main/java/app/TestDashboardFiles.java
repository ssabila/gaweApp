package app;

import java.net.URL;

public class TestDashboardFiles {
    public static void main(String[] args) {
        System.out.println("=== TESTING DASHBOARD FXML FILES ===");

        String[] fxmlFiles = {
                "/ui/ManagerDashboardView.fxml",
                "/ui/SupervisorDashboardView.fxml",
                "/ui/EmployeeDashboardView.fxml",
                "/ui/dashboard.css"
        };

        String[] controllerClasses = {
                "ui.ManagerDashboardController",
                "ui.SupervisorDashboardController",
                "ui.EmployeeDashboardController"
        };

        System.out.println("Checking FXML files...");
        for (String fxmlFile : fxmlFiles) {
            URL resource = TestDashboardFiles.class.getResource(fxmlFile);
            if (resource != null) {
                System.out.println("✅ Found: " + fxmlFile + " at " + resource);
            } else {
                System.err.println("❌ Missing: " + fxmlFile);
                System.err.println("   Expected location: src/main/resources" + fxmlFile);
            }
        }

        System.out.println("\nChecking Controller classes...");
        for (String className : controllerClasses) {
            try {
                Class.forName(className);
                System.out.println("✅ Found: " + className);
            } catch (ClassNotFoundException e) {
                System.err.println("❌ Missing: " + className);
                System.err.println("   Expected location: src/main/java/" + className.replace('.', '/') + ".java");
            }
        }

        System.out.println("\n=== TEST COMPLETE ===");
    }
}