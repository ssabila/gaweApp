package id.ac.stis.pbo.demo1.models;

/**
 * Server response model for client-server communication
 */
public class ServerResponse {
    private String status;
    private String message;
    private Object data;

    // Constructors
    public ServerResponse() {}

    public ServerResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public ServerResponse(String status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    @Override
    public String toString() {
        return String.format("ServerResponse{status='%s', message='%s'}", status, message);
    }
}
