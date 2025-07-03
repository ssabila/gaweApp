package id.ac.stis.pbo.demo1.models;

import java.util.Date;

/**
 * Employee Evaluation model for supervisor assessments
 */
public class EmployeeEvaluation {
    private int id;
    private String employeeId;
    private String supervisorId;
    private double punctualityScore;
    private double attendanceScore;
    private double overallRating;
    private String comments;
    private Date evaluationDate;

    // Constructors
    public EmployeeEvaluation() {}

    public EmployeeEvaluation(String employeeId, String supervisorId, double punctualityScore, 
                             double attendanceScore, double overallRating, String comments) {
        this.employeeId = employeeId;
        this.supervisorId = supervisorId;
        this.punctualityScore = punctualityScore;
        this.attendanceScore = attendanceScore;
        this.overallRating = overallRating;
        this.comments = comments;
        this.evaluationDate = new Date();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getSupervisorId() { return supervisorId; }
    public void setSupervisorId(String supervisorId) { this.supervisorId = supervisorId; }

    public double getPunctualityScore() { return punctualityScore; }
    public void setPunctualityScore(double punctualityScore) { this.punctualityScore = punctualityScore; }

    public double getAttendanceScore() { return attendanceScore; }
    public void setAttendanceScore(double attendanceScore) { this.attendanceScore = attendanceScore; }

    public double getOverallRating() { return overallRating; }
    public void setOverallRating(double overallRating) { this.overallRating = overallRating; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public Date getEvaluationDate() { return evaluationDate; }
    public void setEvaluationDate(Date evaluationDate) { this.evaluationDate = evaluationDate; }

    @Override
    public String toString() {
        return "Evaluation - " + overallRating + "% (" + evaluationDate + ")";
    }
}
