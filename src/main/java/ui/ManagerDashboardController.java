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
import javafx.stage.Stage;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ManagerDashboardController {

    @FXML
    private BorderPane rootPane;
    @FXML
    private Label userWelcomeLabel;
    @FXML
    private VBox navButtonContainer;
    @FXML
    private StackPane contentArea;

    private Employee manager;
    private MySQLDataStore dataStore;
    private Stage stage;

    // TableView instances for refreshing
    private TableView<KPI> kpiHistoryTable;
    private TableView<Report> pendingReportsTable;
    private TableView<Report> reportHistoryTable;
    private TableView<LeaveRequest> leaveApprovalsTable;

    private DecimalFormat df = new DecimalFormat("#.##");
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

    private static final Logger logger = Logger.getLogger(ManagerDashboardController.class.getName());

    public void setManager(Employee manager) {
        this.manager = manager;
        if (userWelcomeLabel != null) {
            userWelcomeLabel.setText("Welcome, " + manager.getNama() + " (Manager)");
        }
        initializeContent();
    }

    public void setDataStore(MySQLDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        if (manager != null) {
            this.stage.setTitle("GAWE - Manager Dashboard - " + manager.getNama());
        }
        this.stage.setOnCloseRequest(e -> stopApplication());
    }

    @FXML
    public void initialize() {
        populateNavigationButtons();
    }

    private void initializeContent() {
        if (manager != null && dataStore != null) {
            if (userWelcomeLabel != null) {
                userWelcomeLabel.setText("Welcome, " + manager.getNama() + " (Manager)");
            }
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
                    createNavButton("📈 KPI Management", this::showKPIManagementContent),
                    createNavButton("📄 Report Reviews", this::showReportReviewsContent),
                    createNavButton("⭐ Evaluation History", this::showEvaluationHistoryContent),
                    createNavButton("🏖️ Leave Approvals", this::showLeaveApprovalsContent),
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
        if (manager == null || dataStore == null || contentArea == null) {
            logger.warning("Cannot show dashboard content - missing required objects");
            return;
        }

        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Manager Dashboard");
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
            if (manager == null || dataStore == null) {
                return false;
            }
            List<Attendance> todayAttendance = dataStore.getTodayAttendance(manager.getId());
            return !todayAttendance.isEmpty();
        } catch (Exception e) {
            logger.severe("Error checking today's attendance: " + e.getMessage());
            return false;
        }
    }

    private boolean hasCompletedAttendanceToday() {
        try {
            if (manager == null || dataStore == null) {
                return false;
            }
            List<Attendance> todayAttendance = dataStore.getTodayAttendance(manager.getId());
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

        boolean success = dataStore.saveAttendance(manager.getId(), new Date(), timeStr, null, "hadir");
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

        boolean success = dataStore.updateAttendanceClockOut(manager.getId(), timeStr);
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

        if (dataStore == null) {
            return statsContainer;
        }

        try {
            List<Employee> allEmployees = dataStore.getAllEmployees();
            List<Report> pendingReports = dataStore.getPendingReports();
            List<LeaveRequest> pendingLeaves = dataStore.getPendingLeaveRequests();

            // Refresh manager object to get updated leave balance
            Employee refreshedManager = dataStore.authenticateUser(manager.getId(), manager.getPassword());
            int managerLeaveDays = (refreshedManager != null) ? refreshedManager.getSisaCuti() : manager.getSisaCuti();

            VBox totalEmployeesCard = createStatsCard("Total Employees", String.valueOf(allEmployees.size()), "👥", "#3498db");
            VBox pendingReportsCard = createStatsCard("Pending Reports", String.valueOf(pendingReports.size()), "📄", "#e74c3c");
            VBox pendingLeavesCard = createStatsCard("Pending Leaves", String.valueOf(pendingLeaves.size()), "🏖️", "#f39c12");
            VBox myLeaveCard = createStatsCard("My Leave Days", String.valueOf(managerLeaveDays), "🌴", "#9b59b6");

            statsContainer.getChildren().addAll(totalEmployeesCard, pendingReportsCard, pendingLeavesCard, myLeaveCard);
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

        if (dataStore != null) {
            try {
                ObservableList<String> activities = FXCollections.observableArrayList(
                        "📊 Dashboard accessed - just now",
                        "📄 " + dataStore.getPendingReports().size() + " reports pending review",
                        "🏖️ " + dataStore.getPendingLeaveRequests().size() + " leave requests pending approval",
                        "👥 Managing " + dataStore.getAllEmployees().size() + " employees"
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
        if (manager == null || dataStore == null || contentArea == null) {
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

        if (dataStore != null && manager != null) {
            try {
                List<Attendance> myAttendance = dataStore.getAttendanceByEmployee(manager.getId());
                table.setItems(FXCollections.observableArrayList(myAttendance));
            } catch (Exception e) {
                logger.severe("Error loading attendance: " + e.getMessage());
            }
        }
        table.setPrefHeight(400);

        return table;
    }

    private void showMyMeetings() {
        if (manager == null || dataStore == null || contentArea == null) {
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

        if (dataStore != null && manager != null) {
            try {
                List<Meeting> myMeetings = dataStore.getMeetingsByEmployee(manager.getId());
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
                        List<String> participants = dataStore.getAllEmployees().stream()
                                .map(Employee::getId)
                                .collect(Collectors.toList());

                        boolean success = dataStore.saveMeeting(
                                titleField.getText(),
                                descriptionArea.getText(),
                                meetingDate,
                                startTimeField.getText(),
                                endTimeField.getText(),
                                locationField.getText(),
                                manager.getId(),
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
        if (manager == null || dataStore == null || contentArea == null) {
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

        if (dataStore != null && manager != null) {
            try {
                List<LeaveRequest> myLeaveRequests = dataStore.getLeaveRequestsByEmployee(manager.getId());
                table.setItems(FXCollections.observableArrayList(myLeaveRequests));
            } catch (Exception e) {
                logger.severe("Error loading leave requests: " + e.getMessage());
            }
        }
        table.setPrefHeight(400);

        return table;
    }

    private void showLeaveRequestDialog() {
        if (manager == null || dataStore == null) {
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
                        boolean success = dataStore.saveLeaveRequest(manager.getId(), leaveTypeCombo.getValue(),
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

    // Manager-specific features
    private void showKPIManagementContent() {
        if (manager == null || dataStore == null || contentArea == null) {
            return;
        }

        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("KPI Management");
        title.getStyleClass().add("content-title");

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("custom-tab-pane");

        Tab setKpiTab = new Tab("Set KPI", createKPISetForm());

        if (kpiHistoryTable == null) {
            kpiHistoryTable = createKPIHistoryTable();
        }
        Tab kpiHistoryTab = new Tab("KPI History", kpiHistoryTable);

        setKpiTab.setClosable(false);
        kpiHistoryTab.setClosable(false);

        tabPane.getTabs().addAll(setKpiTab, kpiHistoryTab);

        content.getChildren().addAll(title, tabPane);
        contentArea.getChildren().add(content);
    }

    private VBox createKPISetForm() {
        VBox form = new VBox(20);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(30));
        form.getStyleClass().add("kpi-set-form");

        Label formTitle = new Label("Set Division KPI");
        formTitle.getStyleClass().add("form-title");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(20);
        formGrid.setVgap(15);
        formGrid.setAlignment(Pos.CENTER);

        ComboBox<String> divisionCombo = new ComboBox<>();
        divisionCombo.getItems().addAll("HR", "Marketing", "Sales", "IT", "Finance");
        divisionCombo.setPromptText("Select Division");

        ComboBox<String> monthCombo = new ComboBox<>();
        monthCombo.getItems().addAll("January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December");
        monthCombo.setValue("December");

        ComboBox<Integer> yearCombo = new ComboBox<>();
        for (int year = 2020; year <= 2030; year++) {
            yearCombo.getItems().add(year);
        }
        yearCombo.setValue(LocalDate.now().getYear());

        Slider kpiSlider = new Slider(0, 100, 75);
        kpiSlider.setShowTickLabels(true);
        kpiSlider.setShowTickMarks(true);
        kpiSlider.setMajorTickUnit(25);

        Label kpiValue = new Label("75.0");
        kpiSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                kpiValue.setText(String.format("%.1f", newVal.doubleValue())));

        formGrid.add(new Label("Division:"), 0, 0);
        formGrid.add(divisionCombo, 1, 0);
        formGrid.add(new Label("Month:"), 0, 1);
        formGrid.add(monthCombo, 1, 1);
        formGrid.add(new Label("Year:"), 0, 2);
        formGrid.add(yearCombo, 1, 2);
        formGrid.add(new Label("KPI Score:"), 0, 3);
        formGrid.add(kpiSlider, 1, 3);
        formGrid.add(kpiValue, 2, 3);

        Button submitBtn = new Button("Set KPI");
        submitBtn.getStyleClass().add("action-button-green");

        submitBtn.setOnAction(e -> {
            if (divisionCombo.getValue() != null) {
                try {
                    boolean success = dataStore.saveKPI(
                            divisionCombo.getValue(),
                            monthCombo.getSelectionModel().getSelectedIndex() + 1,
                            yearCombo.getValue(),
                            kpiSlider.getValue(),
                            manager.getId()
                    );

                    if (success) {
                        showAlert(Alert.AlertType.INFORMATION, "Success", "KPI set successfully!");
                        divisionCombo.setValue(null);
                        kpiSlider.setValue(75);
                        refreshKPIHistoryTable();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to set KPI.");
                    }
                } catch (Exception ex) {
                    logger.severe("Error setting KPI: " + ex.getMessage());
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to set KPI: " + ex.getMessage());
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "Warning", "Please select a division.");
            }
        });

        form.getChildren().addAll(formTitle, formGrid, submitBtn);
        return form;
    }

    private TableView<KPI> createKPIHistoryTable() {
        TableView<KPI> table = new TableView<>();
        table.setPrefHeight(400);
        table.getStyleClass().add("data-table");

        TableColumn<KPI, String> divisionCol = new TableColumn<>("Division");
        divisionCol.setCellValueFactory(new PropertyValueFactory<>("divisi"));

        TableColumn<KPI, String> monthCol = new TableColumn<>("Month");
        monthCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMonthName()));

        TableColumn<KPI, Integer> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(new PropertyValueFactory<>("tahun"));

        TableColumn<KPI, String> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getScore()) + "%"));

        TableColumn<KPI, String> dateCol = new TableColumn<>("Created Date");
        dateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getCreatedDate())));

        table.getColumns().addAll(divisionCol, monthCol, yearCol, scoreCol, dateCol);

        refreshKPIHistoryTable(table);

        return table;
    }

    private void refreshKPIHistoryTable() {
        if (kpiHistoryTable != null && dataStore != null) {
            try {
                List<KPI> allKPI = dataStore.getAllKPI();
                kpiHistoryTable.setItems(FXCollections.observableArrayList(allKPI));
            } catch (Exception e) {
                logger.severe("Error refreshing KPI history: " + e.getMessage());
            }
        }
    }

    private void refreshKPIHistoryTable(TableView<KPI> table) {
        if (dataStore != null) {
            try {
                List<KPI> allKPI = dataStore.getAllKPI();
                table.setItems(FXCollections.observableArrayList(allKPI));
            } catch (Exception e) {
                logger.severe("Error loading KPI history: " + e.getMessage());
            }
        }
    }

    private void showReportReviewsContent() {
        if (manager == null || dataStore == null || contentArea == null) {
            return;
        }

        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Report Reviews");
        title.getStyleClass().add("content-title");

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("custom-tab-pane");

        if (pendingReportsTable == null) {
            pendingReportsTable = createPendingReportsTable();
        }
        Tab pendingTab = new Tab("Pending Reports", pendingReportsTable);

        if (reportHistoryTable == null) {
            reportHistoryTable = createReportHistoryTable();
        }
        Tab historyTab = new Tab("Report History", reportHistoryTable);

        pendingTab.setClosable(false);
        historyTab.setClosable(false);

        tabPane.getTabs().addAll(pendingTab, historyTab);

        content.getChildren().addAll(title, tabPane);
        contentArea.getChildren().add(content);
    }

    private TableView<Report> createPendingReportsTable() {
        TableView<Report> table = new TableView<>();
        table.setPrefHeight(400);
        table.getStyleClass().add("data-table");

        TableColumn<Report, String> divisionCol = new TableColumn<>("Division");
        divisionCol.setCellValueFactory(new PropertyValueFactory<>("divisi"));

        TableColumn<Report, String> monthCol = new TableColumn<>("Month");
        monthCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMonthName()));

        TableColumn<Report, Integer> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(new PropertyValueFactory<>("tahun"));

        TableColumn<Report, String> supervisorCol = new TableColumn<>("Supervisor");
        supervisorCol.setCellValueFactory(new PropertyValueFactory<>("supervisorId"));

        TableColumn<Report, String> uploadDateCol = new TableColumn<>("Upload Date");
        uploadDateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getUploadDate())));

        TableColumn<Report, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<Report, Void>() {
            private final Button reviewBtn = new Button("Review");

            {
                reviewBtn.getStyleClass().add("review-button");
                reviewBtn.setOnAction(e -> {
                    Report report = getTableView().getItems().get(getIndex());
                    showReportReviewDialog(report);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : reviewBtn);
            }
        });

        table.getColumns().addAll(divisionCol, monthCol, yearCol, supervisorCol, uploadDateCol, actionCol);

        refreshPendingReportsTable(table);

        return table;
    }

    private void refreshPendingReportsTable(TableView<Report> table) {
        if (dataStore != null) {
            try {
                List<Report> pendingReports = dataStore.getPendingReports();
                table.setItems(FXCollections.observableArrayList(pendingReports));
            } catch (Exception e) {
                logger.severe("Error loading pending reports: " + e.getMessage());
            }
        }
    }

    private TableView<Report> createReportHistoryTable() {
        TableView<Report> table = new TableView<>();
        table.setPrefHeight(400);
        table.getStyleClass().add("data-table");

        TableColumn<Report, String> divisionCol = new TableColumn<>("Division");
        divisionCol.setCellValueFactory(new PropertyValueFactory<>("divisi"));

        TableColumn<Report, String> monthCol = new TableColumn<>("Month");
        monthCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMonthName()));

        TableColumn<Report, Integer> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(new PropertyValueFactory<>("tahun"));

        TableColumn<Report, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<Report, String> reviewedByCol = new TableColumn<>("Reviewed By");
        reviewedByCol.setCellValueFactory(new PropertyValueFactory<>("reviewedBy"));

        TableColumn<Report, String> reviewDateCol = new TableColumn<>("Review Date");
        reviewDateCol.setCellValueFactory(cellData -> {
            Date reviewDate = cellData.getValue().getReviewedDate();
            return new javafx.beans.property.SimpleStringProperty(
                    reviewDate != null ? sdf.format(reviewDate) : "");
        });

        TableColumn<Report, String> notesCol = new TableColumn<>("Manager Notes");
        notesCol.setCellValueFactory(new PropertyValueFactory<>("managerNotes"));

        table.getColumns().addAll(divisionCol, monthCol, yearCol, statusCol, reviewedByCol, reviewDateCol, notesCol);

        refreshReportHistoryTable(table);

        return table;
    }

    private void refreshReportHistoryTable(TableView<Report> table) {
        if (dataStore != null) {
            try {
                List<Report> allReports = dataStore.getAllReports();
                table.setItems(FXCollections.observableArrayList(allReports));
            } catch (Exception e) {
                logger.severe("Error loading report history: " + e.getMessage());
            }
        }
    }

    private void showReportReviewDialog(Report report) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Review Report");
        dialog.setHeaderText("Review " + report.getDivisi() + " Division Report");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label infoLabel = new Label(String.format("Division: %s\nMonth: %s %d\nSupervisor: %s",
                report.getDivisi(), report.getMonthName(), report.getTahun(), report.getSupervisorId()));

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Enter review notes...");
        notesArea.setPrefRowCount(4);

        content.getChildren().addAll(
                new Label("Report Information:"), infoLabel,
                new Label("Review Notes:"), notesArea
        );

        dialog.getDialogPane().setContent(content);

        ButtonType approveBtn = new ButtonType("Approve", ButtonBar.ButtonData.OK_DONE);
        ButtonType rejectBtn = new ButtonType("Reject", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(approveBtn, rejectBtn, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == approveBtn || result == rejectBtn) {
                String status = (result == approveBtn) ? "approved" : "rejected";
                try {
                    boolean success = dataStore.updateReportStatus(report.getId(), status, notesArea.getText(), manager.getId());

                    if (success) {
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Report " + status + " successfully!");
                        refreshPendingReportsTable(pendingReportsTable);
                        refreshReportHistoryTable(reportHistoryTable);
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to update report status.");
                    }
                } catch (Exception e) {
                    logger.severe("Error updating report status: " + e.getMessage());
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to update report status: " + e.getMessage());
                }
            }
        });
    }

    // Continue with the rest of the implementation...
    private void showEvaluationHistoryContent() {
        if (manager == null || dataStore == null || contentArea == null) {
            return;
        }

        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Evaluation History");
        title.getStyleClass().add("content-title");

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("custom-tab-pane");

        Tab monthlyEvaluationsTab = new Tab("Monthly Evaluations", createMonthlyEvaluationsTable());
        Tab regularEvaluationsTab = new Tab("Regular Evaluations", createRegularEvaluationsTable());

        monthlyEvaluationsTab.setClosable(false);
        regularEvaluationsTab.setClosable(false);

        tabPane.getTabs().addAll(monthlyEvaluationsTab, regularEvaluationsTab);

        content.getChildren().addAll(title, tabPane);
        contentArea.getChildren().add(content);
    }

    private TableView<MySQLDataStore.MonthlyEvaluation> createMonthlyEvaluationsTable() {
        TableView<MySQLDataStore.MonthlyEvaluation> table = new TableView<>();
        table.getStyleClass().add("data-table");

        TableColumn<MySQLDataStore.MonthlyEvaluation, String> employeeCol = new TableColumn<>("Employee");
        employeeCol.setCellValueFactory(cellData -> {
            Employee emp = dataStore.getEmployeeById(cellData.getValue().getEmployeeId());
            return new javafx.beans.property.SimpleStringProperty(emp != null ? emp.getNama() : "Unknown");
        });

        TableColumn<MySQLDataStore.MonthlyEvaluation, String> supervisorCol = new TableColumn<>("Supervisor");
        supervisorCol.setCellValueFactory(cellData -> {
            Employee sup = dataStore.getEmployeeById(cellData.getValue().getSupervisorId());
            return new javafx.beans.property.SimpleStringProperty(sup != null ? sup.getNama() : "Unknown");
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

        table.getColumns().addAll(employeeCol, supervisorCol, monthCol, yearCol, overallCol, dateCol);

        try {
            List<MySQLDataStore.MonthlyEvaluation> evaluations = dataStore.getAllMonthlyEvaluations();
            table.setItems(FXCollections.observableArrayList(evaluations));
        } catch (Exception e) {
            logger.severe("Error loading monthly evaluations: " + e.getMessage());
        }

        table.setPrefHeight(400);
        return table;
    }

    private TableView<EmployeeEvaluation> createRegularEvaluationsTable() {
        TableView<EmployeeEvaluation> table = new TableView<>();
        table.getStyleClass().add("data-table");

        TableColumn<EmployeeEvaluation, String> employeeCol = new TableColumn<>("Employee");
        employeeCol.setCellValueFactory(cellData -> {
            Employee emp = dataStore.getEmployeeById(cellData.getValue().getEmployeeId());
            return new javafx.beans.property.SimpleStringProperty(emp != null ? emp.getNama() : "Unknown");
        });

        TableColumn<EmployeeEvaluation, String> supervisorCol = new TableColumn<>("Supervisor");
        supervisorCol.setCellValueFactory(cellData -> {
            Employee sup = dataStore.getEmployeeById(cellData.getValue().getSupervisorId());
            return new javafx.beans.property.SimpleStringProperty(sup != null ? sup.getNama() : "Unknown");
        });

        TableColumn<EmployeeEvaluation, String> punctualityCol = new TableColumn<>("Punctuality");
        punctualityCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getPunctualityScore()) + "%"));

        TableColumn<EmployeeEvaluation, String> attendanceCol = new TableColumn<>("Attendance");
        attendanceCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getAttendanceScore()) + "%"));

        TableColumn<EmployeeEvaluation, String> overallCol = new TableColumn<>("Overall Rating");
        overallCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getOverallRating()) + "%"));

        TableColumn<EmployeeEvaluation, String> dateCol = new TableColumn<>("Evaluation Date");
        dateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getEvaluationDate())));

        table.getColumns().addAll(employeeCol, supervisorCol, punctualityCol, attendanceCol, overallCol, dateCol);

        try {
            List<EmployeeEvaluation> evaluations = dataStore.getAllEvaluations();
            table.setItems(FXCollections.observableArrayList(evaluations));
        } catch (Exception e) {
            logger.severe("Error loading regular evaluations: " + e.getMessage());
        }

        table.setPrefHeight(400);
        return table;
    }

    private void showLeaveApprovalsContent() {
        if (manager == null || dataStore == null || contentArea == null) {
            return;
        }

        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Leave Request Approvals - Manager Level");
        title.getStyleClass().add("content-title");

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("custom-tab-pane");

        Tab pendingTab = new Tab("Pending Approvals", createManagerLeaveApprovalTable());
        Tab historyTab = new Tab("Approval History", createManagerLeaveApprovalHistoryTable());

        pendingTab.setClosable(false);
        historyTab.setClosable(false);

        tabPane.getTabs().addAll(pendingTab, historyTab);

        content.getChildren().addAll(title, tabPane);
        contentArea.getChildren().add(content);
    }

