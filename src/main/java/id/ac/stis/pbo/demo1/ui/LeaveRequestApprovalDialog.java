package id.ac.stis.pbo.demo1.ui;

import id.ac.stis.pbo.demo1.data.MySQLDataStore;
import id.ac.stis.pbo.demo1.models.Employee;
import id.ac.stis.pbo.demo1.models.LeaveRequest;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.text.SimpleDateFormat;

public class LeaveRequestApprovalDialog extends Dialog<ButtonType> {
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    private final MySQLDataStore dataStore;
    private final LeaveRequest request;
    private final Employee supervisor;
    private final boolean isApproval;
    private TextArea notesArea;

    public LeaveRequestApprovalDialog(LeaveRequest request, Employee supervisor, MySQLDataStore dataStore, boolean isApproval) {
        this.request = request;
        this.supervisor = supervisor;
        this.dataStore = dataStore;
        this.isApproval = isApproval;

        setupDialog();
    }

    private void setupDialog() {
        setTitle(isApproval ? "Approve Leave Request" : "Reject Leave Request");
        setHeaderText("Leave request from " + request.getEmployeeId());

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Employee requestingEmployee = dataStore.getEmployeeById(request.getEmployeeId());
        String employeeName = requestingEmployee != null ? requestingEmployee.getNama() : "Unknown";

        Label infoLabel = new Label(String.format(
                "Employee: %s\nType: %s\nDates: %s to %s\nDays: %d\nReason: %s",
                employeeName,
                request.getLeaveType(),
                sdf.format(request.getStartDate()),
                sdf.format(request.getEndDate()),
                request.getTotalDays(),
                request.getReason()
        ));

        notesArea = new TextArea();
        notesArea.setPromptText("Enter approval/rejection notes...");
        notesArea.setPrefRowCount(3);

        content.getChildren().addAll(
                new Label("Request Details:"), infoLabel,
                new Label("Notes:"), notesArea
        );

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    }

    public String getNotes() {
        return notesArea.getText();
    }

    public boolean processResult() {
        if (isApproval) {
            return dataStore.approveLeaveRequest(request.getId(), supervisor.getId(), getNotes());
        } else {
            return dataStore.rejectLeaveRequest(request.getId(), supervisor.getId(), getNotes());
        }
    }
}
