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
                    createNavButton("🏖️ My Leave Requests", this::showMyLeaveRequests),
                    createNavButton("📈 KPI Management", this::showKPIManagementContent),
                    createNavButton("📄 Report Reviews", this::showReportReviewsContent),
                    createNavButton("⭐ Evaluation History", this::showEvaluationHistoryContent),
                    createNavButton("🏖️ Leave Approvals", this::showLeaveApprovalsContent),
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
        if (manager == null || dataStore == null || contentArea == null) {
            logger.warning("Cannot show dashboard content - missing required objects");
            return;
        }

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("Manager Dashboard");
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

            // Get pending leave requests for manager approval (supervisors + employees)
            List<LeaveRequest> pendingLeaves = dataStore.getLeaveRequestsForApproval(manager.getId());

            // Refresh manager object to get updated leave balance
            Employee refreshedManager = dataStore.authenticateUser(manager.getId(), manager.getPassword());
            int managerLeaveDays = (refreshedManager != null) ? refreshedManager.getSisaCuti() : manager.getSisaCuti();

            VBox totalEmployeesCard = createStatsCard("Total Employees", String.valueOf(allEmployees.size()), "👥", "#3498db");
            VBox pendingReportsCard = createStatsCard("Pending Reports", String.valueOf(pendingReports.size()), "📄", "#e74c3c");
            VBox pendingLeavesCard = createStatsCard("Leave Approvals", String.valueOf(pendingLeaves.size()), "🏖️", "#f39c12");
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
        card.getStyleClass().add("stats-card");
        card.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setPrefWidth(180);
        card.setPrefHeight(120);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 24px;");
        iconLabel.setTextFill(Color.WHITE);

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        valueLabel.setTextFill(Color.WHITE);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12px;");
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setWrapText(true);
        titleLabel.setAlignment(Pos.CENTER);

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

    // PERBAIKAN: Menggunakan setScrollableContent()
    private void showMyAttendance() {
        if (manager == null || dataStore == null || contentArea == null) {
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

    // PERBAIKAN: Menggunakan setScrollableContent()
    private void showMyMeetings() {
        if (manager == null || dataStore == null || contentArea == null) {
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

    // PERBAIKAN: Menggunakan setScrollableContent()
    private void showMyLeaveRequests() {
        if (manager == null || dataStore == null || contentArea == null) {
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

                    Date startUtilDate = java.util.Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    Date endUtilDate = java.util.Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

                    try {
                        boolean success = dataStore.saveLeaveRequest(manager.getId(), leaveTypeCombo.getValue(),
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

    // PERBAIKAN: Menggunakan setScrollableContent() untuk semua metode lainnya
    private void showKPIManagementContent() {
        if (manager == null || dataStore == null || contentArea == null) {
            return;
        }

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
        setScrollableContent(content); // PERBAIKAN: Menggunakan ScrollPane
    }

    private void showReportReviewsContent() {
        if (manager == null || dataStore == null || contentArea == null) {
            return;
        }

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
        setScrollableContent(content); // PERBAIKAN: Menggunakan ScrollPane
    }

    private void showEvaluationHistoryContent() {
        if (manager == null || dataStore == null || contentArea == null) {
            return;
        }

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
        setScrollableContent(content); // PERBAIKAN: Menggunakan ScrollPane
    }

    private void showLeaveApprovalsContent() {
        if (manager == null || dataStore == null || contentArea == null) {
            return;
        }

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("🏖️ Leave Request Approvals - Manager Level");
        title.getStyleClass().add("content-title");

        Label infoLabel = new Label("📋 As a Manager, you can approve leave requests from Supervisors and all Employees");
        infoLabel.getStyleClass().add("page-subtitle");

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("custom-tab-pane");

        // Pending Approvals Tab
        Tab pendingTab = new Tab("⏳ Pending Approvals", createManagerLeaveApprovalTable());

        // Approval History Tab
        Tab historyTab = new Tab("📋 Approval History", createManagerLeaveApprovalHistoryTable());

        pendingTab.setClosable(false);
        historyTab.setClosable(false);

        tabPane.getTabs().addAll(pendingTab, historyTab);

        content.getChildren().addAll(title, infoLabel, tabPane);
        setScrollableContent(content);
    }

    private void showSalaryManagementContent() {
        if (manager == null || dataStore == null || contentArea == null) {
            return;
        }

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
        setScrollableContent(content); // PERBAIKAN: Menggunakan ScrollPane
    }

    private void showAllHistoryContent() {
        if (manager == null || dataStore == null || contentArea == null) {
            return;
        }

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
        setScrollableContent(content); // PERBAIKAN: Menggunakan ScrollPane
    }

    // Implementasi method helper - Saya akan menambahkan beberapa method yang diperlukan
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

    // Method helper untuk refresh table dan create table
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

    // Methods untuk table-table lainnya - saya akan implementasikan yang esensial
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

    // Dummy implementations untuk table-table lainnya
    private TableView<Report> createReportHistoryTable() {
        TableView<Report> table = new TableView<>();
        table.setPrefHeight(400);
        table.getStyleClass().add("data-table");
        // Implementation serupa dengan createPendingReportsTable tapi tanpa action column
        return table;
    }

    private void refreshReportHistoryTable(TableView<Report> table) {
        // Implementation refresh untuk report history
    }

    private TableView<MySQLDataStore.MonthlyEvaluation> createMonthlyEvaluationsTable() {
        TableView<MySQLDataStore.MonthlyEvaluation> table = new TableView<>();
        table.setPrefHeight(400);
        table.getStyleClass().add("data-table");
        // Implementation untuk monthly evaluations table
        return table;
    }

    private TableView<EmployeeEvaluation> createRegularEvaluationsTable() {
        TableView<EmployeeEvaluation> table = new TableView<>();
        table.setPrefHeight(400);
        table.getStyleClass().add("data-table");
        // Implementation untuk regular evaluations table
        return table;
    }

    private TableView<LeaveRequest> createManagerLeaveApprovalTable() {
        TableView<LeaveRequest> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setPrefHeight(400);

        TableColumn<LeaveRequest, String> employeeCol = new TableColumn<>("👤 Employee");
        employeeCol.setCellValueFactory(cellData -> {
            Employee emp = dataStore.getEmployeeById(cellData.getValue().getEmployeeId());
            return new javafx.beans.property.SimpleStringProperty(emp != null ? emp.getNama() : "Unknown");
        });
        employeeCol.setPrefWidth(150);

        TableColumn<LeaveRequest, String> roleCol = new TableColumn<>("👔 Role");
        roleCol.setCellValueFactory(cellData -> {
            Employee emp = dataStore.getEmployeeById(cellData.getValue().getEmployeeId());
            String role = emp != null ? emp.getRole() : "Unknown";
            String icon = role.equals("supervisor") ? "👨‍💼" : "👤";
            return new javafx.beans.property.SimpleStringProperty(icon + " " + role.substring(0, 1).toUpperCase() + role.substring(1));
        });
        roleCol.setPrefWidth(120);

        TableColumn<LeaveRequest, String> divisionCol = new TableColumn<>("🏢 Division");
        divisionCol.setCellValueFactory(cellData -> {
            Employee emp = dataStore.getEmployeeById(cellData.getValue().getEmployeeId());
            return new javafx.beans.property.SimpleStringProperty(emp != null ? emp.getDivisi() : "Unknown");
        });
        divisionCol.setPrefWidth(100);

        TableColumn<LeaveRequest, String> typeCol = new TableColumn<>("📝 Leave Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("leaveType"));
        typeCol.setPrefWidth(120);

        TableColumn<LeaveRequest, String> startDateCol = new TableColumn<>("📅 Start Date");
        startDateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getStartDate())));
        startDateCol.setPrefWidth(100);

        TableColumn<LeaveRequest, String> endDateCol = new TableColumn<>("📅 End Date");
        endDateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getEndDate())));
        endDateCol.setPrefWidth(100);

        TableColumn<LeaveRequest, Integer> daysCol = new TableColumn<>("📊 Days");
        daysCol.setCellValueFactory(new PropertyValueFactory<>("totalDays"));
        daysCol.setPrefWidth(70);

        TableColumn<LeaveRequest, String> reasonCol = new TableColumn<>("📋 Reason");
        reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));
        reasonCol.setPrefWidth(200);

        TableColumn<LeaveRequest, String> requestDateCol = new TableColumn<>("📅 Requested");
        requestDateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getRequestDate())));
        requestDateCol.setPrefWidth(100);

        TableColumn<LeaveRequest, Void> actionCol = new TableColumn<>("⚡ Actions");
        actionCol.setCellFactory(col -> new TableCell<LeaveRequest, Void>() {
            private final HBox actionBox = new HBox(5);
            private final Button approveBtn = new Button("✅ Approve");
            private final Button rejectBtn = new Button("❌ Reject");
            private final Button viewBtn = new Button("👁️ View");

            {
                approveBtn.getStyleClass().add("action-button-small-green");
                rejectBtn.getStyleClass().add("action-button-small-red");
                viewBtn.getStyleClass().add("review-button");

                actionBox.getChildren().addAll(viewBtn, approveBtn, rejectBtn);
                actionBox.setAlignment(Pos.CENTER);

                viewBtn.setOnAction(e -> {
                    LeaveRequest request = getTableView().getItems().get(getIndex());
                    showManagerLeaveRequestDetailsDialog(request);
                });

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
        actionCol.setPrefWidth(180);

        table.getColumns().addAll(employeeCol, roleCol, divisionCol, typeCol, startDateCol, endDateCol,
                daysCol, reasonCol, requestDateCol, actionCol);

        refreshManagerLeaveApprovalsTable(table);

        return table;
    }

    private void refreshManagerLeaveApprovalsTable(TableView<LeaveRequest> table) {
        if (dataStore != null && manager != null) {
            try {
                System.out.println("=== MANAGER LEAVE APPROVALS DEBUG ===");
                System.out.println("Manager: " + manager.getNama() + " (ID: " + manager.getId() + ")");
                System.out.println("Role: " + manager.getRole());

                List<LeaveRequest> pendingRequests = dataStore.getLeaveRequestsForApproval(manager.getId());
                System.out.println("Found " + pendingRequests.size() + " pending requests for manager approval");

                for (LeaveRequest req : pendingRequests) {
                    Employee emp = dataStore.getEmployeeById(req.getEmployeeId());
                    System.out.println("- Request from: " + (emp != null ? emp.getNama() + " (" + emp.getRole() + ", " + emp.getDivisi() + ")" : req.getEmployeeId()));
                }

                table.setItems(FXCollections.observableArrayList(pendingRequests));
            } catch (Exception e) {
                logger.severe("Error loading leave requests for manager approval: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void showManagerLeaveRequestDetailsDialog(LeaveRequest request) {
        Employee requestingEmployee = dataStore.getEmployeeById(request.getEmployeeId());
        String employeeName = requestingEmployee != null ? requestingEmployee.getNama() : "Unknown";

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("👁️ Leave Request Details");
        dialog.setHeaderText("Leave request details for " + employeeName);

        VBox content = new VBox(15);
        content.setPadding(new Insets(25));

        // Employee Information Section
        Label empInfoTitle = new Label("👤 Employee Information");
        empInfoTitle.getStyleClass().add("section-title");

        GridPane empInfoGrid = new GridPane();
        empInfoGrid.setHgap(15);
        empInfoGrid.setVgap(10);
        empInfoGrid.setPadding(new Insets(10));

        empInfoGrid.add(new Label("Name:"), 0, 0);
        empInfoGrid.add(new Label(employeeName), 1, 0);

        if (requestingEmployee != null) {
            empInfoGrid.add(new Label("Role:"), 0, 1);
            empInfoGrid.add(new Label(requestingEmployee.getRole().substring(0, 1).toUpperCase() + requestingEmployee.getRole().substring(1)), 1, 1);

            empInfoGrid.add(new Label("Division:"), 0, 2);
            empInfoGrid.add(new Label(requestingEmployee.getDivisi()), 1, 2);

            empInfoGrid.add(new Label("Position:"), 0, 3);
            empInfoGrid.add(new Label(requestingEmployee.getJabatan()), 1, 3);

            empInfoGrid.add(new Label("Remaining Leave Days:"), 0, 4);
            empInfoGrid.add(new Label(String.valueOf(requestingEmployee.getSisaCuti())), 1, 4);
        }

        // Leave Request Details Section
        Label reqDetailsTitle = new Label("📋 Leave Request Details");
        reqDetailsTitle.getStyleClass().add("section-title");

        GridPane reqDetailsGrid = new GridPane();
        reqDetailsGrid.setHgap(15);
        reqDetailsGrid.setVgap(10);
        reqDetailsGrid.setPadding(new Insets(10));

        reqDetailsGrid.add(new Label("Leave Type:"), 0, 0);
        reqDetailsGrid.add(new Label(request.getLeaveType()), 1, 0);

        reqDetailsGrid.add(new Label("Start Date:"), 0, 1);
        reqDetailsGrid.add(new Label(sdf.format(request.getStartDate())), 1, 1);

        reqDetailsGrid.add(new Label("End Date:"), 0, 2);
        reqDetailsGrid.add(new Label(sdf.format(request.getEndDate())), 1, 2);

        reqDetailsGrid.add(new Label("Total Days:"), 0, 3);
        reqDetailsGrid.add(new Label(String.valueOf(request.getTotalDays())), 1, 3);

        reqDetailsGrid.add(new Label("Request Date:"), 0, 4);
        reqDetailsGrid.add(new Label(sdf.format(request.getRequestDate())), 1, 4);

        reqDetailsGrid.add(new Label("Reason:"), 0, 5);
        Label reasonLabel = new Label(request.getReason());
        reasonLabel.setWrapText(true);
        reasonLabel.setMaxWidth(300);
        reqDetailsGrid.add(reasonLabel, 1, 5);

        content.getChildren().addAll(
                empInfoTitle, empInfoGrid,
                new Separator(),
                reqDetailsTitle, reqDetailsGrid
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }

    // NEW METHOD: Show Manager Leave Approval Dialog
    private void showManagerLeaveApprovalDialog(LeaveRequest request, boolean isApproval) {
        Employee requestingEmployee = dataStore.getEmployeeById(request.getEmployeeId());
        String employeeName = requestingEmployee != null ? requestingEmployee.getNama() : "Unknown";
        String employeeRole = requestingEmployee != null ? requestingEmployee.getRole() : "Unknown";

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isApproval ? "✅ Approve Leave Request" : "❌ Reject Leave Request");
        dialog.setHeaderText((isApproval ? "Approve" : "Reject") + " leave request from " + employeeName + " (" + employeeRole + ")");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Quick Summary
        GridPane summaryGrid = new GridPane();
        summaryGrid.setHgap(15);
        summaryGrid.setVgap(10);
        summaryGrid.getStyleClass().add("form-container");
        summaryGrid.setPadding(new Insets(15));

        summaryGrid.add(new Label("👤 Employee:"), 0, 0);
        summaryGrid.add(new Label(employeeName), 1, 0);

        summaryGrid.add(new Label("👔 Role:"), 0, 1);
        summaryGrid.add(new Label(employeeRole.substring(0, 1).toUpperCase() + employeeRole.substring(1)), 1, 1);

        summaryGrid.add(new Label("🏢 Division:"), 0, 2);
        summaryGrid.add(new Label(requestingEmployee != null ? requestingEmployee.getDivisi() : "Unknown"), 1, 2);

        summaryGrid.add(new Label("📝 Leave Type:"), 0, 3);
        summaryGrid.add(new Label(request.getLeaveType()), 1, 3);

        summaryGrid.add(new Label("📅 Period:"), 0, 4);
        summaryGrid.add(new Label(sdf.format(request.getStartDate()) + " to " + sdf.format(request.getEndDate())), 1, 4);

        summaryGrid.add(new Label("📊 Total Days:"), 0, 5);
        summaryGrid.add(new Label(String.valueOf(request.getTotalDays())), 1, 5);

        summaryGrid.add(new Label("📋 Reason:"), 0, 6);
        Label reasonLabel = new Label(request.getReason());
        reasonLabel.setWrapText(true);
        reasonLabel.setMaxWidth(300);
        summaryGrid.add(reasonLabel, 1, 6);

        // Manager Notes Section
        Label notesLabel = new Label("💬 Manager " + (isApproval ? "Approval" : "Rejection") + " Notes:");
        notesLabel.getStyleClass().add("form-label");

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Enter your decision notes here (required)...");
        notesArea.setPrefRowCount(4);
        notesArea.setMaxWidth(400);

        // Impact Analysis (for Manager decision making)
        Label impactTitle = new Label("📊 Impact Analysis");
        impactTitle.getStyleClass().add("section-title");

        VBox impactBox = new VBox(8);
        impactBox.getStyleClass().add("form-container");
        impactBox.setPadding(new Insets(15));

        if (requestingEmployee != null) {
            impactBox.getChildren().addAll(
                    new Label("• Current leave balance: " + requestingEmployee.getSisaCuti() + " days"),
                    new Label("• After approval: " + (requestingEmployee.getSisaCuti() - request.getTotalDays()) + " days remaining"),
                    new Label("• Employee performance: " + df.format(requestingEmployee.getSupervisorRating()) + "% supervisor rating"),
                    new Label("• Role impact: " + (employeeRole.equals("supervisor") ? "High - Team leadership role" : "Medium - Individual contributor"))
            );
        }

        content.getChildren().addAll(
                new Label("📋 Request Summary:"),
                summaryGrid,
                new Separator(),
                impactTitle,
                impactBox,
                new Separator(),
                notesLabel,
                notesArea
        );

        dialog.getDialogPane().setContent(content);

        ButtonType confirmButton = new ButtonType(isApproval ? "✅ Approve Request" : "❌ Reject Request", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(confirmButton, cancelButton);

        dialog.showAndWait().ifPresent(result -> {
            if (result == confirmButton) {
                if (notesArea.getText().trim().isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Missing Notes",
                            "Please provide notes for your decision. This is required for audit purposes.");
                    return;
                }

                try {
                    boolean success;
                    if (isApproval) {
                        success = dataStore.approveLeaveRequest(request.getId(), manager.getId(), notesArea.getText());
                    } else {
                        success = dataStore.rejectLeaveRequest(request.getId(), manager.getId(), notesArea.getText());
                    }

                    if (success) {
                        String action = isApproval ? "approved" : "rejected";
                        showAlert(Alert.AlertType.INFORMATION, "Success",
                                "Leave request " + action + " successfully!\n\n" +
                                        "Employee: " + employeeName + "\n" +
                                        "Period: " + sdf.format(request.getStartDate()) + " to " + sdf.format(request.getEndDate()));

                        // Refresh tables and dashboard
                        refreshManagerLeaveApprovalsTables();
                        showDashboardContent(); // Refresh stats
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to process leave request.");
                    }
                } catch (Exception e) {
                    logger.severe("Error processing leave request: " + e.getMessage());
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to process leave request: " + e.getMessage());
                }
            }
        });
    }

    // NEW METHOD: Create Manager Leave Approval History Table
    private TableView<LeaveRequest> createManagerLeaveApprovalHistoryTable() {
        TableView<LeaveRequest> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setPrefHeight(400);

        TableColumn<LeaveRequest, String> employeeCol = new TableColumn<>("👤 Employee");
        employeeCol.setCellValueFactory(cellData -> {
            Employee emp = dataStore.getEmployeeById(cellData.getValue().getEmployeeId());
            return new javafx.beans.property.SimpleStringProperty(emp != null ? emp.getNama() : "Unknown");
        });
        employeeCol.setPrefWidth(120);

        TableColumn<LeaveRequest, String> roleCol = new TableColumn<>("👔 Role");
        roleCol.setCellValueFactory(cellData -> {
            Employee emp = dataStore.getEmployeeById(cellData.getValue().getEmployeeId());
            String role = emp != null ? emp.getRole() : "Unknown";
            return new javafx.beans.property.SimpleStringProperty(role.substring(0, 1).toUpperCase() + role.substring(1));
        });
        roleCol.setPrefWidth(100);

        TableColumn<LeaveRequest, String> typeCol = new TableColumn<>("📝 Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("leaveType"));
        typeCol.setPrefWidth(100);

        TableColumn<LeaveRequest, String> startDateCol = new TableColumn<>("📅 Start");
        startDateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getStartDate())));
        startDateCol.setPrefWidth(90);

        TableColumn<LeaveRequest, Integer> daysCol = new TableColumn<>("📊 Days");
        daysCol.setCellValueFactory(new PropertyValueFactory<>("totalDays"));
        daysCol.setPrefWidth(60);

        TableColumn<LeaveRequest, String> statusCol = new TableColumn<>("✅ Status");
        statusCol.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getStatus();
            String icon = status.equals("approved") ? "✅" : "❌";
            return new javafx.beans.property.SimpleStringProperty(icon + " " + status.substring(0, 1).toUpperCase() + status.substring(1));
        });
        statusCol.setPrefWidth(100);

        TableColumn<LeaveRequest, String> approvalDateCol = new TableColumn<>("📅 Decision Date");
        approvalDateCol.setCellValueFactory(cellData -> {
            Date approvalDate = cellData.getValue().getApprovalDate();
            return new javafx.beans.property.SimpleStringProperty(
                    approvalDate != null ? sdf.format(approvalDate) : "N/A");
        });
        approvalDateCol.setPrefWidth(100);

        TableColumn<LeaveRequest, String> notesCol = new TableColumn<>("📋 Manager Notes");
        notesCol.setCellValueFactory(new PropertyValueFactory<>("approverNotes"));
        notesCol.setPrefWidth(200);

        TableColumn<LeaveRequest, Void> actionCol = new TableColumn<>("⚡ Action");
        actionCol.setCellFactory(col -> new TableCell<LeaveRequest, Void>() {
            private final Button viewBtn = new Button("👁️ View");

            {
                viewBtn.getStyleClass().add("review-button");
                viewBtn.setOnAction(e -> {
                    LeaveRequest request = getTableView().getItems().get(getIndex());
                    showManagerLeaveRequestDetailsDialog(request);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewBtn);
            }
        });
        actionCol.setPrefWidth(80);

        table.getColumns().addAll(employeeCol, roleCol, typeCol, startDateCol, daysCol,
                statusCol, approvalDateCol, notesCol, actionCol);

        refreshManagerLeaveApprovalHistoryTable(table);

        return table;
    }

    // NEW METHOD: Refresh Manager Leave Approval History Table
    private void refreshManagerLeaveApprovalHistoryTable(TableView<LeaveRequest> table) {
        if (dataStore != null && manager != null) {
            try {
                List<LeaveRequest> allRequests = dataStore.getAllLeaveRequests();

                // Filter requests that were approved/rejected by this manager
                List<LeaveRequest> managerApprovals = allRequests.stream()
                        .filter(req -> manager.getId().equals(req.getApproverId()))
                        .filter(req -> "approved".equals(req.getStatus()) || "rejected".equals(req.getStatus()))
                        .sorted((r1, r2) -> {
                            // Sort by approval date, newest first
                            if (r1.getApprovalDate() == null && r2.getApprovalDate() == null) return 0;
                            if (r1.getApprovalDate() == null) return 1;
                            if (r2.getApprovalDate() == null) return -1;
                            return r2.getApprovalDate().compareTo(r1.getApprovalDate());
                        })
                        .collect(Collectors.toList());

                table.setItems(FXCollections.observableArrayList(managerApprovals));
            } catch (Exception e) {
                logger.severe("Error loading manager approval history: " + e.getMessage());
            }
        }
    }

    // NEW METHOD: Refresh all Manager Leave Approval Tables
    private void refreshManagerLeaveApprovalsTables() {
        // This method would refresh any existing leave approval tables
        // You would call this after approving/rejecting requests
        Platform.runLater(() -> {
            showLeaveApprovalsContent(); // This will recreate the tabs with fresh data
        });
    }

    private TableView<Employee> createSalaryOverviewTable() {
        TableView<Employee> table = new TableView<>();
        table.setPrefHeight(400);
        table.getStyleClass().add("data-table");
        // Implementation untuk salary overview table
        return table;
    }

    private TableView<SalaryHistory> createAllSalaryHistoryTable() {
        TableView<SalaryHistory> table = new TableView<>();
        table.setPrefHeight(400);
        table.getStyleClass().add("data-table");
        // Implementation untuk all salary history table
        return table;
    }

    private TableView<LeaveRequest> createAllLeaveRequestsTable() {
        TableView<LeaveRequest> table = new TableView<>();
        table.setPrefHeight(350);
        table.getStyleClass().add("data-table");
        // Implementation untuk all leave requests table
        return table;
    }

    private TableView<Meeting> createAllMeetingsTable() {
        TableView<Meeting> table = new TableView<>();
        table.setPrefHeight(350);
        table.getStyleClass().add("data-table");
        // Implementation untuk all meetings table
        return table;
    }

    private TableView<Attendance> createAllAttendanceTable() {
        TableView<Attendance> table = new TableView<>();
        table.setPrefHeight(350);
        table.getStyleClass().add("data-table");
        // Implementation untuk all attendance table
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
        if (manager == null || dataStore == null) {
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
        nameField.setText(manager.getNama());

        PasswordField passwordField = new PasswordField();
        passwordField.setText(manager.getPassword());

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                manager.setNama(nameField.getText());
                manager.setPassword(passwordField.getText());
                try {
                    dataStore.updateEmployee(manager);
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Profile updated successfully!");
                    userWelcomeLabel.setText("Welcome, " + manager.getNama() + " (Manager)");
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to update profile.");
                }
            }
        });
    }
}