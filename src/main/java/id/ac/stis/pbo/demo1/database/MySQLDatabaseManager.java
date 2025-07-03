package id.ac.stis.pbo.demo1.database;

import id.ac.stis.pbo.demo1.models.Employee;
import id.ac.stis.pbo.demo1.models.KPI;
import id.ac.stis.pbo.demo1.models.Report;
import id.ac.stis.pbo.demo1.models.EmployeeEvaluation;
import id.ac.stis.pbo.demo1.models.Attendance;
import id.ac.stis.pbo.demo1.models.Meeting;
import id.ac.stis.pbo.demo1.models.LeaveRequest;
import id.ac.stis.pbo.demo1.models.SalaryHistory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.Date;

/**
 * MySQL Database manager for GAWE application with connection pooling
 */
public class MySQLDatabaseManager {
    private static final Logger logger = Logger.getLogger(MySQLDatabaseManager.class.getName());
    
    // Database configuration
    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "gawe_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = ""; // Default MySQL root password
    
    public HikariDataSource dataSource;

    public MySQLDatabaseManager() {
        initializeConnectionPool();
    }

    private void initializeConnectionPool() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + 
                            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
            config.setUsername(DB_USER);
            config.setPassword(DB_PASSWORD);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            
            // Connection pool settings
            config.setMaximumPoolSize(20);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            
            dataSource = new HikariDataSource(config);
            logger.info("MySQL connection pool initialized successfully");
        } catch (Exception e) {
            logger.severe("Failed to initialize MySQL connection pool: " + e.getMessage());
            throw new RuntimeException("Database connection failed", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void initializeDatabase() {
        System.out.println("Starting database initialization...");
        try {
            createDatabase();
            System.out.println("Database created successfully");
            
            createTables();
            System.out.println("Tables created successfully");
            
            // Check if data already exists
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM employees")) {
                
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("Data already exists in database, skipping sample data insertion");
                } else {
                    System.out.println("No existing data found, inserting sample data...");
                    insertSampleData();
                    System.out.println("Sample data inserted successfully");
                }
            }
        } catch (Exception e) {
            System.err.println("Error during database initialization: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Database initialization failed", e);
        }
        System.out.println("Database initialization completed successfully");
    }

    private void createDatabase() {
        // Create database if it doesn't exist
        String createDbUrl = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + 
                           "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        
        try (Connection conn = DriverManager.getConnection(createDbUrl, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME + 
                             " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            logger.info("Database " + DB_NAME + " created or already exists");
            
        } catch (SQLException e) {
            logger.severe("Failed to create database: " + e.getMessage());
        }
    }

    private void createTables() {
        String[] createTableQueries = {
            // Employees table
            """
            CREATE TABLE IF NOT EXISTS employees (
                id VARCHAR(20) PRIMARY KEY,
                nama VARCHAR(100) NOT NULL,
                password VARCHAR(100) NOT NULL,
                role ENUM('manajer', 'supervisor', 'pegawai') NOT NULL,
                divisi VARCHAR(50) NOT NULL,
                jabatan VARCHAR(100) NOT NULL,
                tgl_masuk DATE NOT NULL,
                sisa_cuti INT DEFAULT 12,
                gaji_pokok DECIMAL(15,2) NOT NULL,
                kpi_score DECIMAL(5,2) DEFAULT 0.0,
                supervisor_rating DECIMAL(5,2) DEFAULT 0.0,
                layoff_risk BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """,
            
            // KPI table
            """
            CREATE TABLE IF NOT EXISTS kpi (
                id INT AUTO_INCREMENT PRIMARY KEY,
                divisi VARCHAR(50) NOT NULL,
                bulan INT NOT NULL,
                tahun INT NOT NULL,
                score DECIMAL(5,2) NOT NULL,
                manager_id VARCHAR(20) NOT NULL,
                notes TEXT,
                created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (manager_id) REFERENCES employees(id) ON DELETE CASCADE,
                UNIQUE KEY unique_kpi (divisi, bulan, tahun)
            )
            """,
            
            // Reports table
            """
            CREATE TABLE IF NOT EXISTS reports (
                id INT AUTO_INCREMENT PRIMARY KEY,
                supervisor_id VARCHAR(20) NOT NULL,
                divisi VARCHAR(50) NOT NULL,
                bulan INT NOT NULL,
                tahun INT NOT NULL,
                file_path TEXT NOT NULL,
                upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                status ENUM('pending', 'reviewed', 'approved', 'rejected') DEFAULT 'pending',
                manager_notes TEXT,
                reviewed_by VARCHAR(20),
                reviewed_date TIMESTAMP NULL,
                FOREIGN KEY (supervisor_id) REFERENCES employees(id) ON DELETE CASCADE,
                FOREIGN KEY (reviewed_by) REFERENCES employees(id) ON DELETE SET NULL
            )
            """,
            
            // Employee evaluations table
            """
            CREATE TABLE IF NOT EXISTS employee_evaluations (
                id INT AUTO_INCREMENT PRIMARY KEY,
                employee_id VARCHAR(20) NOT NULL,
                supervisor_id VARCHAR(20) NOT NULL,
                punctuality_score DECIMAL(5,2) NOT NULL,
                attendance_score DECIMAL(5,2) NOT NULL,
                overall_rating DECIMAL(5,2) NOT NULL,
                evaluation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                comments TEXT,
                FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
                FOREIGN KEY (supervisor_id) REFERENCES employees(id) ON DELETE CASCADE
            )
            """,
            
            // Monthly evaluations table
            """
            CREATE TABLE IF NOT EXISTS monthly_evaluations (
                id INT AUTO_INCREMENT PRIMARY KEY,
                employee_id VARCHAR(20) NOT NULL,
                supervisor_id VARCHAR(20) NOT NULL,
                month INT NOT NULL,
                year INT NOT NULL,
                punctuality_score DECIMAL(5,2) NOT NULL,
                attendance_score DECIMAL(5,2) NOT NULL,
                productivity_score DECIMAL(5,2) NOT NULL,
                overall_rating DECIMAL(5,2) NOT NULL,
                comments TEXT,
                evaluation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
                FOREIGN KEY (supervisor_id) REFERENCES employees(id) ON DELETE CASCADE,
                UNIQUE KEY unique_monthly_eval (employee_id, month, year)
            )
            """,
            
            // Attendance table
            """
            CREATE TABLE IF NOT EXISTS attendance (
                id INT AUTO_INCREMENT PRIMARY KEY,
                employee_id VARCHAR(20) NOT NULL,
                tanggal DATE NOT NULL,
                jam_masuk TIME,
                jam_keluar TIME,
                status ENUM('hadir', 'sakit', 'izin', 'alpha') NOT NULL,
                keterangan TEXT,
                is_late BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
                UNIQUE KEY unique_attendance (employee_id, tanggal)
            )
            """,
            
            // Meetings table
            """
            CREATE TABLE IF NOT EXISTS meetings (
                id INT AUTO_INCREMENT PRIMARY KEY,
                title VARCHAR(200) NOT NULL,
                description TEXT,
                tanggal DATE NOT NULL,
                waktu_mulai TIME NOT NULL,
                waktu_selesai TIME NOT NULL,
                lokasi VARCHAR(200) NOT NULL,
                organizer_id VARCHAR(20) NOT NULL,
                status ENUM('scheduled', 'ongoing', 'completed', 'cancelled') DEFAULT 'scheduled',
                created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (organizer_id) REFERENCES employees(id) ON DELETE CASCADE
            )
            """,
            
            // Meeting participants table
            """
            CREATE TABLE IF NOT EXISTS meeting_participants (
                id INT AUTO_INCREMENT PRIMARY KEY,
                meeting_id INT NOT NULL,
                participant_id VARCHAR(20) NOT NULL,
                FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE,
                FOREIGN KEY (participant_id) REFERENCES employees(id) ON DELETE CASCADE,
                UNIQUE KEY unique_participant (meeting_id, participant_id)
            )
            """,
            
            // Leave requests table
            """
            CREATE TABLE IF NOT EXISTS leave_requests (
                id INT AUTO_INCREMENT PRIMARY KEY,
                employee_id VARCHAR(20) NOT NULL,
                leave_type VARCHAR(50) NOT NULL,
                start_date DATE NOT NULL,
                end_date DATE NOT NULL,
                total_days INT NOT NULL,
                reason TEXT NOT NULL,
                status ENUM('pending', 'approved', 'rejected') DEFAULT 'pending',
                approver_id VARCHAR(20),
                approver_notes TEXT,
                request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                approval_date TIMESTAMP NULL,
                FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
                FOREIGN KEY (approver_id) REFERENCES employees(id) ON DELETE SET NULL
            )
            """,
            
            // Salary history table
            """
            CREATE TABLE IF NOT EXISTS salary_history (
                id INT AUTO_INCREMENT PRIMARY KEY,
                employee_id VARCHAR(20) NOT NULL,
                bulan INT NOT NULL,
                tahun INT NOT NULL,
                base_salary DECIMAL(15,2) NOT NULL,
                kpi_bonus DECIMAL(15,2) DEFAULT 0.0,
                supervisor_bonus DECIMAL(15,2) DEFAULT 0.0,
                penalty DECIMAL(15,2) DEFAULT 0.0,
                total_salary DECIMAL(15,2) NOT NULL,
                kpi_score DECIMAL(5,2) NOT NULL,
                supervisor_rating DECIMAL(5,2) NOT NULL,
                payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                notes TEXT,
                FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
                UNIQUE KEY unique_salary (employee_id, bulan, tahun)
            )
            """
        };

        try (Connection conn = dataSource.getConnection()) {
            for (String query : createTableQueries) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(query);
                }
            }
            logger.info("All database tables created successfully");
        } catch (SQLException e) {
            logger.severe("Error creating tables: " + e.getMessage());
            throw new RuntimeException("Failed to create database tables", e);
        }
    }

    private void insertSampleData() {
        // Check if data already exists
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM employees")) {
            
            if (rs.next() && rs.getInt(1) > 0) {
                logger.info("Sample data already exists, skipping insertion");
                return;
            }
        } catch (SQLException e) {
            logger.warning("Error checking existing data: " + e.getMessage());
        }

        insertSampleEmployees();
        insertSampleKPI();
        insertSampleReports();
        insertSampleEvaluations();
        insertSampleMonthlyEvaluations();
        insertSampleAttendance();
        insertSampleMeetings();
        insertSampleLeaveRequests();
        insertSampleSalaryHistory();
        
        logger.info("Sample data inserted successfully");
    }

    private void insertSampleEmployees() {
        String insertQuery = """
            INSERT INTO employees (id, nama, password, role, divisi, jabatan, tgl_masuk, sisa_cuti, gaji_pokok, kpi_score, supervisor_rating, layoff_risk) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        Object[][] sampleEmployees = {
            {"MNG001", "John Manager", "password123", "manajer", "HR", "General Manager", "2020-01-15", 12, 10000000.0, 85.0, 90.0, false},
            {"SUP001", "Alice Supervisor", "password123", "supervisor", "HR", "HR Supervisor", "2021-03-10", 12, 7200000.0, 80.0, 85.0, false},
            {"SUP002", "Bob Supervisor", "password123", "supervisor", "Marketing", "Marketing Supervisor", "2021-04-15", 12, 7200000.0, 75.0, 80.0, false},
            {"SUP003", "Carol Supervisor", "password123", "supervisor", "Sales", "Sales Supervisor", "2021-05-20", 12, 7200000.0, 88.0, 92.0, false},
            {"SUP004", "David Supervisor", "password123", "supervisor", "IT", "IT Supervisor", "2021-06-25", 12, 7200000.0, 82.0, 87.0, false},
            {"SUP005", "Eva Supervisor", "password123", "supervisor", "Finance", "Finance Supervisor", "2021-07-30", 12, 7200000.0, 78.0, 83.0, false},
            {"EMP001", "Mike Employee", "password123", "pegawai", "Marketing", "Marketing Specialist", "2022-01-10", 12, 4800000.0, 70.0, 75.0, false},
            {"EMP002", "Sarah Employee", "password123", "pegawai", "Sales", "Sales Representative", "2022-02-15", 12, 4800000.0, 55.0, 60.0, true},
            {"EMP003", "Tom Employee", "password123", "pegawai", "IT", "Software Developer", "2022-03-20", 12, 4800000.0, 90.0, 95.0, false},
            {"EMP004", "Lisa Employee", "password123", "pegawai", "Finance", "Financial Analyst", "2022-04-25", 12, 4800000.0, 65.0, 70.0, false},
            {"EMP005", "James Employee", "password123", "pegawai", "HR", "HR Assistant", "2022-05-30", 12, 4800000.0, 72.0, 78.0, false},
            {"EMP006", "Anna Employee", "password123", "pegawai", "Marketing", "Marketing Assistant", "2022-06-15", 12, 4800000.0, 68.0, 73.0, false},
            {"EMP007", "Peter Employee", "password123", "pegawai", "Sales", "Sales Assistant", "2022-07-20", 12, 4800000.0, 77.0, 82.0, false},
            {"EMP008", "Maria Employee", "password123", "pegawai", "IT", "System Analyst", "2022-08-25", 12, 4800000.0, 85.0, 88.0, false},
            {"EMP009", "Robert Employee", "password123", "pegawai", "Finance", "Accountant", "2022-09-30", 12, 4800000.0, 71.0, 76.0, false},
            {"EMP010", "Linda Employee", "password123", "pegawai", "HR", "Recruiter", "2022-10-15", 12, 4800000.0, 74.0, 79.0, false}
        };

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
            
            for (Object[] employee : sampleEmployees) {
                pstmt.setString(1, (String) employee[0]);
                pstmt.setString(2, (String) employee[1]);
                pstmt.setString(3, (String) employee[2]);
                pstmt.setString(4, (String) employee[3]);
                pstmt.setString(5, (String) employee[4]);
                pstmt.setString(6, (String) employee[5]);
                pstmt.setDate(7, java.sql.Date.valueOf((String) employee[6]));
                pstmt.setInt(8, (Integer) employee[7]);
                pstmt.setDouble(9, (Double) employee[8]);
                pstmt.setDouble(10, (Double) employee[9]);
                pstmt.setDouble(11, (Double) employee[10]);
                pstmt.setBoolean(12, (Boolean) employee[11]);
                pstmt.executeUpdate();
            }
            logger.info("Sample employees inserted successfully");
        } catch (SQLException e) {
            logger.severe("Error inserting sample employees: " + e.getMessage());
        }
    }

    private void insertSampleKPI() {
        String insertQuery = "INSERT INTO kpi (divisi, bulan, tahun, score, manager_id, notes) VALUES (?, ?, ?, ?, ?, ?)";
        Object[][] sampleKpi = {
            {"HR", 10, 2024, 78.5, "MNG001", "Good performance in HR operations"},
            {"Marketing", 10, 2024, 82.3, "MNG001", "Strong marketing campaigns"},
            {"Sales", 10, 2024, 91.2, "MNG001", "Excellent sales results"},
            {"IT", 10, 2024, 87.8, "MNG001", "Solid technical delivery"},
            {"Finance", 10, 2024, 75.6, "MNG001", "Steady financial management"},
            {"HR", 11, 2024, 80.2, "MNG001", "Improved HR processes"},
            {"Marketing", 11, 2024, 85.1, "MNG001", "Creative marketing initiatives"},
            {"Sales", 11, 2024, 93.5, "MNG001", "Outstanding sales performance"},
            {"IT", 11, 2024, 89.3, "MNG001", "Excellent system improvements"},
            {"Finance", 11, 2024, 77.8, "MNG001", "Better financial controls"},
            {"HR", 12, 2024, 82.7, "MNG001", "Strong year-end performance"},
            {"Marketing", 12, 2024, 87.9, "MNG001", "Successful holiday campaigns"},
            {"Sales", 12, 2024, 95.2, "MNG001", "Record-breaking sales"},
            {"IT", 12, 2024, 91.1, "MNG001", "Exceptional technical support"},
            {"Finance", 12, 2024, 79.4, "MNG001", "Solid year-end closing"}
        };

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
            
            for (Object[] kpi : sampleKpi) {
                pstmt.setString(1, (String) kpi[0]);
                pstmt.setInt(2, (Integer) kpi[1]);
                pstmt.setInt(3, (Integer) kpi[2]);
                pstmt.setDouble(4, (Double) kpi[3]);
                pstmt.setString(5, (String) kpi[4]);
                pstmt.setString(6, (String) kpi[5]);
                pstmt.executeUpdate();
            }
            logger.info("Sample KPI data inserted successfully");
        } catch (SQLException e) {
            logger.severe("Error inserting sample KPI data: " + e.getMessage());
        }
    }

    private void insertSampleReports() {
        String insertQuery = """
            INSERT INTO reports (supervisor_id, divisi, bulan, tahun, file_path, status, manager_notes, reviewed_by, reviewed_date) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        Object[][] sampleReports = {
            {"SUP001", "HR", 10, 2024, "reports/HR_10_2024.pdf", "reviewed", "Good comprehensive report", "MNG001", "2024-11-05 10:30:00"},
            {"SUP002", "Marketing", 10, 2024, "reports/Marketing_10_2024.pdf", "approved", "Excellent marketing analysis", "MNG001", "2024-11-05 11:15:00"},
            {"SUP003", "Sales", 10, 2024, "reports/Sales_10_2024.pdf", "approved", "Outstanding sales performance", "MNG001", "2024-11-05 14:20:00"},
            {"SUP004", "IT", 10, 2024, "reports/IT_10_2024.pdf", "reviewed", "Technical report needs minor updates", "MNG001", "2024-11-05 15:45:00"},
            {"SUP005", "Finance", 10, 2024, "reports/Finance_10_2024.pdf", "approved", "Solid financial reporting", "MNG001", "2024-11-05 16:30:00"},
            {"SUP001", "HR", 11, 2024, "reports/HR_11_2024.pdf", "pending", null, null, null},
            {"SUP002", "Marketing", 11, 2024, "reports/Marketing_11_2024.pdf", "pending", null, null, null},
            {"SUP003", "Sales", 11, 2024, "reports/Sales_11_2024.pdf", "reviewed", "Great improvement in reporting", "MNG001", "2024-12-05 09:15:00"},
            {"SUP004", "IT", 11, 2024, "reports/IT_11_2024.pdf", "pending", null, null, null},
            {"SUP005", "Finance", 11, 2024, "reports/Finance_11_2024.pdf", "approved", "Excellent financial analysis", "MNG001", "2024-12-05 13:20:00"}
        };

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
            
            for (Object[] report : sampleReports) {
                pstmt.setString(1, (String) report[0]);
                pstmt.setString(2, (String) report[1]);
                pstmt.setInt(3, (Integer) report[2]);
                pstmt.setInt(4, (Integer) report[3]);
                pstmt.setString(5, (String) report[4]);
                pstmt.setString(6, (String) report[5]);
                pstmt.setString(7, (String) report[6]);
                pstmt.setString(8, (String) report[7]);
                if (report[8] != null) {
                    pstmt.setTimestamp(9, Timestamp.valueOf((String) report[8]));
                } else {
                    pstmt.setNull(9, Types.TIMESTAMP);
                }
                pstmt.executeUpdate();
            }
            logger.info("Sample reports inserted successfully");
        } catch (SQLException e) {
            logger.severe("Error inserting sample reports: " + e.getMessage());
        }
    }

    private void insertSampleEvaluations() {
        String insertQuery = """
            INSERT INTO employee_evaluations (employee_id, supervisor_id, punctuality_score, attendance_score, overall_rating, comments) 
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        
        String[] employees = {"EMP001", "EMP002", "EMP003", "EMP004", "EMP005", "EMP006", "EMP007", "EMP008", "EMP009", "EMP010"};
        String[] supervisors = {"SUP002", "SUP003", "SUP004", "SUP005", "SUP001", "SUP002", "SUP003", "SUP004", "SUP005", "SUP001"};

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
            
            for (int i = 0; i < employees.length; i++) {
                double punctuality = 70 + Math.random() * 25;
                double attendance = 75 + Math.random() * 20;
                double overall = (punctuality + attendance) / 2;
                
                pstmt.setString(1, employees[i]);
                pstmt.setString(2, supervisors[i]);
                pstmt.setDouble(3, punctuality);
                pstmt.setDouble(4, attendance);
                pstmt.setDouble(5, overall);
                pstmt.setString(6, "Regular evaluation for " + employees[i]);
                pstmt.executeUpdate();
            }
            logger.info("Sample evaluations inserted successfully");
        } catch (SQLException e) {
            logger.severe("Error inserting sample evaluations: " + e.getMessage());
        }
    }

    private void insertSampleMonthlyEvaluations() {
        String insertQuery = """
            INSERT INTO monthly_evaluations (employee_id, supervisor_id, month, year, punctuality_score, attendance_score, productivity_score, overall_rating, comments) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        String[] employees = {"EMP001", "EMP002", "EMP003", "EMP004", "EMP005", "EMP006", "EMP007", "EMP008", "EMP009", "EMP010"};
        String[] supervisors = {"SUP002", "SUP003", "SUP004", "SUP005", "SUP001", "SUP002", "SUP003", "SUP004", "SUP005", "SUP001"};

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
            
            for (String employee : employees) {
                for (int month = 10; month <= 12; month++) {
                    int supervisorIndex = Arrays.asList(employees).indexOf(employee);
                    double punctuality = 70 + Math.random() * 25;
                    double attendance = 75 + Math.random() * 20;
                    double productivity = 70 + Math.random() * 25;
                    double overall = (punctuality + attendance + productivity) / 3;
                    
                    pstmt.setString(1, employee);
                    pstmt.setString(2, supervisors[supervisorIndex]);
                    pstmt.setInt(3, month);
                    pstmt.setInt(4, 2024);
                    pstmt.setDouble(5, punctuality);
                    pstmt.setDouble(6, attendance);
                    pstmt.setDouble(7, productivity);
                    pstmt.setDouble(8, overall);
                    pstmt.setString(9, "Monthly evaluation for " + employee + " - Month " + month);
                    pstmt.executeUpdate();
                }
            }
            logger.info("Sample monthly evaluations inserted successfully");
        } catch (SQLException e) {
            logger.severe("Error inserting sample monthly evaluations: " + e.getMessage());
        }
    }

    private void insertSampleAttendance() {
        String insertQuery = """
            INSERT INTO attendance (employee_id, tanggal, jam_masuk, jam_keluar, status, is_late) 
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
            
            List<Employee> employees = getAllEmployees();
            
            // Create attendance for last 30 days
            for (int day = 1; day <= 30; day++) {
                java.sql.Date attendanceDate = java.sql.Date.valueOf("2024-12-" + String.format("%02d", day));
                
                for (Employee emp : employees) {
                    if (Math.random() > 0.1) { // 90% attendance rate
                        String jamMasuk = "08:" + String.format("%02d", (int)(Math.random() * 60));
                        String jamKeluar = "17:" + String.format("%02d", (int)(Math.random() * 60));
                        boolean isLate = Math.random() > 0.8; // 20% chance of being late
                        
                        pstmt.setString(1, emp.getId());
                        pstmt.setDate(2, attendanceDate);
                        pstmt.setTime(3, Time.valueOf(jamMasuk + ":00"));
                        pstmt.setTime(4, Time.valueOf(jamKeluar + ":00"));
                        pstmt.setString(5, "hadir");
                        pstmt.setBoolean(6, isLate);
                        pstmt.executeUpdate();
                    }
                }
            }
            logger.info("Sample attendance inserted successfully");
        } catch (SQLException e) {
            logger.severe("Error inserting sample attendance: " + e.getMessage());
        }
    }

    private void insertSampleMeetings() {
        System.out.println("Starting to insert sample meetings...");
        
        String insertMeetingQuery = """
            INSERT INTO meetings (title, description, tanggal, waktu_mulai, waktu_selesai, lokasi, organizer_id, status) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        String insertParticipantQuery = """
            INSERT INTO meeting_participants (meeting_id, participant_id) VALUES (?, ?)
        """;

        try (Connection conn = dataSource.getConnection()) {
            // First check if meetings already exist
            try (Statement checkStmt = conn.createStatement();
                 ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) FROM meetings")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("Meetings already exist in database, skipping insertion");
                    return;
                }
            }

            System.out.println("No existing meetings found, proceeding with insertion");
            conn.setAutoCommit(false);
            
            try (PreparedStatement meetingStmt = conn.prepareStatement(insertMeetingQuery, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement participantStmt = conn.prepareStatement(insertParticipantQuery)) {
                
                String[] divisions = {"HR", "Marketing", "Sales", "IT", "Finance"};
                String[] supervisorIds = {"SUP001", "SUP002", "SUP003", "SUP004", "SUP005"};
                
                // Weekly team meetings
                for (int i = 0; i < divisions.length; i++) {
                    for (int week = 1; week <= 4; week++) {
                        java.sql.Date meetingDate = java.sql.Date.valueOf("2024-12-" + String.format("%02d", week * 7));
                        
                        meetingStmt.setString(1, divisions[i] + " Weekly Team Meeting");
                        meetingStmt.setString(2, "Weekly team sync and updates");
                        meetingStmt.setDate(3, meetingDate);
                        meetingStmt.setTime(4, Time.valueOf("09:00:00"));
                        meetingStmt.setTime(5, Time.valueOf("10:00:00"));
                        meetingStmt.setString(6, "Meeting Room " + (i + 1));
                        meetingStmt.setString(7, supervisorIds[i]);
                        meetingStmt.setString(8, "scheduled");
                        meetingStmt.executeUpdate();
                        
                        ResultSet rs = meetingStmt.getGeneratedKeys();
                        if (rs.next()) {
                            int meetingId = rs.getInt(1);
                            
                            // Add team members as participants
                            List<Employee> teamMembers = getEmployeesByDivision(divisions[i]);
                            for (Employee member : teamMembers) {
                                if (member.getRole().equals("pegawai")) {
                                    participantStmt.setInt(1, meetingId);
                                    participantStmt.setString(2, member.getId());
                                    participantStmt.executeUpdate();
                                }
                            }
                        }
                    }
                }
                
                // Monthly all-hands meeting
                meetingStmt.setString(1, "Monthly All-Hands Meeting");
                meetingStmt.setString(2, "Company updates and announcements");
                meetingStmt.setDate(3, java.sql.Date.valueOf("2024-12-15"));
                meetingStmt.setTime(4, Time.valueOf("14:00:00"));
                meetingStmt.setTime(5, Time.valueOf("15:30:00"));
                meetingStmt.setString(6, "Main Conference Room");
                meetingStmt.setString(7, "MNG001");
                meetingStmt.setString(8, "scheduled");
                meetingStmt.executeUpdate();
                
                ResultSet rs = meetingStmt.getGeneratedKeys();
                if (rs.next()) {
                    int meetingId = rs.getInt(1);
                    
                    // Add all employees as participants
                    List<Employee> allEmployees = getAllEmployees();
                    for (Employee emp : allEmployees) {
                        participantStmt.setInt(1, meetingId);
                        participantStmt.setString(2, emp.getId());
                        participantStmt.executeUpdate();
                    }
                }
                
                conn.commit();
                logger.info("Sample meetings inserted successfully");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.severe("Error inserting sample meetings: " + e.getMessage());
        }
    }

    private void insertSampleLeaveRequests() {
        String insertQuery = """
            INSERT INTO leave_requests (employee_id, leave_type, start_date, end_date, total_days, reason, status, approver_id, approver_notes, approval_date) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
            
            List<Employee> allEmployees = getAllEmployees();
            String[] leaveTypes = {"Annual Leave", "Sick Leave", "Personal Leave", "Emergency Leave"};
            
            for (int i = 0; i < 15; i++) {
                Employee emp = allEmployees.get(i % allEmployees.size());
                java.sql.Date startDate = java.sql.Date.valueOf("2024-12-" + String.format("%02d", 20 + (i % 10)));
                java.sql.Date endDate = java.sql.Date.valueOf("2024-12-" + String.format("%02d", 22 + (i % 10) + (i % 3)));
                
                pstmt.setString(1, emp.getId());
                pstmt.setString(2, leaveTypes[i % 4]);
                pstmt.setDate(3, startDate);
                pstmt.setDate(4, endDate);
                pstmt.setInt(5, 3 + (i % 3));
                pstmt.setString(6, "Sample leave request " + (i + 1));
                
                // Set status and approver based on role
                if (i < 5) {
                    pstmt.setString(7, "pending");
                    pstmt.setNull(8, Types.VARCHAR);
                    pstmt.setNull(9, Types.VARCHAR);
                    pstmt.setNull(10, Types.TIMESTAMP);
                } else if (i < 10) {
                    pstmt.setString(7, "approved");
                    String approverId = emp.getRole().equals("pegawai") ? getSupervisorByDivision(emp.getDivisi()) : "MNG001";
                    pstmt.setString(8, approverId);
                    pstmt.setString(9, "Approved - good standing employee");
                    pstmt.setTimestamp(10, new Timestamp(System.currentTimeMillis()));
                } else {
                    pstmt.setString(7, "rejected");
                    String approverId = emp.getRole().equals("pegawai") ? getSupervisorByDivision(emp.getDivisi()) : "MNG001";
                    pstmt.setString(8, approverId);
                    pstmt.setString(9, "Rejected due to workload constraints");
                    pstmt.setTimestamp(10, new Timestamp(System.currentTimeMillis()));
                }
                
                pstmt.executeUpdate();
            }
            logger.info("Sample leave requests inserted successfully");
        } catch (SQLException e) {
            logger.severe("Error inserting sample leave requests: " + e.getMessage());
        }
    }

    private void insertSampleSalaryHistory() {
        String insertQuery = """
            INSERT INTO salary_history (employee_id, bulan, tahun, base_salary, kpi_bonus, supervisor_bonus, penalty, total_salary, kpi_score, supervisor_rating, notes) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
            
            List<Employee> allEmployees = getAllEmployees();
            
            for (Employee emp : allEmployees) {
                for (int month = 10; month <= 12; month++) {
                    double baseSalary = emp.getGajiPokok();
                    double kpiBonus = 0;
                    double supervisorBonus = 0;
                    double penalty = 0;
                    
                    // Calculate bonuses based on scores
                    if (emp.getKpiScore() >= 90) kpiBonus = baseSalary * 0.20;
                    else if (emp.getKpiScore() >= 80) kpiBonus = baseSalary * 0.15;
                    else if (emp.getKpiScore() >= 70) kpiBonus = baseSalary * 0.10;
                    else if (emp.getKpiScore() >= 60) kpiBonus = baseSalary * 0.05;
                    
                    if (emp.getSupervisorRating() >= 90) supervisorBonus = baseSalary * 0.15;
                    else if (emp.getSupervisorRating() >= 80) supervisorBonus = baseSalary * 0.10;
                    else if (emp.getSupervisorRating() >= 70) supervisorBonus = baseSalary * 0.05;
                    
                    if (emp.getKpiScore() < 60 || emp.getSupervisorRating() < 60) {
                        penalty = baseSalary * 0.10;
                    }
                    
                    double totalSalary = baseSalary + kpiBonus + supervisorBonus - penalty;
                    
                    pstmt.setString(1, emp.getId());
                    pstmt.setInt(2, month);
                    pstmt.setInt(3, 2024);
                    pstmt.setDouble(4, baseSalary);
                    pstmt.setDouble(5, kpiBonus);
                    pstmt.setDouble(6, supervisorBonus);
                    pstmt.setDouble(7, penalty);
                    pstmt.setDouble(8, totalSalary);
                    pstmt.setDouble(9, emp.getKpiScore());
                    pstmt.setDouble(10, emp.getSupervisorRating());
                    pstmt.setString(11, "Monthly salary for " + emp.getNama());
                    pstmt.executeUpdate();
                }
            }
            logger.info("Sample salary history inserted successfully");
        } catch (SQLException e) {
            logger.severe("Error inserting sample salary history: " + e.getMessage());
        }
    }

    // Database operation methods
    public Employee authenticateUser(String employeeId, String password) {
        String query = "SELECT * FROM employees WHERE id = ? AND password = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, employeeId);
            pstmt.setString(2, password);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToEmployee(rs);
            }
        } catch (SQLException e) {
            logger.severe("Error authenticating user: " + e.getMessage());
        }
        
        return null;
    }

    public List<Employee> getAllEmployees() {
        List<Employee> employees = new ArrayList<>();
        String query = "SELECT * FROM employees ORDER BY nama";
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                employees.add(mapResultSetToEmployee(rs));
            }
        } catch (SQLException e) {
            logger.severe("Error getting all employees: " + e.getMessage());
        }
        
        return employees;
    }

    public List<Employee> getEmployeesByRole(String role) {
        List<Employee> employees = new ArrayList<>();
        String query = "SELECT * FROM employees WHERE role = ? ORDER BY nama";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, role);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                employees.add(mapResultSetToEmployee(rs));
            }
        } catch (SQLException e) {
            logger.severe("Error getting employees by role: " + e.getMessage());
        }
        
        return employees;
    }

    public List<Employee> getEmployeesByDivision(String divisi) {
        List<Employee> employees = new ArrayList<>();
        String query = "SELECT * FROM employees WHERE divisi = ? ORDER BY nama";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, divisi);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                employees.add(mapResultSetToEmployee(rs));
            }
        } catch (SQLException e) {
            logger.severe("Error getting employees by division: " + e.getMessage());
        }
        
        return employees;
    }

    public Employee getEmployeeById(String id) {
        String query = "SELECT * FROM employees WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToEmployee(rs);
            }
        } catch (SQLException e) {
            logger.severe("Error getting employee by ID: " + e.getMessage());
        }
        
        return null;
    }

    public String getSupervisorByDivision(String divisi) {
        String query = "SELECT id FROM employees WHERE role = 'supervisor' AND divisi = ? LIMIT 1";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, divisi);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("id");
            }
        } catch (SQLException e) {
            logger.severe("Error getting supervisor by division: " + e.getMessage());
        }
        
        return "SUP001"; // Default fallback
    }

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
        emp.setSupervisorRating(rs.getDouble("supervisor_rating"));
        emp.setLayoffRisk(rs.getBoolean("layoff_risk"));
        return emp;
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }
}