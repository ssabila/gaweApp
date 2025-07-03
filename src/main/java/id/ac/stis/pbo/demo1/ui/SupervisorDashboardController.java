package id.ac.stis.pbo.demo1.ui;

import id.ac.stis.pbo.demo1.HelloApplication;
import id.ac.stis.pbo.demo1.data.MySQLDataStore;
import id.ac.stis.pbo.demo1.models.Attendance;
import id.ac.stis.pbo.demo1.models.Employee;
import id.ac.stis.pbo.demo1.models.LeaveRequest;
import id.ac.stis.pbo.demo1.models.Meeting;
import id.ac.stis.pbo.demo1.models.Report;
import id.ac.stis.pbo.demo1.models.SalaryHistory;
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
        userWelcomeLabel.setText("Welcome, " + supervisor.getNama() + " (" + supervisor.getDivisi() + ")");
    }

    public void setDataStore(MySQLDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        this.stage.setTitle("GAWE - Supervisor Dashboard - " + supervisor.getNama());
        this.stage.setOnCloseRequest(e -> {
            stopApplication();
        });
    }

    @FXML
    public void initialize() {
        populateNavigationButtons();
        showDashboardContent(); // Show default content on startup
    }

    private void populateNavigationButtons() {
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
            List<Attendance> todayAttendance = dataStore.getTodayAttendance(supervisor.getId());
            return !todayAttendance.isEmpty();
        } catch (Exception e) {
            logger.severe("Error checking today's attendance: " + e.getMessage());
            return false;
        }
    }

    private boolean hasCompletedAttendanceToday() {
        try {
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

        ObservableList<String> activities = FXCollections.observableArrayList(
                "📊 Dashboard accessed - just now",
                "📄 Ready to upload monthly report for " + supervisor.getDivisi(),
                "⭐ Team evaluations pending",
                "👥 Managing " + supervisor.getDivisi() + " team"
        );
        activitiesList.setItems(activities);

        section.getChildren().addAll(sectionTitle, activitiesList);
        return section;
    }

    // Personal Features
    private void showMyAttendance() {
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

        List<Attendance> myAttendance = dataStore.getAttendanceByEmployee(supervisor.getId());
        table.setItems(FXCollections.observableArrayList(myAttendance));
        table.setPrefHeight(400);

        return table;
    }

    private void showMyMeetings() {
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

        List<Meeting> myMeetings = dataStore.getMeetingsByEmployee(supervisor.getId());
        table.setItems(FXCollections.observableArrayList(myMeetings));
        table.setPrefHeight(400);

        return table;
    }

    private void showNewMeetingDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Schedule New Meeting");
        dialog.setHeaderText("Create a new meeting for your team");

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

        ListView<Employee> teamMembersList = new ListView<>();
        teamMembersList.setPrefHeight(150);
        teamMembersList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        List<Employee> teamMembers = dataStore.getEmployeesByDivision(supervisor.getDivisi());
        teamMembersList.setItems(FXCollections.observableArrayList(teamMembers));

        content.getChildren().addAll(
                new Label("Title:"), titleField,
                new Label("Description:"), descriptionArea,
                new Label("Date:"), datePicker,
                new Label("Start Time:"), startTimeField,
                new Label("End Time:"), endTimeField,
                new Label("Location:"), locationField,
                new Label("Team Members (select multiple):"), teamMembersList
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                if (!titleField.getText().isEmpty() && datePicker.getValue() != null) {
                    Date meetingDate = java.sql.Date.valueOf(datePicker.getValue());

                    List<String> selectedParticipants = teamMembersList.getSelectionModel().getSelectedItems()
                            .stream()
                            .map(Employee::getId)
                            .collect(Collectors.toList());

                    selectedParticipants.add(supervisor.getId());

                    boolean success = dataStore.saveMeeting(
                            titleField.getText(),
                            descriptionArea.getText(),
                            meetingDate,
                            startTimeField.getText(),
                            endTimeField.getText(),
                            locationField.getText(),
                            supervisor.getId(),
                            selectedParticipants
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

        List<LeaveRequest> myLeaveRequests = dataStore.getLeaveRequestsByEmployee(supervisor.getId());
        table.setItems(FXCollections.observableArrayList(myLeaveRequests));
        table.setPrefHeight(400);

        return table;
    }

    private void showLeaveRequestDialog() {
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

    private void showTeamManagementContent() {
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

        TableColumn<Employee, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("nama"));
        nameCol.setPrefWidth(200);

        TableColumn<Employee, String> positionCol = new TableColumn<>("Position");
        positionCol.setCellValueFactory(new PropertyValueFactory<>("jabatan"));
        positionCol.setPrefWidth(180);

        TableColumn<Employee, String> kpiCol = new TableColumn<>("KPI Score");
        kpiCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.format("%.1f%%", cellData.getValue().getKpiScore())));
        kpiCol.setPrefWidth(100);

        TableColumn<Employee, String> ratingCol = new TableColumn<>("Supervisor Rating");
        ratingCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.format("%.1f%%", cellData.getValue().getSupervisorRating())));
        ratingCol.setPrefWidth(150);

        TableColumn<Employee, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> {
            Employee emp = cellData.getValue();
            String status = emp.isLayoffRisk() ? "⚠️ At Risk" : "✅ Good Standing";
            return new javafx.beans.property.SimpleStringProperty(status);
        });
        statusCol.setPrefWidth(120);

        table.getColumns().addAll(nameCol, positionCol, kpiCol, ratingCol, statusCol);

        List<Employee> teamMembers = dataStore.getAllEmployees().stream()
                .filter(emp -> emp.getDivisi().equals(supervisor.getDivisi()) &&
                        emp.getRole().equalsIgnoreCase("pegawai"))
                .toList();

        table.setItems(FXCollections.observableArrayList(teamMembers));
        table.setPrefHeight(400);

        return table;
    }

    private void showMonthlyEvaluationContent() {
        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Monthly Employee Evaluation");
        title.getStyleClass().add("content-title");

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("custom-tab-pane");

        Tab evaluationTab = new Tab("New Evaluation", createMonthlyEvaluationForm());
        Tab historyTab = new Tab("Evaluation History", createMonthlyEvaluationHistoryTable());
        evaluationTab.setClosable(false);
        historyTab.setClosable(false);


        tabPane.getTabs().addAll(evaluationTab, historyTab);

        content.getChildren().addAll(title, tabPane);
        contentArea.getChildren().add(content);
    }

    private VBox createMonthlyEvaluationForm() {
        VBox form = new VBox(20);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(30));
        form.setMaxWidth(700);
        form.getStyleClass().add("kpi-set-form"); // Reusing style for forms

        Label formTitle = new Label("Monthly Performance Evaluation");
        formTitle.getStyleClass().add("form-title");

        GridPane selectionGrid = new GridPane();
        selectionGrid.setHgap(20);
        selectionGrid.setVgap(15);
        selectionGrid.setAlignment(Pos.CENTER);

        ComboBox<Employee> employeeCombo = new ComboBox<>();
        employeeCombo.setPromptText("Select Employee");
        employeeCombo.setPrefWidth(300);

        List<Employee> teamMembers = dataStore.getAllEmployees().stream()
                .filter(emp -> emp.getDivisi().equals(supervisor.getDivisi()) &&
                        emp.getRole().equalsIgnoreCase("pegawai"))
                .toList();
        employeeCombo.setItems(FXCollections.observableArrayList(teamMembers));

        ComboBox<String> monthCombo = new ComboBox<>();
        monthCombo.getItems().addAll("January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December");
        monthCombo.setValue("January");
        monthCombo.setPrefWidth(150);

        ComboBox<Integer> yearCombo = new ComboBox<>();
        for (int year = 2020; year <= 2030; year++) {
            yearCombo.getItems().add(year);
        }
        yearCombo.setValue(LocalDate.now().getYear());
        yearCombo.setPrefWidth(100);

        selectionGrid.add(new Label("Employee:"), 0, 0);
        selectionGrid.add(employeeCombo, 1, 0);
        selectionGrid.add(new Label("Month:"), 0, 1);
        selectionGrid.add(monthCombo, 1, 1);
        selectionGrid.add(new Label("Year:"), 0, 2);
        selectionGrid.add(yearCombo, 1, 2);

        GridPane criteriaGrid = new GridPane();
        criteriaGrid.setHgap(20);
        criteriaGrid.setVgap(15);
        criteriaGrid.setAlignment(Pos.CENTER);

        Label punctualityLabel = new Label("Punctuality Score (0-100):");
        Slider punctualitySlider = new Slider(0, 100, 75);
        punctualitySlider.setShowTickLabels(true);
        punctualitySlider.setShowTickMarks(true);
        punctualitySlider.setMajorTickUnit(25);
        Label punctualityValue = new Label("75");
        punctualitySlider.valueProperty().addListener((obs, oldVal, newVal) ->
                punctualityValue.setText(String.format("%.0f", newVal.doubleValue())));

        Label attendanceLabel = new Label("Attendance Score (0-100):");
        Slider attendanceSlider = new Slider(0, 100, 80);
        attendanceSlider.setShowTickLabels(true);
        attendanceSlider.setShowTickMarks(true);
        attendanceSlider.setMajorTickUnit(25);
        Label attendanceValue = new Label("80");
        attendanceSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                attendanceValue.setText(String.format("%.0f", newVal.doubleValue())));

        Label productivityLabel = new Label("Productivity Score (0-100):");
        Slider productivitySlider = new Slider(0, 100, 75);
        productivitySlider.setShowTickLabels(true);
        productivitySlider.setShowTickMarks(true);
        productivitySlider.setMajorTickUnit(25);
        Label productivityValue = new Label("75");
        productivitySlider.valueProperty().addListener((obs, oldVal, newVal) ->
                productivityValue.setText(String.format("%.0f", newVal.doubleValue())));

        Label overallLabel = new Label("Overall Rating (0-100):");
        Slider overallSlider = new Slider(0, 100, 75);
        overallSlider.setShowTickLabels(true);
        overallSlider.setShowTickMarks(true);
        overallSlider.setMajorTickUnit(25);
        Label overallValue = new Label("75");
        overallSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                overallValue.setText(String.format("%.0f", newVal.doubleValue())));

        criteriaGrid.add(punctualityLabel, 0, 0);
        criteriaGrid.add(punctualitySlider, 1, 0);
        criteriaGrid.add(punctualityValue, 2, 0);
        criteriaGrid.add(attendanceLabel, 0, 1);
        criteriaGrid.add(attendanceSlider, 1, 1);
        criteriaGrid.add(attendanceValue, 2, 1);
        criteriaGrid.add(productivityLabel, 0, 2);
        criteriaGrid.add(productivitySlider, 1, 2);
        criteriaGrid.add(productivityValue, 2, 2);
        criteriaGrid.add(overallLabel, 0, 3);
        criteriaGrid.add(overallSlider, 1, 3);
        criteriaGrid.add(overallValue, 2, 3);

        Label commentsLabel = new Label("Monthly Comments:");
        TextArea commentsArea = new TextArea();
        commentsArea.setPrefRowCount(4);
        commentsArea.setPromptText("Enter monthly evaluation comments and feedback...");

        Button submitBtn = new Button("Submit Monthly Evaluation");
        submitBtn.getStyleClass().add("action-button-green");

        submitBtn.setOnAction(e -> {
            Employee selectedEmployee = employeeCombo.getValue();
            String selectedMonth = monthCombo.getValue();
            Integer selectedYear = yearCombo.getValue();

            if (selectedEmployee != null && selectedMonth != null && selectedYear != null) {
                try {
                    boolean alreadyExists = dataStore.hasMonthlyEvaluation(
                            selectedEmployee.getId(),
                            monthCombo.getSelectionModel().getSelectedIndex() + 1,
                            selectedYear
                    );

                    if (alreadyExists) {
                        showAlert(Alert.AlertType.WARNING, "Evaluation Exists",
                                "Monthly evaluation for " + selectedEmployee.getNama() + " in " +
                                        selectedMonth + " " + selectedYear + " already exists!");
                        return;
                    }

                    boolean success = dataStore.saveMonthlyEmployeeEvaluation(
                            selectedEmployee.getId(),
                            supervisor.getId(),
                            monthCombo.getSelectionModel().getSelectedIndex() + 1,
                            selectedYear,
                            punctualitySlider.getValue(),
                            attendanceSlider.getValue(),
                            productivitySlider.getValue(),
                            overallSlider.getValue(),
                            commentsArea.getText()
                    );

                    if (success) {
                        showAlert(Alert.AlertType.INFORMATION, "Success",
                                "Monthly evaluation for " + selectedEmployee.getNama() +
                                        " (" + selectedMonth + " " + selectedYear + ") submitted successfully!");
                        // Reset form
                        employeeCombo.setValue(null);
                        punctualitySlider.setValue(75);
                        attendanceSlider.setValue(80);
                        productivitySlider.setValue(75);
                        overallSlider.setValue(75);
                        commentsArea.clear();
                        // Refresh history table if visible
                        // Need to access the TabPane from this method scope to switch tabs or refresh
                        // For simplicity, we'll just rely on the user to switch or re-click
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error",
                                "Failed to submit monthly evaluation. Please try again.");
                    }
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Error",
                            "An error occurred: " + ex.getMessage());
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "Warning",
                        "Please select an employee, month, and year.");
            }
        });

        form.getChildren().addAll(formTitle, selectionGrid, criteriaGrid,
                commentsLabel, commentsArea, submitBtn);
        return form;
    }

    private TableView<MySQLDataStore.MonthlyEvaluation> createMonthlyEvaluationHistoryTable() {
        TableView<MySQLDataStore.MonthlyEvaluation> table = new TableView<>();
        table.setPrefHeight(400);
        table.getStyleClass().add("data-table");

        TableColumn<MySQLDataStore.MonthlyEvaluation, String> employeeCol = new TableColumn<>("Employee");
        employeeCol.setCellValueFactory(new PropertyValueFactory<>("employeeId"));

        TableColumn<MySQLDataStore.MonthlyEvaluation, String> monthCol = new TableColumn<>("Month");
        monthCol.setCellValueFactory(cellData -> {
            String[] months = {"", "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"};
            return new javafx.beans.property.SimpleStringProperty(months[cellData.getValue().getMonth()]);
        });

        TableColumn<MySQLDataStore.MonthlyEvaluation, Integer> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(new PropertyValueFactory<>("year"));

        TableColumn<MySQLDataStore.MonthlyEvaluation, String> overallCol = new TableColumn<>("Overall Rating");
        overallCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getOverallRating()) + "%"));

        TableColumn<MySQLDataStore.MonthlyEvaluation, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getEvaluationDate())));

        table.getColumns().addAll(employeeCol, monthCol, yearCol, overallCol, dateCol);

        List<MySQLDataStore.MonthlyEvaluation> monthlyEvaluations = dataStore.getMonthlyEvaluationsBySupervisor(supervisor.getId());
        table.setItems(FXCollections.observableArrayList(monthlyEvaluations));

        return table;
    }

    private void showUploadReportContent() {
        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Upload Monthly Report");
        title.getStyleClass().add("content-title");

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("custom-tab-pane");

        Tab uploadTab = new Tab("Upload Report", createUploadForm());
        Tab historyTab = new Tab("Report History", createMyReportHistoryTable());
        uploadTab.setClosable(false);
        historyTab.setClosable(false);


        tabPane.getTabs().addAll(uploadTab, historyTab);

        content.getChildren().addAll(title, tabPane);
        contentArea.getChildren().add(content);
    }

    private VBox createUploadForm() {
        VBox form = new VBox(20);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(30));
        form.setMaxWidth(500);
        form.getStyleClass().add("kpi-set-form"); // Reusing style for forms

        Label formTitle = new Label("Monthly Division Report");
        formTitle.getStyleClass().add("form-title");

        HBox dateSelection = new HBox(10);
        dateSelection.setAlignment(Pos.CENTER);

        ComboBox<String> monthCombo = new ComboBox<>();
        monthCombo.getItems().addAll("January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December");
        monthCombo.setValue("January");

        ComboBox<Integer> yearCombo = new ComboBox<>();
        for (int year = 2020; year <= 2030; year++) {
            yearCombo.getItems().add(year);
        }
        yearCombo.setValue(LocalDate.now().getYear());

        dateSelection.getChildren().addAll(new Label("Month:"), monthCombo,
                new Label("Year:"), yearCombo);

        HBox fileSelection = new HBox(10);
        fileSelection.setAlignment(Pos.CENTER);

        TextField filePathField = new TextField();
        filePathField.setPromptText("Select PDF file...");
        filePathField.setPrefWidth(300);
        filePathField.setEditable(false);

        Button browseBtn = new Button("Browse");
        browseBtn.getStyleClass().add("review-button"); // Reusing style
        browseBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select PDF Report");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

            File selectedFile = fileChooser.showOpenDialog(null);
            if (selectedFile != null) {
                filePathField.setText(selectedFile.getAbsolutePath());
            }
        });

        fileSelection.getChildren().addAll(filePathField, browseBtn);

        Button uploadBtn = new Button("Upload Report");
        uploadBtn.getStyleClass().add("action-button-green");

        uploadBtn.setOnAction(e -> {
            if (!filePathField.getText().isEmpty()) {
                int monthIndex = monthCombo.getSelectionModel().getSelectedIndex() + 1;
                int year = yearCombo.getValue();

                boolean success = dataStore.saveReport(
                        supervisor.getId(),
                        supervisor.getDivisi(),
                        monthIndex,
                        year,
                        filePathField.getText()
                );

                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Success",
                            "Report uploaded successfully!");
                    filePathField.clear();
                    // Potentially refresh history table if visible
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error",
                            "Failed to upload report. Please try again.");
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "Warning",
                        "Please select a PDF file to upload.");
            }
        });

        form.getChildren().addAll(formTitle, dateSelection, fileSelection, uploadBtn);
        return form;
    }

    private TableView<Report> createMyReportHistoryTable() {
        TableView<Report> table = new TableView<>();
        table.setPrefHeight(400);
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

        List<Report> myReports = dataStore.getReportsByDivision(supervisor.getDivisi())
                .stream()
                .filter(report -> report.getSupervisorId().equals(supervisor.getId()))
                .collect(Collectors.toList());
        table.setItems(FXCollections.observableArrayList(myReports));

        return table;
    }

    private void showPerformanceAnalyticsContent() {
        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Performance Analytics - " + supervisor.getDivisi());
        title.getStyleClass().add("content-title");

        VBox analyticsSection = createAnalyticsSection();

        content.getChildren().addAll(title, analyticsSection);
        contentArea.getChildren().add(content);
    }

    private VBox createAnalyticsSection() {
        VBox section = new VBox(20);
        section.setAlignment(Pos.CENTER);
        section.setPadding(new Insets(20));
        section.getStyleClass().add("recent-activities-section"); // Reusing general section style

        Label sectionTitle = new Label("Division Performance Overview");
        sectionTitle.getStyleClass().add("section-title");

        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(30);
        metricsGrid.setVgap(20);
        metricsGrid.setAlignment(Pos.CENTER);

        List<Employee> teamMembers = dataStore.getAllEmployees().stream()
                .filter(emp -> emp.getDivisi().equals(supervisor.getDivisi()) &&
                        emp.getRole().equalsIgnoreCase("pegawai"))
                .toList();

        double avgKPI = teamMembers.stream()
                .mapToDouble(Employee::getKpiScore)
                .average()
                .orElse(0.0);

        double avgRating = teamMembers.stream()
                .mapToDouble(Employee::getSupervisorRating)
                .average()
                .orElse(0.0);

        long atRiskCount = teamMembers.stream()
                .filter(Employee::isLayoffRisk)
                .count();

        VBox avgKpiCard = createMetricCard("Average KPI", String.format("%.1f%%", avgKPI), "#3498db");
        VBox avgRatingCard = createMetricCard("Average Rating", String.format("%.1f", avgRating), "#2ecc71");
        VBox atRiskCard = createMetricCard("At Risk Employees", String.valueOf(atRiskCount), "#e74c3c");
        VBox teamSizeCard = createMetricCard("Team Size", String.valueOf(teamMembers.size()), "#9b59b6");

        metricsGrid.add(avgKpiCard, 0, 0);
        metricsGrid.add(avgRatingCard, 1, 0);
        metricsGrid.add(atRiskCard, 0, 1);
        metricsGrid.add(teamSizeCard, 1, 1);

        section.getChildren().addAll(sectionTitle, metricsGrid);
        return section;
    }

    private VBox createMetricCard(String title, String value, String color) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setPrefSize(180, 100);
        card.getStyleClass().add("metric-card"); // Add a new style class if needed, or use existing generic one
        card.setStyle(String.format("""
            -fx-background-color: %s;
            -fx-background-radius: 10;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);
        """, color));

        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        valueLabel.setTextFill(Color.WHITE);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        titleLabel.setTextFill(Color.WHITE);

        card.getChildren().addAll(valueLabel, titleLabel);
        return card;
    }

    private void showSalaryManagementContent() {
        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Salary Management");
        title.getStyleClass().add("content-title");

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("custom-tab-pane");

        Tab mySalaryTab = new Tab("My Salary", createMySalaryView());
        Tab teamSalariesTab = new Tab("Team Salaries", createTeamSalariesTable());
        mySalaryTab.setClosable(false);
        teamSalariesTab.setClosable(false);

        tabPane.getTabs().addAll(mySalaryTab, teamSalariesTab);

        content.getChildren().addAll(title, tabPane);
        contentArea.getChildren().add(content);
    }

    private VBox createMySalaryView() {
        VBox salaryView = new VBox(20);
        salaryView.setPadding(new Insets(20));
        salaryView.getStyleClass().add("my-salary-view");

        VBox salaryBreakdown = createSalaryBreakdown();
        TableView<SalaryHistory> salaryTable = createMySalaryHistoryTable();

        salaryView.getChildren().addAll(salaryBreakdown, salaryTable);
        return salaryView;
    }

    private VBox createSalaryBreakdown() {
        VBox breakdownBox = new VBox(15);
        breakdownBox.setPadding(new Insets(20));
        breakdownBox.getStyleClass().add("salary-breakdown-box");

        Label breakdownTitle = new Label("Current Monthly Salary Breakdown");
        breakdownTitle.getStyleClass().add("form-title");

        GridPane salaryGrid = new GridPane();
        salaryGrid.setHgap(20);
        salaryGrid.setVgap(15);

        // Fetch the latest supervisor data to ensure accurate salary calculation
        Employee latestSupervisorData = dataStore.authenticateUser(supervisor.getId(), supervisor.getPassword());
        if (latestSupervisorData != null) {
            supervisor = latestSupervisorData; // Update supervisor object with latest data
        } else {
            logger.warning("Could not fetch latest supervisor data for salary breakdown.");
        }

        double baseSalary = supervisor.getGajiPokok();
        double kpiBonus = 0;
        double supervisorBonus = 0;
        double penalty = 0;

        if (supervisor.getKpiScore() >= 90) {
            kpiBonus = baseSalary * 0.20;
        } else if (supervisor.getKpiScore() >= 80) {
            kpiBonus = baseSalary * 0.15;
        } else if (supervisor.getKpiScore() >= 70) {
            kpiBonus = baseSalary * 0.10;
        } else if (supervisor.getKpiScore() >= 60) {
            kpiBonus = baseSalary * 0.05;
        }

        if (supervisor.getSupervisorRating() >= 90) {
            supervisorBonus = baseSalary * 0.15;
        } else if (supervisor.getSupervisorRating() >= 80) {
            supervisorBonus = baseSalary * 0.10;
        } else if (supervisor.getSupervisorRating() >= 70) {
            supervisorBonus = baseSalary * 0.05;
        }

        if (supervisor.getKpiScore() < 60 || supervisor.getSupervisorRating() < 60) {
            penalty = baseSalary * 0.10;
        }

        double totalSalary = supervisor.calculateGajiBulanan();

        salaryGrid.add(new Label("Base Salary:"), 0, 0);
        salaryGrid.add(new Label("Rp " + String.format("%,.0f", baseSalary)), 1, 0);

        salaryGrid.add(new Label("KPI Bonus:"), 0, 1);
        salaryGrid.add(new Label("Rp " + String.format("%,.0f", kpiBonus)), 1, 1);

        salaryGrid.add(new Label("Supervisor Bonus:"), 0, 2);
        salaryGrid.add(new Label("Rp " + String.format("%,.0f", supervisorBonus)), 1, 2);

        if (penalty > 0) {
            salaryGrid.add(new Label("Performance Penalty:"), 0, 3);
            Label penaltyLabel = new Label("-Rp " + String.format("%,.0f", penalty));
            penaltyLabel.setTextFill(Color.RED);
            salaryGrid.add(penaltyLabel, 1, 3);
        }

        salaryGrid.add(new Separator(), 0, 4);
        salaryGrid.add(new Separator(), 1, 4);

        salaryGrid.add(new Label("Total Monthly Salary:"), 0, 5);
        Label totalLabel = new Label("Rp " + String.format("%,.0f", totalSalary));
        totalLabel.getStyleClass().add("total-salary"); // Use CSS class for total
        salaryGrid.add(totalLabel, 1, 5);

        breakdownBox.getChildren().addAll(breakdownTitle, salaryGrid);
        return breakdownBox;
    }

    private TableView<SalaryHistory> createMySalaryHistoryTable() {
        TableView<SalaryHistory> table = new TableView<>();
        table.setPrefHeight(300);
        table.getStyleClass().add("data-table");

        TableColumn<SalaryHistory, String> monthCol = new TableColumn<>("Month");
        monthCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMonthName()));

        TableColumn<SalaryHistory, Integer> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(new PropertyValueFactory<>("tahun"));

        TableColumn<SalaryHistory, String> baseCol = new TableColumn<>("Base Salary");
        baseCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty("Rp " + String.format("%,.0f", cellData.getValue().getBaseSalary())));

        TableColumn<SalaryHistory, String> totalCol = new TableColumn<>("Total Salary");
        totalCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty("Rp " + String.format("%,.0f", cellData.getValue().getTotalSalary())));

        table.getColumns().addAll(monthCol, yearCol, baseCol, totalCol);

        List<SalaryHistory> mySalaryHistory = dataStore.getSalaryHistoryByEmployee(supervisor.getId());
        table.setItems(FXCollections.observableArrayList(mySalaryHistory));

        return table;
    }

    private TableView<SalaryHistory> createTeamSalariesTable() {
        TableView<SalaryHistory> table = new TableView<>();
        table.setPrefHeight(500);
        table.getStyleClass().add("data-table");

        TableColumn<SalaryHistory, String> employeeCol = new TableColumn<>("Employee");
        employeeCol.setCellValueFactory(new PropertyValueFactory<>("employeeId"));

        TableColumn<SalaryHistory, String> monthCol = new TableColumn<>("Month");
        monthCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMonthName()));

        TableColumn<SalaryHistory, Integer> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(new PropertyValueFactory<>("tahun"));

        TableColumn<SalaryHistory, String> baseCol = new TableColumn<>("Base Salary");
        baseCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty("Rp " + String.format("%,.0f", cellData.getValue().getBaseSalary())));

        TableColumn<SalaryHistory, String> totalCol = new TableColumn<>("Total Salary");
        totalCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty("Rp " + String.format("%,.0f", cellData.getValue().getTotalSalary())));

        table.getColumns().addAll(employeeCol, monthCol, yearCol, baseCol, totalCol);

        List<Employee> teamMembers = dataStore.getEmployeesByDivision(supervisor.getDivisi());
        List<String> teamMemberIds = teamMembers.stream().map(Employee::getId).collect(Collectors.toList());

        List<SalaryHistory> teamSalaryHistory = dataStore.getAllSalaryHistory()
                .stream()
                .filter(salary -> teamMemberIds.contains(salary.getEmployeeId()))
                .collect(Collectors.toList());

        table.setItems(FXCollections.observableArrayList(teamSalaryHistory));

        return table;
    }

    private void showLeaveApprovalsContent() {
        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Pending Leave Approvals");
        title.getStyleClass().add("content-title");

        List<LeaveRequest> pendingRequests = dataStore.getLeaveRequestsForApproval(supervisor.getId());

        TableView<LeaveRequest> table = new TableView<>();
        table.setPrefHeight(400);
        table.getStyleClass().add("data-table");


        TableColumn<LeaveRequest, String> employeeCol = new TableColumn<>("Employee");
        employeeCol.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        employeeCol.setPrefWidth(150);

        TableColumn<LeaveRequest, String> typeCol = new TableColumn<>("Leave Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("leaveType"));
        typeCol.setPrefWidth(120);

        TableColumn<LeaveRequest, String> startDateCol = new TableColumn<>("Start Date");
        startDateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getStartDate())));
        startDateCol.setPrefWidth(100);

        TableColumn<LeaveRequest, String> endDateCol = new TableColumn<>("End Date");
        endDateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getEndDate())));
        endDateCol.setPrefWidth(100);

        TableColumn<LeaveRequest, Integer> daysCol = new TableColumn<>("Days");
        daysCol.setCellValueFactory(new PropertyValueFactory<>("totalDays"));
        daysCol.setPrefWidth(60);

        TableColumn<LeaveRequest, String> reasonCol = new TableColumn<>("Reason");
        reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));
        reasonCol.setPrefWidth(200);

        TableColumn<LeaveRequest, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(200);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button approveBtn = new Button("Approve");
            private final Button rejectBtn = new Button("Reject");

            {
                approveBtn.getStyleClass().add("action-button-small-green");
                rejectBtn.getStyleClass().add("action-button-small-red");

                approveBtn.setOnAction(e -> {
                    LeaveRequest request = getTableView().getItems().get(getIndex());
                    // Directly handle approval logic or use a helper method/dialog from controller
                    showLeaveApprovalConfirmationDialog(request, true);
                });

                rejectBtn.setOnAction(e -> {
                    LeaveRequest request = getTableView().getItems().get(getIndex());
                    // Directly handle rejection logic or use a helper method/dialog from controller
                    showLeaveApprovalConfirmationDialog(request, false);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    LeaveRequest request = getTableView().getItems().get(getIndex());
                    if (request.getStatus().equals("pending")) {
                        HBox pane = new HBox(10, approveBtn, rejectBtn);
                        pane.setAlignment(Pos.CENTER);
                        setGraphic(pane);
                    } else {
                        setGraphic(null); // Hide buttons if not pending
                    }
                }
            }
        });

        table.getColumns().addAll(employeeCol, typeCol, startDateCol, endDateCol, daysCol, reasonCol, actionCol);
        table.setItems(FXCollections.observableArrayList(pendingRequests));

        content.getChildren().addAll(title, table);
        contentArea.getChildren().add(content);
    }

    private void showLeaveApprovalConfirmationDialog(LeaveRequest request, boolean isApproval) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isApproval ? "Approve Leave Request" : "Reject Leave Request");
        dialog.setHeaderText("Leave request from " + request.getEmployeeId());

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Employee requestingEmployee = dataStore.getEmployeeById(request.getEmployeeId());
        String employeeName = requestingEmployee != null ? requestingEmployee.getNama() : "Unknown";

        Label infoLabel = new Label(String.format(
                "Employee: %s\nType: %s\nDates: %s to %s\nDays: %d\nReason: %s",
                employeeName,
                request.getLeaveType(),
                sdf.format(request.getStartDate()),
                sdf.format(request.getEndDate()),
                request.getTotalDays(),
                request.getReason()
        ));

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Enter approval/rejection notes...");
        notesArea.setPrefRowCount(3);

        content.getChildren().addAll(
                new Label("Request Details:"), infoLabel,
                new Label("Notes:"), notesArea
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                boolean success;
                if (isApproval) {
                    success = dataStore.approveLeaveRequest(request.getId(), supervisor.getId(), notesArea.getText());
                } else {
                    success = dataStore.rejectLeaveRequest(request.getId(), supervisor.getId(), notesArea.getText());
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

    private void showAllHistoryContent() {
        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("All History & Submissions");
        title.getStyleClass().add("content-title");

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("custom-tab-pane");

        Tab leaveHistoryTab = new Tab("Leave Requests", createAllLeaveRequestsTable());
        Tab reportHistoryTab = new Tab("My Reports", createMyReportHistoryTable());
        Tab evaluationHistoryTab = new Tab("Monthly Evaluations", createMonthlyEvaluationHistoryTable());
        leaveHistoryTab.setClosable(false);
        reportHistoryTab.setClosable(false);
        evaluationHistoryTab.setClosable(false);


        tabPane.getTabs().addAll(leaveHistoryTab, reportHistoryTab, evaluationHistoryTab);

        content.getChildren().addAll(title, tabPane);
        contentArea.getChildren().add(content);
    }

    private TableView<LeaveRequest> createAllLeaveRequestsTable() {
        TableView<LeaveRequest> table = new TableView<>();
        table.setPrefHeight(400);
        table.getStyleClass().add("data-table");

        TableColumn<LeaveRequest, String> employeeCol = new TableColumn<>("Employee");
        employeeCol.setCellValueFactory(new PropertyValueFactory<>("employeeId"));

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

        table.getColumns().addAll(employeeCol, typeCol, startDateCol, endDateCol, daysCol, statusCol);

        List<Employee> teamMembers = dataStore.getEmployeesByDivision(supervisor.getDivisi());
        List<String> teamMemberIds = teamMembers.stream().map(Employee::getId).collect(Collectors.toList());
        teamMemberIds.add(supervisor.getId()); // Include supervisor's own requests

        List<LeaveRequest> teamLeaveRequests = dataStore.getAllLeaveRequests()
                .stream()
                .filter(leave -> teamMemberIds.contains(leave.getEmployeeId()))
                .collect(Collectors.toList());

        table.setItems(FXCollections.observableArrayList(teamLeaveRequests));

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