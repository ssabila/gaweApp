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

    // TableView instance for KPI History to allow refreshing
    private TableView<KPI> kpiHistoryTable;

    private DecimalFormat df = new DecimalFormat("#.##");
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

    private static final Logger logger = Logger.getLogger(ManagerDashboardController.class.getName());

    public void setManager(Employee manager) {
        this.manager = manager;
        userWelcomeLabel.setText("Welcome, " + manager.getNama() + " (Manager)");
    }

    public void setDataStore(MySQLDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        this.stage.setTitle("GAWE - Manager Dashboard - " + manager.getNama());
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
            List<Attendance> todayAttendance = dataStore.getTodayAttendance(manager.getId());
            return !todayAttendance.isEmpty();
        } catch (Exception e) {
            logger.severe("Error checking today's attendance: " + e.getMessage());
            return false;
        }
    }

    private boolean hasCompletedAttendanceToday() {
        try {
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

        boolean success = dataStore.updateAttendanceClockOut(manager.getId(), timeStr);
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

        List<Employee> allEmployees = dataStore.getAllEmployees();
        List<Report> pendingReports = dataStore.getPendingReports();
        List<LeaveRequest> pendingLeaves = dataStore.getPendingLeaveRequests();

        // Refresh manager object to get updated leave balance if any changes occurred
        Employee refreshedManager = dataStore.authenticateUser(manager.getId(), manager.getPassword()); // Re-fetch manager details
        int managerLeaveDays = (refreshedManager != null) ? refreshedManager.getSisaCuti() : manager.getSisaCuti();

        VBox totalEmployeesCard = createStatsCard("Total Employees", String.valueOf(allEmployees.size()), "👥", "#3498db");
        VBox pendingReportsCard = createStatsCard("Pending Reports", String.valueOf(pendingReports.size()), "📄", "#e74c3c");
        VBox pendingLeavesCard = createStatsCard("Pending Leaves", String.valueOf(pendingLeaves.size()), "🏖️", "#f39c12");
        VBox myLeaveCard = createStatsCard("My Leave Days", String.valueOf(managerLeaveDays), "🌴", "#9b59b6");

        statsContainer.getChildren().addAll(totalEmployeesCard, pendingReportsCard, pendingLeavesCard, myLeaveCard);
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
                "📄 " + dataStore.getPendingReports().size() + " reports pending review",
                "🏖️ " + dataStore.getPendingLeaveRequests().size() + " leave requests pending approval",
                "👥 Managing " + dataStore.getAllEmployees().size() + " employees"
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

        List<Attendance> myAttendance = dataStore.getAttendanceByEmployee(manager.getId());
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

        List<Meeting> myMeetings = dataStore.getMeetingsByEmployee(manager.getId());
        table.setItems(FXCollections.observableArrayList(myMeetings));
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

        List<LeaveRequest> myLeaveRequests = dataStore.getLeaveRequestsByEmployee(manager.getId());
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

                    boolean success = dataStore.saveLeaveRequest(manager.getId(), leaveTypeCombo.getValue(),
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

    // Manager-specific features
    private void showKPIManagementContent() {
        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("KPI Management");
        title.getStyleClass().add("content-title");

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("custom-tab-pane");

        Tab setKpiTab = new Tab("Set KPI", createKPISetForm());

        // Initialize kpiHistoryTable once when this content is shown
        if (kpiHistoryTable == null) {
            kpiHistoryTable = createKPIHistoryTable(); // This now initializes the member
        }
        Tab kpiHistoryTab = new Tab("KPI History", kpiHistoryTable); // Use the member variable

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
        monthCombo.setValue("January");

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
                    refreshKPIHistoryTable(); // Call to refresh the table
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to set KPI.");
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
        table.getStyleClass().add("data-table"); // Apply CSS class

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

        // Populate the table immediately
        refreshKPIHistoryTable(table);

        return table;
    }

    private void refreshKPIHistoryTable() {
        if (kpiHistoryTable != null) {
            List<KPI> allKPI = dataStore.getAllKPI();
            kpiHistoryTable.setItems(FXCollections.observableArrayList(allKPI));
        }
    }

    // Overloaded method to populate a given table instance (used during initial creation)
    private void refreshKPIHistoryTable(TableView<KPI> table) {
        List<KPI> allKPI = dataStore.getAllKPI();
        table.setItems(FXCollections.observableArrayList(allKPI));
    }


    private void showReportReviewsContent() {
        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Report Reviews");
        title.getStyleClass().add("content-title");

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("custom-tab-pane");

        Tab pendingTab = new Tab("Pending Reports", createPendingReportsTable());
        Tab historyTab = new Tab("Report History", createReportHistoryTable());
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

        List<Report> pendingReports = dataStore.getPendingReports();
        table.setItems(FXCollections.observableArrayList(pendingReports));

        return table;
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

        List<Report> allReports = dataStore.getAllReports();
        table.setItems(FXCollections.observableArrayList(allReports));

        return table;
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
                boolean success = dataStore.updateReportStatus(report.getId(), status, notesArea.getText(), manager.getId());

                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Report " + status + " successfully!");
                    showReportReviewsContent();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to update report status.");
                }
            }
        });
    }

    private void showEvaluationHistoryContent() {
        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Evaluation History");
        title.getStyleClass().add("content-title");

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("custom-tab-pane");

        Tab regularEvalTab = new Tab("Regular Evaluations", createRegularEvaluationHistoryTable());
        Tab monthlyEvalTab = new Tab("Monthly Evaluations", createMonthlyEvaluationHistoryTable());
        regularEvalTab.setClosable(false);
        monthlyEvalTab.setClosable(false);

        tabPane.getTabs().addAll(regularEvalTab, monthlyEvalTab);

        content.getChildren().addAll(title, tabPane);
        contentArea.getChildren().add(content);
    }

    private TableView<EmployeeEvaluation> createRegularEvaluationHistoryTable() {
        TableView<EmployeeEvaluation> table = new TableView<>();
        table.setPrefHeight(400);
        table.getStyleClass().add("data-table");

        TableColumn<EmployeeEvaluation, String> employeeCol = new TableColumn<>("Employee");
        employeeCol.setCellValueFactory(new PropertyValueFactory<>("employeeId"));

        TableColumn<EmployeeEvaluation, String> supervisorCol = new TableColumn<>("Supervisor");
        supervisorCol.setCellValueFactory(new PropertyValueFactory<>("supervisorId"));

        TableColumn<EmployeeEvaluation, String> punctualityCol = new TableColumn<>("Punctuality");
        punctualityCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getPunctualityScore()) + "%"));

        TableColumn<EmployeeEvaluation, String> attendanceCol = new TableColumn<>("Attendance");
        attendanceCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getAttendanceScore()) + "%"));

        TableColumn<EmployeeEvaluation, String> overallCol = new TableColumn<>("Overall");
        overallCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getOverallRating()) + "%"));

        TableColumn<EmployeeEvaluation, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getEvaluationDate())));

        table.getColumns().addAll(employeeCol, supervisorCol, punctualityCol, attendanceCol, overallCol, dateCol);

        List<EmployeeEvaluation> allEvaluations = dataStore.getAllEvaluations();
        table.setItems(FXCollections.observableArrayList(allEvaluations));

        return table;
    }

    private TableView<MySQLDataStore.MonthlyEvaluation> createMonthlyEvaluationHistoryTable() {
        TableView<MySQLDataStore.MonthlyEvaluation> table = new TableView<>();
        table.setPrefHeight(400);
        table.getStyleClass().add("data-table");

        TableColumn<MySQLDataStore.MonthlyEvaluation, String> employeeCol = new TableColumn<>("Employee");
        employeeCol.setCellValueFactory(new PropertyValueFactory<>("employeeId"));

        TableColumn<MySQLDataStore.MonthlyEvaluation, String> supervisorCol = new TableColumn<>("Supervisor");
        supervisorCol.setCellValueFactory(new PropertyValueFactory<>("supervisorId"));

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

        table.getColumns().addAll(employeeCol, supervisorCol, monthCol, yearCol, overallCol, dateCol);

        List<MySQLDataStore.MonthlyEvaluation> monthlyEvaluations = dataStore.getAllMonthlyEvaluations();
        table.setItems(FXCollections.observableArrayList(monthlyEvaluations));

        return table;
    }

    private void showLeaveApprovalsContent() {
        contentArea.getChildren().clear();

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Leave Request Approvals");
        title.getStyleClass().add("content-title");

        TableView<LeaveRequest> leaveTable = createLeaveApprovalsTable();

        content.getChildren().addAll(title, leaveTable);
        contentArea.getChildren().add(content);
    }

    private TableView<LeaveRequest> createLeaveApprovalsTable() {
        TableView<LeaveRequest> table = new TableView<>();
        table.setPrefHeight(500);
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

        TableColumn<LeaveRequest, String> reasonCol = new TableColumn<>("Reason");
        reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));

        TableColumn<LeaveRequest, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<LeaveRequest, Void>() {
            private final HBox actionBox = new HBox(5);
            private final Button approveBtn = new Button("✓");
            private final Button rejectBtn = new Button("✗");

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

                actionBox.getChildren().addAll(approveBtn, rejectBtn);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    LeaveRequest request = getTableView().getItems().get(getIndex());
                    setGraphic(request.getStatus().equals("pending") ? actionBox : null);
                }
            }
        });

        table.getColumns().addAll(employeeCol, typeCol, startDateCol, endDateCol, daysCol, statusCol, reasonCol, actionCol);

        List<LeaveRequest> leaveRequests = dataStore.getLeaveRequestsForApproval(manager.getId());
        table.setItems(FXCollections.observableArrayList(leaveRequests));

        return table;
    }

    private void showLeaveApprovalDialog(LeaveRequest request, boolean isApproval) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isApproval ? "Approve Leave Request" : "Reject Leave Request");
        dialog.setHeaderText("Leave request from " + request.getEmployeeId());

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label infoLabel = new Label(String.format(
                "Employee: %s\nType: %s\nDates: %s to %s\nDays: %d\nReason: %s",
                request.getEmployeeId(),
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
                    success = dataStore.approveLeaveRequest(request.getId(), manager.getId(), notesArea.getText());
                } else {
                    success = dataStore.rejectLeaveRequest(request.getId(), manager.getId(), notesArea.getText());
                }

                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Success",
                            "Leave request " + (isApproval ? "approved" : "rejected") + " successfully!");
                    showLeaveApprovalsContent();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to process leave request.");
                }
            }
        });
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
        Tab allSalariesTab = new Tab("All Employee Salaries", createAllSalariesTable());
        mySalaryTab.setClosable(false);
        allSalariesTab.setClosable(false);

        tabPane.getTabs().addAll(mySalaryTab, allSalariesTab);

        content.getChildren().addAll(title, tabPane);
        contentArea.getChildren().add(content);
    }

    private VBox createMySalaryView() {
        VBox salaryView = new VBox(20);
        salaryView.setPadding(new Insets(20));
        salaryView.getStyleClass().add("my-salary-view");

        // Current salary breakdown
        VBox salaryBreakdown = createSalaryBreakdown();

        // Salary history table
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

        // Fetch the latest manager data to ensure accurate salary calculation
        Employee latestManagerData = dataStore.authenticateUser(manager.getId(), manager.getPassword());
        if (latestManagerData != null) {
            manager = latestManagerData; // Update manager object with latest data
        } else {
            logger.warning("Could not fetch latest manager data for salary breakdown.");
        }

        double baseSalary = manager.getGajiPokok();
        double kpiBonus = 0;
        double supervisorBonus = 0;
        double penalty = 0;

        // Calculate KPI bonus for display
        if (manager.getKpiScore() >= 90) {
            kpiBonus = baseSalary * 0.20;
        } else if (manager.getKpiScore() >= 80) {
            kpiBonus = baseSalary * 0.15;
        } else if (manager.getKpiScore() >= 70) {
            kpiBonus = baseSalary * 0.10;
        } else if (manager.getKpiScore() >= 60) {
            kpiBonus = baseSalary * 0.05;
        }

        // Calculate supervisor bonus for display
        if (manager.getSupervisorRating() >= 90) {
            supervisorBonus = baseSalary * 0.15;
        } else if (manager.getSupervisorRating() >= 80) {
            supervisorBonus = baseSalary * 0.10;
        } else if (manager.getSupervisorRating() >= 70) {
            supervisorBonus = baseSalary * 0.05;
        }

        // Calculate penalty for display
        if (manager.getKpiScore() < 60 || manager.getSupervisorRating() < 60) {
            penalty = baseSalary * 0.10; // 10% penalty
        }

        // The actual total salary should be calculated by the Employee model's method
        double totalSalary = manager.calculateGajiBulanan();

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
        totalLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        totalLabel.setTextFill(Color.web("#27ae60"));
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

        List<SalaryHistory> mySalaryHistory = dataStore.getSalaryHistoryByEmployee(manager.getId());
        table.setItems(FXCollections.observableArrayList(mySalaryHistory));

        return table;
    }

    private TableView<SalaryHistory> createAllSalariesTable() {
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

        TableColumn<SalaryHistory, String> kpiBonusCol = new TableColumn<>("KPI Bonus");
        kpiBonusCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty("Rp " + String.format("%,.0f", cellData.getValue().getKpiBonus())));

        TableColumn<SalaryHistory, String> totalCol = new TableColumn<>("Total Salary");
        totalCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty("Rp " + String.format("%,.0f", cellData.getValue().getTotalSalary())));

        table.getColumns().addAll(employeeCol, monthCol, yearCol, baseCol, kpiBonusCol, totalCol);

        List<SalaryHistory> allSalaryHistory = dataStore.getAllSalaryHistory();
        table.setItems(FXCollections.observableArrayList(allSalaryHistory));

        return table;
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
        Tab reportHistoryTab = new Tab("Reports", createReportHistoryTable());
        Tab kpiHistoryTab = new Tab("KPI History", kpiHistoryTable); // Use the member table here too
        Tab evaluationHistoryTab = new Tab("Evaluations", createAllEvaluationsTable());
        leaveHistoryTab.setClosable(false);
        reportHistoryTab.setClosable(false);
        kpiHistoryTab.setClosable(false);
        evaluationHistoryTab.setClosable(false);


        tabPane.getTabs().addAll(leaveHistoryTab, reportHistoryTab, kpiHistoryTab, evaluationHistoryTab);

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

        TableColumn<LeaveRequest, String> requestDateCol = new TableColumn<>("Request Date");
        requestDateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getRequestDate())));

        table.getColumns().addAll(employeeCol, typeCol, startDateCol, endDateCol, daysCol, statusCol, requestDateCol);

        List<LeaveRequest> allLeaveRequests = dataStore.getAllLeaveRequests();
        table.setItems(FXCollections.observableArrayList(allLeaveRequests));

        return table;
    }

    private TableView<EmployeeEvaluation> createAllEvaluationsTable() {
        TableView<EmployeeEvaluation> table = new TableView<>();
        table.setPrefHeight(400);
        table.getStyleClass().add("data-table");

        TableColumn<EmployeeEvaluation, String> employeeCol = new TableColumn<>("Employee");
        employeeCol.setCellValueFactory(new PropertyValueFactory<>("employeeId"));

        TableColumn<EmployeeEvaluation, String> supervisorCol = new TableColumn<>("Supervisor");
        supervisorCol.setCellValueFactory(new PropertyValueFactory<>("supervisorId"));

        TableColumn<EmployeeEvaluation, String> overallCol = new TableColumn<>("Overall Rating");
        overallCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getOverallRating()) + "%"));

        TableColumn<EmployeeEvaluation, String> dateCol = new TableColumn<>("Evaluation Date");
        dateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getEvaluationDate())));

        TableColumn<EmployeeEvaluation, String> commentsCol = new TableColumn<>("Comments");
        commentsCol.setCellValueFactory(new PropertyValueFactory<>("comments"));

        table.getColumns().addAll(employeeCol, supervisorCol, overallCol, dateCol, commentsCol);

        List<EmployeeEvaluation> allEvaluations = dataStore.getAllEvaluations();
        table.setItems(FXCollections.observableArrayList(allEvaluations));

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