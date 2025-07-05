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

    // PERBAIKAN UTAMA: Menambahkan metode setScrollableContent()
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

    // PERBAIKAN: Menggunakan setScrollableContent()
    private void showDashboardContent() {
        if (supervisor == null || dataStore == null || contentArea == null) {
            logger.warning("Cannot show dashboard content - missing required objects");
            return;
        }

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Supervisor Dashboard - " + supervisor.getDivisi() + " Division");
        title.getStyleClass().add("content-title");

        HBox quickActions = createQuickActions();
        HBox statsCards = createStatsCards();
        VBox recentActivities = createRecentActivitiesSection();

        content.getChildren().addAll(title, quickActions, statsCards, recentActivities);
        setScrollableContent(content); // PERBAIKAN: Menggunakan ScrollPane
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

            double avgKpi = teamMembers.stream().mapToDouble(Employee::getKpiScore).average().orElse(0.0);
            VBox avgKpiCard = createStatsCard("Avg KPI", String.format("%.1f%%", avgKpi), "📊", "#2ecc71");

            long atRiskCount = teamMembers.stream().filter(Employee::isLayoffRisk).count();
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

    // PERBAIKAN: Menggunakan setScrollableContent() untuk semua metode content
    private void showMyAttendance() {
        if (supervisor == null || dataStore == null || contentArea == null) {
            return;
        }

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("My Attendance");
        title.getStyleClass().add("content-title");

        TableView<Attendance> attendanceTable = createMyAttendanceTable();

        content.getChildren().addAll(title, attendanceTable);
        setScrollableContent(content); // PERBAIKAN: Menggunakan ScrollPane
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
        setScrollableContent(content); // PERBAIKAN: Menggunakan ScrollPane
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
        setScrollableContent(content); // PERBAIKAN: Menggunakan ScrollPane
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

                    Date startUtilDate = java.util.Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    Date endUtilDate = java.util.Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

                    try {
                        boolean success = dataStore.saveLeaveRequest(supervisor.getId(), leaveTypeCombo.getValue(),
                                startUtilDate, endUtilDate, reasonArea.getText());
                        if (success) {
                            showAlert(Alert.AlertType.INFORMATION, "Success", "Leave request submitted successfully!");
                            showMyLeaveRequests();
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

    // PERBAIKAN: Menggunakan setScrollableContent() untuk semua metode content lainnya
    private void showLeaveApprovalsContent() {
        if (supervisor == null || dataStore == null || contentArea == null) {
            return;
        }

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Leave Request Approvals");
        title.getStyleClass().add("content-title");

        if (leaveApprovalsTable == null) {
            leaveApprovalsTable = createLeaveApprovalTable();
        }

        content.getChildren().addAll(title, leaveApprovalsTable);
        setScrollableContent(content); // PERBAIKAN: Menggunakan ScrollPane
    }

    private void showTeamManagementContent() {
        if (supervisor == null || dataStore == null || contentArea == null) {
            return;
        }

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
        setScrollableContent(content); // PERBAIKAN: Menggunakan ScrollPane
    }

    private void showMonthlyEvaluationContent() {
        if (supervisor == null || dataStore == null || contentArea == null) {
            return;
        }

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
        setScrollableContent(content); // PERBAIKAN: Menggunakan ScrollPane
    }

    private void showUploadReportContent() {
        if (supervisor == null || dataStore == null || contentArea == null) {
            return;
        }

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
        setScrollableContent(content); // PERBAIKAN: Menggunakan ScrollPane
    }

    private void showPerformanceAnalyticsContent() {
        if (supervisor == null || dataStore == null || contentArea == null) {
            return;
        }

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Performance Analytics - " + supervisor.getDivisi());
        title.getStyleClass().add("content-title");

        HBox performanceCards = createPerformanceOverviewCards();
        TableView<Employee> performanceTable = createPerformanceDetailsTable();

        content.getChildren().addAll(title, performanceCards, performanceTable);
        setScrollableContent(content); // PERBAIKAN: Menggunakan ScrollPane
    }

    private void showSalaryManagementContent() {
        if (supervisor == null || dataStore == null || contentArea == null) {
            return;
        }

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
        setScrollableContent(content); // PERBAIKAN: Menggunakan ScrollPane
    }

    private void showAllHistoryContent() {
        if (supervisor == null || dataStore == null || contentArea == null) {
            return;
        }

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
        setScrollableContent(content); // PERBAIKAN: Menggunakan ScrollPane
    }

    // Implementasi helper methods - Dummy implementations untuk kelengkapan
    private TableView<LeaveRequest> createLeaveApprovalTable() {
        TableView<LeaveRequest> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setPrefHeight(400);

        // Basic columns setup
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

        table.getColumns().addAll(employeeCol, typeCol, startDateCol, daysCol);

        // Load data
        if (dataStore != null && supervisor != null) {
            try {
                List<LeaveRequest> pendingRequests = dataStore.getLeaveRequestsForApproval(supervisor.getId());
                table.setItems(FXCollections.observableArrayList(pendingRequests));
            } catch (Exception e) {
                logger.severe("Error loading leave requests for approval: " + e.getMessage());
            }
        }

        return table;
    }

    private TableView<Employee> createTeamTable() {
        TableView<Employee> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setPrefHeight(400);

        TableColumn<Employee, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("nama"));

        TableColumn<Employee, String> positionCol = new TableColumn<>("Position");
        positionCol.setCellValueFactory(new PropertyValueFactory<>("jabatan"));

        TableColumn<Employee, String> kpiCol = new TableColumn<>("KPI Score");
        kpiCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getKpiScore()) + "%"));

        table.getColumns().addAll(nameCol, positionCol, kpiCol);

        // Load team data
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

        return table;
    }

    private VBox createMonthlyEvaluationForm() {
        VBox form = new VBox(20);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(30));
        form.getStyleClass().add("kpi-set-form");

        Label formTitle = new Label("Evaluate Team Member");
        formTitle.getStyleClass().add("form-title");

        // Simple form with basic elements
        ComboBox<String> employeeCombo = new ComboBox<>();
        employeeCombo.setPromptText("Select Employee");

        Slider ratingSlider = new Slider(0, 100, 75);
        ratingSlider.setShowTickLabels(true);
        ratingSlider.setShowTickMarks(true);

        Button submitBtn = new Button("Submit Evaluation");
        submitBtn.getStyleClass().add("action-button-green");

        form.getChildren().addAll(formTitle,
                new Label("Employee:"), employeeCombo,
                new Label("Rating:"), ratingSlider,
                submitBtn);

        return form;
    }

    private TableView<MySQLDataStore.MonthlyEvaluation> createEvaluationHistoryTable() {
        TableView<MySQLDataStore.MonthlyEvaluation> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setPrefHeight(400);

        TableColumn<MySQLDataStore.MonthlyEvaluation, String> employeeCol = new TableColumn<>("Employee");
        employeeCol.setCellValueFactory(cellData -> {
            Employee emp = dataStore.getEmployeeById(cellData.getValue().getEmployeeId());
            return new javafx.beans.property.SimpleStringProperty(emp != null ? emp.getNama() : "Unknown");
        });

        TableColumn<MySQLDataStore.MonthlyEvaluation, Integer> monthCol = new TableColumn<>("Month");
        monthCol.setCellValueFactory(new PropertyValueFactory<>("month"));

        table.getColumns().addAll(employeeCol, monthCol);

        return table;
    }

    private VBox createReportUploadForm() {
        VBox form = new VBox(20);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(30));
        form.getStyleClass().add("kpi-set-form");

        Label formTitle = new Label("Upload Division Report");
        formTitle.getStyleClass().add("form-title");

        TextField filePathField = new TextField();
        filePathField.setPromptText("Select report file...");
        filePathField.setEditable(false);

        Button browseBtn = new Button("Browse");
        Button uploadBtn = new Button("Upload Report");
        uploadBtn.getStyleClass().add("action-button-green");

        form.getChildren().addAll(formTitle,
                new Label("Report File:"), filePathField, browseBtn, uploadBtn);

        return form;
    }

    private TableView<Report> createReportHistoryTable() {
        TableView<Report> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setPrefHeight(400);

        TableColumn<Report, String> monthCol = new TableColumn<>("Month");
        monthCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMonthName()));

        TableColumn<Report, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(monthCol, statusCol);

        return table;
    }

    private HBox createPerformanceOverviewCards() {
        HBox cardsContainer = new HBox(20);
        cardsContainer.setAlignment(Pos.CENTER);

        VBox avgKpiCard = createStatsCard("Average KPI", "75.5%", "📊", "#3498db");
        VBox highPerformersCard = createStatsCard("High Performers", "3", "🏆", "#f39c12");

        cardsContainer.getChildren().addAll(avgKpiCard, highPerformersCard);
        return cardsContainer;
    }

    private TableView<Employee> createPerformanceDetailsTable() {
        TableView<Employee> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setPrefHeight(400);

        TableColumn<Employee, String> nameCol = new TableColumn<>("Employee");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("nama"));

        TableColumn<Employee, String> performanceCol = new TableColumn<>("Performance Level");
        performanceCol.setCellValueFactory(cellData -> {
            double avgScore = (cellData.getValue().getKpiScore() + cellData.getValue().getSupervisorRating()) / 2;
            String level = avgScore >= 80 ? "⭐ Good" : "✅ Satisfactory";
            return new javafx.beans.property.SimpleStringProperty(level);
        });

        table.getColumns().addAll(nameCol, performanceCol);

        return table;
    }

    private TableView<Employee> createTeamSalaryTable() {
        TableView<Employee> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setPrefHeight(400);

        TableColumn<Employee, String> nameCol = new TableColumn<>("Employee");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("nama"));

        TableColumn<Employee, String> salaryCol = new TableColumn<>("Base Salary");
        salaryCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.format("Rp %,.0f", cellData.getValue().getGajiPokok())));

        table.getColumns().addAll(nameCol, salaryCol);

        return table;
    }

    private TableView<SalaryHistory> createTeamSalaryHistoryTable() {
        TableView<SalaryHistory> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setPrefHeight(400);

        TableColumn<SalaryHistory, String> employeeCol = new TableColumn<>("Employee");
        employeeCol.setCellValueFactory(cellData -> {
            Employee emp = dataStore.getEmployeeById(cellData.getValue().getEmployeeId());
            return new javafx.beans.property.SimpleStringProperty(emp != null ? emp.getNama() : "Unknown");
        });

        TableColumn<SalaryHistory, String> monthCol = new TableColumn<>("Month");
        monthCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMonthName()));

        table.getColumns().addAll(employeeCol, monthCol);

        return table;
    }

    private TableView<LeaveRequest> createTeamLeaveRequestsTable() {
        TableView<LeaveRequest> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setPrefHeight(350);

        TableColumn<LeaveRequest, String> employeeCol = new TableColumn<>("Employee");
        employeeCol.setCellValueFactory(cellData -> {
            Employee emp = dataStore.getEmployeeById(cellData.getValue().getEmployeeId());
            return new javafx.beans.property.SimpleStringProperty(emp != null ? emp.getNama() : "Unknown");
        });

        TableColumn<LeaveRequest, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(employeeCol, statusCol);

        return table;
    }

    private TableView<Meeting> createTeamMeetingsTable() {
        TableView<Meeting> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setPrefHeight(350);

        TableColumn<Meeting, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Meeting, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(titleCol, statusCol);

        return table;
    }

    private TableView<Attendance> createTeamAttendanceTable() {
        TableView<Attendance> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setPrefHeight(350);

        TableColumn<Attendance, String> employeeCol = new TableColumn<>("Employee");
        employeeCol.setCellValueFactory(cellData -> {
            Employee emp = dataStore.getEmployeeById(cellData.getValue().getEmployeeId());
            return new javafx.beans.property.SimpleStringProperty(emp != null ? emp.getNama() : "Unknown");
        });

        TableColumn<Attendance, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(employeeCol, statusCol);

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

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label("Position:"), 0, 2);
        grid.add(positionField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(nameField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButton) {
                if (nameField.getText().isEmpty() || passwordField.getText().isEmpty() || positionField.getText().isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Invalid Input", "Please fill in all fields.");
                    return null;
                }
                Employee newEmployee = new Employee();
                newEmployee.setNama(nameField.getText());
                newEmployee.setPassword(passwordField.getText());
                newEmployee.setJabatan(positionField.getText());
                newEmployee.setDivisi(supervisor.getDivisi());
                newEmployee.setRole("pegawai");
                newEmployee.setTglMasuk(new Date());
                newEmployee.setSisaCuti(12);
                newEmployee.setGajiPokok(4800000.0);
                newEmployee.setKpiScore(75.0);
                newEmployee.setSupervisorRating(75.0);
                newEmployee.setAttendanceScore(100.0);
                newEmployee.setOverallRating(75.0);
                newEmployee.setLayoffRisk(false);
                return newEmployee;
            }
            return null;
        });

        Optional<Employee> result = dialog.showAndWait();
        result.ifPresent(newEmployee -> {
            try {
                dataStore.addEmployee(newEmployee);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Employee added successfully!");
                if (teamTable != null) {
                    // Refresh team table
                    List<Employee> teamMembers = dataStore.getEmployeesByDivision(supervisor.getDivisi()).stream()
                            .filter(emp -> emp.getRole().equals("pegawai"))
                            .collect(Collectors.toList());
                    teamTable.setItems(FXCollections.observableArrayList(teamMembers));
                }
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add employee: " + e.getMessage());
            }
        });
    }
}