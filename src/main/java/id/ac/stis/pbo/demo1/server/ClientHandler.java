package id.ac.stis.pbo.demo1.server;

import id.ac.stis.pbo.demo1.data.DataStoreFactory;
import id.ac.stis.pbo.demo1.data.MySQLDataStore;
import id.ac.stis.pbo.demo1.models.Employee;
import id.ac.stis.pbo.demo1.models.ServerRequest;
import id.ac.stis.pbo.demo1.models.ServerResponse;
import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * Handles individual client connections with MySQL integration
 */
public class ClientHandler implements Runnable {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    private Socket clientSocket;
    private Gson gson;
    private BufferedReader in;
    private PrintWriter out;
    private final MySQLDataStore dataStore;

    public ClientHandler(Socket socket, Gson gson) {
        this.clientSocket = socket;
        this.gson = gson;
        this.dataStore = DataStoreFactory.getMySQLDataStore();
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                processRequest(inputLine);
            }
        } catch (IOException e) {
            logger.warning("Client disconnected: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                logger.warning("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private void processRequest(String request) {
        try {
            ServerRequest serverRequest = gson.fromJson(request, ServerRequest.class);
            ServerResponse response = handleRequest(serverRequest);
            out.println(gson.toJson(response));
        } catch (Exception e) {
            ServerResponse errorResponse = new ServerResponse("error", "Invalid request: " + e.getMessage());
            out.println(gson.toJson(errorResponse));
        }
    }

    private ServerResponse handleRequest(ServerRequest request) {
        try {
            switch (request.getAction()) {
                case "login":
                    return handleLogin(request);
                case "getEmployees":
                    return handleGetEmployees(request);
                case "updateKPI":
                    return handleUpdateKPI(request);
                case "uploadReport":
                    return handleUploadReport(request);
                case "evaluateEmployee":
                    return handleEvaluateEmployee(request);
                case "getAttendance":
                    return handleGetAttendance(request);
                case "saveAttendance":
                    return handleSaveAttendance(request);
                case "getDashboardStats":
                    return handleGetDashboardStats(request);
                case "getMeetings":
                    return handleGetMeetings(request);
                case "saveMeeting":
                    return handleSaveMeeting(request);
                case "getLeaveRequests":
                    return handleGetLeaveRequests(request);
                case "saveLeaveRequest":
                    return handleSaveLeaveRequest(request);
                case "approveLeaveRequest":
                    return handleApproveLeaveRequest(request);
                case "getSalaryHistory":
                    return handleGetSalaryHistory(request);
                case "saveMonthlyEvaluation":
                    return handleSaveMonthlyEvaluation(request);
                default:
                    return new ServerResponse("error", "Unknown action: " + request.getAction());
            }
        } catch (Exception e) {
            logger.severe("Error handling request: " + e.getMessage());
            return new ServerResponse("error", "Server error: " + e.getMessage());
        }
    }

    private ServerResponse handleLogin(ServerRequest request) {
        try {
            String employeeId = (String) request.getData().get("employeeId");
            String password = (String) request.getData().get("password");
            
            Employee employee = dataStore.authenticateUser(employeeId, password);
            if (employee != null) {
                return new ServerResponse("success", "Login successful", employee);
            } else {
                return new ServerResponse("error", "Invalid credentials");
            }
        } catch (Exception e) {
            return new ServerResponse("error", "Login failed: " + e.getMessage());
        }
    }

    private ServerResponse handleGetEmployees(ServerRequest request) {
        try {
            return new ServerResponse("success", "Employees retrieved", dataStore.getAllEmployees());
        } catch (Exception e) {
            return new ServerResponse("error", "Failed to get employees: " + e.getMessage());
        }
    }

    private ServerResponse handleUpdateKPI(ServerRequest request) {
        try {
            String divisi = (String) request.getData().get("divisi");
            int bulan = ((Double) request.getData().get("bulan")).intValue();
            int tahun = ((Double) request.getData().get("tahun")).intValue();
            double score = (Double) request.getData().get("score");
            String managerId = request.getUserId();
            
            boolean success = dataStore.saveKPI(divisi, bulan, tahun, score, managerId);
            if (success) {
                return new ServerResponse("success", "KPI updated successfully");
            } else {
                return new ServerResponse("error", "Failed to update KPI");
            }
        } catch (Exception e) {
            return new ServerResponse("error", "KPI update failed: " + e.getMessage());
        }
    }

    private ServerResponse handleUploadReport(ServerRequest request) {
        try {
            String supervisorId = request.getUserId();
            String divisi = (String) request.getData().get("divisi");
            int bulan = ((Double) request.getData().get("bulan")).intValue();
            int tahun = ((Double) request.getData().get("tahun")).intValue();
            String filePath = (String) request.getData().get("filePath");
            
            boolean success = dataStore.saveReport(supervisorId, divisi, bulan, tahun, filePath);
            if (success) {
                return new ServerResponse("success", "Report uploaded successfully");
            } else {
                return new ServerResponse("error", "Failed to upload report");
            }
        } catch (Exception e) {
            return new ServerResponse("error", "Report upload failed: " + e.getMessage());
        }
    }

    private ServerResponse handleEvaluateEmployee(ServerRequest request) {
        try {
            String employeeId = (String) request.getData().get("employeeId");
            String supervisorId = request.getUserId();
            double punctualityScore = (Double) request.getData().get("punctualityScore");
            double attendanceScore = (Double) request.getData().get("attendanceScore");
            double overallRating = (Double) request.getData().get("overallRating");
            String comments = (String) request.getData().get("comments");
            
            boolean success = dataStore.saveEmployeeEvaluation(employeeId, supervisorId,
                                                             punctualityScore, attendanceScore, 
                                                             overallRating, comments);
            if (success) {
                return new ServerResponse("success", "Employee evaluation saved successfully");
            } else {
                return new ServerResponse("error", "Failed to save evaluation");
            }
        } catch (Exception e) {
            return new ServerResponse("error", "Evaluation failed: " + e.getMessage());
        }
    }

    private ServerResponse handleSaveMonthlyEvaluation(ServerRequest request) {
        try {
            String employeeId = (String) request.getData().get("employeeId");
            String supervisorId = request.getUserId();
            int month = ((Double) request.getData().get("month")).intValue();
            int year = ((Double) request.getData().get("year")).intValue();
            double punctualityScore = (Double) request.getData().get("punctualityScore");
            double attendanceScore = (Double) request.getData().get("attendanceScore");
            double productivityScore = (Double) request.getData().get("productivityScore");
            double overallRating = (Double) request.getData().get("overallRating");
            String comments = (String) request.getData().get("comments");
            
            boolean success = dataStore.saveMonthlyEmployeeEvaluation(employeeId, supervisorId,
                                                                    month, year, punctualityScore, 
                                                                    attendanceScore, productivityScore, 
                                                                    overallRating, comments);
            if (success) {
                return new ServerResponse("success", "Monthly evaluation saved successfully");
            } else {
                return new ServerResponse("error", "Failed to save monthly evaluation");
            }
        } catch (Exception e) {
            return new ServerResponse("error", "Monthly evaluation failed: " + e.getMessage());
        }
    }

    private ServerResponse handleGetAttendance(ServerRequest request) {
        try {
            String employeeId = (String) request.getData().get("employeeId");
            return new ServerResponse("success", "Attendance retrieved", 
                                    dataStore.getAttendanceByEmployee(employeeId));
        } catch (Exception e) {
            return new ServerResponse("error", "Failed to get attendance: " + e.getMessage());
        }
    }

    private ServerResponse handleSaveAttendance(ServerRequest request) {
        try {
            String employeeId = request.getUserId();
            String status = (String) request.getData().get("status");
            String jamMasuk = (String) request.getData().get("jamMasuk");
            String jamKeluar = (String) request.getData().get("jamKeluar");
            
            boolean success = dataStore.saveAttendance(employeeId, new java.util.Date(),
                                                     jamMasuk, jamKeluar, status);
            if (success) {
                return new ServerResponse("success", "Attendance saved successfully");
            } else {
                return new ServerResponse("error", "Failed to save attendance");
            }
        } catch (Exception e) {
            return new ServerResponse("error", "Attendance save failed: " + e.getMessage());
        }
    }

    private ServerResponse handleGetDashboardStats(ServerRequest request) {
        try {
            return new ServerResponse("success", "Dashboard stats retrieved", 
                                    dataStore.getDashboardStats());
        } catch (Exception e) {
            return new ServerResponse("error", "Failed to get dashboard stats: " + e.getMessage());
        }
    }

    private ServerResponse handleGetMeetings(ServerRequest request) {
        try {
            String employeeId = request.getUserId();
            return new ServerResponse("success", "Meetings retrieved", 
                                    dataStore.getMeetingsByEmployee(employeeId));
        } catch (Exception e) {
            return new ServerResponse("error", "Failed to get meetings: " + e.getMessage());
        }
    }

    private ServerResponse handleSaveMeeting(ServerRequest request) {
        try {
            String title = (String) request.getData().get("title");
            String description = (String) request.getData().get("description");
            java.util.Date tanggal = new java.util.Date((Long) request.getData().get("tanggal"));
            String waktuMulai = (String) request.getData().get("waktuMulai");
            String waktuSelesai = (String) request.getData().get("waktuSelesai");
            String lokasi = (String) request.getData().get("lokasi");
            String organizerId = request.getUserId();
            @SuppressWarnings("unchecked")
            java.util.List<String> participantIds = (java.util.List<String>) request.getData().get("participantIds");
            
            boolean success = dataStore.saveMeeting(title, description, tanggal, waktuMulai,
                                                  waktuSelesai, lokasi, organizerId, participantIds);
            if (success) {
                return new ServerResponse("success", "Meeting saved successfully");
            } else {
                return new ServerResponse("error", "Failed to save meeting");
            }
        } catch (Exception e) {
            return new ServerResponse("error", "Meeting save failed: " + e.getMessage());
        }
    }

    private ServerResponse handleGetLeaveRequests(ServerRequest request) {
        try {
            String employeeId = request.getUserId();
            return new ServerResponse("success", "Leave requests retrieved", 
                                    dataStore.getLeaveRequestsByEmployee(employeeId));
        } catch (Exception e) {
            return new ServerResponse("error", "Failed to get leave requests: " + e.getMessage());
        }
    }

    private ServerResponse handleSaveLeaveRequest(ServerRequest request) {
        try {
            String employeeId = request.getUserId();
            String leaveType = (String) request.getData().get("leaveType");
            java.util.Date startDate = new java.util.Date((Long) request.getData().get("startDate"));
            java.util.Date endDate = new java.util.Date((Long) request.getData().get("endDate"));
            String reason = (String) request.getData().get("reason");
            
            boolean success = dataStore.saveLeaveRequest(employeeId, leaveType, startDate, endDate, reason);
            if (success) {
                return new ServerResponse("success", "Leave request submitted successfully");
            } else {
                return new ServerResponse("error", "Failed to submit leave request");
            }
        } catch (Exception e) {
            return new ServerResponse("error", "Leave request failed: " + e.getMessage());
        }
    }

    private ServerResponse handleApproveLeaveRequest(ServerRequest request) {
        try {
            int leaveRequestId = ((Double) request.getData().get("leaveRequestId")).intValue();
            String approverId = request.getUserId();
            String notes = (String) request.getData().get("notes");
            String action = (String) request.getData().get("action");
            
            boolean success;
            if ("approve".equals(action)) {
                success = dataStore.approveLeaveRequest(leaveRequestId, approverId, notes);
            } else {
                success = dataStore.rejectLeaveRequest(leaveRequestId, approverId, notes);
            }
            
            if (success) {
                return new ServerResponse("success", "Leave request " + action + "d successfully");
            } else {
                return new ServerResponse("error", "Failed to " + action + " leave request");
            }
        } catch (Exception e) {
            return new ServerResponse("error", "Leave request approval failed: " + e.getMessage());
        }
    }

    private ServerResponse handleGetSalaryHistory(ServerRequest request) {
        try {
            String employeeId = request.getUserId();
            return new ServerResponse("success", "Salary history retrieved", 
                                    dataStore.getSalaryHistoryByEmployee(employeeId));
        } catch (Exception e) {
            return new ServerResponse("error", "Failed to get salary history: " + e.getMessage());
        }
    }
}