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
import javafx.stage.Stage; // <-- IMPORT YANG DITAMBAHKAN

import java.time.ZoneId;
import java.util.*;

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
    private Label navUserGreeting;
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
        if (navUserGreeting != null) {
            navUserGreeting.setText("Hello, " + employee.getNama() + "!");
        }
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
        populateNavigationButtons();
    }

    private void initializeContent() {
        if (employee != null && dataStore != null) {
            if (userWelcomeLabel != null) {
                userWelcomeLabel.setText("Welcome, " + employee.getNama() + " (Employee)");
            }
            if (navUserGreeting != null) {
                navUserGreeting.setText("Hello, " + employee.getNama() + "!");
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
                    createNavButton("💰 My Salary", this::showMySalaryContent),
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

    private void setScrollableContent(Region contentNode) {
        contentArea.getChildren().clear();
        ScrollPane scrollPane = new ScrollPane(contentNode);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");
        contentArea.getChildren().add(scrollPane);
    }

    private void showDashboardContent() {
        if (employee == null || dataStore == null || contentArea == null) {
            logger.warning("Cannot show dashboard content - missing required objects");
            return;
        }

        VBox content = new VBox(25);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("🏠 Employee Dashboard");
        title.getStyleClass().add("content-title");

        HBox quickActions = createQuickActions();
        HBox statsCards = createStatsCards();
        VBox recentActivities = createRecentActivitiesSection();

        content.getChildren().addAll(title, quickActions, statsCards, recentActivities);
        setScrollableContent(content);
    }

    private HBox createQuickActions() {
        HBox actionsBox = new HBox(20);
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

        boolean success = dataStore.updateAttendanceClockOut(employee.getId(), timeStr);
        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Clock Out", "Successfully clocked out at " + timeStr);
            showDashboardContent();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to clock out.");
        }
    }

    private HBox createStatsCards() {
        HBox statsContainer = new HBox(25);
        statsContainer.setAlignment(Pos.CENTER);
        statsContainer.getStyleClass().add("stats-cards-container");

        if (dataStore == null || employee == null) {
            return statsContainer;
        }

        Employee refreshedEmployee = dataStore.authenticateUser(employee.getId(), employee.getPassword());
        if (refreshedEmployee != null) {
            employee = refreshedEmployee;
        } else {
            logger.warning("Could not refresh employee data for stats cards.");
        }

        VBox kpiScoreCard = createStatsCard("📈 KPI Score", String.format("%.1f%%", employee.getKpiScore()), "#4facfe");
        VBox leaveDaysCard = createStatsCard("🏖️ Leave Days", String.valueOf(employee.getSisaCuti()), "#56ab2f");
        VBox overallRatingCard = createStatsCard("⭐ Rating", String.format("%.1f%%", employee.getOverallRating()), "#f093fb");

        statsContainer.getChildren().addAll(kpiScoreCard, leaveDaysCard, overallRatingCard);
        return statsContainer;
    }

    private VBox createStatsCard(String title, String value, String color) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(25));
        card.setPrefSize(220, 140);
        card.getStyleClass().add("stats-card");
        card.setStyle(String.format("-fx-border-color: %s; -fx-border-width: 0 0 4 0;", color));

        String[] parts = title.split(" ", 2);
        String icon = parts[0];
        String text = parts.length > 1 ? parts[1] : "";

        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("stats-card-icon");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stats-card-value");
        valueLabel.setStyle(String.format("-fx-text-fill: %s;", color));

        Label titleLabel = new Label(text);
        titleLabel.getStyleClass().add("stats-card-title");

        card.getChildren().addAll(iconLabel, valueLabel, titleLabel);
        return card;
    }

    private VBox createRecentActivitiesSection() {
        VBox section = new VBox(18);
        section.setAlignment(Pos.CENTER);
        section.setPadding(new Insets(25));
        section.getStyleClass().add("recent-activities-section");

        Label sectionTitle = new Label("📋 Recent Activities");
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
        if (employee == null || dataStore == null) {
            return;
        }
        VBox content = new VBox(25);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("⏰ My Attendance");
        title.getStyleClass().add("content-title");

        TableView<Attendance> attendanceTable = createMyAttendanceTable();

        content.getChildren().addAll(title, attendanceTable);
        setScrollableContent(content);
    }

    private TableView<Attendance> createMyAttendanceTable() {
        TableView<Attendance> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); // PERUBAHAN DI SINI

        TableColumn<Attendance, String> dateCol = new TableColumn<>("📅 Date");
        dateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getTanggal())));
        dateCol.setPrefWidth(120);

        TableColumn<Attendance, String> clockInCol = new TableColumn<>("⏰ Clock In");
        clockInCol.setCellValueFactory(new PropertyValueFactory<>("jamMasuk"));
        clockInCol.setPrefWidth(100);

        TableColumn<Attendance, String> clockOutCol = new TableColumn<>("🏃 Clock Out");
        clockOutCol.setCellValueFactory(new PropertyValueFactory<>("jamKeluar"));
        clockOutCol.setPrefWidth(100);

        TableColumn<Attendance, String> statusCol = new TableColumn<>("📊 Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(80);

        table.getColumns().addAll(dateCol, clockInCol, clockOutCol, statusCol);

        if (dataStore != null && employee != null) {
            List<Attendance> myAttendance = dataStore.getAttendanceByEmployee(employee.getId());
            table.setItems(FXCollections.observableArrayList(myAttendance));
        }
        table.setPrefHeight(600);

        return table;
    }

    private void showMyMeetings() {
        if (employee == null || dataStore == null) {
            return;
        }
        VBox content = new VBox(25);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("📅 My Meetings");
        title.getStyleClass().add("content-title");

        TableView<Meeting> meetingsTable = createMyMeetingsTable();

        content.getChildren().addAll(title, meetingsTable);
        setScrollableContent(content);
    }

    private TableView<Meeting> createMyMeetingsTable() {
        TableView<Meeting> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); // PERUBAHAN DI SINI

        TableColumn<Meeting, String> titleCol = new TableColumn<>("📋 Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(200);

        TableColumn<Meeting, String> dateCol = new TableColumn<>("📅 Date");
        dateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getTanggal())));
        dateCol.setPrefWidth(100);

        TableColumn<Meeting, String> timeCol = new TableColumn<>("⏰ Time");
        timeCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getWaktuMulai() + " - " + cellData.getValue().getWaktuSelesai()));
        timeCol.setPrefWidth(120);

        TableColumn<Meeting, String> locationCol = new TableColumn<>("📍 Location");
        locationCol.setCellValueFactory(new PropertyValueFactory<>("lokasi"));
        locationCol.setPrefWidth(150);

        table.getColumns().addAll(titleCol, dateCol, timeCol, locationCol);

        if (dataStore != null && employee != null) {
            List<Meeting> myMeetings = dataStore.getMeetingsByEmployee(employee.getId());
            table.setItems(FXCollections.observableArrayList(myMeetings));
        }
        table.setPrefHeight(600);

        return table;
    }

    private void showMyLeaveRequests() {
        if (employee == null || dataStore == null) {
            return;
        }
        VBox content = new VBox(25);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("🏖️ My Leave Requests");
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
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); // PERUBAHAN DI SINI

        TableColumn<LeaveRequest, String> typeCol = new TableColumn<>("📝 Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("leaveType"));
        typeCol.setPrefWidth(120);

        TableColumn<LeaveRequest, String> startDateCol = new TableColumn<>("📅 Start Date");
        startDateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getStartDate())));
        startDateCol.setPrefWidth(100);

        TableColumn<LeaveRequest, Integer> daysCol = new TableColumn<>("📊 Days");
        daysCol.setCellValueFactory(new PropertyValueFactory<>("totalDays"));
        daysCol.setPrefWidth(80);

        TableColumn<LeaveRequest, String> statusCol = new TableColumn<>("✅ Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);

        TableColumn<LeaveRequest, String> notesCol = new TableColumn<>("📋 Notes");
        notesCol.setCellValueFactory(new PropertyValueFactory<>("approverNotes"));
        notesCol.setPrefWidth(200);

        table.getColumns().addAll(typeCol, startDateCol, daysCol, statusCol, notesCol);

        if (dataStore != null && employee != null) {
            List<LeaveRequest> myLeaveRequests = dataStore.getLeaveRequestsByEmployee(employee.getId());
            table.setItems(FXCollections.observableArrayList(myLeaveRequests));
        }
        table.setPrefHeight(600);

        return table;
    }

    private void showLeaveRequestDialog() {
        if (employee == null || dataStore == null) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("🏖️ Request Leave");
        dialog.setHeaderText("Submit a new leave request");

        VBox content = new VBox(18);
        content.setPadding(new Insets(25));
        content.getStyleClass().add("kpi-set-form");

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
                new Label("📝 Leave Type:"), leaveTypeCombo,
                new Label("📅 Start Date (No weekends):"), startDatePicker,
                new Label("📅 End Date (No weekends):"), endDatePicker,
                new Label("📋 Reason:"), reasonArea
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
                        boolean success = dataStore.saveLeaveRequest(employee.getId(), leaveTypeCombo.getValue(),
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

    private void showMySalaryContent() {
        if (employee == null || dataStore == null) {
            return;
        }
        VBox content = new VBox(25);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("💰 My Salary Information");
        title.getStyleClass().add("content-title");

        VBox currentSalaryBox = createCurrentSalaryBreakdown();
        TableView<SalaryHistory> salaryHistoryTable = createSalaryHistoryTable();

        content.getChildren().addAll(title, currentSalaryBox, salaryHistoryTable);
        setScrollableContent(content);
    }

    private VBox createCurrentSalaryBreakdown() {
        VBox salaryBox = new VBox(18);
        salaryBox.getStyleClass().add("salary-breakdown-box");
        salaryBox.setPadding(new Insets(25));

        Label breakdownTitle = new Label("💰 Current Month Salary Breakdown");
        breakdownTitle.getStyleClass().add("section-title");

        GridPane salaryGrid = new GridPane();
        salaryGrid.setHgap(25);
        salaryGrid.setVgap(12);

        double baseSalary = employee.getGajiPokok();
        double kpiBonus = calculateKPIBonus(employee.getKpiScore(), baseSalary);
        double supervisorBonus = calculateSupervisorBonus(employee.getSupervisorRating(), baseSalary);
        double penalty = calculatePenalty(employee.getKpiScore(), employee.getSupervisorRating(), baseSalary);
        double totalSalary = baseSalary + kpiBonus + supervisorBonus - penalty;

        int row = 0;
        addSalaryRow(salaryGrid, "💼 Base Salary:", String.format("Rp %,.0f", baseSalary), row++);
        addSalaryRow(salaryGrid, "📈 KPI Bonus (" + df.format(employee.getKpiScore()) + "%):", String.format("Rp %,.0f", kpiBonus), row++);
        addSalaryRow(salaryGrid, "⭐ Supervisor Bonus (" + df.format(employee.getSupervisorRating()) + "%):", String.format("Rp %,.0f", supervisorBonus), row++);
        if (penalty > 0) {
            addSalaryRow(salaryGrid, "⚠️ Performance Penalty:", String.format("- Rp %,.0f", penalty), row++);
        }

        Separator separator = new Separator();
        salaryGrid.add(separator, 0, row, 2, 1);
        row++;

        Label totalLabel = new Label("💰 Total Salary:");
        totalLabel.getStyleClass().add("total-salary");
        Label totalValue = new Label(String.format("Rp %,.0f", totalSalary));
        totalValue.getStyleClass().add("total-salary");

        salaryGrid.add(totalLabel, 0, row);
        salaryGrid.add(totalValue, 1, row);

        salaryBox.getChildren().addAll(breakdownTitle, salaryGrid);
        return salaryBox;
    }

    private void addSalaryRow(GridPane grid, String label, String value, int row) {
        Label labelNode = new Label(label);
        Label valueNode = new Label(value);
        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
    }

    private double calculateKPIBonus(double kpiScore, double baseSalary) {
        if (kpiScore >= 90) return baseSalary * 0.20;
        else if (kpiScore >= 80) return baseSalary * 0.15;
        else if (kpiScore >= 70) return baseSalary * 0.10;
        else if (kpiScore >= 60) return baseSalary * 0.05;
        return 0;
    }

    private double calculateSupervisorBonus(double supervisorRating, double baseSalary) {
        if (supervisorRating >= 90) return baseSalary * 0.15;
        else if (supervisorRating >= 80) return baseSalary * 0.10;
        else if (supervisorRating >= 70) return baseSalary * 0.05;
        return 0;
    }

    private double calculatePenalty(double kpiScore, double supervisorRating, double baseSalary) {
        if (kpiScore < 60 || supervisorRating < 60) {
            return baseSalary * 0.10;
        }
        return 0;
    }

    private TableView<SalaryHistory> createSalaryHistoryTable() {
        TableView<SalaryHistory> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); // PERUBAHAN DI SINI

        TableColumn<SalaryHistory, String> monthCol = new TableColumn<>("📅 Month");
        monthCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMonthName()));
        monthCol.setPrefWidth(100);

        TableColumn<SalaryHistory, Integer> yearCol = new TableColumn<>("📅 Year");
        yearCol.setCellValueFactory(new PropertyValueFactory<>("tahun"));
        yearCol.setPrefWidth(80);

        TableColumn<SalaryHistory, String> baseSalaryCol = new TableColumn<>("💼 Base Salary");
        baseSalaryCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.format("Rp %,.0f", cellData.getValue().getBaseSalary())));
        baseSalaryCol.setPrefWidth(120);

        TableColumn<SalaryHistory, String> totalSalaryCol = new TableColumn<>("💰 Total Salary");
        totalSalaryCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.format("Rp %,.0f", cellData.getValue().getTotalSalary())));
        totalSalaryCol.setPrefWidth(120);

        table.getColumns().addAll(monthCol, yearCol, baseSalaryCol, totalSalaryCol);

        if (dataStore != null && employee != null) {
            List<SalaryHistory> mySalaryHistory = dataStore.getSalaryHistoryByEmployee(employee.getId());
            table.setItems(FXCollections.observableArrayList(mySalaryHistory));
        }
        table.setPrefHeight(300);

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
        if (employee == null || dataStore == null) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("👤 Edit Profile");
        dialog.setHeaderText("Update your name and password.");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(25, 150, 15, 15));
        grid.getStyleClass().add("kpi-set-form");

        TextField nameField = new TextField();
        nameField.setText(employee.getNama());

        PasswordField passwordField = new PasswordField();
        passwordField.setText(employee.getPassword());

        grid.add(new Label("👤 Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("🔒 Password:"), 0, 1);
        grid.add(passwordField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                employee.setNama(nameField.getText());
                employee.setPassword(passwordField.getText());
                try {
                    dataStore.updateEmployee(employee);
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Profile updated successfully!");
                    userWelcomeLabel.setText("Welcome, " + employee.getNama() + " (Employee)");
                    navUserGreeting.setText("Hello, " + employee.getNama() + "!");
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to update profile.");
                }
            }
        });
    }
}