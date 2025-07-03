package id.ac.stis.pbo.demo1.data;

import id.ac.stis.pbo.demo1.models.*;
import id.ac.stis.pbo.demo1.data.MySQLDataStore.MonthlyEvaluation; // Import MonthlyEvaluation if it's used directly in interface methods
import java.util.List;
import java.util.Map;
import java.util.Date;

/**
 * Interface defining data store operations for GAWE application
 */
public interface IDataStore {
    // Authentication
    Employee authenticateUser(String employeeId, String password);

    // Employee operations
    List<Employee> getAllEmployees();
    // Assuming getEmployeesByRole is not needed or implemented in MySQLDataStore from previous context,
    // if it were, it would need to be added.
    // List<Employee> getEmployeesByRole(String role);
    List<Employee> getEmployeesByDivision(String divisi);
    Employee getEmployeeById(String id);
    void updateEmployee(Employee employee); // This should be 'boolean updateEmployee(Employee employee);' based on MySQLDataStore return type

    // KPI operations
    List<KPI> getAllKPI();
    // Assuming getKPIByDivision is not needed or implemented in MySQLDataStore from previous context
    // List<KPI> getKPIByDivision(String divisi);
    boolean saveKPI(String divisi, int bulan, int tahun, double score, String managerId);

    // Report operations
    List<Report> getAllReports();
    List<Report> getPendingReports();
    List<Report> getReportsByDivision(String divisi);
    boolean saveReport(String supervisorId, String divisi, int bulan, int tahun, String filePath);
    boolean updateReportStatus(int reportId, String status, String managerNotes, String reviewedBy);

    // Attendance operations
    // Assuming getAllAttendance is not needed or implemented in MySQLDataStore from previous context
    // List<Attendance> getAllAttendance();
    List<Attendance> getAttendanceByEmployee(String employeeId);
    List<Attendance> getTodayAttendance(String employeeId);
    boolean saveAttendance(String employeeId, Date tanggal, String jamMasuk, String jamKeluar, String status);
    boolean updateAttendanceClockOut(String employeeId, String jamKeluar);

    // Meeting operations
    // Assuming getAllMeetings and getUpcomingMeetings are not needed or implemented in MySQLDataStore from previous context
    // List<Meeting> getAllMeetings();
    List<Meeting> getMeetingsByEmployee(String employeeId);
    // List<Meeting> getUpcomingMeetings();
    boolean saveMeeting(String title, String description, Date tanggal, String waktuMulai,
                        String waktuSelesai, String lokasi, String organizerId, List<String> participantIds);
    // Assuming updateMeetingStatus is not needed or implemented in MySQLDataStore from previous context
    // boolean updateMeetingStatus(int meetingId, String status);

    // Leave Request operations
    List<LeaveRequest> getAllLeaveRequests();
    List<LeaveRequest> getLeaveRequestsByEmployee(String employeeId);
    List<LeaveRequest> getPendingLeaveRequests();
    List<LeaveRequest> getLeaveRequestsForApproval(String approverId);
    boolean saveLeaveRequest(String employeeId, String leaveType, Date startDate, Date endDate, String reason);
    boolean approveLeaveRequest(int leaveRequestId, String approverId, String notes);
    boolean rejectLeaveRequest(int leaveRequestId, String approverId, String notes);
    List<LeaveRequest> getPendingLeaveRequestsByEmployee(String employeeId); // Added this method

    // Salary History operations
    List<SalaryHistory> getAllSalaryHistory();
    List<SalaryHistory> getSalaryHistoryByEmployee(String employeeId);

    // Monthly Employee Evaluation operations
    boolean saveMonthlyEmployeeEvaluation(String employeeId, String supervisorId, int month, int year, double punctualityScore, double attendanceScore, double productivityScore, double overallRating, String comments);
    boolean hasMonthlyEvaluation(String employeeId, int month, int year);
    List<EmployeeEvaluation> getAllEvaluations(); // General employee evaluations, not necessarily monthly.
    List<MonthlyEvaluation> getAllMonthlyEvaluations(); // Specific monthly evaluations
    List<MonthlyEvaluation> getMonthlyEvaluationsBySupervisor(String supervisorId);

    // Utility operations
    // Assuming these are not needed or implemented in MySQLDataStore from previous context
    // String getSupervisorByDivision(String divisi);
    // Map<String, Object> getDashboardStats();

    // Resource cleanup
    void close();
}