package id.ac.stis.pbo.demo1.models;

import java.util.Date;

/**
 * Salary History model for tracking monthly salary payments
 */
public class SalaryHistory {
    private int id;
    private String employeeId;
    private int bulan;
    private int tahun;
    private double baseSalary;
    private double kpiBonus;
    private double supervisorBonus;
    private double penalty;
    private double totalSalary;
    private double kpiScore;
    private double supervisorRating;
    private Date paymentDate;
    private String notes;

    // Constructors
    public SalaryHistory() {}

    public SalaryHistory(String employeeId, int bulan, int tahun, double baseSalary, 
                        double kpiBonus, double supervisorBonus, double penalty, 
                        double totalSalary, double kpiScore, double supervisorRating) {
        this.employeeId = employeeId;
        this.bulan = bulan;
        this.tahun = tahun;
        this.baseSalary = baseSalary;
        this.kpiBonus = kpiBonus;
        this.supervisorBonus = supervisorBonus;
        this.penalty = penalty;
        this.totalSalary = totalSalary;
        this.kpiScore = kpiScore;
        this.supervisorRating = supervisorRating;
        this.paymentDate = new Date();
    }

    // Utility method to get month name
    public String getMonthName() {
        String[] months = {"", "January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};
        return months[bulan];
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public int getBulan() { return bulan; }
    public void setBulan(int bulan) { this.bulan = bulan; }

    public int getTahun() { return tahun; }
    public void setTahun(int tahun) { this.tahun = tahun; }

    public double getBaseSalary() { return baseSalary; }
    public void setBaseSalary(double baseSalary) { this.baseSalary = baseSalary; }

    public double getKpiBonus() { return kpiBonus; }
    public void setKpiBonus(double kpiBonus) { this.kpiBonus = kpiBonus; }

    public double getSupervisorBonus() { return supervisorBonus; }
    public void setSupervisorBonus(double supervisorBonus) { this.supervisorBonus = supervisorBonus; }

    public double getPenalty() { return penalty; }
    public void setPenalty(double penalty) { this.penalty = penalty; }

    public double getTotalSalary() { return totalSalary; }
    public void setTotalSalary(double totalSalary) { this.totalSalary = totalSalary; }

    public double getKpiScore() { return kpiScore; }
    public void setKpiScore(double kpiScore) { this.kpiScore = kpiScore; }

    public double getSupervisorRating() { return supervisorRating; }
    public void setSupervisorRating(double supervisorRating) { this.supervisorRating = supervisorRating; }

    public Date getPaymentDate() { return paymentDate; }
    public void setPaymentDate(Date paymentDate) { this.paymentDate = paymentDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return getMonthName() + " " + tahun + " - Rp " + String.format("%,.0f", totalSalary);
    }
}
