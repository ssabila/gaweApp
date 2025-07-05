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
                    createNavButton("⭐ Team KPI Evaluation", this::showTeamKPIEvaluationContent), // NEW: KPI Evaluation
                    createNavButton("📊 Monthly Evaluation", this::showMonthlyEvaluationContent),
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

            double avgKpi = teamMembers.stream().mapToDouble(Employee::getKpiScore).average().orElse(0.0);
            VBox avgKpiCard = createStatsCard("Avg KPI", String.format("%.1f%%", avgKpi), "📊", "#2ecc71");

            long atRiskCount = teamMembers.stream().filter(Employee::isLayoffRisk).count();
            VBox atRiskCard = createStatsCard("At Risk", String.valueOf(atRiskCount), "⚠️", "#e74c3c");

            // Get pending leave requests for this supervisor
            List<LeaveRequest> pendingApprovals = dataStore.getLeaveRequestsForApproval(supervisor.getId());
            VBox pendingApprovalsCard = createStatsCard("Pending Approvals", String.valueOf(pendingApprovals.size()), "✅", "#f39c12");

            statsContainer.getChildren().addAll(teamSizeCard, avgKpiCard, atRiskCard, pendingApprovalsCard);
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

    // NEW METHOD: Team KPI Evaluation Content
    private void showTeamKPIEvaluationContent() {
        if (supervisor == null || dataStore == null || contentArea == null) {
            return;
        }

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("⭐ Team KPI Evaluation - " + supervisor.getDivisi());
        title.getStyleClass().add("content-title");

        VBox evaluationForm = createTeamKPIEvaluationForm();
        TableView<Employee> teamKPITable = createTeamKPITable();

        content.getChildren().addAll(title, evaluationForm, teamKPITable);
        setScrollableContent(content);
    }

    // NEW METHOD: Create Team KPI Evaluation Form
    private VBox createTeamKPIEvaluationForm() {
        VBox form = new VBox(20);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(30));
        form.getStyleClass().add("kpi-set-form");

        Label formTitle = new Label("📊 Evaluate Team Member KPI");
        formTitle.getStyleClass().add("form-title");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(20);
        formGrid.setVgap(15);
        formGrid.setAlignment(Pos.CENTER);

        // Employee selection
        ComboBox<Employee> employeeCombo = new ComboBox<>();
        employeeCombo.setPromptText("Select Team Member");
        employeeCombo.setCellFactory(listView -> new ListCell<Employee>() {
            @Override
            protected void updateItem(Employee item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNama() + " (" + item.getJabatan() + ")");
                }
            }
        });
        employeeCombo.setButtonCell(new ListCell<Employee>() {
            @Override
            protected void updateItem(Employee item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNama() + " (" + item.getJabatan() + ")");
                }
            }
        });

        // Load team members (employees only)
        if (dataStore != null && supervisor != null) {
            try {
                List<Employee> teamMembers = dataStore.getEmployeesByDivision(supervisor.getDivisi()).stream()
                        .filter(emp -> emp.getRole().equals("pegawai"))
                        .collect(Collectors.toList());
                employeeCombo.getItems().addAll(teamMembers);
            } catch (Exception e) {
                logger.severe("Error loading team members: " + e.getMessage());
            }
        }

        // Month and Year selection
        ComboBox<String> monthCombo = new ComboBox<>();
        monthCombo.getItems().addAll("January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December");
        monthCombo.setValue("December");

        ComboBox<Integer> yearCombo = new ComboBox<>();
        for (int year = 2020; year <= 2030; year++) {
            yearCombo.getItems().add(year);
        }
        yearCombo.setValue(LocalDate.now().getYear());

        // Performance Metrics
        Slider punctualitySlider = new Slider(0, 100, 75);
        punctualitySlider.setShowTickLabels(true);
        punctualitySlider.setShowTickMarks(true);
        punctualitySlider.setMajorTickUnit(25);

        Label punctualityValue = new Label("75.0");
        punctualitySlider.valueProperty().addListener((obs, oldVal, newVal) ->
                punctualityValue.setText(String.format("%.1f", newVal.doubleValue())));

        Slider attendanceSlider = new Slider(0, 100, 80);
        attendanceSlider.setShowTickLabels(true);
        attendanceSlider.setShowTickMarks(true);
        attendanceSlider.setMajorTickUnit(25);

        Label attendanceValue = new Label("80.0");
        attendanceSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                attendanceValue.setText(String.format("%.1f", newVal.doubleValue())));

        Slider productivitySlider = new Slider(0, 100, 75);
        productivitySlider.setShowTickLabels(true);
        productivitySlider.setShowTickMarks(true);
        productivitySlider.setMajorTickUnit(25);

        Label productivityValue = new Label("75.0");
        productivitySlider.valueProperty().addListener((obs, oldVal, newVal) ->
                productivityValue.setText(String.format("%.1f", newVal.doubleValue())));

        // Comments
        TextArea commentsArea = new TextArea();
        commentsArea.setPromptText("Enter evaluation comments...");
        commentsArea.setPrefRowCount(3);
        commentsArea.setMaxWidth(400);

        // Add to grid
        formGrid.add(new Label("👤 Team Member:"), 0, 0);
        formGrid.add(employeeCombo, 1, 0);
        formGrid.add(new Label("📅 Month:"), 0, 1);
        formGrid.add(monthCombo, 1, 1);
        formGrid.add(new Label("📅 Year:"), 0, 2);
        formGrid.add(yearCombo, 1, 2);
        formGrid.add(new Label("⏰ Punctuality:"), 0, 3);
        formGrid.add(punctualitySlider, 1, 3);
        formGrid.add(punctualityValue, 2, 3);
        formGrid.add(new Label("📊 Attendance:"), 0, 4);
        formGrid.add(attendanceSlider, 1, 4);
        formGrid.add(attendanceValue, 2, 4);
        formGrid.add(new Label("💼 Productivity:"), 0, 5);
        formGrid.add(productivitySlider, 1, 5);
        formGrid.add(productivityValue, 2, 5);
        formGrid.add(new Label("💬 Comments:"), 0, 6);
        formGrid.add(commentsArea, 1, 6, 2, 1);

        Button submitBtn = new Button("✅ Submit Evaluation");
        submitBtn.getStyleClass().add("action-button-green");

        submitBtn.setOnAction(e -> {
            Employee selectedEmployee = employeeCombo.getValue();
            if (selectedEmployee != null) {
                try {
                    int month = monthCombo.getSelectionModel().getSelectedIndex() + 1;
                    int year = yearCombo.getValue();

                    // Check if evaluation already exists
                    if (dataStore.hasMonthlyEvaluation(selectedEmployee.getId(), month, year)) {
                        showAlert(Alert.AlertType.WARNING, "Evaluation Exists",
                                "An evaluation for this employee already exists for " + monthCombo.getValue() + " " + year);
                        return;
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
                        showAlert(Alert.AlertType.INFORMATION, "Success",
                                "Monthly evaluation for " + selectedEmployee.getNama() + " saved successfully!");

                        // Reset form
                        employeeCombo.setValue(null);
                        punctualitySlider.setValue(75);
                        attendanceSlider.setValue(80);
                        productivitySlider.setValue(75);
                        commentsArea.setText("");

                        // Refresh team KPI table if it exists
                        refreshTeamKPITable();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to save evaluation.");
                    }
                } catch (Exception ex) {
                    logger.severe("Error saving monthly evaluation: " + ex.getMessage());
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to save evaluation: " + ex.getMessage());
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "Warning", "Please select a team member.");
            }
        });

        form.getChildren().addAll(formTitle, formGrid, submitBtn);
        return form;
    }

    // NEW METHOD: Create Team KPI Table
    private TableView<Employee> createTeamKPITable() {
        TableView<Employee> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setPrefHeight(400);

        TableColumn<Employee, String> nameCol = new TableColumn<>("👤 Employee");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("nama"));
        nameCol.setPrefWidth(150);

        TableColumn<Employee, String> positionCol = new TableColumn<>("💼 Position");
        positionCol.setCellValueFactory(new PropertyValueFactory<>("jabatan"));
        positionCol.setPrefWidth(120);

        TableColumn<Employee, String> kpiCol = new TableColumn<>("📊 Current KPI");
        kpiCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getKpiScore()) + "%"));
        kpiCol.setPrefWidth(100);

        TableColumn<Employee, String> supervisorRatingCol = new TableColumn<>("⭐ Supervisor Rating");
        supervisorRatingCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(df.format(cellData.getValue().getSupervisorRating()) + "%"));
        supervisorRatingCol.setPrefWidth(120);

        TableColumn<Employee, String> riskCol = new TableColumn<>("⚠️ Risk Status");
        riskCol.setCellValueFactory(cellData -> {
            boolean isAtRisk = cellData.getValue().isLayoffRisk();
            return new javafx.beans.property.SimpleStringProperty(isAtRisk ? "⚠️ At Risk" : "✅ Good");
        });
        riskCol.setPrefWidth(100);

        table.getColumns().addAll(nameCol, positionCol, kpiCol, supervisorRatingCol, riskCol);

        refreshTeamKPITable(table);

        return table;
    }

    private void refreshTeamKPITable() {
        // This method can be called to refresh any existing team KPI table
        if (teamTable != null) {
            refreshTeamKPITable(teamTable);
        }
    }

    private void refreshTeamKPITable(TableView<Employee> table) {
        if (dataStore != null && supervisor != null) {
            try {
                List<Employee> teamMembers = dataStore.getEmployeesByDivision(supervisor.getDivisi()).stream()
                        .filter(emp -> emp.getRole().equals("pegawai"))
                        .collect(Collectors.toList());
                table.setItems(FXCollections.observableArrayList(teamMembers));
            } catch (Exception e) {
                logger.severe("Error loading team members for KPI table: " + e.getMessage());
            }
        }
    }

    // IMPROVED METHOD: Leave Approvals Content with better filtering
    private void showLeaveApprovalsContent() {
        if (supervisor == null || dataStore == null || contentArea == null) {
            return;
        }

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("✅ Leave Request Approvals - Supervisor Level");
        title.getStyleClass().add("content-title");

        Label infoLabel = new Label("📋 You can approve leave requests from employees in your " + supervisor.getDivisi() + " division");
        infoLabel.getStyleClass().add("page-subtitle");

        if (leaveApprovalsTable == null) {
            leaveApprovalsTable = createImprovedLeaveApprovalTable();
        } else {
            refreshLeaveApprovalsTable();
        }

        content.getChildren().addAll(title, infoLabel, leaveApprovalsTable);
        setScrollableContent(content);
    }

    // IMPROVED METHOD: Create Enhanced Leave Approval Table
    private TableView<LeaveRequest> createImprovedLeaveApprovalTable() {
        TableView<LeaveRequest> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setPrefHeight(400);

        TableColumn<LeaveRequest, String> employeeCol = new TableColumn<>("👤 Employee");
        employeeCol.setCellValueFactory(cellData -> {
            Employee emp = dataStore.getEmployeeById(cellData.getValue().getEmployeeId());
            return new javafx.beans.property.SimpleStringProperty(emp != null ? emp.getNama() : "Unknown");
        });
        employeeCol.setPrefWidth(150);

        TableColumn<LeaveRequest, String> positionCol = new TableColumn<>("💼 Position");
        positionCol.setCellValueFactory(cellData -> {
            Employee emp = dataStore.getEmployeeById(cellData.getValue().getEmployeeId());
            return new javafx.beans.property.SimpleStringProperty(emp != null ? emp.getJabatan() : "Unknown");
        });
        positionCol.setPrefWidth(120);

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

        TableColumn<LeaveRequest, Void> actionCol = new TableColumn<>("⚡ Actions");
        actionCol.setCellFactory(col -> new TableCell<LeaveRequest, Void>() {
            private final HBox actionBox = new HBox(5);
            private final Button approveBtn = new Button("✅ Approve");
            private final Button rejectBtn = new Button("❌ Reject");

            {
                approveBtn.getStyleClass().add("action-button-small-green");
                rejectBtn.getStyleClass().add("action-button-small-red");

                actionBox.getChildren().addAll(approveBtn, rejectBtn);
                actionBox.setAlignment(Pos.CENTER);

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
        actionCol.setPrefWidth(150);

        table.getColumns().addAll(employeeCol, positionCol, typeCol, startDateCol, endDateCol, daysCol, reasonCol, actionCol);

        refreshLeaveApprovalsTable(table);

        return table;
    }

    private void refreshLeaveApprovalsTable() {
        if (leaveApprovalsTable != null) {
            refreshLeaveApprovalsTable(leaveApprovalsTable);
        }
    }

    private void refreshLeaveApprovalsTable(TableView<LeaveRequest> table) {
        if (dataStore != null && supervisor != null) {
            try {
                System.out.println("=== SUPERVISOR LEAVE APPROVALS DEBUG ===");
                System.out.println("Supervisor: " + supervisor.getNama() + " (ID: " + supervisor.getId() + ")");
                System.out.println("Division: " + supervisor.getDivisi());
                System.out.println("Role: " + supervisor.getRole());

                List<LeaveRequest> pendingRequests = dataStore.getLeaveRequestsForApproval(supervisor.getId());
                System.out.println("Found " + pendingRequests.size() + " pending requests for approval");

                for (LeaveRequest req : pendingRequests) {
                    Employee emp = dataStore.getEmployeeById(req.getEmployeeId());
                    System.out.println("- Request from: " + (emp != null ? emp.getNama() + " (" + emp.getDivisi() + ")" : req.getEmployeeId()));
                }

                table.setItems(FXCollections.observableArrayList(pendingRequests));
            } catch (Exception e) {
                logger.severe("Error loading leave requests for approval: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // NEW METHOD: Show Leave Approval Dialog
    private void showLeaveApprovalDialog(LeaveRequest request, boolean isApproval) {
        Employee requestingEmployee = dataStore.getEmployeeById(request.getEmployeeId());
        String employeeName = requestingEmployee != null ? requestingEmployee.getNama() : "Unknown";

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isApproval ? "✅ Approve Leave Request" : "❌ Reject Leave Request");
        dialog.setHeaderText((isApproval ? "Approve" : "Reject") + " leave request from " + employeeName);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Request details
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(15);
        detailsGrid.setVgap(10);

        detailsGrid.add(new Label("👤 Employee:"), 0, 0);
        detailsGrid.add(new Label(employeeName), 1, 0);

        detailsGrid.add(new Label("💼 Position:"), 0, 1);
        detailsGrid.add(new Label(requestingEmployee != null ? requestingEmployee.getJabatan() : "Unknown"), 1, 1);

        detailsGrid.add(new Label("📝 Leave Type:"), 0, 2);
        detailsGrid.add(new Label(request.getLeaveType()), 1, 2);

        detailsGrid.add(new Label("📅 Start Date:"), 0, 3);
        detailsGrid.add(new Label(sdf.format(request.getStartDate())), 1, 3);

        detailsGrid.add(new Label("📅 End Date:"), 0, 4);
        detailsGrid.add(new Label(sdf.format(request.getEndDate())), 1, 4);

        detailsGrid.add(new Label("📊 Total Days:"), 0, 5);
        detailsGrid.add(new Label(String.valueOf(request.getTotalDays())), 1, 5);

        detailsGrid.add(new Label("📋 Reason:"), 0, 6);
        Label reasonLabel = new Label(request.getReason());
        reasonLabel.setWrapText(true);
        reasonLabel.setMaxWidth(300);
        detailsGrid.add(reasonLabel, 1, 6);

        // Notes area
        Label notesLabel = new Label("💬 " + (isApproval ? "Approval" : "Rejection") + " Notes:");
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Enter your notes here...");
        notesArea.setPrefRowCount(4);
        notesArea.setMaxWidth(400);

        content.getChildren().addAll(
                new Label("📋 Request Details:"),
                detailsGrid,
                new Separator(),
                notesLabel,
                notesArea
        );

        dialog.getDialogPane().setContent(content);

        ButtonType confirmButton = new ButtonType(isApproval ? "✅ Approve" : "❌ Reject", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(confirmButton, cancelButton);

        dialog.showAndWait().ifPresent(result -> {
            if (result == confirmButton) {
                if (notesArea.getText().trim().isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Missing Notes", "Please provide notes for your decision.");
                    return;
                }

                try {
                    boolean success;
                    if (isApproval) {
                        success = dataStore.approveLeaveRequest(request.getId(), supervisor.getId(), notesArea.getText());
                    } else {
                        success = dataStore.rejectLeaveRequest(request.getId(), supervisor.getId(), notesArea.getText());
                    }

                    if (success) {
                        String action = isApproval ? "approved" : "rejected";
                        showAlert(Alert.AlertType.INFORMATION, "Success",
                                "Leave request " + action + " successfully!");
                        refreshLeaveApprovalsTable();
                        // Refresh dashboard stats
                        showDashboardContent();
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

    // Other existing methods remain the same...
    private void showMyAttendance() {
        if (supervisor == null || dataStore == null || contentArea == null) {
            return;
        }

        VBox content = new VBox(20);
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

        TableColumn<Attendance, String> dateCol = new TableColumn<>("📅 Date");
        dateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getTanggal())));

        TableColumn<Attendance, String> clockInCol = new TableColumn<>("⏰ Clock In");
        clockInCol.setCellValueFactory(new PropertyValueFactory<>("jamMasuk"));

        TableColumn<Attendance, String> clockOutCol = new TableColumn<>("🏃 Clock Out");
        clockOutCol.setCellValueFactory(new PropertyValueFactory<>("jamKeluar"));

        TableColumn<Attendance, String> statusCol = new TableColumn<>("📊 Status");
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

        Label title = new Label("📅 My Meetings");
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

        TableColumn<Meeting, String> titleCol = new TableColumn<>("📋 Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Meeting, String> dateCol = new TableColumn<>("📅 Date");
        dateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getTanggal())));

        TableColumn<Meeting, String> timeCol = new TableColumn<>("⏰ Time");
        timeCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getWaktuMulai() + " - " + cellData.getValue().getWaktuSelesai()));

        TableColumn<Meeting, String> locationCol = new TableColumn<>("📍 Location");
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
        dialog.setTitle("📅 Schedule New Meeting");
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
        startTimeField.setText("09:00");

        TextField endTimeField = new TextField();
        endTimeField.setPromptText("End time (HH:MM)");
        endTimeField.setText("10:00");

        TextField locationField = new TextField();
        locationField.setPromptText("Meeting location...");

        content.getChildren().addAll(
                new Label("📋 Title:"), titleField,
                new Label("📝 Description:"), descriptionArea,
                new Label("📅 Date:"), datePicker,
                new Label("⏰ Start Time:"), startTimeField,
                new Label("⏰ End Time:"), endTimeField,
                new Label("📍 Location:"), locationField
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

        TableColumn<LeaveRequest, String> typeCol = new TableColumn<>("📝 Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("leaveType"));

        TableColumn<LeaveRequest, String> startDateCol = new TableColumn<>("📅 Start Date");
        startDateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getStartDate())));

        TableColumn<LeaveRequest, String> endDateCol = new TableColumn<>("📅 End Date");
        endDateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(sdf.format(cellData.getValue().getEndDate())));

        TableColumn<LeaveRequest, Integer> daysCol = new TableColumn<>("📊 Days");
        daysCol.setCellValueFactory(new PropertyValueFactory<>("totalDays"));

        TableColumn<LeaveRequest, String> statusCol = new TableColumn<>("✅ Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<LeaveRequest, String> notesCol = new TableColumn<>("📋 Approval Notes");
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
        dialog.setTitle("🏖️ Request Leave");
        dialog.setHeaderText("Submit a new leave request (will be approved by Manager)");

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
                        boolean success = dataStore.saveLeaveRequest(supervisor.getId(), leaveTypeCombo.getValue(),
                                startUtilDate, endUtilDate, reasonArea.getText());
                        if (success) {
                            showAlert(Alert.AlertType.INFORMATION, "Success",
                                    "Leave request submitted successfully!\nIt will be reviewed by the Manager.");
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

    // Dummy implementations for other methods to keep the class complete
    private void showTeamManagementContent() {
        // Implementation stays the same as before
        if (supervisor == null || dataStore == null || contentArea == null) {
            return;
        }

        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("dashboard-content-container");

        Label title = new Label("👥 Team Management - " + supervisor.getDivisi() + " Division");
        title.getStyleClass().add("content-title");

        Button addEmployeeBtn = new Button("➕ Add Employee");
        addEmployeeBtn.getStyleClass().add("action-button-green");
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
        table.setPrefHeight(400);

        TableColumn<Employee, String> nameCol = new TableColumn<>("👤 Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("nama"));

        TableColumn<Employee, String> positionCol = new TableColumn<>("💼 Position");
        positionCol.setCellValueFactory(new PropertyValueFactory<>("jabatan"));

        TableColumn<Employee, String> kpiCol = new TableColumn<>("📊 KPI Score");
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

    // Other dummy methods
    private void showMonthlyEvaluationContent() { /* Implementation */ }
    private void showUploadReportContent() { /* Implementation */ }
    private void showPerformanceAnalyticsContent() { /* Implementation */ }
    private void showSalaryManagementContent() { /* Implementation */ }
    private void showAllHistoryContent() { /* Implementation */ }

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
        dialog.setTitle("👤 Edit Profile");
        dialog.setHeaderText("Update your name and password.");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setText(supervisor.getNama());

        PasswordField passwordField = new PasswordField();
        passwordField.setText(supervisor.getPassword());

        grid.add(new Label("👤 Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("🔒 Password:"), 0, 1);
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
        dialog.setTitle("➕ Add New Employee");
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

        grid.add(new Label("👤 Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("🔒 Password:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label("💼 Position:"), 0, 2);
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