package id.ac.stis.pbo.demo1.models;

import java.util.Date;
import java.util.List;

/**
 * Meeting model for scheduling and managing meetings
 */
public class Meeting {
    private int id;
    private String title;
    private String description;
    private Date tanggal;
    private String waktuMulai;
    private String waktuSelesai;
    private String lokasi;
    private String organizerId;
    private List<String> participantIds;
    private String status; // scheduled, ongoing, completed, cancelled
    private Date createdDate;

    // Constructors
    public Meeting() {}

    public Meeting(String title, String description, Date tanggal, String waktuMulai, 
                  String waktuSelesai, String lokasi, String organizerId, List<String> participantIds) {
        this.title = title;
        this.description = description;
        this.tanggal = tanggal;
        this.waktuMulai = waktuMulai;
        this.waktuSelesai = waktuSelesai;
        this.lokasi = lokasi;
        this.organizerId = organizerId;
        this.participantIds = participantIds;
        this.status = "scheduled";
        this.createdDate = new Date();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Date getTanggal() { return tanggal; }
    public void setTanggal(Date tanggal) { this.tanggal = tanggal; }

    public String getWaktuMulai() { return waktuMulai; }
    public void setWaktuMulai(String waktuMulai) { this.waktuMulai = waktuMulai; }

    public String getWaktuSelesai() { return waktuSelesai; }
    public void setWaktuSelesai(String waktuSelesai) { this.waktuSelesai = waktuSelesai; }

    public String getLokasi() { return lokasi; }
    public void setLokasi(String lokasi) { this.lokasi = lokasi; }

    public String getOrganizerId() { return organizerId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    public List<String> getParticipantIds() { return participantIds; }
    public void setParticipantIds(List<String> participantIds) { this.participantIds = participantIds; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }

    @Override
    public String toString() {
        return title + " - " + tanggal + " " + waktuMulai;
    }
}
