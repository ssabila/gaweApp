package id.ac.stis.pbo.demo1.models;

import java.util.Date;

/**
 * Enhanced Employee model with new features
 */
public class Employee {
    private String id;
    private String nama;
    private String password;
    private String role;
    private String divisi;
    private String jabatan;
    private Date tglMasuk;
    private int sisaCuti;
    private double gajiPokok;
    private double kpiScore;
    private double supervisorRating;
    private double attendanceScore;
    private double overallRating; // Added overallRating field
    private boolean layoffRisk;

    // Constructors
    public Employee() {}

    public Employee(String id, String nama, String password, String role,
                    String divisi, String jabatan, Date tglMasuk) {
        this.id = id;
        this.nama = nama;
        this.password = password;
        this.role = role;
        this.divisi = divisi;
        this.jabatan = jabatan;
        this.tglMasuk = tglMasuk;
        this.sisaCuti = 12;
        this.gajiPokok = calculateGajiPokok(role, divisi);
        this.kpiScore = 0.0;
        this.supervisorRating = 0.0;
        this.attendanceScore = 0.0;
        this.overallRating = 0.0; // Initialized overallRating
        this.layoffRisk = false;
    }

    // Calculate base salary based on role and division
    public double calculateGajiPokok(String role, String divisi) {
        double baseGaji = 4000000; // Base salary 4 million

        // Role multiplier
        switch (role.toLowerCase()) {
            case "manajer":
                baseGaji *= 2.5;
                break;
            case "supervisor":
                baseGaji *= 1.8;
                break;
            default: // pegawai
                baseGaji *= 1.0;
                break;
        }

        // Division multiplier
        switch (divisi) {
            case "Finance":
                baseGaji *= 1.2;
                break;
            case "IT":
                baseGaji *= 1.15;
                break;
            case "Sales":
                baseGaji *= 1.1;
                break;
            case "Marketing":
                baseGaji *= 1.05;
                break;
            default: // HR
                baseGaji *= 1.0;
                break;
        }

        return baseGaji;
    }

    // Calculate monthly salary with bonuses and adjustments
    public double calculateGajiBulanan() {
        double totalGaji = gajiPokok;

        // KPI bonus (up to 20% of base salary)
        if (kpiScore >= 90) {
            totalGaji += gajiPokok * 0.20;
        } else if (kpiScore >= 80) {
            totalGaji += gajiPokok * 0.15;
        } else if (kpiScore >= 70) {
            totalGaji += gajiPokok * 0.10;
        } else if (kpiScore >= 60) {
            totalGaji += gajiPokok * 0.05;
        }

        // Supervisor rating bonus (up to 15% of base salary)
        if (supervisorRating >= 90) {
            totalGaji += gajiPokok * 0.15;
        } else if (supervisorRating >= 80) {
            totalGaji += gajiPokok * 0.10;
        } else if (supervisorRating >= 70) {
            totalGaji += gajiPokok * 0.05;
        }

        // Penalty for low performance
        if (kpiScore < 60 || supervisorRating < 60) {
            totalGaji -= gajiPokok * 0.10; // 10% penalty
        }

        return Math.max(totalGaji, gajiPokok * 0.5); // Minimum 50% of base salary
    }

    // Calculate additional leave days based on performance
    public int calculateAdditionalLeave() {
        int additionalDays = 0;

        if (kpiScore >= 90 && supervisorRating >= 90) {
            additionalDays = 5;
        } else if (kpiScore >= 80 && supervisorRating >= 80) {
            additionalDays = 3;
        } else if (kpiScore >= 70 && supervisorRating >= 70) {
            additionalDays = 1;
        }

        return additionalDays;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDivisi() { return divisi; }
    public void setDivisi(String divisi) { this.divisi = divisi; }

    public String getJabatan() { return jabatan; }
    public void setJabatan(String jabatan) { this.jabatan = jabatan; }

    public Date getTglMasuk() { return tglMasuk; }
    public void setTglMasuk(Date tglMasuk) { this.tglMasuk = tglMasuk; }

    public int getSisaCuti() { return sisaCuti; }
    public void setSisaCuti(int sisaCuti) { this.sisaCuti = sisaCuti; }

    public double getGajiPokok() { return gajiPokok; }
    public void setGajiPokok(double gajiPokok) { this.gajiPokok = gajiPokok; }

    public double getKpiScore() { return kpiScore; }
    public void setKpiScore(double kpiScore) { this.kpiScore = kpiScore; }

    public double getSupervisorRating() { return supervisorRating; }
    public void setSupervisorRating(double supervisorRating) { this.supervisorRating = supervisorRating; }

    public double getAttendanceScore() { return attendanceScore; }
    public void setAttendanceScore(double attendanceScore) { this.attendanceScore = attendanceScore; }

    // New getter and setter for overallRating
    public double getOverallRating() { return overallRating; }
    public void setOverallRating(double overallRating) { this.overallRating = overallRating; }

    public boolean isLayoffRisk() { return layoffRisk; }
    public void setLayoffRisk(boolean layoffRisk) { this.layoffRisk = layoffRisk; }

    @Override
    public String toString() {
        return nama + " (" + id + ")";
    }
}