package id.ac.stis.pbo.demo1.models;

import java.util.Date;

/**
 * Leave Request model for managing employee leave applications
 */
public class LeaveRequest {
    private int id;
    private String employeeId;
    private String leaveType;
    private Date startDate;
    private Date endDate;
    private int totalDays;
    private String reason;
    private String status; // pending, approved, rejected
    private String approverId;
    private String approverNotes;
    private Date requestDate;
    private Date approvalDate;

    // Constructors
    public LeaveRequest() {}

    public LeaveRequest(String employeeId, String leaveType, Date startDate, Date endDate, 
                       int totalDays, String reason) {
        this.employeeId = employeeId;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalDays = totalDays;
        this.reason = reason;
        this.status = "pending";
        this.requestDate = new Date();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public int getTotalDays() { return totalDays; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getApproverId() { return approverId; }
    public void setApproverId(String approverId) { this.approverId = approverId; }

    public String getApproverNotes() { return approverNotes; }
    public void setApproverNotes(String approverNotes) { this.approverNotes = approverNotes; }

    public Date getRequestDate() { return requestDate; }
    public void setRequestDate(Date requestDate) { this.requestDate = requestDate; }

    public Date getApprovalDate() { return approvalDate; }
    public void setApprovalDate(Date approvalDate) { this.approvalDate = approvalDate; }

    @Override
    public String toString() {
        return leaveType + " - " + totalDays + " days (" + status + ")";
    }
}
