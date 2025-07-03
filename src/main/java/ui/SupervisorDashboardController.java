package ui;

import app.HelloApplication;
import data.MySQLDataStore;
import models.*;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SupervisorDashboardController {

    @FXML
    private BorderPane rootPane;
    @FXML
    private Label userWelcomeLabel;
    @FXML
    private VBox navButtonContainer;
    @FXML
    private StackPane contentArea;

    private Employee supervisor;
    private MySQLDataStore dataStore;
    private Stage stage;

    private DecimalFormat df = new DecimalFormat("#.##");
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

    private static final Logger logger = Logger.getLogger(SupervisorDashboardController.class.getName());

    public void setSupervisor(Employee supervisor) {
        this.supervisor = supervisor;
        if (userWelcomeLabel != null) {
            userWelcomeLabel.setText("Welcome, " + supervisor.getNama() + " (" + supervisor.getDivisi() + ")");
        }

        // Initialize content after supervisor is set
        initializeContent();
    }

    public void setDataStore(MySQLDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        if (supervisor != null) {
            this.stage.setTitle("GAWE - Supervisor Dashboard - " + supervisor.getNama());
        }
        this.stage.setOnCloseRequest(e -> {
            stopApplication();
        });
    }

    @FXML
    public void initialize() {
        // Only do basic FXML initialization here
        // Don't call methods that need supervisor or dataStore
        populateNavigationButtons();

        // Content will be initialized when supervisor and dataStore are set
    }

    private void initializeContent() {
        // Only initialize content if both supervisor and dataStore are available
        if (supervisor != null && dataStore != null) {
            // Update welcome label if it wasn't set before
            if (userWelcomeLabel != null) {
                userWelcomeLabel.setText("Welcome, " + supervisor.getNama() + " (" + supervisor.getDivisi() + ")");
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
                    createNavButton("✅ Leave Approvals", this::showLeaveApprovalsContent),
                    createNavButton("🏖️ My Leave Requests", this::showMyLeaveRequests),
                    createNavButton("👥 Team Management", this::showTeamManagementContent),
                    createNavButton("⭐ Monthly Evaluation", this::showMonthlyEvaluationContent),
                    createNavButton("📄 Upload Report", this::showUploadReportContent),
                    createNavButton("📈 Performance Analytics", this::showPerformanceAnalyticsContent),
                    createNavButton("💰 Salary Management", this::showSalaryManagementContent),
                    createNavButton("📋 All History", this::showAllHistoryContent)
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
        if (supervisor == null || dataStore == null || contentArea == null) {
            logger.warning("Cannot show dashboard content - missing required objects");
            return;
        }

        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Supervisor Dashboard - " + supervisor.getDivisi() + " Division");
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
            if (supervisor == null || dataStore == null) {
                return false;
            }
            List<Attendance> todayAttendance = dataStore.getTodayAttendance(supervisor.getId());
            return !todayAttendance.isEmpty();
        } catch (Exception e) {
            logger.severe("Error checking today's attendance: " + e.getMessage());
            return false;
        }
    }

    private boolean hasCompletedAttendanceToday() {
        try {
            if (supervisor == null || dataStore == null) {
                return false;
            }
            List<Attendance> todayAttendance = dataStore.getTodayAttendance(supervisor.getId());
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

        boolean success = dataStore.saveAttendance(supervisor.getId(), new Date(), timeStr, null, "hadir");
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

        boolean success = dataStore.updateAttendanceClockOut(supervisor.getId(), timeStr);
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

        if (dataStore == null || supervisor == null) {
            return statsContainer;
        }

        List<Employee> teamMembers = dataStore.getEmployeesByDivision(supervisor.getDivisi()).stream()
                .filter(emp -> emp.getRole().equalsIgnoreCase("pegawai"))
                .toList();

        VBox teamSizeCard = createStatsCard("Team Size", String.valueOf(teamMembers.size()), "👥", "#3498db");

        double avgKpi = teamMembers.stream()
                .mapToDouble(Employee::getKpiScore)
                .average()
                .orElse(0.0);
        VBox avgKpiCard = createStatsCard("Avg KPI", String.format("%.1f%%", avgKpi), "📊", "#2ecc71");

        long atRiskCount = teamMembers.stream()
                .filter(Employee::isLayoffRisk)
                .count();
        VBox atRiskCard = createStatsCard("At Risk", String.valueOf(atRiskCount), "⚠️", "#e74c3c");

        // Refresh supervisor object to get updated leave balance if any changes occurred
        Employee refreshedSupervisor = dataStore.authenticateUser(supervisor.getId(), supervisor.getPassword());
        int supervisorLeaveDays = (refreshedSupervisor != null) ? refreshedSupervisor.getSisaCuti() : supervisor.getSisaCuti();
        VBox leaveCard = createStatsCard("My Leave Days", String.valueOf(supervisorLeaveDays), "🏖️", "#9b59b6");

        statsContainer.getChildren().addAll(teamSizeCard, avgKpiCard, atRiskCard, leaveCard);
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

        if (supervisor != null && dataStore != null) {
            List<LeaveRequest> pendingApprovals = dataStore.getLeaveRequestsForApproval(supervisor.getId());
            ObservableList<String> activities = FXCollections.observableArrayList(
                    "📊 Dashboard accessed - just now",
                    "📄 Ready to upload monthly report for " + supervisor.getDivisi(),
                    "⭐ Team evaluations pending",
                    "✅ " + pendingApprovals.size() + " leave requests pending approval",
                    "👥 Managing " + supervisor.getDivisi() + " team"
            );
            activitiesList.setItems(activities);
        }

        section.getChildren().addAll(sectionTitle, activitiesList);
        return section;
    }

    // Personal Features
    private void showMyAttendance() {
        if (supervisor == null || dataStore == null || contentArea == null) {
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

        if (dataStore != null && supervisor != null) {
            List<Attendance> myAttendance = dataStore.getAttendanceByEmployee(supervisor.getId());
            table.setItems(FXCollections.observableArrayList(myAttendance));
        }
        table.setPrefHeight(400);

        return table;
    }

    private void showMyMeetings() {
        if (supervisor == null || dataStore == null || contentArea == null) {
            return;
        }

        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("My Meetings");
        title.getStyleClass().add("content-title");

        Button newMeetingBtn = new Button("➕ Schedule New Meeting");
        newMeetingBtn.getStyleClass().add("action-button-green");
        newMeetingBtn.setOnAction(e -> showNewMeetingDialog());

        TableView<Meeting> meetingsTable = createMyMeetingsTable();

        content.getChildren().addAll(title, newMeetingBtn, meetingsTable);
        contentArea.getChildren().add(content);
    }

    private TableView<Meeting> createMyMeetingsTable() {
        TableView<Meeting> table = new TableView<>();
        table.getStyleClass().add("data-table");

        TableColumn<Meeting, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Meeting, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getTanggal())));

        TableColumn<Meeting, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getWaktuMulai() + " - " + cellData.getValue().getWaktuSelesai()));

        TableColumn<Meeting, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(new PropertyValueFactory<>("lokasi"));

        table.getColumns().addAll(titleCol, dateCol, timeCol, locationCol);

        if (dataStore != null && supervisor != null) {
            List<Meeting> myMeetings = dataStore.getMeetingsByEmployee(supervisor.getId());
            table.setItems(FXCollections.observableArrayList(myMeetings));
        }
        table.setPrefHeight(400);

        return table;
    }

    private void showNewMeetingDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Schedule New Meeting");
        dialog.setHeaderText("Create a new meeting");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        TextField titleField = new TextField();
        titleField.setPromptText("Meeting title...");

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Meeting description...");
        descriptionArea.setPrefRowCount(3);

        DatePicker datePicker = new DatePicker();
        datePicker.setValue(LocalDate.now().plusDays(1));

        TextField startTimeField = new TextField();
        startTimeField.setPromptText("Start time (HH:MM)");

        TextField endTimeField = new TextField();
        endTimeField.setPromptText("End time (HH:MM)");

        TextField locationField = new TextField();
        locationField.setPromptText("Meeting location...");

        content.getChildren().addAll(
                new Label("Title:"), titleField,
                new Label("Description:"), descriptionArea,
                new Label("Date:"), datePicker,
                new Label("Start Time:"), startTimeField,
                new Label("End Time:"), endTimeField,
                new Label("Location:"), locationField
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                if (!titleField.getText().isEmpty() && datePicker.getValue() != null) {
                    Date meetingDate = java.sql.Date.valueOf(datePicker.getValue());
                    List<String> participants = dataStore.getEmployeesByDivision(supervisor.getDivisi()).stream()
                            .filter(emp -> emp.getRole().equals("pegawai"))
                            .map(Employee::getId)
                            .collect(Collectors.toList());

                    boolean success = dataStore.saveMeeting(
                            titleField.getText(),
                            descriptionArea.getText(),
                            meetingDate,
                            startTimeField.getText(),
                            endTimeField.getText(),
                            locationField.getText(),
                            supervisor.getId(),
                            participants
                    );

                    if (success) {
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Meeting scheduled successfully!");
                        showMyMeetings();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to schedule meeting.");
                    }
                } else {
                    showAlert(Alert.AlertType.WARNING, "Invalid Input", "Please fill in required fields.");
                }
            }
        });
    }

    private void showMyLeaveRequests() {
        if (supervisor == null || dataStore == null || contentArea == null) {
            return;
        }

        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("My Leave Requests");
        title.getStyleClass().add("content-title");

        Button newRequestBtn = new Button("➕ New Leave Request");
        newRequestBtn.getStyleClass().add("action-button-green");
        newRequestBtn.setOnAction(e -> showLeaveRequestDialog());

        TableView<LeaveRequest> leaveTable = createMyLeaveRequestsTable();

        content.getChildren().addAll(title, newRequestBtn, leaveTable);
        contentArea.getChildren().add(content);
    }

    private TableView<LeaveRequest> createMyLeaveRequestsTable() {
        TableView<LeaveRequest> table = new TableView<>();
        table.getStyleClass().add("data-table");

        TableColumn<LeaveRequest, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("leaveType"));

        TableColumn<LeaveRequest, String> startDateCol = new TableColumn<>("Start Date");
        startDateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getStartDate())));

        TableColumn<LeaveRequest, String> endDateCol = new TableColumn<>("End Date");
        endDateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getEndDate())));

        TableColumn<LeaveRequest, Integer> daysCol = new TableColumn<>("Days");
        daysCol.setCellValueFactory(new PropertyValueFactory<>("totalDays"));

        TableColumn<LeaveRequest, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<LeaveRequest, String> notesCol = new TableColumn<>("Approval Notes");
        notesCol.setCellValueFactory(new PropertyValueFactory<>("approverNotes"));

        table.getColumns().addAll(typeCol, startDateCol, endDateCol, daysCol, statusCol, notesCol);

        if (dataStore != null && supervisor != null) {
            List<LeaveRequest> myLeaveRequests = dataStore.getLeaveRequestsByEmployee(supervisor.getId());
            table.setItems(FXCollections.observableArrayList(myLeaveRequests));
        }
        table.setPrefHeight(400);

        return table;
    }

    private void showLeaveRequestDialog() {
        if (supervisor == null || dataStore == null) {
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

                    boolean success = dataStore.saveLeaveRequest(supervisor.getId(), leaveTypeCombo.getValue(),
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

    // Supervisor-specific features
    private void showLeaveApprovalsContent() {
        if (supervisor == null || dataStore == null || contentArea == null) {
            return;
        }

        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Leave Request Approvals");
        title.getStyleClass().add("content-title");

        TableView<LeaveRequest> approvalTable = createLeaveApprovalTable();

        content.getChildren().addAll(title, approvalTable);
        contentArea.getChildren().add(content);
    }

    private TableView<LeaveRequest> createLeaveApprovalTable() {
        TableView<LeaveRequest> table = new TableView<>();
        table.getStyleClass().add("data-table");

        TableColumn<LeaveRequest, String> employeeCol = new TableColumn<>("Employee");
        employeeCol.setCellValueFactory(cellData -> {
            Employee emp = dataStore.getEmployeeById(cellData.getValue().getEmployeeId());
            return new javafx.beans.property.SimpleStringProperty(emp != null ? emp.getNama() : "Unknown");
        });

        TableColumn<LeaveRequest, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("leaveType"));

        TableColumn<LeaveRequest, String> startDateCol = new TableColumn<>("Start Date");
        startDateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getStartDate())));

        TableColumn<LeaveRequest, String> endDateCol = new TableColumn<>("End Date");
        endDateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getEndDate())));

        TableColumn<LeaveRequest, Integer> daysCol = new TableColumn<>("Days");
        daysCol.setCellValueFactory(new PropertyValueFactory<>("totalDays"));

        TableColumn<LeaveRequest, String> reasonCol = new TableColumn<>("Reason");
        reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));

        TableColumn<LeaveRequest, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<LeaveRequest, Void>() {
            private final Button approveBtn = new Button("✅ Approve");
            private final Button rejectBtn = new Button("❌ Reject");
            private final HBox actionBox = new HBox(5, approveBtn, rejectBtn);

            {
                approveBtn.getStyleClass().add("action-button-small-green");
                rejectBtn.getStyleClass().add("action-button-small-red");

                approveBtn.setOnAction(e -> {
                    LeaveRequest request = getTableView().getItems().get(getIndex());
                    showLeaveApprovalDialog(request, true);
                });

                rejectBtn.setOnAction(e -> {
                    LeaveRequest request = getTableView().getItems().get(getIndex());
                    showLeaveApprovalDialog(request, false);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actionBox);
            }
        });

        table.getColumns().addAll(employeeCol, typeCol, startDateCol, endDateCol, daysCol, reasonCol, actionCol);

        if (dataStore != null && supervisor != null) {
            List<LeaveRequest> pendingRequests = dataStore.getLeaveRequestsForApproval(supervisor.getId());
            table.setItems(FXCollections.observableArrayList(pendingRequests));
        }
        table.setPrefHeight(400);

        return table;
    }

    private void showLeaveApprovalDialog(LeaveRequest request, boolean isApproval) {
        LeaveRequestApprovalDialog dialog = new LeaveRequestApprovalDialog(request, supervisor, dataStore, isApproval);
        dialog.showAndWait().ifPresent(result -> {
            ButtonType approveBtn = new ButtonType("Approve", ButtonBar.ButtonData.OK_DONE);
            ButtonType rejectBtn = new ButtonType("Reject", ButtonBar.ButtonData.OTHER);

            if (result == approveBtn) {
                boolean success = dataStore.approveLeaveRequest(request.getId(), supervisor.getId(), dialog.getNotes());
                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Leave request approved successfully!");
                    showLeaveApprovalsContent(); // Refresh
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to approve leave request.");
                }
            } else if (result == rejectBtn) {
                boolean success = dataStore.rejectLeaveRequest(request.getId(), supervisor.getId(), dialog.getNotes());
                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Leave request rejected.");
                    showLeaveApprovalsContent(); // Refresh
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to reject leave request.");
                }
            }
        });
    }

    private void showTeamManagementContent() {
        if (supervisor == null || dataStore == null || contentArea == null) {
            return;
        }

        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Team Management - " + supervisor.getDivisi() + " Division");
        title.getStyleClass().add("content-title");

        TableView<Employee> teamTable = createTeamTable();

        content.getChildren().addAll(title, teamTable);
        contentArea.getChildren().add(content);
    }

    private TableView<Employee> createTeamTable() {
        TableView<Employee> table = new TableView<>();
        table.getStyleClass().add("data-table");

        TableColumn<Employee, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Employee, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("nama"));

        TableColumn<Employee, String> positionCol = new TableColumn<>("Position");
        positionCol.setCellValueFactory(new PropertyValueFactory<>("jabatan"));

        TableColumn<Employee, String> kpiCol = new TableColumn<>("KPI Score");
        kpiCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getKpiScore()) + "%"));

        TableColumn<Employee, String> ratingCol = new TableColumn<>("Supervisor Rating");
        ratingCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getSupervisorRating()) + "%"));

        TableColumn<Employee, String> riskCol = new TableColumn<>("At Risk");
        riskCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().isLayoffRisk() ? "⚠️ Yes" : "✅ No"));

        table.getColumns().addAll(idCol, nameCol, positionCol, kpiCol, ratingCol, riskCol);

        if (dataStore != null && supervisor != null) {
            List<Employee> teamMembers = dataStore.getEmployeesByDivision(supervisor.getDivisi()).stream()
                    .filter(emp -> emp.getRole().equals("pegawai"))
                    .collect(Collectors.toList());
            table.setItems(FXCollections.observableArrayList(teamMembers));
        }
        table.setPrefHeight(400);

        return table;
    }

    private void showMonthlyEvaluationContent() {
        if (supervisor == null || dataStore == null || contentArea == null) {
            return;
        }

        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Monthly Employee Evaluation");
        title.getStyleClass().add("content-title");

        VBox evaluationForm = createMonthlyEvaluationForm();

        content.getChildren().addAll(title, evaluationForm);
        contentArea.getChildren().add(content);
    }

    private VBox createMonthlyEvaluationForm() {
        VBox form = new VBox(20);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(30));
        form.getStyleClass().add("kpi-set-form");

        Label formTitle = new Label("Evaluate Team Member");
        formTitle.getStyleClass().add("form-title");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(20);
        formGrid.setVgap(15);
        formGrid.setAlignment(Pos.CENTER);

        ComboBox<Employee> employeeCombo = new ComboBox<>();
        List<Employee> teamMembers = dataStore.getEmployeesByDivision(supervisor.getDivisi()).stream()
                .filter(emp -> emp.getRole().equals("pegawai"))
                .collect(Collectors.toList());
        employeeCombo.getItems().addAll(teamMembers);
        employeeCombo.setConverter(new javafx.util.StringConverter<Employee>() {
            @Override
            public String toString(Employee employee) {
                return employee != null ? employee.getNama() + " (" + employee.getId() + ")" : "";
            }

            @Override
            public Employee fromString(String string) {
                return null;
            }
        });

        ComboBox<String> monthCombo = new ComboBox<>();
        monthCombo.getItems().addAll("January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December");
        monthCombo.setValue("December");

        ComboBox<Integer> yearCombo = new ComboBox<>();
        for (int year = 2020; year <= 2030; year++) {
            yearCombo.getItems().add(year);
        }
        yearCombo.setValue(LocalDate.now().getYear());

        Slider punctualitySlider = new Slider(0, 100, 75);
        punctualitySlider.setShowTickLabels(true);
        punctualitySlider.setShowTickMarks(true);
        punctualitySlider.setMajorTickUnit(25);

        Label punctualityValue = new Label("75.0");
        punctualitySlider.valueProperty().addListener((obs, oldVal, newVal) ->
                punctualityValue.setText(String.format("%.1f", newVal.doubleValue())));

        Slider attendanceSlider = new Slider(0, 100, 75);
        attendanceSlider.setShowTickLabels(true);
        attendanceSlider.setShowTickMarks(true);
        attendanceSlider.setMajorTickUnit(25);

        Label attendanceValue = new Label("75.0");
        attendanceSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                attendanceValue.setText(String.format("%.1f", newVal.doubleValue())));

        Slider productivitySlider = new Slider(0, 100, 75);
        productivitySlider.setShowTickLabels(true);
        productivitySlider.setShowTickMarks(true);
        productivitySlider.setMajorTickUnit(25);

        Label productivityValue = new Label("75.0");
        productivitySlider.valueProperty().addListener((obs, oldVal, newVal) ->
                productivityValue.setText(String.format("%.1f", newVal.doubleValue())));

        TextArea commentsArea = new TextArea();
        commentsArea.setPromptText("Evaluation comments...");
        commentsArea.setPrefRowCount(3);

        int row = 0;
        formGrid.add(new Label("Employee:"), 0, row);
        formGrid.add(employeeCombo, 1, row++);
        formGrid.add(new Label("Month:"), 0, row);
        formGrid.add(monthCombo, 1, row++);
        formGrid.add(new Label("Year:"), 0, row);
        formGrid.add(yearCombo, 1, row++);
        formGrid.add(new Label("Punctuality Score:"), 0, row);
        formGrid.add(punctualitySlider, 1, row);
        formGrid.add(punctualityValue, 2, row++);
        formGrid.add(new Label("Attendance Score:"), 0, row);
        formGrid.add(attendanceSlider, 1, row);
        formGrid.add(attendanceValue, 2, row++);
        formGrid.add(new Label("Productivity Score:"), 0, row);
        formGrid.add(productivitySlider, 1, row);
        formGrid.add(productivityValue, 2, row++);
        formGrid.add(new Label("Comments:"), 0, row);
        formGrid.add(commentsArea, 1, row, 2, 1);

        Button submitBtn = new Button("Submit Evaluation");
        submitBtn.getStyleClass().add("action-button-green");

        submitBtn.setOnAction(e -> {
            if (employeeCombo.getValue() != null) {
                Employee selectedEmployee = employeeCombo.getValue();
                int month = monthCombo.getSelectionModel().getSelectedIndex() + 1;
                int year = yearCombo.getValue();

                // Check if evaluation already exists
                if (dataStore.hasMonthlyEvaluation(selectedEmployee.getId(), month, year)) {
                    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmAlert.setTitle("Evaluation Exists");
                    confirmAlert.setContentText("Monthly evaluation for this employee already exists. Do you want to update it?");
                    if (confirmAlert.showAndWait().get() != ButtonType.OK) {
                        return;
                    }
                }

                double punctuality = punctualitySlider.getValue();
                double attendance = attendanceSlider.getValue();
                double productivity = productivitySlider.getValue();
                double overall = (punctuality + attendance + productivity) / 3;

                boolean success = dataStore.saveMonthlyEmployeeEvaluation(
                        selectedEmployee.getId(),
                        supervisor.getId(),
                        month,
                        year,
                        punctuality,
                        attendance,
                        productivity,
                        overall,
                        commentsArea.getText()
                );

                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Monthly evaluation saved successfully!");
                    employeeCombo.setValue(null);
                    punctualitySlider.setValue(75);
                    attendanceSlider.setValue(75);
                    productivitySlider.setValue(75);
                    commentsArea.clear();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to save evaluation.");
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "Warning", "Please select an employee.");
            }
        });

        form.getChildren().addAll(formTitle, formGrid, submitBtn);
        return form;
    }

    private void showUploadReportContent() {
        if (supervisor == null || dataStore == null || contentArea == null) {
            return;
        }

        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Upload Monthly Report");
        title.getStyleClass().add("content-title");

        VBox uploadForm = createReportUploadForm();

        content.getChildren().addAll(title, uploadForm);
        contentArea.getChildren().add(content);
    }

    private VBox createReportUploadForm() {
        VBox form = new VBox(20);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(30));
        form.getStyleClass().add("kpi-set-form");

        Label formTitle = new Label("Upload Division Report");
        formTitle.getStyleClass().add("form-title");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(20);
        formGrid.setVgap(15);
        formGrid.setAlignment(Pos.CENTER);

        Label divisionLabel = new Label("Division: " + supervisor.getDivisi());
        divisionLabel.setStyle("-fx-font-weight: bold;");

        ComboBox<String> monthCombo = new ComboBox<>();
        monthCombo.getItems().addAll("January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December");
        monthCombo.setValue("December");

        ComboBox<Integer> yearCombo = new ComboBox<>();
        for (int year = 2020; year <= 2030; year++) {
            yearCombo.getItems().add(year);
        }
        yearCombo.setValue(LocalDate.now().getYear());

        TextField filePathField = new TextField();
        filePathField.setPromptText("Select report file...");
        filePathField.setEditable(false);

        Button browseBtn = new Button("Browse");
        browseBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Report File");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                    new FileChooser.ExtensionFilter("Word Files", "*.docx", "*.doc"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                filePathField.setText(selectedFile.getAbsolutePath());
            }
        });

        int row = 0;
        formGrid.add(new Label("Division:"), 0, row);
        formGrid.add(divisionLabel, 1, row++);
        formGrid.add(new Label("Month:"), 0, row);
        formGrid.add(monthCombo, 1, row++);
        formGrid.add(new Label("Year:"), 0, row);
        formGrid.add(yearCombo, 1, row++);
        formGrid.add(new Label("Report File:"), 0, row);
        HBox fileBox = new HBox(10, filePathField, browseBtn);
        formGrid.add(fileBox, 1, row);

        Button uploadBtn = new Button("Upload Report");
        uploadBtn.getStyleClass().add("action-button-green");

        uploadBtn.setOnAction(e -> {
            if (!filePathField.getText().isEmpty()) {
                int month = monthCombo.getSelectionModel().getSelectedIndex() + 1;
                int year = yearCombo.getValue();

                boolean success = dataStore.saveReport(
                        supervisor.getId(),
                        supervisor.getDivisi(),
                        month,
                        year,
                        filePathField.getText()
                );

                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Report uploaded successfully!");
                    filePathField.clear();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to upload report.");
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "Warning", "Please select a report file.");
            }
        });

        form.getChildren().addAll(formTitle, formGrid, uploadBtn);
        return form;
    }

    // Placeholder methods for remaining features
    private void showPerformanceAnalyticsContent() {
        if (supervisor == null || dataStore == null || contentArea == null) {
            return;
        }

        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Performance Analytics - " + supervisor.getDivisi());
        title.getStyleClass().add("content-title");

        // Team Performance Overview
        HBox performanceCards = createPerformanceOverviewCards();

        // Performance Trend Chart (placeholder)
        Label chartPlaceholder = new Label("📊 Performance trends and analytics will be displayed here");
        chartPlaceholder.setStyle("-fx-font-size: 16px; -fx-padding: 50;");

        content.getChildren().addAll(title, performanceCards, chartPlaceholder);
        contentArea.getChildren().add(content);
    }

    private HBox createPerformanceOverviewCards() {
        HBox cardsContainer = new HBox(20);
        cardsContainer.setAlignment(Pos.CENTER);

        if (dataStore != null && supervisor != null) {
            List<Employee> teamMembers = dataStore.getEmployeesByDivision(supervisor.getDivisi()).stream()
                    .filter(emp -> emp.getRole().equals("pegawai"))
                    .collect(Collectors.toList());

            double avgKpi = teamMembers.stream().mapToDouble(Employee::getKpiScore).average().orElse(0.0);
            double avgRating = teamMembers.stream().mapToDouble(Employee::getSupervisorRating).average().orElse(0.0);
            long highPerformers = teamMembers.stream().filter(emp -> emp.getKpiScore() >= 80 && emp.getSupervisorRating() >= 80).count();

            VBox avgKpiCard = createStatsCard("Average KPI", String.format("%.1f%%", avgKpi), "📊", "#3498db");
            VBox avgRatingCard = createStatsCard("Average Rating", String.format("%.1f%%", avgRating), "⭐", "#2ecc71");
            VBox highPerformersCard = createStatsCard("High Performers", String.valueOf(highPerformers), "🏆", "#f39c12");

            cardsContainer.getChildren().addAll(avgKpiCard, avgRatingCard, highPerformersCard);
        }

        return cardsContainer;
    }

    private void showSalaryManagementContent() {
        if (supervisor == null || dataStore == null || contentArea == null) {
            return;
        }

        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Salary Management - " + supervisor.getDivisi());
        title.getStyleClass().add("content-title");

        Label placeholder = new Label("💰 Salary management features for team members will be available here");
        placeholder.setStyle("-fx-font-size: 16px; -fx-padding: 50;");

        content.getChildren().addAll(title, placeholder);
        contentArea.getChildren().add(content);
    }

    private void showAllHistoryContent() {
        if (supervisor == null || dataStore == null || contentArea == null) {
            return;
        }

        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("All History - " + supervisor.getDivisi());
        title.getStyleClass().add("content-title");

        TabPane historyTabs = new TabPane();
        historyTabs.getStyleClass().add("custom-tab-pane");

        // Reports History
        Tab reportsTab = new Tab("Reports", createReportsHistoryTable());

        // Evaluations History
        Tab evaluationsTab = new Tab("Evaluations", createEvaluationsHistoryTable());

        reportsTab.setClosable(false);
        evaluationsTab.setClosable(false);

        historyTabs.getTabs().addAll(reportsTab, evaluationsTab);

        content.getChildren().addAll(title, historyTabs);
        contentArea.getChildren().add(content);
    }

    private TableView<Report> createReportsHistoryTable() {
        TableView<Report> table = new TableView<>();
        table.getStyleClass().add("data-table");

        TableColumn<Report, String> monthCol = new TableColumn<>("Month");
        monthCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMonthName()));

        TableColumn<Report, Integer> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(new PropertyValueFactory<>("tahun"));

        TableColumn<Report, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<Report, String> uploadDateCol = new TableColumn<>("Upload Date");
        uploadDateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getUploadDate())));

        TableColumn<Report, String> notesCol = new TableColumn<>("Manager Notes");
        notesCol.setCellValueFactory(new PropertyValueFactory<>("managerNotes"));

        table.getColumns().addAll(monthCol, yearCol, statusCol, uploadDateCol, notesCol);

        if (dataStore != null && supervisor != null) {
            List<Report> myReports = dataStore.getReportsByDivision(supervisor.getDivisi());
            table.setItems(FXCollections.observableArrayList(myReports));
        }
        table.setPrefHeight(350);

        return table;
    }

    private TableView<MySQLDataStore.MonthlyEvaluation> createEvaluationsHistoryTable() {
        TableView<MySQLDataStore.MonthlyEvaluation> table = new TableView<>();
        table.getStyleClass().add("data-table");

        TableColumn<MySQLDataStore.MonthlyEvaluation, String> employeeCol = new TableColumn<>("Employee");
        employeeCol.setCellValueFactory(cellData -> {
            Employee emp = dataStore.getEmployeeById(cellData.getValue().getEmployeeId());
            return new javafx.beans.property.SimpleStringProperty(emp != null ? emp.getNama() : "Unknown");
        });

        TableColumn<MySQLDataStore.MonthlyEvaluation, Integer> monthCol = new TableColumn<>("Month");
        monthCol.setCellValueFactory(new PropertyValueFactory<>("month"));

        TableColumn<MySQLDataStore.MonthlyEvaluation, Integer> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(new PropertyValueFactory<>("year"));

        TableColumn<MySQLDataStore.MonthlyEvaluation, String> overallCol = new TableColumn<>("Overall Rating");
        overallCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getOverallRating()) + "%"));

        TableColumn<MySQLDataStore.MonthlyEvaluation, String> dateCol = new TableColumn<>("Evaluation Date");
        dateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getEvaluationDate())));

        table.getColumns().addAll(employeeCol, monthCol, yearCol, overallCol, dateCol);

        if (dataStore != null && supervisor != null) {
            List<MySQLDataStore.MonthlyEvaluation> myEvaluations = dataStore.getMonthlyEvaluationsBySupervisor(supervisor.getId());
            table.setItems(FXCollections.observableArrayList(myEvaluations));
        }
        table.setPrefHeight(350);

        return table;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}