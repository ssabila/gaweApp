package id.ac.stis.pbo.demo1.models;

import java.util.Date;

/**
 * Attendance model for employee attendance tracking
 */
public class Attendance {
    private int id;
    private String employeeId;
    private Date tanggal;
    private String jamMasuk;
    private String jamKeluar;
    private String status;
    private String keterangan;
    private boolean isLate;

    // Constructors
    public Attendance() {}

    public Attendance(String employeeId, Date tanggal, String jamMasuk, String jamKeluar, String status) {
        this.employeeId = employeeId;
        this.tanggal = tanggal;
        this.jamMasuk = jamMasuk;
        this.jamKeluar = jamKeluar;
        this.status = status;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public Date getTanggal() { return tanggal; }
    public void setTanggal(Date tanggal) { this.tanggal = tanggal; }

    public String getJamMasuk() { return jamMasuk; }
    public void setJamMasuk(String jamMasuk) { this.jamMasuk = jamMasuk; }

    public String getJamKeluar() { return jamKeluar; }
    public void setJamKeluar(String jamKeluar) { this.jamKeluar = jamKeluar; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    public boolean isLate() { return isLate; }
    public void setLate(boolean late) { isLate = late; }

    @Override
    public String toString() {
        return String.format("%s - %s (%s)", employeeId, status, tanggal);
    }
}
