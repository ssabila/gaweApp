package id.ac.stis.pbo.demo1.database;

import id.ac.stis.pbo.demo1.models.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.Date;

/**
 * Database manager for GAWE application
 */
public class DatabaseManager {
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());
    private static final String DB_URL = "jdbc:sqlite:gawe.db";
    private Connection connection;

    public DatabaseManager() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            logger.info("Database connection established");
        } catch (SQLException e) {
            logger.severe("Failed to connect to database: " + e.getMessage());
        }
    }

    public void initializeDatabase() {
        createTables();
        insertSampleData();
    }

    private void createTables() {
        String[] createTableQueries = {
            // Employees table
            """
            CREATE TABLE IF NOT EXISTS employees (
                id TEXT PRIMARY KEY,
                nama TEXT NOT NULL,
                password TEXT NOT NULL,
                role TEXT NOT NULL,
                divisi TEXT NOT NULL,
                jabatan TEXT NOT NULL,
                tgl_masuk DATE NOT NULL,
                sisa_cuti INTEGER DEFAULT 12,
                gaji_pokok REAL NOT NULL,
                kpi_score REAL DEFAULT 0.0,
                supervisor_rating REAL DEFAULT 0.0,
                layoff_risk BOOLEAN DEFAULT FALSE
            )
            """,
            
            // KPI table
            """
            CREATE TABLE IF NOT EXISTS kpi (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                divisi TEXT NOT NULL,
                bulan INTEGER NOT NULL,
                tahun INTEGER NOT NULL,
                score REAL NOT NULL,
                manager_id TEXT NOT NULL,
                created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (manager_id) REFERENCES employees(id)
            )
            """,
            
            // Reports table
            """
            CREATE TABLE IF NOT EXISTS reports (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                supervisor_id TEXT NOT NULL,
                divisi TEXT NOT NULL,
                bulan INTEGER NOT NULL,
                tahun INTEGER NOT NULL,
                file_path TEXT NOT NULL,
                upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                status TEXT DEFAULT 'pending',
                manager_notes TEXT,
                FOREIGN KEY (supervisor_id) REFERENCES employees(id)
            )
            """,
            
            // Employee evaluations table
            """
            CREATE TABLE IF NOT EXISTS employee_evaluations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                employee_id TEXT NOT NULL,
                supervisor_id TEXT NOT NULL,
                punctuality_score REAL NOT NULL,
                attendance_score REAL NOT NULL,
                overall_rating REAL NOT NULL,
                evaluation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                comments TEXT,
                FOREIGN KEY (employee_id) REFERENCES employees(id),
                FOREIGN KEY (supervisor_id) REFERENCES employees(id)
            )
            """,
            
            // Salary adjustments table
            """
            CREATE TABLE IF NOT EXISTS salary_adjustments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                employee_id TEXT NOT NULL,
                adjustment_type TEXT NOT NULL,
                amount REAL NOT NULL,
                reason TEXT NOT NULL,
                effective_date DATE NOT NULL,
                created_by TEXT NOT NULL,
                FOREIGN KEY (employee_id) REFERENCES employees(id),
                FOREIGN KEY (created_by) REFERENCES employees(id)
            )
            """,
            
            // Attendance table
            """
            CREATE TABLE IF NOT EXISTS attendance (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                employee_id TEXT NOT NULL,
                tanggal DATE NOT NULL,
                jam_masuk TIME,
                jam_keluar TIME,
                status TEXT NOT NULL,
                keterangan TEXT,
                is_late BOOLEAN DEFAULT FALSE,
                FOREIGN KEY (employee_id) REFERENCES employees(id)
            )
            """
        };

        for (String query : createTableQueries) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(query);
            } catch (SQLException e) {
                logger.severe("Error creating table: " + e.getMessage());
            }
        }
        logger.info("Database tables created successfully");
    }

    private void insertSampleData() {
        // Check if data already exists
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM employees")) {
            if (rs.next() && rs.getInt(1) > 0) {
                logger.info("Sample data already exists, skipping insertion");
                return;
            }
        } catch (SQLException e) {
            logger.warning("Error checking existing data: " + e.getMessage());
        }

        // Insert sample employees
        String insertEmployeeQuery = """
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
            {"EMP005", "James Employee", "password123", "pegawai", "HR", "HR Assistant", "2022-05-30", 12, 4800000.0, 72.0, 78.0, false}
        };

        try (PreparedStatement pstmt = connection.prepareStatement(insertEmployeeQuery)) {
            for (Object[] employee : sampleEmployees) {
                pstmt.setString(1, (String) employee[0]);
                pstmt.setString(2, (String) employee[1]);
                pstmt.setString(3, (String) employee[2]);
                pstmt.setString(4, (String) employee[3]);
                pstmt.setString(5, (String) employee[4]);
                pstmt.setString(6, (String) employee[5]);
                pstmt.setString(7, (String) employee[6]);
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

        // Insert sample KPI data
        String insertKpiQuery = "INSERT INTO kpi (divisi, bulan, tahun, score, manager_id) VALUES (?, ?, ?, ?, ?)";
        Object[][] sampleKpi = {
            {"HR", 12, 2024, 78.5, "MNG001"},
            {"Marketing", 12, 2024, 82.3, "MNG001"},
            {"Sales", 12, 2024, 91.2, "MNG001"},
            {"IT", 12, 2024, 87.8, "MNG001"},
            {"Finance", 12, 2024, 75.6, "MNG001"}
        };

        try (PreparedStatement pstmt = connection.prepareStatement(insertKpiQuery)) {
            for (Object[] kpi : sampleKpi) {
                pstmt.setString(1, (String) kpi[0]);
                pstmt.setInt(2, (Integer) kpi[1]);
                pstmt.setInt(3, (Integer) kpi[2]);
                pstmt.setDouble(4, (Double) kpi[3]);
                pstmt.setString(5, (String) kpi[4]);
                pstmt.executeUpdate();
            }
            logger.info("Sample KPI data inserted successfully");
        } catch (SQLException e) {
            logger.severe("Error inserting sample KPI data: " + e.getMessage());
        }
    }

    // Database operation methods
    public List<Employee> getAllEmployees() {
        List<Employee> employees = new ArrayList<>();
        String query = "SELECT * FROM employees";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
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
                employees.add(emp);
            }
        } catch (SQLException e) {
            logger.severe("Error getting employees: " + e.getMessage());
        }
        
        return employees;
    }

    public boolean updateKPI(String divisi, int bulan, int tahun, double score, String managerId) {
        String query = "INSERT OR REPLACE INTO kpi (divisi, bulan, tahun, score, manager_id) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, divisi);
            pstmt.setInt(2, bulan);
            pstmt.setInt(3, tahun);
            pstmt.setDouble(4, score);
            pstmt.setString(5, managerId);
            
            int result = pstmt.executeUpdate();
            
            // Update employee layoff risk based on KPI
            updateLayoffRisk(divisi, score);
            
            return result > 0;
        } catch (SQLException e) {
            logger.severe("Error updating KPI: " + e.getMessage());
            return false;
        }
    }

    private void updateLayoffRisk(String divisi, double kpiScore) {
        String query = "UPDATE employees SET layoff_risk = ?, kpi_score = ? WHERE divisi = ? AND role = 'pegawai'";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setBoolean(1, kpiScore < 60.0);
            pstmt.setDouble(2, kpiScore);
            pstmt.setString(3, divisi);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Error updating layoff risk: " + e.getMessage());
        }
    }

    public boolean saveReport(String supervisorId, String divisi, int bulan, int tahun, String filePath) {
        String query = "INSERT INTO reports (supervisor_id, divisi, bulan, tahun, file_path) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, supervisorId);
            pstmt.setString(2, divisi);
            pstmt.setInt(3, bulan);
            pstmt.setInt(4, tahun);
            pstmt.setString(5, filePath);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Error saving report: " + e.getMessage());
            return false;
        }
    }

    public boolean saveEmployeeEvaluation(String employeeId, String supervisorId, 
                                        double punctualityScore, double attendanceScore, 
                                        double overallRating, String comments) {
        String query = """
            INSERT INTO employee_evaluations 
            (employee_id, supervisor_id, punctuality_score, attendance_score, overall_rating, comments) 
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, employeeId);
            pstmt.setString(2, supervisorId);
            pstmt.setDouble(3, punctualityScore);
            pstmt.setDouble(4, attendanceScore);
            pstmt.setDouble(5, overallRating);
            pstmt.setString(6, comments);
            
            int result = pstmt.executeUpdate();
            
            // Update employee supervisor rating
            updateEmployeeSupervisorRating(employeeId, overallRating);
            
            return result > 0;
        } catch (SQLException e) {
            logger.severe("Error saving employee evaluation: " + e.getMessage());
            return false;
        }
    }

    private void updateEmployeeSupervisorRating(String employeeId, double rating) {
        String query = "UPDATE employees SET supervisor_rating = ? WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setDouble(1, rating);
            pstmt.setString(2, employeeId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Error updating supervisor rating: " + e.getMessage());
        }
    }

    public List<Report> getPendingReports() {
        List<Report> reports = new ArrayList<>();
        String query = "SELECT * FROM reports WHERE status = 'pending' ORDER BY upload_date DESC";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                Report report = new Report();
                report.setId(rs.getInt("id"));
                report.setSupervisorId(rs.getString("supervisor_id"));
                report.setDivisi(rs.getString("divisi"));
                report.setBulan(rs.getInt("bulan"));
                report.setTahun(rs.getInt("tahun"));
                report.setFilePath(rs.getString("file_path"));
                report.setUploadDate(rs.getTimestamp("upload_date"));
                report.setStatus(rs.getString("status"));
                report.setManagerNotes(rs.getString("manager_notes"));
                reports.add(report);
            }
        } catch (SQLException e) {
            logger.severe("Error getting pending reports: " + e.getMessage());
        }
        
        return reports;
    }

    public List<KPI> getKPIByDivision(String divisi) {
        List<KPI> kpiList = new ArrayList<>();
        String query = "SELECT * FROM kpi WHERE divisi = ? ORDER BY tahun DESC, bulan DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, divisi);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                KPI kpi = new KPI();
                kpi.setId(rs.getInt("id"));
                kpi.setDivisi(rs.getString("divisi"));
                kpi.setBulan(rs.getInt("bulan"));
                kpi.setTahun(rs.getInt("tahun"));
                kpi.setScore(rs.getDouble("score"));
                kpi.setManagerId(rs.getString("manager_id"));
                kpi.setCreatedDate(rs.getTimestamp("created_date"));
                kpiList.add(kpi);
            }
        } catch (SQLException e) {
            logger.severe("Error getting KPI by division: " + e.getMessage());
        }
        
        return kpiList;
    }

    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.severe("Error closing database connection: " + e.getMessage());
        }
    }
}
