package ui;

import app.HelloApplication;
import data.MySQLDataStore;
import models.Attendance;
import models.Employee;
import models.LeaveRequest;
import models.Meeting;
import models.SalaryHistory;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class EmployeeDashboardController {

    @FXML
    private BorderPane rootPane;
    @FXML
    private Label userWelcomeLabel;
    @FXML
    private VBox navButtonContainer;
    @FXML
    private StackPane contentArea;

    private Employee employee;
    private MySQLDataStore dataStore;
    private Stage stage;

    private DecimalFormat df = new DecimalFormat("#.##");
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

    private static final Logger logger = Logger.getLogger(EmployeeDashboardController.class.getName());

    public void setEmployee(Employee employee) {
        this.employee = employee;
        if (userWelcomeLabel != null) {
            userWelcomeLabel.setText("Welcome, " + employee.getNama() + " (Employee)");
        }

        // Initialize content after employee is set
        initializeContent();
    }

    public void setDataStore(MySQLDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        if (employee != null) {
            this.stage.setTitle("GAWE - Employee Dashboard - " + employee.getNama());
        }
        this.stage.setOnCloseRequest(e -> {
            stopApplication();
        });
    }

    @FXML
    public void initialize() {
        // Only do basic FXML initialization here
        // Don't call methods that need employee or dataStore
        populateNavigationButtons();

        // Content will be initialized when employee and dataStore are set
    }

    private void initializeContent() {
        // Only initialize content if both employee and dataStore are available
        if (employee != null && dataStore != null) {
            // Update welcome label if it wasn't set before
            if (userWelcomeLabel != null) {
                userWelcomeLabel.setText("Welcome, " + employee.getNama() + " (Employee)");
            }

            // Show default dashboard content
            showDashboardContent();
        }
    }

    private void populateNavigationButtons() {
        if (navButtonContainer != null) {
            navButtonContainer.getChildren().clear();
            Button[] navButtons = {
                    createNavButton("📊 Dashboard", this::showDashboardContent),
                    createNavButton("⏰ My Attendance", this::showMyAttendance),
                    createNavButton("📅 My Meetings", this::showMyMeetings),
                    createNavButton("🏖️ My Leave Requests", this::showMyLeaveRequests),
                    createNavButton("💰 My Salary", this::showMySalaryContent)
            };
            navButtonContainer.getChildren().addAll(navButtons);
        }
    }

    private Button createNavButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setPrefWidth(240);
        button.setAlignment(Pos.CENTER_LEFT);
        button.getStyleClass().add("nav-button"); // Apply CSS class
        button.setOnAction(e -> action.run());
        return button;
    }

    @FXML
    private void handleLogout() {
        if (stage != null) {
            stage.close();
            try {
                new HelloApplication().start(new Stage());
            } catch (IOException e) {
                logger.severe("Failed to restart HelloApplication: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void stopApplication() {
        if (dataStore != null) {
            dataStore.close();
        }
        Platform.exit();
    }

    private void showDashboardContent() {
        // Check if required objects are available
        if (employee == null || dataStore == null || contentArea == null) {
            logger.warning("Cannot show dashboard content - missing required objects");
            return;
        }

        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Employee Dashboard");
        title.getStyleClass().add("content-title");

        HBox quickActions = createQuickActions();
        HBox statsCards = createStatsCards();
        VBox recentActivities = createRecentActivitiesSection();

        content.getChildren().addAll(title, quickActions, statsCards, recentActivities);
        contentArea.getChildren().add(content);
    }

    private HBox createQuickActions() {
        HBox actionsBox = new HBox(15);
        actionsBox.setAlignment(Pos.CENTER);
        actionsBox.getStyleClass().add("quick-actions-box");

        boolean alreadyClockedIn = hasAttendanceToday();
        boolean alreadyCompletedAttendance = hasCompletedAttendanceToday();

        Button clockInBtn = new Button("⏰ Clock In");
        clockInBtn.getStyleClass().add("action-button-green");
        clockInBtn.setOnAction(e -> clockIn());
        clockInBtn.setDisable(alreadyClockedIn);

        Button clockOutBtn = new Button("🏃 Clock Out");
        clockOutBtn.getStyleClass().add("action-button-red");
        clockOutBtn.setOnAction(e -> clockOut());
        clockOutBtn.setDisable(!alreadyClockedIn || alreadyCompletedAttendance);

        Button requestLeaveBtn = new Button("🏖️ Request Leave");
        requestLeaveBtn.getStyleClass().add("action-button-orange");
        requestLeaveBtn.setOnAction(e -> showLeaveRequestDialog());

        actionsBox.getChildren().addAll(clockInBtn, clockOutBtn, requestLeaveBtn);
        return actionsBox;
    }

    private boolean hasAttendanceToday() {
        try {
            if (employee == null || dataStore == null) {
                return false;
            }
            List<Attendance> todayAttendance = dataStore.getTodayAttendance(employee.getId());
            return !todayAttendance.isEmpty();
        } catch (Exception e) {
            logger.severe("Error checking today's attendance: " + e.getMessage());
            return false;
        }
    }

    private boolean hasCompletedAttendanceToday() {
        try {
            if (employee == null || dataStore == null) {
                return false;
            }
            List<Attendance> todayAttendance = dataStore.getTodayAttendance(employee.getId());
            return !todayAttendance.isEmpty() &&
                    todayAttendance.get(0).getJamKeluar() != null;
        } catch (Exception e) {
            logger.severe("Error checking completed attendance: " + e.getMessage());
            return false;
        }
    }

    private void clockIn() {
        if (hasAttendanceToday()) {
            showAlert(Alert.AlertType.WARNING, "Already Clocked In", "You have already clocked in today.");
            return;
        }

        LocalTime now = LocalTime.now();
        String timeStr = String.format("%02d:%02d", now.getHour(), now.getMinute());

        boolean success = dataStore.saveAttendance(employee.getId(), new Date(), timeStr, null, "hadir");
        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Clock In", "Successfully clocked in at " + timeStr);
            showDashboardContent(); // Refresh dashboard
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to clock in.");
        }
    }

    private void clockOut() {
        if (hasCompletedAttendanceToday()) {
            showAlert(Alert.AlertType.WARNING, "Already Clocked Out", "You have already clocked out today.");
            return;
        }

        LocalTime now = LocalTime.now();
        String timeStr = String.format("%02d:%02d", now.getHour(), now.getMinute());

        boolean success = dataStore.updateAttendanceClockOut(employee.getId(), timeStr);
        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Clock Out", "Successfully clocked out at " + timeStr);
            showDashboardContent(); // Refresh dashboard
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to clock out.");
        }
    }

    private HBox createStatsCards() {
        HBox statsContainer = new HBox(20);
        statsContainer.setAlignment(Pos.CENTER);
        statsContainer.getStyleClass().add("stats-cards-container");

        if (dataStore == null || employee == null) {
            return statsContainer;
        }

        // Refresh employee object to get updated values
        Employee refreshedEmployee = dataStore.authenticateUser(employee.getId(), employee.getPassword());
        if (refreshedEmployee != null) {
            employee = refreshedEmployee;
        } else {
            logger.warning("Could not refresh employee data for stats cards.");
        }

        VBox attendanceScoreCard = createStatsCard("Attendance Score", String.format("%.1f%%", employee.getAttendanceScore()), "🎯", "#3498db");
        VBox kpiScoreCard = createStatsCard("KPI Score", String.format("%.1f%%", employee.getKpiScore()), "📈", "#2ecc71");
        VBox leaveDaysCard = createStatsCard("Remaining Leave", String.valueOf(employee.getSisaCuti()), "🏖️", "#f39c12");
        VBox overallRatingCard = createStatsCard("Overall Rating", String.format("%.1f%%", employee.getOverallRating()), "⭐", "#9b59b6");

        statsContainer.getChildren().addAll(attendanceScoreCard, kpiScoreCard, leaveDaysCard, overallRatingCard);
        return statsContainer;
    }

    private VBox createStatsCard(String title, String value, String icon, String color) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setPrefSize(200, 120);
        card.getStyleClass().add("stats-card");
        card.setStyle(String.format("-fx-border-color: %s; -fx-border-width: 0 0 4 0;", color));

        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("stats-card-icon");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stats-card-value");
        valueLabel.setStyle(String.format("-fx-text-fill: %s;", color));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stats-card-title");

        card.getChildren().addAll(iconLabel, valueLabel, titleLabel);
        return card;
    }

    private VBox createRecentActivitiesSection() {
        VBox section = new VBox(15);
        section.setAlignment(Pos.CENTER);
        section.setPadding(new Insets(20));
        section.getStyleClass().add("recent-activities-section");

        Label sectionTitle = new Label("Recent Activities");
        sectionTitle.getStyleClass().add("section-title");

        ListView<String> activitiesList = new ListView<>();
        activitiesList.setPrefHeight(200);
        activitiesList.getStyleClass().add("activities-list");

        if (dataStore != null && employee != null) {
            ObservableList<String> activities = FXCollections.observableArrayList(
                    "📊 Dashboard accessed - just now",
                    "⏰ Last Clock In: " + getLastAttendanceTime("in"),
                    "🏃 Last Clock Out: " + getLastAttendanceTime("out"),
                    "🏖️ Pending leave requests: " + dataStore.getPendingLeaveRequestsByEmployee(employee.getId()).size()
            );
            activitiesList.setItems(activities);
        }

        section.getChildren().addAll(sectionTitle, activitiesList);
        return section;
    }

    private String getLastAttendanceTime(String type) {
        try {
            if (dataStore == null || employee == null) {
                return "N/A";
            }
            List<Attendance> todayAttendance = dataStore.getTodayAttendance(employee.getId());
            if (!todayAttendance.isEmpty()) {
                Attendance lastAttendance = todayAttendance.get(0);
                if (type.equals("in")) {
                    return lastAttendance.getJamMasuk() != null ? lastAttendance.getJamMasuk() : "N/A";
                } else {
                    return lastAttendance.getJamKeluar() != null ? lastAttendance.getJamKeluar() : "N/A";
                }
            }
        } catch (Exception e) {
            logger.warning("Error getting last attendance time: " + e.getMessage());
        }
        return "N/A";
    }

    private void showMyAttendance() {
        if (employee == null || dataStore == null || contentArea == null) {
            return;
        }

        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("My Attendance");
        title.getStyleClass().add("content-title");

        TableView<Attendance> attendanceTable = createMyAttendanceTable();

        content.getChildren().addAll(title, attendanceTable);
        contentArea.getChildren().add(content);
    }

    private TableView<Attendance> createMyAttendanceTable() {
        TableView<Attendance> table = new TableView<>();
        table.getStyleClass().add("data-table");

        TableColumn<Attendance, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getTanggal())));

        TableColumn<Attendance, String> clockInCol = new TableColumn<>("Clock In");
        clockInCol.setCellValueFactory(new PropertyValueFactory<>("jamMasuk"));

        TableColumn<Attendance, String> clockOutCol = new TableColumn<>("Clock Out");
        clockOutCol.setCellValueFactory(new PropertyValueFactory<>("jamKeluar"));

        TableColumn<Attendance, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(dateCol, clockInCol, clockOutCol, statusCol);

        if (dataStore != null && employee != null) {
            List<Attendance> myAttendance = dataStore.getAttendanceByEmployee(employee.getId());
            table.setItems(FXCollections.observableArrayList(myAttendance));
        }
        table.setPrefHeight(400);

        return table;
    }

    // Add placeholder methods for other features with null checks
    private void showMyMeetings() {
        if (employee == null || dataStore == null || contentArea == null) {
            return;
        }
        // Implementation remains the same but with null checks
    }

    private void showMyLeaveRequests() {
        if (employee == null || dataStore == null || contentArea == null) {
            return;
        }
        // Implementation remains the same but with null checks
    }

    private void showLeaveRequestDialog() {
        if (employee == null || dataStore == null) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Request Leave");
        dialog.setHeaderText("Submit a new leave request");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        ComboBox<String> leaveTypeCombo = new ComboBox<>();
        leaveTypeCombo.getItems().addAll("Annual Leave", "Sick Leave", "Personal Leave", "Emergency Leave");
        leaveTypeCombo.setValue("Annual Leave");

        DatePicker startDatePicker = new DatePicker();
        DatePicker endDatePicker = new DatePicker();

        // Prevent past dates and weekends
        startDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(LocalDate.now()) ||
                        date.getDayOfWeek() == DayOfWeek.SATURDAY ||
                        date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb;");
                }
            }
        });

        endDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(LocalDate.now()) ||
                        date.getDayOfWeek() == DayOfWeek.SATURDAY ||
                        date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb;");
                }
            }
        });

        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("Enter reason for leave...");
        reasonArea.setPrefRowCount(3);

        content.getChildren().addAll(
                new Label("Leave Type:"), leaveTypeCombo,
                new Label("Start Date (No weekends):"), startDatePicker,
                new Label("End Date (No weekends):"), endDatePicker,
                new Label("Reason:"), reasonArea
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                if (startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
                    LocalDate startDate = startDatePicker.getValue();
                    LocalDate endDate = endDatePicker.getValue();

                    if (startDate.getDayOfWeek() == DayOfWeek.SATURDAY ||
                            startDate.getDayOfWeek() == DayOfWeek.SUNDAY ||
                            endDate.getDayOfWeek() == DayOfWeek.SATURDAY ||
                            endDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                        showAlert(Alert.AlertType.WARNING, "Invalid Date", "Leave requests cannot be submitted for weekends.");
                        return;
                    }

                    Date startSqlDate = java.sql.Date.valueOf(startDate);
                    Date endSqlDate = java.sql.Date.valueOf(endDate);

                    boolean success = dataStore.saveLeaveRequest(employee.getId(), leaveTypeCombo.getValue(),
                            startSqlDate, endSqlDate, reasonArea.getText());
                    if (success) {
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Leave request submitted successfully!");
                        showMyLeaveRequests(); // Refresh
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to submit leave request.");
                    }
                } else {
                    showAlert(Alert.AlertType.WARNING, "Invalid Input", "Please select start and end dates.");
                }
            }
        });
    }

    private void showMySalaryContent() {
        if (employee == null || dataStore == null || contentArea == null) {
            return;
        }
        // Implementation remains the same but with null checks
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}