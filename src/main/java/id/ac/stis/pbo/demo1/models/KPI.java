package id.ac.stis.pbo.demo1.models;

import java.util.Date;

/**
 * KPI model for tracking division performance
 */
public class KPI {
    private int id;
    private String divisi;
    private int bulan;
    private int tahun;
    private double score;
    private String managerId;
    private Date createdDate;
    private String notes;

    // Constructors
    public KPI() {}

    public KPI(String divisi, int bulan, int tahun, double score, String managerId) {
        this.divisi = divisi;
        this.bulan = bulan;
        this.tahun = tahun;
        this.score = score;
        this.managerId = managerId;
        this.createdDate = new Date();
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

    public String getDivisi() { return divisi; }
    public void setDivisi(String divisi) { this.divisi = divisi; }

    public int getBulan() { return bulan; }
    public void setBulan(int bulan) { this.bulan = bulan; }

    public int getTahun() { return tahun; }
    public void setTahun(int tahun) { this.tahun = tahun; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getManagerId() { return managerId; }
    public void setManagerId(String managerId) { this.managerId = managerId; }

    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return divisi + " - " + getMonthName() + " " + tahun + " (" + score + "%)";
    }
}
