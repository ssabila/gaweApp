package data;

import database.DatabaseConnection;
import database.DatabaseException;
import database.MySQLDatabaseManager;
import models.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.logging.Logger;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

public class MySQLDataStore implements IDataStore {
    private static final Logger logger = Logger.getLogger(MySQLDataStore.class.getName());
    private final DatabaseConnection dbConnection;
    private MySQLDatabaseManager dbManager;

    public static class MonthlyEvaluation {
        private int id;
        private String employeeId;
        private String supervisorId;
        private int month;
        private int year;
        private double punctualityScore;
        private double attendanceScore;
        private double productivityScore;
        private double overallRating;
        private String comments;
        private Date evaluationDate;

        public MonthlyEvaluation() {}

        // Getters and setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
        public String getSupervisorId() { return supervisorId; }
        public void setSupervisorId(String supervisorId) { this.supervisorId = supervisorId; }
        public int getMonth() { return month; }
        public void setMonth(int month) { this.month = month; }
        public int getYear() { return year; }
        public void setYear(int year) { this.year = year; }
        public double getPunctualityScore() { return punctualityScore; }
        public void setPunctualityScore(double punctualityScore) { this.punctualityScore = punctualityScore; }
        public double getAttendanceScore() { return attendanceScore; }
        public void setAttendanceScore(double attendanceScore) { this.attendanceScore = attendanceScore; }
        public double getProductivityScore() { return productivityScore; }
        public void setProductivityScore(double productivityScore) { this.productivityScore = productivityScore; }
        public double getOverallRating() { return overallRating; }
        public void setOverallRating(double overallRating) { this.overallRating = overallRating; }
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
        public Date getEvaluationDate() { return evaluationDate; }
        public void setEvaluationDate(Date evaluationDate) { this.evaluationDate = evaluationDate; }
    }

    public MySQLDataStore() {
        System.out.println("Initializing MySQLDataStore...");

        try {
            // Initialize database connection
            this.dbConnection = DatabaseConnection.getInstance();
            System.out.println("✅ Database connection instance created");

            // Test connection
            try (Connection conn = dbConnection.getConnection()) {
                System.out.println("✅ Test connection successful");
            }

            // Initialize database schema and data
            this.dbManager = new MySQLDatabaseManager();
            this.dbManager.initializeDatabase();
            System.out.println("✅ Database initialized successfully");

            logger.info("MySQL DataStore initialized successfully");

        } catch (SQLException e) {
            String errorMsg = "Failed to connect to MySQL database: " + e.getMessage();
            System.err.println("❌ " + errorMsg);
            throw new DatabaseException.InitializationException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Failed to initialize MySQL DataStore: " + e.getMessage();
            System.err.println("❌ " + errorMsg);
            e.printStackTrace();
            throw new DatabaseException.InitializationException(errorMsg, e);
        }
    }