// Add these new methods to ManagerDashboardController.java:

    private TableView<LeaveRequest> createManagerLeaveApprovalTable() {
        TableView<LeaveRequest> table = new TableView<>();
        table.getStyleClass().add("data-table");

        TableColumn<LeaveRequest, String> employeeCol = new TableColumn<>("Employee");
        employeeCol.setCellValueFactory(cellData -> {
            Employee emp = dataStore.getEmployeeById(cellData.getValue().getEmployeeId());
            return new javafx.beans.property.SimpleStringProperty(emp != null ? emp.getNama() + " (" + emp.getId() + ")" : "Unknown");
        });
        employeeCol.setPrefWidth(180);

        TableColumn<LeaveRequest, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(cellData -> {
            Employee emp = dataStore.getEmployeeById(cellData.getValue().getEmployeeId());
            String role = emp != null ? emp.getRole() : "Unknown";
            return new javafx.beans.property.SimpleStringProperty(
                    role.substring(0, 1).toUpperCase() + role.substring(1));
        });

        TableColumn<LeaveRequest, String> divisionCol = new TableColumn<>("Division");
        divisionCol.setCellValueFactory(cellData -> {
            Employee emp = dataStore.getEmployeeById(cellData.getValue().getEmployeeId());
            return new javafx.beans.property.SimpleStringProperty(emp != null ? emp.getDivisi() : "Unknown");
        });

        TableColumn<LeaveRequest, String> typeCol = new TableColumn<>("Leave Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("leaveType"));

        TableColumn<LeaveRequest, String> startDateCol = new TableColumn<>("Start Date");
        startDateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getStartDate())));

        TableColumn<LeaveRequest, String> endDateCol = new TableColumn<>("End Date");
        endDateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getEndDate())));

        TableColumn<LeaveRequest, Integer> daysCol = new TableColumn<>("Days");
        daysCol.setCellValueFactory(new PropertyValueFactory<>("totalDays"));

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
                    showManagerLeaveApprovalDialog(request, true);
                });

                rejectBtn.setOnAction(e -> {
                    LeaveRequest request = getTableView().getItems().get(getIndex());
                    showManagerLeaveApprovalDialog(request, false);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actionBox);
            }
        });

        table.getColumns().addAll(employeeCol, roleCol, divisionCol, typeCol, startDateCol, endDateCol, daysCol, actionCol);

        if (dataStore != null && manager != null) {
            List<LeaveRequest> pendingRequests = dataStore.getLeaveRequestsForApproval(manager.getId());
            table.setItems(FXCollections.observableArrayList(pendingRequests));
        }
        table.setPrefHeight(400);

        return table;
    }

    private TableView<LeaveRequest> createManagerLeaveApprovalHistoryTable() {
        TableView<LeaveRequest> table = new TableView<>();
        table.getStyleClass().add("data-table");

        TableColumn<LeaveRequest, String> employeeCol = new TableColumn<>("Employee");
        employeeCol.setCellValueFactory(cellData -> {
            Employee emp = dataStore.getEmployeeById(cellData.getValue().getEmployeeId());
            return new javafx.beans.property.SimpleStringProperty(emp != null ? emp.getNama() : "Unknown");
        });

        TableColumn<LeaveRequest, String> typeCol = new TableColumn<>("Leave Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("leaveType"));

        TableColumn<LeaveRequest, String> startDateCol = new TableColumn<>("Start Date");
        startDateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getStartDate())));

        TableColumn<LeaveRequest, Integer> daysCol = new TableColumn<>("Days");
        daysCol.setCellValueFactory(new PropertyValueFactory<>("totalDays"));

        TableColumn<LeaveRequest, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<LeaveRequest, String> approvalDateCol = new TableColumn<>("Approval Date");
        approvalDateCol.setCellValueFactory(cellData -> {
            Date approvalDate = cellData.getValue().getApprovalDate();
            return new javafx.beans.property.SimpleStringProperty(
                    approvalDate != null ? sdf.format(approvalDate) : "");
        });

        TableColumn<LeaveRequest, String> notesCol = new TableColumn<>("Manager Notes");
        notesCol.setCellValueFactory(new PropertyValueFactory<>("approverNotes"));

        table.getColumns().addAll(employeeCol, typeCol, startDateCol, daysCol, statusCol, approvalDateCol, notesCol);

        if (dataStore != null && manager != null) {
            List<LeaveRequest> processedRequests = dataStore.getAllLeaveRequests().stream()
                    .filter(lr -> manager.getId().equals(lr.getApproverId()))
                    .filter(lr -> !"pending".equals(lr.getStatus()))
                    .sorted((l1, l2) -> {
                        Date d1 = l1.getApprovalDate();
                        Date d2 = l2.getApprovalDate();
                        if (d1 == null && d2 == null) return 0;
                        if (d1 == null) return 1;
                        if (d2 == null) return -1;
                        return d2.compareTo(d1);
                    })
                    .collect(Collectors.toList());
            table.setItems(FXCollections.observableArrayList(processedRequests));
        }
        table.setPrefHeight(400);

        return table;
    }

    private void showManagerLeaveApprovalDialog(LeaveRequest request, boolean isApproval) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isApproval ? "Approve Leave Request" : "Reject Leave Request");

        Employee requestingEmployee = dataStore.getEmployeeById(request.getEmployeeId());
        String employeeName = requestingEmployee != null ? requestingEmployee.getNama() : "Unknown";
        String employeeRole = requestingEmployee != null ? requestingEmployee.getRole() : "Unknown";

        dialog.setHeaderText((isApproval ? "Approve" : "Reject") + " leave request from " + employeeName + " (" + employeeRole + ")");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Request summary
        VBox summaryBox = new VBox(5);
        summaryBox.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-background-radius: 5;");
        summaryBox.getChildren().addAll(
                new Label("Employee: " + employeeName + " (" + request.getEmployeeId() + ")"),
                new Label("Role: " + employeeRole + " | Division: " + (requestingEmployee != null ? requestingEmployee.getDivisi() : "Unknown")),
                new Label("Leave Type: " + request.getLeaveType()),
                new Label("Period: " + sdf.format(request.getStartDate()) + " to " + sdf.format(request.getEndDate())),
                new Label("Total Days: " + request.getTotalDays()),
                new Label("Reason: " + request.getReason())
        );

        // Leave balance warning
        VBox warningBox = new VBox(5);
        if (requestingEmployee != null) {
            int remainingLeave = requestingEmployee.getSisaCuti();
            if (request.getTotalDays() > remainingLeave) {
                Label warningLabel = new Label("⚠️ WARNING: Employee has insufficient leave days!");
                Label detailLabel = new Label("Requested: " + request.getTotalDays() + " days | Available: " + remainingLeave + " days");
                warningLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                detailLabel.setStyle("-fx-text-fill: #e74c3c;");
                warningBox.getChildren().addAll(warningLabel, detailLabel);
            } else {
                Label okLabel = new Label("✅ Employee has sufficient leave balance (" + remainingLeave + " days available)");
                okLabel.setStyle("-fx-text-fill: #27ae60;");
                warningBox.getChildren().add(okLabel);
            }
        }

        // Manager notes
        Label notesLabel = new Label("Manager Notes:");
        notesLabel.setStyle("-fx-font-weight: bold;");
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Enter your " + (isApproval ? "approval" : "rejection") + " notes...");
        notesArea.setPrefRowCount(4);

        content.getChildren().addAll(summaryBox, warningBox, new Separator(), notesLabel, notesArea);

        dialog.getDialogPane().setContent(content);

        ButtonType actionButton = new ButtonType(isApproval ? "Approve" : "Reject", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(actionButton, cancelButton);

        dialog.showAndWait().ifPresent(result -> {
            if (result == actionButton) {
                String notes = notesArea.getText().trim();
                if (notes.isEmpty()) {
                    notes = isApproval ? "Approved by manager" : "Rejected by manager";
                }

                // Additional confirmation for insufficient balance
                if (isApproval && requestingEmployee != null && request.getTotalDays() > requestingEmployee.getSisaCuti()) {
                    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmAlert.setTitle("Confirm Approval");
                    confirmAlert.setHeaderText("Employee has insufficient leave balance");
                    confirmAlert.setContentText("This approval will result in negative leave balance. Continue?");
                    if (confirmAlert.showAndWait().get() != ButtonType.OK) {
                        return;
                    }
                }

                boolean success;
                if (isApproval) {
                    success = dataStore.approveLeaveRequest(request.getId(), manager.getId(), notes);
                } else {
                    success = dataStore.rejectLeaveRequest(request.getId(), manager.getId(), notes);
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


    private void refreshLeaveApprovalsTable(TableView<LeaveRequest> table) {
        if (dataStore != null && manager != null) {
            try {
                List<LeaveRequest> pendingRequests = dataStore.getLeaveRequestsForApproval(manager.getId());
                table.setItems(FXCollections.observableArrayList(pendingRequests));
            } catch (Exception e) {
                logger.severe("Error loading leave requests for approval: " + e.getMessage());
            }
        }
    }

    private void showSalaryManagementContent() {
        if (manager == null || dataStore == null || contentArea == null) {
            return;
        }

        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Salary Management");
        title.getStyleClass().add("content-title");

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("custom-tab-pane");

        Tab salaryOverviewTab = new Tab("Salary Overview", createSalaryOverviewTable());
        Tab salaryHistoryTab = new Tab("Salary History", createAllSalaryHistoryTable());

        salaryOverviewTab.setClosable(false);
        salaryHistoryTab.setClosable(false);

        tabPane.getTabs().addAll(salaryOverviewTab, salaryHistoryTab);

        content.getChildren().addAll(title, tabPane);
        contentArea.getChildren().add(content);
    }

    private TableView<Employee> createSalaryOverviewTable() {
        TableView<Employee> table = new TableView<>();
        table.getStyleClass().add("data-table");

        TableColumn<Employee, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Employee, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("nama"));

        TableColumn<Employee, String> divisionCol = new TableColumn<>("Division");
        divisionCol.setCellValueFactory(new PropertyValueFactory<>("divisi"));

        TableColumn<Employee, String> baseSalaryCol = new TableColumn<>("Base Salary");
        baseSalaryCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.format("Rp %,.0f", cellData.getValue().getGajiPokok())));

        TableColumn<Employee, String> kpiScoreCol = new TableColumn<>("KPI Score");
        kpiScoreCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getKpiScore()) + "%"));

        TableColumn<Employee, String> supervisorRatingCol = new TableColumn<>("Supervisor Rating");
        supervisorRatingCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getSupervisorRating()) + "%"));

        TableColumn<Employee, String> currentSalaryCol = new TableColumn<>("Current Salary");
        currentSalaryCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.format("Rp %,.0f", cellData.getValue().calculateGajiBulanan())));

        table.getColumns().addAll(idCol, nameCol, divisionCol, baseSalaryCol, kpiScoreCol, supervisorRatingCol, currentSalaryCol);

        try {
            List<Employee> allEmployees = dataStore.getAllEmployees();
            table.setItems(FXCollections.observableArrayList(allEmployees));
        } catch (Exception e) {
            logger.severe("Error loading salary overview: " + e.getMessage());
        }

        table.setPrefHeight(400);
        return table;
    }

    private TableView<SalaryHistory> createAllSalaryHistoryTable() {
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

        try {
            List<SalaryHistory> allSalaryHistory = dataStore.getAllSalaryHistory();
            table.setItems(FXCollections.observableArrayList(allSalaryHistory));
        } catch (Exception e) {
            logger.severe("Error loading salary history: " + e.getMessage());
        }

        table.setPrefHeight(400);
        return table;
    }

    private void showAllHistoryContent() {
        if (manager == null || dataStore == null || contentArea == null) {
            return;
        }

        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("All History & Records");
        title.getStyleClass().add("content-title");

        TabPane historyTabs = new TabPane();
        historyTabs.getStyleClass().add("custom-tab-pane");

        Tab kpiHistoryTab = new Tab("KPI History", createKPIHistoryTable());
        Tab reportsHistoryTab = new Tab("Reports", createReportHistoryTable());
        Tab evaluationsHistoryTab = new Tab("Evaluations", createMonthlyEvaluationsTable());
        Tab leaveHistoryTab = new Tab("Leave Requests", createAllLeaveRequestsTable());
        Tab meetingsHistoryTab = new Tab("Meetings", createAllMeetingsTable());
        Tab attendanceHistoryTab = new Tab("Attendance", createAllAttendanceTable());

        kpiHistoryTab.setClosable(false);
        reportsHistoryTab.setClosable(false);
        evaluationsHistoryTab.setClosable(false);
        leaveHistoryTab.setClosable(false);
        meetingsHistoryTab.setClosable(false);
        attendanceHistoryTab.setClosable(false);

        historyTabs.getTabs().addAll(kpiHistoryTab, reportsHistoryTab, evaluationsHistoryTab,
                leaveHistoryTab, meetingsHistoryTab, attendanceHistoryTab);

        content.getChildren().addAll(title, historyTabs);
        contentArea.getChildren().add(content);
    }

    private TableView<LeaveRequest> createAllLeaveRequestsTable() {
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

        TableColumn<LeaveRequest, String> approverCol = new TableColumn<>("Approver");
        approverCol.setCellValueFactory(cellData -> {
            String approverId = cellData.getValue().getApproverId();
            if (approverId != null) {
                Employee approver = dataStore.getEmployeeById(approverId);
                return new javafx.beans.property.SimpleStringProperty(approver != null ? approver.getNama() : approverId);
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });

        table.getColumns().addAll(employeeCol, typeCol, startDateCol, daysCol, statusCol, approverCol);

        try {
            List<LeaveRequest> allLeaveRequests = dataStore.getAllLeaveRequests();
            table.setItems(FXCollections.observableArrayList(allLeaveRequests));
        } catch (Exception e) {
            logger.severe("Error loading leave requests history: " + e.getMessage());
        }

        table.setPrefHeight(350);
        return table;
    }

    private TableView<Meeting> createAllMeetingsTable() {
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

        TableColumn<Meeting, String> organizerCol = new TableColumn<>("Organizer");
        organizerCol.setCellValueFactory(cellData -> {
            Employee organizer = dataStore.getEmployeeById(cellData.getValue().getOrganizerId());
            return new javafx.beans.property.SimpleStringProperty(organizer != null ? organizer.getNama() : "Unknown");
        });

        TableColumn<Meeting, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(titleCol, dateCol, timeCol, organizerCol, statusCol);

        try {
            // For simplicity, we'll get all meetings by getting meetings for all employees
            List<Meeting> allMeetings = dataStore.getAllEmployees().stream()
                    .flatMap(emp -> dataStore.getMeetingsByEmployee(emp.getId()).stream())
                    .distinct()
                    .collect(Collectors.toList());
            table.setItems(FXCollections.observableArrayList(allMeetings));
        } catch (Exception e) {
            logger.severe("Error loading meetings history: " + e.getMessage());
        }

        table.setPrefHeight(350);
        return table;
    }

    private TableView<Attendance> createAllAttendanceTable() {
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

        try {
            // For simplicity, we'll get attendance for all employees in the last 30 days
            List<Attendance> allAttendance = dataStore.getAllEmployees().stream()
                    .flatMap(emp -> dataStore.getAttendanceByEmployee(emp.getId()).stream())
                    .collect(Collectors.toList());
            table.setItems(FXCollections.observableArrayList(allAttendance));
        } catch (Exception e) {
            logger.severe("Error loading attendance history: " + e.getMessage());
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