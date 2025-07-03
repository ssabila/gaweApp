package id.ac.stis.pbo.demo1.models;

import java.util.Date;

/**
 * Report model for monthly division reports
 */
public class Report {
    private int id;
    private String supervisorId;
    private String divisi;
    private int bulan;
    private int tahun;
    private String filePath;
    private Date uploadDate;
    private String status; // pending, reviewed, approved, rejected
    private String managerNotes;
    private String reviewedBy;
    private Date reviewedDate;

    // Constructors
    public Report() {}

    public Report(String supervisorId, String divisi, int bulan, int tahun, String filePath) {
        this.supervisorId = supervisorId;
        this.divisi = divisi;
        this.bulan = bulan;
        this.tahun = tahun;
        this.filePath = filePath;
        this.uploadDate = new Date();
        this.status = "pending";
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

    public String getSupervisorId() { return supervisorId; }
    public void setSupervisorId(String supervisorId) { this.supervisorId = supervisorId; }

    public String getDivisi() { return divisi; }
    public void setDivisi(String divisi) { this.divisi = divisi; }

    public int getBulan() { return bulan; }
    public void setBulan(int bulan) { this.bulan = bulan; }

    public int getTahun() { return tahun; }
    public void setTahun(int tahun) { this.tahun = tahun; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Date getUploadDate() { return uploadDate; }
    public void setUploadDate(Date uploadDate) { this.uploadDate = uploadDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getManagerNotes() { return managerNotes; }
    public void setManagerNotes(String managerNotes) { this.managerNotes = managerNotes; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public Date getReviewedDate() { return reviewedDate; }
    public void setReviewedDate(Date reviewedDate) { this.reviewedDate = reviewedDate; }

    @Override
    public String toString() {
        return divisi + " - " + getMonthName() + " " + tahun + " (" + status + ")";
    }
}
