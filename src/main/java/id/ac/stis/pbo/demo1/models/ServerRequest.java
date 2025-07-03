package id.ac.stis.pbo.demo1.models;

import java.util.Map;

/**
 * Server request model for client-server communication
 */
public class ServerRequest {
    private String action;
    private Map<String, Object> data;
    private String userId;

    // Constructors
    public ServerRequest() {}

    public ServerRequest(String action, Map<String, Object> data, String userId) {
        this.action = action;
        this.data = data;
        this.userId = userId;
    }

    // Getters and Setters
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @Override
    public String toString() {
        return String.format("ServerRequest{action='%s', userId='%s'}", action, userId);
    }
}
