package data;

import models.*;
import data.MySQLDataStore.MonthlyEvaluation;
import java.util.List;
import java.util.Map;
import java.util.Date;

/**
 * Interface defining data store operations for GAWE application.
 * VERSI LENGKAP DAN SUDAH DIPERBAIKI.
 */
public interface IDataStore {
    // Authentication
    Employee authenticateUser(String employeeId, String password);

    // Employee operations
    List<Employee> getAllEmployees();
    List<Employee> getEmployeesByDivision(String divisi);
    Employee getEmployeeById(String id);
    void updateEmployee(Employee employee);

    // KPI operations
    List<KPI> getAllKPI();
    boolean saveKPI(String divisi, int bulan, int tahun, double score, String managerId);

    // Report operations
    List<Report> getAllReports();
    List<Report> getPendingReports();
    List<Report> getReportsByDivision(String divisi);
    boolean saveReport(String supervisorId, String divisi, int bulan, int tahun, String filePath);
    boolean updateReportStatus(int reportId, String status, String managerNotes, String reviewedBy);

    // Attendance operations
    List<Attendance> getAttendanceByEmployee(String employeeId);
    List<Attendance> getTodayAttendance(String employeeId);
    boolean saveAttendance(String employeeId, Date tanggal, String jamMasuk, String jamKeluar, String status);
    boolean updateAttendanceClockOut(String employeeId, String jamKeluar);

    // Meeting operations
    List<Meeting> getMeetingsByEmployee(String employeeId);
    boolean saveMeeting(String title, String description, Date tanggal, String waktuMulai,
                        String waktuSelesai, String lokasi, String organizerId, List<String> participantIds);

    // Leave Request operations
    List<LeaveRequest> getAllLeaveRequests();
    List<LeaveRequest> getLeaveRequestsByEmployee(String employeeId);
    List<LeaveRequest> getPendingLeaveRequests();
    List<LeaveRequest> getLeaveRequestsForApproval(String approverId);
    boolean saveLeaveRequest(String employeeId, String leaveType, Date startDate, Date endDate, String reason);
    boolean approveLeaveRequest(int leaveRequestId, String approverId, String notes);
    boolean rejectLeaveRequest(int leaveRequestId, String approverId, String notes);
    List<LeaveRequest> getPendingLeaveRequestsByEmployee(String employeeId);

    // Salary History operations
    List<SalaryHistory> getAllSalaryHistory();
    List<SalaryHistory> getSalaryHistoryByEmployee(String employeeId);

    // Employee Evaluation operations
    List<EmployeeEvaluation> getAllEvaluations();
    // PERBAIKAN: Menambahkan metode saveEmployeeEvaluation yang hilang
    boolean saveEmployeeEvaluation(String employeeId, String supervisorId, double punctualityScore, double attendanceScore, double overallRating, String comments);

    // Monthly Employee Evaluation operations
    boolean saveMonthlyEmployeeEvaluation(String employeeId, String supervisorId, int month, int year, double punctualityScore, double attendanceScore, double productivityScore, double overallRating, String comments);
    boolean hasMonthlyEvaluation(String employeeId, int month, int year);
    List<MonthlyEvaluation> getAllMonthlyEvaluations();
    List<MonthlyEvaluation> getMonthlyEvaluationsBySupervisor(String supervisorId);

    // Dashboard & Utility operations
    // PERBAIKAN: Menambahkan metode getDashboardStats yang hilang
    Map<String, Object> getDashboardStats();

    // Resource cleanup
    void close();
}