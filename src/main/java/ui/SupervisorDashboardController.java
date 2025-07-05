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
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
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

    // TableView instances for refreshing
    private TableView<LeaveRequest> leaveApprovalsTable;
    private TableView<Employee> teamTable;
    private TableView<Report> reportHistoryTable;
    private TableView<MySQLDataStore.MonthlyEvaluation> evaluationsHistoryTable;

    private DecimalFormat df = new DecimalFormat("#.##");
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

    private static final Logger logger = Logger.getLogger(SupervisorDashboardController.class.getName());

    public void setSupervisor(Employee supervisor) {
        this.supervisor = supervisor;
        if (userWelcomeLabel != null) {
            userWelcomeLabel.setText("Welcome, " + supervisor.getNama() + " (" + supervisor.getDivisi() + ")");
        }
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
        this.stage.setOnCloseRequest(e -> stopApplication());
    }

    @FXML
    public void initialize() {
        populateNavigationButtons();
    }

    private void initializeContent() {
        if (supervisor != null && dataStore != null) {
            if (userWelcomeLabel != null) {
                userWelcomeLabel.setText("Welcome, " + supervisor.getNama() + " (" + supervisor.getDivisi() + ")");
            }
            showDashboardContent();
        }
    }

    private void setScrollableContent(Region contentNode) {
        contentArea.getChildren().clear();
        ScrollPane scrollPane = new ScrollPane(contentNode);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");
        contentArea.getChildren().add(scrollPane);
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
                    createNavButton("📋 All History", this::showAllHistoryContent),
                    createNavButton("👤 Edit Profile", this::showEditProfileDialog)
            };
            navButtonContainer.getChildren().addAll(navButtons);
        }
    }

    private Button createNavButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setPrefWidth(240);
        button.setAlignment(Pos.CENTER_LEFT);
        button.getStyleClass().add("nav-button");
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
        setScrollableContent(content);
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
            showDashboardContent();
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
            showDashboardContent();
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

        try {
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

            // Refresh supervisor object to get updated leave balance
            Employee refreshedSupervisor = dataStore.authenticateUser(supervisor.getId(), supervisor.getPassword());
            int supervisorLeaveDays = (refreshedSupervisor != null) ? refreshedSupervisor.getSisaCuti() : supervisor.getSisaCuti();
            VBox leaveCard = createStatsCard("My Leave Days", String.valueOf(supervisorLeaveDays), "🏖️", "#9b59b6");

            statsContainer.getChildren().addAll(teamSizeCard, avgKpiCard, atRiskCard, leaveCard);
        } catch (Exception e) {
            logger.severe("Error creating stats cards: " + e.getMessage());
        }

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
            try {
                List<LeaveRequest> pendingApprovals = dataStore.getLeaveRequestsForApproval(supervisor.getId());
                ObservableList<String> activities = FXCollections.observableArrayList(
                        "📊 Dashboard accessed - just now",
                        "📄 Ready to upload monthly report for " + supervisor.getDivisi(),
                        "⭐ Team evaluations pending",
                        "✅ " + pendingApprovals.size() + " leave requests pending approval",
                        "👥 Managing " + supervisor.getDivisi() + " team"
                );
                activitiesList.setItems(activities);
            } catch (Exception e) {
                logger.warning("Error loading recent activities: " + e.getMessage());
            }
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
        setScrollableContent(content);
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
            try {
                List<Attendance> myAttendance = dataStore.getAttendanceByEmployee(supervisor.getId());
                table.setItems(FXCollections.observableArrayList(myAttendance));
            } catch (Exception e) {
                logger.severe("Error loading attendance: " + e.getMessage());
            }
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
        setScrollableContent(content);
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
            try {
                List<Meeting> myMeetings = dataStore.getMeetingsByEmployee(supervisor.getId());
                table.setItems(FXCollections.observableArrayList(myMeetings));
            } catch (Exception e) {
                logger.severe("Error loading meetings: " + e.getMessage());
            }
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
        startTimeField.setText("09:00");

        TextField endTimeField = new TextField();
        endTimeField.setPromptText("End time (HH:MM)");
        endTimeField.setText("10:00");

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
                    try {
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
                    } catch (Exception e) {
                        logger.severe("Error scheduling meeting: " + e.getMessage());
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to schedule meeting: " + e.getMessage());
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
        setScrollableContent(content);
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
            try {
                List<LeaveRequest> myLeaveRequests = dataStore.getLeaveRequestsByEmployee(supervisor.getId());
                table.setItems(FXCollections.observableArrayList(myLeaveRequests));
            } catch (Exception e) {
                logger.severe("Error loading leave requests: " + e.getMessage());
            }
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

                    // FIX: Use java.util.Date instead of java.sql.Date
                    Date startUtilDate = java.util.Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    Date endUtilDate = java.util.Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

                    try {
                        boolean success = dataStore.saveLeaveRequest(supervisor.getId(), leaveTypeCombo.getValue(),
                                startUtilDate, endUtilDate, reasonArea.getText());
                        if (success) {
                            showAlert(Alert.AlertType.INFORMATION, "Success", "Leave request submitted successfully!");
                            showMyLeaveRequests(); // Refresh
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Error", "Failed to submit leave request.");
                        }
                    } catch (Exception e) {
                        logger.severe("Error submitting leave request: " + e.getMessage());
                        e.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to submit leave request: " + e.getMessage());
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

        if (leaveApprovalsTable == null) {
            leaveApprovalsTable = createLeaveApprovalTable();
        }

        content.getChildren().addAll(title, leaveApprovalsTable);
        setScrollableContent(content);
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

        refreshLeaveApprovalsTable(table);

        table.setPrefHeight(400);
        return table;
    }

    private void refreshLeaveApprovalsTable(TableView<LeaveRequest> table) {
        if (dataStore != null && supervisor != null) {
            try {
                List<LeaveRequest> pendingRequests = dataStore.getLeaveRequestsForApproval(supervisor.getId());
                table.setItems(FXCollections.observableArrayList(pendingRequests));
            } catch (Exception e) {
                logger.severe("Error loading leave requests for approval: " + e.getMessage());
            }
        }
    }

    private void showLeaveApprovalDialog(LeaveRequest request, boolean isApproval) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isApproval ? "Approve Leave Request" : "Reject Leave Request");

        Employee requestingEmployee = dataStore.getEmployeeById(request.getEmployeeId());
        String employeeName = requestingEmployee != null ? requestingEmployee.getNama() : "Unknown";

        dialog.setHeaderText((isApproval ? "Approve" : "Reject") + " leave request from " + employeeName);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Request summary
        VBox summaryBox = new VBox(5);
        summaryBox.getChildren().addAll(
                new Label("Employee: " + employeeName + " (" + request.getEmployeeId() + ")"),
                new Label("Leave Type: " + request.getLeaveType()),
                new Label("Period: " + sdf.format(request.getStartDate()) + " to " + sdf.format(request.getEndDate())),
                new Label("Total Days: " + request.getTotalDays()),
                new Label("Reason: " + request.getReason())
        );

        // Supervisor notes
        Label notesLabel = new Label("Supervisor Notes:");
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Enter your " + (isApproval ? "approval" : "rejection") + " notes...");
        notesArea.setPrefRowCount(4);

        content.getChildren().addAll(summaryBox, new Separator(), notesLabel, notesArea);

        dialog.getDialogPane().setContent(content);

        ButtonType actionButton = new ButtonType(isApproval ? "Approve" : "Reject", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(actionButton, cancelButton);

        dialog.showAndWait().ifPresent(result -> {
            if (result == actionButton) {
                String notes = notesArea.getText().trim();
                if (notes.isEmpty()) {
                    notes = isApproval ? "Approved by supervisor" : "Rejected by supervisor";
                }

                boolean success;
                if (isApproval) {
                    success = dataStore.approveLeaveRequest(request.getId(), supervisor.getId(), notes);
                } else {
                    success = dataStore.rejectLeaveRequest(request.getId(), supervisor.getId(), notes);
                }

                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Success",
                            "Leave request " + (isApproval ? "approved" : "rejected") + " successfully!");
                    showLeaveApprovalsContent(); // Refresh the view
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error",
                            "Failed to " + (isApproval ? "approve" : "reject") + " leave request.");
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

        Button addEmployeeBtn = new Button("Add Employee");
        addEmployeeBtn.setOnAction(e -> showAddEmployeeDialog());

        if (teamTable == null) {
            teamTable = createTeamTable();
        }

        content.getChildren().addAll(title, addEmployeeBtn, teamTable);
        setScrollableContent(content);
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

        TableColumn<Employee, String> salaryCol = new TableColumn<>("Current Salary");
        salaryCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.format("Rp %,.0f", cellData.getValue().calculateGajiBulanan())));

        table.getColumns().addAll(idCol, nameCol, positionCol, kpiCol, ratingCol, riskCol, salaryCol);

        refreshTeamTable(table);

        table.setPrefHeight(400);
        return table;
    }

    private void refreshTeamTable(TableView<Employee> table) {
        if (dataStore != null && supervisor != null) {
            try {
                List<Employee> teamMembers = dataStore.getEmployeesByDivision(supervisor.getDivisi()).stream()
                        .filter(emp -> emp.getRole().equals("pegawai"))
                        .collect(Collectors.toList());
                table.setItems(FXCollections.observableArrayList(teamMembers));
            } catch (Exception e) {
                logger.severe("Error loading team members: " + e.getMessage());
            }
        }
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

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("custom-tab-pane");

        Tab evaluateTab = new Tab("Evaluate Employee", createMonthlyEvaluationForm());
        Tab historyTab = new Tab("Evaluation History", createEvaluationHistoryTable());

        evaluateTab.setClosable(false);
        historyTab.setClosable(false);

        tabPane.getTabs().addAll(evaluateTab, historyTab);

        content.getChildren().addAll(title, tabPane);
        setScrollableContent(content);
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
        try {
            List<Employee> teamMembers = dataStore.getEmployeesByDivision(supervisor.getDivisi()).stream()
                    .filter(emp -> emp.getRole().equals("pegawai"))
                    .collect(Collectors.toList());
            employeeCombo.getItems().addAll(teamMembers);
        } catch (Exception e) {
            logger.severe("Error loading team members for evaluation: " + e.getMessage());
        }

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

                try {
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
                        refreshEvaluationHistoryTable();
                        refreshTeamTable(teamTable);
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to save evaluation.");
                    }
                } catch (Exception ex) {
                    logger.severe("Error saving evaluation: " + ex.getMessage());
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to save evaluation: " + ex.getMessage());
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "Warning", "Please select an employee.");
            }
        });

        form.getChildren().addAll(formTitle, formGrid, submitBtn);
        return form;
    }

    private TableView<MySQLDataStore.MonthlyEvaluation> createEvaluationHistoryTable() {
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

        TableColumn<MySQLDataStore.MonthlyEvaluation, String> punctualityCol = new TableColumn<>("Punctuality");
        punctualityCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getPunctualityScore()) + "%"));

        TableColumn<MySQLDataStore.MonthlyEvaluation, String> attendanceCol = new TableColumn<>("Attendance");
        attendanceCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getAttendanceScore()) + "%"));

        TableColumn<MySQLDataStore.MonthlyEvaluation, String> productivityCol = new TableColumn<>("Productivity");
        productivityCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getProductivityScore()) + "%"));

        TableColumn<MySQLDataStore.MonthlyEvaluation, String> overallCol = new TableColumn<>("Overall Rating");
        overallCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getOverallRating()) + "%"));

        table.getColumns().addAll(employeeCol, monthCol, yearCol, punctualityCol, attendanceCol, productivityCol, overallCol);

        if (evaluationsHistoryTable == null) {
            evaluationsHistoryTable = table;
        }

        refreshEvaluationHistoryTable(table);

        table.setPrefHeight(400);
        return table;
    }

    private void refreshEvaluationHistoryTable() {
        if (evaluationsHistoryTable != null) {
            refreshEvaluationHistoryTable(evaluationsHistoryTable);
        }
    }

    private void refreshEvaluationHistoryTable(TableView<MySQLDataStore.MonthlyEvaluation> table) {
        if (dataStore != null && supervisor != null) {
            try {
                List<MySQLDataStore.MonthlyEvaluation> evaluations = dataStore.getMonthlyEvaluationsBySupervisor(supervisor.getId());
                table.setItems(FXCollections.observableArrayList(evaluations));
            } catch (Exception e) {
                logger.severe("Error loading evaluation history: " + e.getMessage());
            }
        }
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

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("custom-tab-pane");

        Tab uploadTab = new Tab("Upload Report", createReportUploadForm());
        Tab historyTab = new Tab("Report History", createReportHistoryTable());

        uploadTab.setClosable(false);
        historyTab.setClosable(false);

        tabPane.getTabs().addAll(uploadTab, historyTab);

        content.getChildren().addAll(title, tabPane);
        setScrollableContent(content);
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
        filePathField.setPrefWidth(300);
        formGrid.add(fileBox, 1, row);

        Button uploadBtn = new Button("Upload Report");
        uploadBtn.getStyleClass().add("action-button-green");

        uploadBtn.setOnAction(e -> {
            if (!filePathField.getText().isEmpty()) {
                try {
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
                        refreshReportHistoryTable();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to upload report.");
                    }
                } catch (Exception ex) {
                    logger.severe("Error uploading report: " + ex.getMessage());
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to upload report: " + ex.getMessage());
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "Warning", "Please select a report file.");
            }
        });

        form.getChildren().addAll(formTitle, formGrid, uploadBtn);
        return form;
    }

    private TableView<Report> createReportHistoryTable() {
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

        if (reportHistoryTable == null) {
            reportHistoryTable = table;
        }

        refreshReportHistoryTable(table);

        table.setPrefHeight(400);
        return table;
    }

    private void refreshReportHistoryTable() {
        if (reportHistoryTable != null) {
            refreshReportHistoryTable(reportHistoryTable);
        }
    }

    private void refreshReportHistoryTable(TableView<Report> table) {
        if (dataStore != null && supervisor != null) {
            try {
                List<Report> myReports = dataStore.getReportsByDivision(supervisor.getDivisi());
                table.setItems(FXCollections.observableArrayList(myReports));
            } catch (Exception e) {
                logger.severe("Error loading report history: " + e.getMessage());
            }
        }
    }

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

        // Performance Details Table
        TableView<Employee> performanceTable = createPerformanceDetailsTable();

        content.getChildren().addAll(title, performanceCards, performanceTable);
        setScrollableContent(content);
    }

    private HBox createPerformanceOverviewCards() {
        HBox cardsContainer = new HBox(20);
        cardsContainer.setAlignment(Pos.CENTER);

        if (dataStore != null && supervisor != null) {
            try {
                List<Employee> teamMembers = dataStore.getEmployeesByDivision(supervisor.getDivisi()).stream()
                        .filter(emp -> emp.getRole().equals("pegawai"))
                        .collect(Collectors.toList());

                double avgKpi = teamMembers.stream().mapToDouble(Employee::getKpiScore).average().orElse(0.0);
                double avgRating = teamMembers.stream().mapToDouble(Employee::getSupervisorRating).average().orElse(0.0);
                long highPerformers = teamMembers.stream().filter(emp -> emp.getKpiScore() >= 80 && emp.getSupervisorRating() >= 80).count();
                long underPerformers = teamMembers.stream().filter(emp -> emp.getKpiScore() < 60 || emp.getSupervisorRating() < 60).count();

                VBox avgKpiCard = createStatsCard("Average KPI", String.format("%.1f%%", avgKpi), "📊", "#3498db");
                VBox avgRatingCard = createStatsCard("Average Rating", String.format("%.1f%%", avgRating), "⭐", "#2ecc71");
                VBox highPerformersCard = createStatsCard("High Performers", String.valueOf(highPerformers), "🏆", "#f39c12");
                VBox underPerformersCard = createStatsCard("Need Improvement", String.valueOf(underPerformers), "⚠️", "#e74c3c");

                cardsContainer.getChildren().addAll(avgKpiCard, avgRatingCard, highPerformersCard, underPerformersCard);
            } catch (Exception e) {
                logger.severe("Error creating performance overview cards: " + e.getMessage());
            }
        }

        return cardsContainer;
    }

    private TableView<Employee> createPerformanceDetailsTable() {
        TableView<Employee> table = new TableView<>();
        table.getStyleClass().add("data-table");

        TableColumn<Employee, String> nameCol = new TableColumn<>("Employee");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("nama"));

        TableColumn<Employee, String> kpiCol = new TableColumn<>("KPI Score");
        kpiCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getKpiScore()) + "%"));

        TableColumn<Employee, String> ratingCol = new TableColumn<>("Supervisor Rating");
        ratingCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getSupervisorRating()) + "%"));

        TableColumn<Employee, String> attendanceCol = new TableColumn<>("Attendance Score");
        attendanceCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getAttendanceScore()) + "%"));

        TableColumn<Employee, String> performanceCol = new TableColumn<>("Performance Level");
        performanceCol.setCellValueFactory(cellData -> {
            Employee emp = cellData.getValue();
            double avgScore = (emp.getKpiScore() + emp.getSupervisorRating()) / 2;
            String level;
            if (avgScore >= 90) level = "🏆 Excellent";
            else if (avgScore >= 80) level = "⭐ Good";
            else if (avgScore >= 70) level = "✅ Satisfactory";
            else if (avgScore >= 60) level = "⚠️ Needs Improvement";
            else level = "❌ Poor";
            return new javafx.beans.property.SimpleStringProperty(level);
        });

        TableColumn<Employee, String> salaryCol = new TableColumn<>("Current Salary");
        salaryCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.format("Rp %,.0f", cellData.getValue().calculateGajiBulanan())));

        table.getColumns().addAll(nameCol, kpiCol, ratingCol, attendanceCol, performanceCol, salaryCol);

        if (dataStore != null && supervisor != null) {
            try {
                List<Employee> teamMembers = dataStore.getEmployeesByDivision(supervisor.getDivisi()).stream()
                        .filter(emp -> emp.getRole().equals("pegawai"))
                        .collect(Collectors.toList());
                table.setItems(FXCollections.observableArrayList(teamMembers));
            } catch (Exception e) {
                logger.severe("Error loading performance details: " + e.getMessage());
            }
        }

        table.setPrefHeight(400);
        return table;
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

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("custom-tab-pane");

        Tab teamSalaryTab = new Tab("Team Salary Overview", createTeamSalaryTable());
        Tab salaryHistoryTab = new Tab("Salary History", createTeamSalaryHistoryTable());

        teamSalaryTab.setClosable(false);
        salaryHistoryTab.setClosable(false);

        tabPane.getTabs().addAll(teamSalaryTab, salaryHistoryTab);

        content.getChildren().addAll(title, tabPane);
        setScrollableContent(content);
    }

    private TableView<Employee> createTeamSalaryTable() {
        TableView<Employee> table = new TableView<>();
        table.getStyleClass().add("data-table");

        TableColumn<Employee, String> nameCol = new TableColumn<>("Employee");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("nama"));

        TableColumn<Employee, String> positionCol = new TableColumn<>("Position");
        positionCol.setCellValueFactory(new PropertyValueFactory<>("jabatan"));

        TableColumn<Employee, String> baseSalaryCol = new TableColumn<>("Base Salary");
        baseSalaryCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.format("Rp %,.0f", cellData.getValue().getGajiPokok())));

        TableColumn<Employee, String> kpiCol = new TableColumn<>("KPI Score");
        kpiCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getKpiScore()) + "%"));

        TableColumn<Employee, String> ratingCol = new TableColumn<>("Rating");
        ratingCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getSupervisorRating()) + "%"));

        TableColumn<Employee, String> currentSalaryCol = new TableColumn<>("Current Total Salary");
        currentSalaryCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.format("Rp %,.0f", cellData.getValue().calculateGajiBulanan())));

        table.getColumns().addAll(nameCol, positionCol, baseSalaryCol, kpiCol, ratingCol, currentSalaryCol);

        if (dataStore != null && supervisor != null) {
            try {
                List<Employee> teamMembers = dataStore.getEmployeesByDivision(supervisor.getDivisi()).stream()
                        .filter(emp -> emp.getRole().equals("pegawai"))
                        .collect(Collectors.toList());
                table.setItems(FXCollections.observableArrayList(teamMembers));
            } catch (Exception e) {
                logger.severe("Error loading team salary data: " + e.getMessage());
            }
        }

        table.setPrefHeight(400);
        return table;
    }

    private TableView<SalaryHistory> createTeamSalaryHistoryTable() {
        TableView<SalaryHistory> table = new TableView<>();
        table.getStyleClass().add("data-table");

        TableColumn<SalaryHistory, String> employeeCol = new TableColumn<>("Employee");
        employeeCol.setCellValueFactory(cellData -> {
            Employee emp = dataStore.getEmployeeById(cellData.getValue().getEmployeeId());
            return new javafx.beans.property.SimpleStringProperty(emp != null ? emp.getNama() : "Unknown");
        });

        TableColumn<SalaryHistory, String> monthCol = new TableColumn<>("Month");
        monthCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMonthName()));

        TableColumn<SalaryHistory, Integer> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(new PropertyValueFactory<>("tahun"));

        TableColumn<SalaryHistory, String> baseSalaryCol = new TableColumn<>("Base Salary");
        baseSalaryCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.format("Rp %,.0f", cellData.getValue().getBaseSalary())));

        TableColumn<SalaryHistory, String> kpiBonusCol = new TableColumn<>("KPI Bonus");
        kpiBonusCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.format("Rp %,.0f", cellData.getValue().getKpiBonus())));

        TableColumn<SalaryHistory, String> supervisorBonusCol = new TableColumn<>("Supervisor Bonus");
        supervisorBonusCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.format("Rp %,.0f", cellData.getValue().getSupervisorBonus())));

        TableColumn<SalaryHistory, String> totalSalaryCol = new TableColumn<>("Total Salary");
        totalSalaryCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.format("Rp %,.0f", cellData.getValue().getTotalSalary())));

        table.getColumns().addAll(employeeCol, monthCol, yearCol, baseSalaryCol, kpiBonusCol, supervisorBonusCol, totalSalaryCol);

        if (dataStore != null && supervisor != null) {
            try {
                // Get salary history for team members
                List<Employee> teamMembers = dataStore.getEmployeesByDivision(supervisor.getDivisi()).stream()
                        .filter(emp -> emp.getRole().equals("pegawai"))
                        .collect(Collectors.toList());

                List<SalaryHistory> teamSalaryHistory = teamMembers.stream()
                        .flatMap(emp -> dataStore.getSalaryHistoryByEmployee(emp.getId()).stream())
                        .collect(Collectors.toList());

                table.setItems(FXCollections.observableArrayList(teamSalaryHistory));
            } catch (Exception e) {
                logger.severe("Error loading team salary history: " + e.getMessage());
            }
        }

        table.setPrefHeight(400);
        return table;
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

        Tab reportsTab = new Tab("Reports", createReportHistoryTable());
        Tab evaluationsTab = new Tab("Evaluations", createEvaluationHistoryTable());
        Tab leaveRequestsTab = new Tab("Leave Requests", createTeamLeaveRequestsTable());
        Tab meetingsTab = new Tab("Meetings", createTeamMeetingsTable());
        Tab attendanceTab = new Tab("Team Attendance", createTeamAttendanceTable());

        reportsTab.setClosable(false);
        evaluationsTab.setClosable(false);
        leaveRequestsTab.setClosable(false);
        meetingsTab.setClosable(false);
        attendanceTab.setClosable(false);

        historyTabs.getTabs().addAll(reportsTab, evaluationsTab, leaveRequestsTab, meetingsTab, attendanceTab);

        content.getChildren().addAll(title, historyTabs);
        setScrollableContent(content);
    }

    private TableView<LeaveRequest> createTeamLeaveRequestsTable() {
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

        TableColumn<LeaveRequest, Integer> daysCol = new TableColumn<>("Days");
        daysCol.setCellValueFactory(new PropertyValueFactory<>("totalDays"));

        TableColumn<LeaveRequest, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<LeaveRequest, String> requestDateCol = new TableColumn<>("Request Date");
        requestDateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getRequestDate())));

        table.getColumns().addAll(employeeCol, typeCol, startDateCol, daysCol, statusCol, requestDateCol);

        if (dataStore != null && supervisor != null) {
            try {
                // Get leave requests for team members
                List<Employee> teamMembers = dataStore.getEmployeesByDivision(supervisor.getDivisi()).stream()
                        .filter(emp -> emp.getRole().equals("pegawai"))
                        .collect(Collectors.toList());

                List<LeaveRequest> teamLeaveRequests = teamMembers.stream()
                        .flatMap(emp -> dataStore.getLeaveRequestsByEmployee(emp.getId()).stream())
                        .collect(Collectors.toList());

                table.setItems(FXCollections.observableArrayList(teamLeaveRequests));
            } catch (Exception e) {
                logger.severe("Error loading team leave requests: " + e.getMessage());
            }
        }

        table.setPrefHeight(350);
        return table;
    }

    private TableView<Meeting> createTeamMeetingsTable() {
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

        TableColumn<Meeting, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(titleCol, dateCol, timeCol, locationCol, statusCol);

        if (dataStore != null && supervisor != null) {
            try {
                List<Meeting> myMeetings = dataStore.getMeetingsByEmployee(supervisor.getId());
                table.setItems(FXCollections.observableArrayList(myMeetings));
            } catch (Exception e) {
                logger.severe("Error loading team meetings: " + e.getMessage());
            }
        }

        table.setPrefHeight(350);
        return table;
    }

    private TableView<Attendance> createTeamAttendanceTable() {
        TableView<Attendance> table = new TableView<>();
        table.getStyleClass().add("data-table");

        TableColumn<Attendance, String> employeeCol = new TableColumn<>("Employee");
        employeeCol.setCellValueFactory(cellData -> {
            Employee emp = dataStore.getEmployeeById(cellData.getValue().getEmployeeId());
            return new javafx.beans.property.SimpleStringProperty(emp != null ? emp.getNama() : "Unknown");
        });

        TableColumn<Attendance, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getTanggal())));

        TableColumn<Attendance, String> clockInCol = new TableColumn<>("Clock In");
        clockInCol.setCellValueFactory(new PropertyValueFactory<>("jamMasuk"));

        TableColumn<Attendance, String> clockOutCol = new TableColumn<>("Clock Out");
        clockOutCol.setCellValueFactory(new PropertyValueFactory<>("jamKeluar"));

        TableColumn<Attendance, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(employeeCol, dateCol, clockInCol, clockOutCol, statusCol);

        if (dataStore != null && supervisor != null) {
            try {
                // Get attendance for team members (last 30 days)
                List<Employee> teamMembers = dataStore.getEmployeesByDivision(supervisor.getDivisi()).stream()
                        .filter(emp -> emp.getRole().equals("pegawai"))
                        .collect(Collectors.toList());

                List<Attendance> teamAttendance = teamMembers.stream()
                        .flatMap(emp -> dataStore.getAttendanceByEmployee(emp.getId()).stream())
                        .collect(Collectors.toList());

                table.setItems(FXCollections.observableArrayList(teamAttendance));
            } catch (Exception e) {
                logger.severe("Error loading team attendance: " + e.getMessage());
            }
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

    private void showEditProfileDialog() {
        if (supervisor == null || dataStore == null) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Profile");
        dialog.setHeaderText("Update your name and password.");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setText(supervisor.getNama());

        PasswordField passwordField = new PasswordField();
        passwordField.setText(supervisor.getPassword());

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                supervisor.setNama(nameField.getText());
                supervisor.setPassword(passwordField.getText());
                try {
                    dataStore.updateEmployee(supervisor);
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Profile updated successfully!");
                    userWelcomeLabel.setText("Welcome, " + supervisor.getNama() + " (" + supervisor.getDivisi() + ")");
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to update profile.");
                }
            }
        });
    }

    private void showAddEmployeeDialog() {
        Dialog<Employee> dialog = new Dialog<>();
        dialog.setTitle("Add New Employee");
        dialog.setHeaderText("Enter new employee details for the " + supervisor.getDivisi() + " division.");

        ButtonType addButton = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Name");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        TextField positionField = new TextField();
        positionField.setPromptText("Position");
        TextField salaryField = new TextField();
        salaryField.setPromptText("Base Salary");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label("Position:"), 0, 2);
        grid.add(positionField, 1, 2);
        grid.add(new Label("Base Salary:"), 0, 3);
        grid.add(salaryField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(nameField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButton) {
                if (nameField.getText().isEmpty() || passwordField.getText().isEmpty() || positionField.getText().isEmpty() || salaryField.getText().isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Invalid Input", "Please fill in all fields.");
                    return null;
                }
                try {
                    Employee newEmployee = new Employee();
                    newEmployee.setNama(nameField.getText());
                    newEmployee.setPassword(passwordField.getText());
                    newEmployee.setJabatan(positionField.getText());
                    newEmployee.setGajiPokok(Double.parseDouble(salaryField.getText()));
                    newEmployee.setDivisi(supervisor.getDivisi());
                    newEmployee.setRole("pegawai");
                    newEmployee.setTglMasuk(new Date());
                    newEmployee.setSisaCuti(12);
                    newEmployee.setKpiScore(75.0); // Default KPI Score
                    newEmployee.setSupervisorRating(75.0); // Default Supervisor Rating
                    newEmployee.setAttendanceScore(100.0); // Default Attendance Score
                    newEmployee.setOverallRating(75.0); // Default Overall Rating
                    newEmployee.setLayoffRisk(false);
                    return newEmployee;
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid number for salary.");
                    return null;
                }
            }
            return null;
        });

        Optional<Employee> result = dialog.showAndWait();

        result.ifPresent(newEmployee -> {
            try {
                dataStore.addEmployee(newEmployee);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Employee added successfully!");
                refreshTeamTable(teamTable);
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add employee: " + e.getMessage());
            }
        });
    }
}