    @Override
    public Employee authenticateUser(String employeeId, String password) {
        String query = "SELECT * FROM employees WHERE id = ? AND password = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, employeeId);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToEmployee(rs);
            }
        } catch (SQLException e) {
            logger.severe("Error authenticating user: " + e.getMessage());
            throw new DatabaseException.AuthenticationException("Authentication failed for user: " + employeeId);
        }
        return null;
    }

    @Override
    public List<Employee> getAllEmployees() {
        List<Employee> employees = new ArrayList<>();
        String query = "SELECT * FROM employees ORDER BY nama";
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                employees.add(mapResultSetToEmployee(rs));
            }
        } catch (SQLException e) {
            logger.severe("Error getting all employees: " + e.getMessage());
            throw new DatabaseException.QueryException("Failed to retrieve employees", e);
        }
        return employees;
    }

    @Override
    public List<Employee> getEmployeesByDivision(String divisi) {
        List<Employee> employees = new ArrayList<>();
        String query = "SELECT * FROM employees WHERE divisi = ? ORDER BY nama";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, divisi);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                employees.add(mapResultSetToEmployee(rs));
            }
        } catch (SQLException e) {
            logger.severe("Error getting employees by division: " + e.getMessage());
            throw new DatabaseException.QueryException("Failed to retrieve employees by division", e);
        }
        return employees;
    }

    @Override
    public Employee getEmployeeById(String id) {
        String query = "SELECT * FROM employees WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToEmployee(rs);
            }
        } catch (SQLException e) {
            logger.severe("Error getting employee by ID: " + e.getMessage());
            throw new DatabaseException.QueryException("Failed to retrieve employee by ID", e);
        }
        return null;
    }

    @Override
    public void updateEmployee(Employee employee) {
        String query = "UPDATE employees SET nama = ?, password = ?, role = ?, divisi = ?, jabatan = ?, tgl_masuk = ?, sisa_cuti = ?, gaji_pokok = ?, kpi_score = ?, supervisor_rating = ?, layoff_risk = ? WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, employee.getNama());
            pstmt.setString(2, employee.getPassword());
            pstmt.setString(3, employee.getRole());
            pstmt.setString(4, employee.getDivisi());
            pstmt.setString(5, employee.getJabatan());
            pstmt.setDate(6, new java.sql.Date(employee.getTglMasuk().getTime()));
            pstmt.setInt(7, employee.getSisaCuti());
            pstmt.setDouble(8, employee.getGajiPokok());
            pstmt.setDouble(9, employee.getKpiScore());
            pstmt.setDouble(10, employee.getSupervisorRating());
            pstmt.setBoolean(11, employee.isLayoffRisk());
            pstmt.setString(12, employee.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Error updating employee: " + e.getMessage());
            throw new DatabaseException.QueryException("Failed to update employee", e);
        }
    }

    @Override
    public void addEmployee(Employee employee) throws SQLException {
        String generatedId = generateNextEmployeeId(employee.getRole());
        employee.setId(generatedId);

        String query = "INSERT INTO employees (id, nama, password, role, divisi, jabatan, tgl_masuk, sisa_cuti, gaji_pokok, kpi_score, supervisor_rating, layoff_risk) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, employee.getId());
            pstmt.setString(2, employee.getNama());
            pstmt.setString(3, employee.getPassword());
            pstmt.setString(4, employee.getRole());
            pstmt.setString(5, employee.getDivisi());
            pstmt.setString(6, employee.getJabatan());
            pstmt.setDate(7, new java.sql.Date(employee.getTglMasuk().getTime()));
            pstmt.setInt(8, employee.getSisaCuti());
            pstmt.setDouble(9, employee.getGajiPokok());
            pstmt.setDouble(10, employee.getKpiScore());
            pstmt.setDouble(11, employee.getSupervisorRating());
            pstmt.setBoolean(12, employee.isLayoffRisk());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Error adding employee: " + e.getMessage());
            throw new DatabaseException.QueryException("Failed to add employee", e);
        }
    }

    private String generateNextEmployeeId(String role) throws SQLException {
        String prefix;
        switch (role.toLowerCase()) {
            case "pegawai":
                prefix = "EMP";
                break;
            case "supervisor":
                prefix = "SUP";
                break;
            case "manajer":
                prefix = "MNG";
                break;
            default:
                prefix = "EMP"; // Default to employee
                break;
        }

        String query = "SELECT id FROM employees WHERE id LIKE ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, prefix + "%");
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String lastId = rs.getString("id");
                int lastNumber = Integer.parseInt(lastId.substring(prefix.length()));
                return prefix + String.format("%03d", lastNumber + 1);
            } else {
                return prefix + "001";
            }
        }
    }

    @Override
    public List<KPI> getAllKPI() {
        List<KPI> kpiList = new ArrayList<>();
        String query = "SELECT * FROM kpi ORDER BY tahun DESC, bulan DESC";
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                kpiList.add(mapResultSetToKPI(rs));
            }
        } catch (SQLException e) {
            logger.severe("Error getting all KPI: " + e.getMessage());
            throw new DatabaseException.QueryException("Failed to retrieve KPI data", e);
        }
        return kpiList;
    }

    @Override
    public boolean saveKPI(String divisi, int bulan, int tahun, double score, String managerId) {
        String query = "INSERT INTO kpi (divisi, bulan, tahun, score, manager_id) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE score = VALUES(score), manager_id = VALUES(manager_id)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, divisi);
            pstmt.setInt(2, bulan);
            pstmt.setInt(3, tahun);
            pstmt.setDouble(4, score);
            pstmt.setString(5, managerId);
            int result = pstmt.executeUpdate();
            updateEmployeeKPIScores(divisi, score);
            return result > 0;
        } catch (SQLException e) {
            logger.severe("Error saving KPI: " + e.getMessage());
            return false;
        }
    }

    private void updateEmployeeKPIScores(String divisi, double kpiScore) {
        String query = "UPDATE employees SET kpi_score = ?, layoff_risk = ? WHERE divisi = ? AND role = 'pegawai'";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setDouble(1, kpiScore);
            pstmt.setBoolean(2, kpiScore < 60.0);
            pstmt.setString(3, divisi);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Error updating employee KPI scores: " + e.getMessage());
        }
    }

    @Override
    public List<Report> getAllReports() {
        List<Report> reports = new ArrayList<>();
        String query = "SELECT * FROM reports ORDER BY upload_date DESC";
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                reports.add(mapResultSetToReport(rs));
            }
        } catch (SQLException e) {
            logger.severe("Error getting all reports: " + e.getMessage());
            throw new DatabaseException.QueryException("Failed to retrieve reports", e);
        }
        return reports;
    }

    @Override
    public List<Report> getPendingReports() {
        List<Report> reports = new ArrayList<>();
        String query = "SELECT * FROM reports WHERE status = 'pending' ORDER BY upload_date DESC";
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                reports.add(mapResultSetToReport(rs));
            }
        } catch (SQLException e) {
            logger.severe("Error getting pending reports: " + e.getMessage());
            throw new DatabaseException.QueryException("Failed to retrieve pending reports", e);
        }
        return reports;
    }

    @Override
    public List<Report> getReportsByDivision(String divisi) {
        List<Report> reports = new ArrayList<>();
        String query = "SELECT * FROM reports WHERE divisi = ? ORDER BY upload_date DESC";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, divisi);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                reports.add(mapResultSetToReport(rs));
            }
        } catch (SQLException e) {
            logger.severe("Error getting reports by division: " + e.getMessage());
            throw new DatabaseException.QueryException("Failed to retrieve reports by division", e);
        }
        return reports;
    }

    @Override
    public boolean saveReport(String supervisorId, String divisi, int bulan, int tahun, String content) {
        String query = "INSERT INTO reports (supervisor_id, divisi, bulan, tahun, content, upload_date, status) VALUES (?, ?, ?, ?, ?, NOW(), 'pending')";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, supervisorId);
            pstmt.setString(2, divisi);
            pstmt.setInt(3, bulan);
            pstmt.setInt(4, tahun);
            pstmt.setString(5, content);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Error saving report: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean updateReportStatus(int reportId, String status, String managerNotes, String reviewedBy) {
        String query = "UPDATE reports SET status = ?, manager_notes = ?, reviewed_by = ?, reviewed_date = NOW() WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, status);
            pstmt.setString(2, managerNotes);
            pstmt.setString(3, reviewedBy);
            pstmt.setInt(4, reportId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Error updating report status: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<Attendance> getAttendanceByEmployee(String employeeId) {
        List<Attendance> attendanceList = new ArrayList<>();
        String query = "SELECT * FROM attendance WHERE employee_id = ? ORDER BY tanggal DESC";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, employeeId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                attendanceList.add(mapResultSetToAttendance(rs));
            }
        } catch (SQLException e) {
            logger.severe("Error getting attendance by employee: " + e.getMessage());
            throw new DatabaseException.QueryException("Failed to retrieve attendance", e);
        }
        return attendanceList;
    }

    @Override
    public List<Attendance> getTodayAttendance(String employeeId) {
        List<Attendance> attendanceList = new ArrayList<>();
        String query = "SELECT * FROM attendance WHERE employee_id = ? AND DATE(tanggal) = CURDATE()";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, employeeId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                attendanceList.add(mapResultSetToAttendance(rs));
            }
        } catch (SQLException e) {
            logger.severe("Error getting today's attendance: " + e.getMessage());
            throw new DatabaseException.QueryException("Failed to retrieve today's attendance", e);
        }
        return attendanceList;
    }

    @Override
    public boolean saveAttendance(String employeeId, Date tanggal, String jamMasuk, String jamKeluar, String status) {
        String query = "INSERT INTO attendance (employee_id, tanggal, jam_masuk, jam_keluar, status, is_late) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE jam_masuk = VALUES(jam_masuk), jam_keluar = VALUES(jam_keluar), status = VALUES(status)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, employeeId);
            pstmt.setDate(2, new java.sql.Date(tanggal.getTime()));
            if (jamMasuk != null && !jamMasuk.isEmpty()) {
                pstmt.setTime(3, Time.valueOf(jamMasuk + ":00"));
            } else {
                pstmt.setNull(3, Types.TIME);
            }
            if (jamKeluar != null && !jamKeluar.isEmpty()) {
                pstmt.setTime(4, Time.valueOf(jamKeluar + ":00"));
            } else {
                pstmt.setNull(4, Types.TIME);
            }
            pstmt.setString(5, status);
            pstmt.setBoolean(6, isLateArrival(jamMasuk));
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Error saving attendance: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean updateAttendanceClockOut(String employeeId, String jamKeluar) {
        String query = "UPDATE attendance SET jam_keluar = ? WHERE employee_id = ? AND DATE(tanggal) = CURDATE()";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setTime(1, Time.valueOf(jamKeluar + ":00"));
            pstmt.setString(2, employeeId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Error updating clock out: " + e.getMessage());
            return false;
        }
    }

    private boolean isLateArrival(String jamMasuk) {
        if (jamMasuk == null) return false;
        try {
            String[] parts = jamMasuk.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            return hour > 8 || (hour == 8 && minute > 30);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<Meeting> getMeetingsByEmployee(String employeeId) {
        List<Meeting> meetings = new ArrayList<>();
        String query = """
            SELECT DISTINCT m.* FROM meetings m 
            LEFT JOIN meeting_participants mp ON m.id = mp.meeting_id 
            WHERE m.organizer_id = ? OR mp.participant_id = ? 
            ORDER BY m.tanggal ASC
            """;
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, employeeId);
            pstmt.setString(2, employeeId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                meetings.add(mapResultSetToMeeting(rs));
            }
        } catch (SQLException e) {
            logger.severe("Error getting meetings by employee: " + e.getMessage());
            throw new DatabaseException.QueryException("Failed to retrieve meetings", e);
        }
        return meetings;
    }

    @Override
    public boolean saveMeeting(String title, String description, Date tanggal, String waktuMulai,
                               String waktuSelesai, String lokasi, String organizerId, List<String> participantIds) {
        String insertMeetingQuery = "INSERT INTO meetings (title, description, tanggal, waktu_mulai, waktu_selesai, lokasi, organizer_id, status) VALUES (?, ?, ?, ?, ?, ?, ?, 'scheduled')";
        String insertParticipantQuery = "INSERT INTO meeting_participants (meeting_id, participant_id) VALUES (?, ?)";

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);

            int meetingId;
            try (PreparedStatement pstmt = conn.prepareStatement(insertMeetingQuery, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, title);
                pstmt.setString(2, description);
                pstmt.setDate(3, new java.sql.Date(tanggal.getTime()));
                pstmt.setTime(4, Time.valueOf(waktuMulai + ":00"));
                pstmt.setTime(5, Time.valueOf(waktuSelesai + ":00"));
                pstmt.setString(6, lokasi);
                pstmt.setString(7, organizerId);
                pstmt.executeUpdate();

                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    meetingId = rs.getInt(1);
                } else {
                    throw new SQLException("Failed to get meeting ID");
                }
            }

            if (participantIds != null && !participantIds.isEmpty()) {
                try (PreparedStatement pstmt = conn.prepareStatement(insertParticipantQuery)) {
                    for (String participantId : participantIds) {
                        pstmt.setInt(1, meetingId);
                        pstmt.setString(2, participantId);
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            logger.severe("Error saving meeting: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<LeaveRequest> getAllLeaveRequests() {
        List<LeaveRequest> leaveRequests = new ArrayList<>();
        String query = "SELECT * FROM leave_requests ORDER BY request_date DESC";
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                leaveRequests.add(mapResultSetToLeaveRequest(rs));
            }
        } catch (SQLException e) {
            logger.severe("Error getting all leave requests: " + e.getMessage());
            throw new DatabaseException.QueryException("Failed to retrieve leave requests", e);
        }
        return leaveRequests;
    }

    @Override
    public List<LeaveRequest> getLeaveRequestsByEmployee(String employeeId) {
        List<LeaveRequest> leaveRequests = new ArrayList<>();
        String query = "SELECT * FROM leave_requests WHERE employee_id = ? ORDER BY request_date DESC";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, employeeId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                leaveRequests.add(mapResultSetToLeaveRequest(rs));
            }
        } catch (SQLException e) {
            logger.severe("Error getting leave requests by employee: " + e.getMessage());
            throw new DatabaseException.QueryException("Failed to retrieve leave requests", e);
        }
        return leaveRequests;
    }

    @Override
    public List<LeaveRequest> getPendingLeaveRequests() {
        List<LeaveRequest> leaveRequests = new ArrayList<>();
        String query = "SELECT * FROM leave_requests WHERE status = 'pending' ORDER BY request_date ASC";
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                leaveRequests.add(mapResultSetToLeaveRequest(rs));
            }
        } catch (SQLException e) {
            logger.severe("Error getting pending leave requests: " + e.getMessage());
            throw new DatabaseException.QueryException("Failed to retrieve pending leave requests", e);
        }
        return leaveRequests;
    }

    @Override
    public List<LeaveRequest> getLeaveRequestsForApproval(String approverId) {
        Employee approver = getEmployeeById(approverId);
        if (approver == null) {
            logger.warning("Approver not found: " + approverId);
            return new ArrayList<>();
        }

        System.out.println("Getting leave requests for approval by: " + approver.getNama() + " (" + approver.getRole() + ")");

        String query;
        if (approver.getRole().equals("supervisor")) {
            // Supervisors approve employees in their division
            query = """
            SELECT lr.* FROM leave_requests lr
            JOIN employees e ON lr.employee_id = e.id
            WHERE lr.status = 'pending' AND e.role = 'pegawai' AND e.divisi = ?
            ORDER BY lr.request_date ASC
            """;
            System.out.println("Supervisor approving employees in division: " + approver.getDivisi());
        } else if (approver.getRole().equals("manajer")) {
            // Managers approve supervisors and any employee
            query = """
            SELECT lr.* FROM leave_requests lr
            JOIN employees e ON lr.employee_id = e.id
            WHERE lr.status = 'pending' AND (e.role = 'supervisor' OR e.role = 'pegawai')
            ORDER BY lr.request_date ASC
            """;
            System.out.println("Manager approving all supervisor and employee requests");
        } else {
            System.out.println("Role not authorized for approvals: " + approver.getRole());
            return new ArrayList<>();
        }

        List<LeaveRequest> leaveRequests = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            if (approver.getRole().equals("supervisor")) {
                pstmt.setString(1, approver.getDivisi());
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                leaveRequests.add(mapResultSetToLeaveRequest(rs));
            }
            System.out.println("Found " + leaveRequests.size() + " pending requests for approval");
        } catch (SQLException e) {
            logger.severe("Error getting leave requests for approval: " + e.getMessage());
            throw new DatabaseException.QueryException("Failed to retrieve leave requests for approval", e);
        }
        return leaveRequests;
    }

    @Override
    public boolean saveLeaveRequest(String employeeId, String leaveType, Date startDate, Date endDate, String reason) {
        // FIX: Use safer date conversion method
        LocalDate start = convertToLocalDate(startDate);
        LocalDate end = convertToLocalDate(endDate);
        int totalDays = (int) ChronoUnit.DAYS.between(start, end) + 1;

        String query = "INSERT INTO leave_requests (employee_id, leave_type, start_date, end_date, total_days, reason, status, request_date) VALUES (?, ?, ?, ?, ?, ?, 'pending', NOW())";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, employeeId);
            pstmt.setString(2, leaveType);
            pstmt.setDate(3, new java.sql.Date(startDate.getTime()));
            pstmt.setDate(4, new java.sql.Date(endDate.getTime()));
            pstmt.setInt(5, totalDays);
            pstmt.setString(6, reason);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Error saving leave request: " + e.getMessage());
            return false;
        }
    }

    // Add this helper method to MySQLDataStore.java
    private LocalDate convertToLocalDate(Date date) {
        if (date instanceof java.sql.Date) {
            // For java.sql.Date, use toLocalDate() directly
            return ((java.sql.Date) date).toLocalDate();
        } else {
            // For java.util.Date, use different approach
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
    }

    @Override
    public boolean approveLeaveRequest(int leaveRequestId, String approverId, String notes) {
        String query = "UPDATE leave_requests SET status = 'approved', approver_id = ?, approver_notes = ?, approval_date = NOW() WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, approverId);
            pstmt.setString(2, notes);
            pstmt.setInt(3, leaveRequestId);

            int result = pstmt.executeUpdate();
            if (result > 0) {
                // Deduct leave days from employee
                LeaveRequest request = getLeaveRequestById(leaveRequestId);
                if (request != null) {
                    Employee employee = getEmployeeById(request.getEmployeeId());
                    if (employee != null) {
                        employee.setSisaCuti(employee.getSisaCuti() - request.getTotalDays());
                        updateEmployee(employee);
                    }
                }
            }
            return result > 0;
        } catch (SQLException e) {
            logger.severe("Error approving leave request: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean rejectLeaveRequest(int leaveRequestId, String approverId, String notes) {
        String query = "UPDATE leave_requests SET status = 'rejected', approver_id = ?, approver_notes = ?, approval_date = NOW() WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, approverId);
            pstmt.setString(2, notes);
            pstmt.setInt(3, leaveRequestId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Error rejecting leave request: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<LeaveRequest> getPendingLeaveRequestsByEmployee(String employeeId) {
        List<LeaveRequest> leaveRequests = new ArrayList<>();
        String query = "SELECT * FROM leave_requests WHERE employee_id = ? AND status = 'pending' ORDER BY request_date DESC";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, employeeId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                leaveRequests.add(mapResultSetToLeaveRequest(rs));
            }
        } catch (SQLException e) {
            logger.severe("Error getting pending leave requests by employee: " + e.getMessage());
            throw new DatabaseException.QueryException("Failed to retrieve pending leave requests", e);
        }
        return leaveRequests;
    }

    private LeaveRequest getLeaveRequestById(int id) {
        String query = "SELECT * FROM leave_requests WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToLeaveRequest(rs);
            }
        } catch (SQLException e) {
            logger.severe("Error getting leave request by ID: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<SalaryHistory> getAllSalaryHistory() {
        List<SalaryHistory> salaryHistories = new ArrayList<>();
        String query = "SELECT * FROM salary_history ORDER BY tahun DESC, bulan DESC";
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                salaryHistories.add(mapResultSetToSalaryHistory(rs));
            }
        } catch (SQLException e) {
            logger.severe("Error getting all salary history: " + e.getMessage());
            throw new DatabaseException.QueryException("Failed to retrieve salary history", e);
        }
        return salaryHistories;
    }

    @Override
    public List<SalaryHistory> getSalaryHistoryByEmployee(String employeeId) {
        List<SalaryHistory> salaryHistories = new ArrayList<>();
        String query = "SELECT * FROM salary_history WHERE employee_id = ? ORDER BY tahun DESC, bulan DESC";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, employeeId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                salaryHistories.add(mapResultSetToSalaryHistory(rs));
            }
        } catch (SQLException e) {
            logger.severe("Error getting salary history by employee: " + e.getMessage());
            throw new DatabaseException.QueryException("Failed to retrieve salary history", e);
        }
        return salaryHistories;
    }

    @Override
    public List<EmployeeEvaluation> getAllEvaluations() {
        List<EmployeeEvaluation> evaluations = new ArrayList<>();
        String query = "SELECT * FROM employee_evaluations ORDER BY evaluation_date DESC";
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                evaluations.add(mapResultSetToEmployeeEvaluation(rs));
            }
        } catch (SQLException e) {
            logger.severe("Error getting all evaluations: " + e.getMessage());
            throw new DatabaseException.QueryException("Failed to retrieve evaluations", e);
        }
        return evaluations;
    }

    @Override
    public boolean saveEmployeeEvaluation(String employeeId, String supervisorId, double punctualityScore,
                                          double attendanceScore, double overallRating, String comments) {
        String query = "INSERT INTO employee_evaluations (employee_id, supervisor_id, punctuality_score, attendance_score, overall_rating, comments, evaluation_date) VALUES (?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, employeeId);
            pstmt.setString(2, supervisorId);
            pstmt.setDouble(3, punctualityScore);
            pstmt.setDouble(4, attendanceScore);
            pstmt.setDouble(5, overallRating);
            pstmt.setString(6, comments);

            int result = pstmt.executeUpdate();
            if (result > 0) {
                // Update employee supervisor rating
                Employee employee = getEmployeeById(employeeId);
                if (employee != null) {
                    employee.setSupervisorRating(overallRating);
                    employee.setLayoffRisk(employee.getKpiScore() < 60 || overallRating < 60);
                    updateEmployee(employee);
                }
            }
            return result > 0;
        } catch (SQLException e) {
            logger.severe("Error saving employee evaluation: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean saveMonthlyEmployeeEvaluation(String employeeId, String supervisorId, int month, int year,
                                                 double punctualityScore, double attendanceScore, double productivityScore,
                                                 double overallRating, String comments) {
        String query = "INSERT INTO monthly_evaluations (employee_id, supervisor_id, month, year, punctuality_score, attendance_score, productivity_score, overall_rating, comments, evaluation_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW()) ON DUPLICATE KEY UPDATE punctuality_score = VALUES(punctuality_score), attendance_score = VALUES(attendance_score), productivity_score = VALUES(productivity_score), overall_rating = VALUES(overall_rating), comments = VALUES(comments), evaluation_date = VALUES(evaluation_date)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, employeeId);
            pstmt.setString(2, supervisorId);
            pstmt.setInt(3, month);
            pstmt.setInt(4, year);
            pstmt.setDouble(5, punctualityScore);
            pstmt.setDouble(6, attendanceScore);
            pstmt.setDouble(7, productivityScore);
            pstmt.setDouble(8, overallRating);
            pstmt.setString(9, comments);

            int result = pstmt.executeUpdate();
            if (result > 0) {
                // Update employee supervisor rating with latest monthly evaluation
                Employee employee = getEmployeeById(employeeId);
                if (employee != null) {
                    employee.setSupervisorRating(overallRating);
                    employee.setOverallRating(overallRating);
                    employee.setLayoffRisk(employee.getKpiScore() < 60 || overallRating < 60);
                    updateEmployee(employee);
                }
            }
            return result > 0;
        } catch (SQLException e) {
            logger.severe("Error saving monthly employee evaluation: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean hasMonthlyEvaluation(String employeeId, int month, int year) {
        String query = "SELECT COUNT(*) FROM monthly_evaluations WHERE employee_id = ? AND month = ? AND year = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, employeeId);
            pstmt.setInt(2, month);
            pstmt.setInt(3, year);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.severe("Error checking monthly evaluation: " + e.getMessage());
        }
        return false;
    }

    @Override
    public List<MonthlyEvaluation> getAllMonthlyEvaluations() {
        List<MonthlyEvaluation> evaluations = new ArrayList<>();
        String query = "SELECT * FROM monthly_evaluations ORDER BY year DESC, month DESC";
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                evaluations.add(mapResultSetToMonthlyEvaluation(rs));
            }
        } catch (SQLException e) {
            logger.severe("Error getting all monthly evaluations: " + e.getMessage());
            throw new DatabaseException.QueryException("Failed to retrieve monthly evaluations", e);
        }
        return evaluations;
    }

    @Override
    public List<MonthlyEvaluation> getMonthlyEvaluationsBySupervisor(String supervisorId) {
        List<MonthlyEvaluation> evaluations = new ArrayList<>();
        String query = "SELECT * FROM monthly_evaluations WHERE supervisor_id = ? ORDER BY year DESC, month DESC";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, supervisorId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                evaluations.add(mapResultSetToMonthlyEvaluation(rs));
            }
        } catch (SQLException e) {
            logger.severe("Error getting monthly evaluations by supervisor: " + e.getMessage());
            throw new DatabaseException.QueryException("Failed to retrieve monthly evaluations", e);
        }
        return evaluations;
    }

    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        try (Connection conn = dbConnection.getConnection()) {
            // Total employees
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM employees");
                if (rs.next()) {
                    stats.put("totalEmployees", rs.getInt("total"));
                }
            }

            // Employees by role
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT role, COUNT(*) as count FROM employees GROUP BY role");
                while (rs.next()) {
                    stats.put("total" + rs.getString("role").substring(0, 1).toUpperCase() + rs.getString("role").substring(1), rs.getInt("count"));
                }
            }

            // Layoff risk employees
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM employees WHERE layoff_risk = true");
                if (rs.next()) {
                    stats.put("layoffRiskEmployees", rs.getInt("count"));
                }
            }

            // Pending reports
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM reports WHERE status = 'pending'");
                if (rs.next()) {
                    stats.put("pendingReports", rs.getInt("count"));
                }
            }

            // Pending leave requests
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM leave_requests WHERE status = 'pending'");
                if (rs.next()) {
                    stats.put("pendingLeaveRequests", rs.getInt("count"));
                }
            }

            // Upcoming meetings
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM meetings WHERE tanggal >= CURDATE() AND status = 'scheduled'");
                if (rs.next()) {
                    stats.put("upcomingMeetings", rs.getInt("count"));
                }
            }

        } catch (SQLException e) {
            logger.severe("Error getting dashboard stats: " + e.getMessage());
        }

        return stats;
    }

    @Override
    public void close() {
        if (dbManager != null) {
            try {
                dbManager.close();
            } catch (Exception e) {
                logger.warning("Error closing database manager: " + e.getMessage());
            }
        }
        logger.info("MySQLDataStore closed");
    }

    // Mapping methods
    private Employee mapResultSetToEmployee(ResultSet rs) throws SQLException {
        Employee emp = new Employee();
        emp.setId(rs.getString("id"));
        emp.setNama(rs.getString("nama"));
        emp.setPassword(rs.getString("password"));
        emp.setRole(rs.getString("role"));
        emp.setDivisi(rs.getString("divisi"));
        emp.setJabatan(rs.getString("jabatan"));
        emp.setTglMasuk(rs.getDate("tgl_masuk"));
        emp.setSisaCuti(rs.getInt("sisa_cuti"));
        emp.setGajiPokok(rs.getDouble("gaji_pokok"));
        emp.setKpiScore(rs.getDouble("kpi_score"));

        double supervisorRating = rs.getDouble("supervisor_rating");

        emp.setSupervisorRating(rs.getDouble("supervisor_rating"));
        emp.setOverallRating(supervisorRating);

        emp.setLayoffRisk(rs.getBoolean("layoff_risk"));
        return emp;
    }

    private KPI mapResultSetToKPI(ResultSet rs) throws SQLException {
        KPI kpi = new KPI();
        kpi.setId(rs.getInt("id"));
        kpi.setDivisi(rs.getString("divisi"));
        kpi.setBulan(rs.getInt("bulan"));
        kpi.setTahun(rs.getInt("tahun"));
        kpi.setScore(rs.getDouble("score"));
        kpi.setManagerId(rs.getString("manager_id"));
        kpi.setCreatedDate(rs.getTimestamp("created_date"));
        kpi.setNotes(rs.getString("notes"));
        return kpi;
    }

    private Report mapResultSetToReport(ResultSet rs) throws SQLException {
        Report report = new Report();
        report.setId(rs.getInt("id"));
        report.setSupervisorId(rs.getString("supervisor_id"));
        report.setDivisi(rs.getString("divisi"));
        report.setBulan(rs.getInt("bulan"));
        report.setTahun(rs.getInt("tahun"));
        report.setContent(rs.getString("content"));
        report.setUploadDate(rs.getTimestamp("upload_date"));
        report.setStatus(rs.getString("status"));
        report.setManagerNotes(rs.getString("manager_notes"));
        report.setReviewedBy(rs.getString("reviewed_by"));
        report.setReviewedDate(rs.getTimestamp("reviewed_date"));
        return report;
    }

    private Attendance mapResultSetToAttendance(ResultSet rs) throws SQLException {
        Attendance attendance = new Attendance();
        attendance.setId(rs.getInt("id"));
        attendance.setEmployeeId(rs.getString("employee_id"));
        attendance.setTanggal(rs.getDate("tanggal"));

        Time jamMasuk = rs.getTime("jam_masuk");
        attendance.setJamMasuk(jamMasuk != null ? jamMasuk.toString().substring(0, 5) : null);

        Time jamKeluar = rs.getTime("jam_keluar");
        attendance.setJamKeluar(jamKeluar != null ? jamKeluar.toString().substring(0, 5) : null);

        attendance.setStatus(rs.getString("status"));
        attendance.setKeterangan(rs.getString("keterangan"));
        attendance.setLate(rs.getBoolean("is_late"));
        return attendance;
    }

    private Meeting mapResultSetToMeeting(ResultSet rs) throws SQLException {
        Meeting meeting = new Meeting();
        meeting.setId(rs.getInt("id"));
        meeting.setTitle(rs.getString("title"));
        meeting.setDescription(rs.getString("description"));
        meeting.setTanggal(rs.getDate("tanggal"));

        Time waktuMulai = rs.getTime("waktu_mulai");
        meeting.setWaktuMulai(waktuMulai != null ? waktuMulai.toString().substring(0, 5) : null);

        Time waktuSelesai = rs.getTime("waktu_selesai");
        meeting.setWaktuSelesai(waktuSelesai != null ? waktuSelesai.toString().substring(0, 5) : null);

        meeting.setLokasi(rs.getString("lokasi"));
        meeting.setOrganizerId(rs.getString("organizer_id"));
        meeting.setStatus(rs.getString("status"));
        meeting.setCreatedDate(rs.getTimestamp("created_date"));

        // Load participants
        List<String> participants = getMeetingParticipants(meeting.getId());
        meeting.setParticipantIds(participants);

        return meeting;
    }

    private List<String> getMeetingParticipants(int meetingId) {
        List<String> participants = new ArrayList<>();
        String query = "SELECT participant_id FROM meeting_participants WHERE meeting_id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, meetingId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                participants.add(rs.getString("participant_id"));
            }
        } catch (SQLException e) {
            logger.warning("Error getting meeting participants: " + e.getMessage());
        }
        return participants;
    }

    private LeaveRequest mapResultSetToLeaveRequest(ResultSet rs) throws SQLException {
        LeaveRequest request = new LeaveRequest();
        request.setId(rs.getInt("id"));
        request.setEmployeeId(rs.getString("employee_id"));
        request.setLeaveType(rs.getString("leave_type"));
        request.setStartDate(rs.getDate("start_date"));
        request.setEndDate(rs.getDate("end_date"));
        request.setTotalDays(rs.getInt("total_days"));
        request.setReason(rs.getString("reason"));
        request.setStatus(rs.getString("status"));
        request.setApproverId(rs.getString("approver_id"));
        request.setApproverNotes(rs.getString("approver_notes"));
        request.setRequestDate(rs.getTimestamp("request_date"));
        request.setApprovalDate(rs.getTimestamp("approval_date"));
        return request;
    }

    private SalaryHistory mapResultSetToSalaryHistory(ResultSet rs) throws SQLException {
        SalaryHistory salary = new SalaryHistory();
        salary.setId(rs.getInt("id"));
        salary.setEmployeeId(rs.getString("employee_id"));
        salary.setBulan(rs.getInt("bulan"));
        salary.setTahun(rs.getInt("tahun"));
        salary.setBaseSalary(rs.getDouble("base_salary"));
        salary.setKpiBonus(rs.getDouble("kpi_bonus"));
        salary.setSupervisorBonus(rs.getDouble("supervisor_bonus"));
        salary.setPenalty(rs.getDouble("penalty"));
        salary.setTotalSalary(rs.getDouble("total_salary"));
        salary.setKpiScore(rs.getDouble("kpi_score"));
        salary.setSupervisorRating(rs.getDouble("supervisor_rating"));
        salary.setPaymentDate(rs.getTimestamp("payment_date"));
        salary.setNotes(rs.getString("notes"));
        return salary;
    }

    private EmployeeEvaluation mapResultSetToEmployeeEvaluation(ResultSet rs) throws SQLException {
        EmployeeEvaluation evaluation = new EmployeeEvaluation();
        evaluation.setId(rs.getInt("id"));
        evaluation.setEmployeeId(rs.getString("employee_id"));
        evaluation.setSupervisorId(rs.getString("supervisor_id"));
        evaluation.setPunctualityScore(rs.getDouble("punctuality_score"));
        evaluation.setAttendanceScore(rs.getDouble("attendance_score"));
        evaluation.setOverallRating(rs.getDouble("overall_rating"));
        evaluation.setComments(rs.getString("comments"));
        evaluation.setEvaluationDate(rs.getTimestamp("evaluation_date"));
        return evaluation;
    }

    private MonthlyEvaluation mapResultSetToMonthlyEvaluation(ResultSet rs) throws SQLException {
        MonthlyEvaluation evaluation = new MonthlyEvaluation();
        evaluation.setId(rs.getInt("id"));
        evaluation.setEmployeeId(rs.getString("employee_id"));
        evaluation.setSupervisorId(rs.getString("supervisor_id"));
        evaluation.setMonth(rs.getInt("month"));
        evaluation.setYear(rs.getInt("year"));
        evaluation.setPunctualityScore(rs.getDouble("punctuality_score"));
        evaluation.setAttendanceScore(rs.getDouble("attendance_score"));
        evaluation.setProductivityScore(rs.getDouble("productivity_score"));
        evaluation.setOverallRating(rs.getDouble("overall_rating"));
        evaluation.setComments(rs.getString("comments"));
        evaluation.setEvaluationDate(rs.getTimestamp("evaluation_date"));
        return evaluation;
    }
}