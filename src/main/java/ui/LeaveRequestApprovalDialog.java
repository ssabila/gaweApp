package ui;

import data.MySQLDataStore;
import models.Employee;
import models.LeaveRequest;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.text.SimpleDateFormat;

/**
 * Dialog for approving or rejecting leave requests
 */
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

        // Request details
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(15);
        detailsGrid.setVgap(10);

        detailsGrid.add(new Label("Employee Name:"), 0, 0);
        detailsGrid.add(new Label(employeeName), 1, 0);

        detailsGrid.add(new Label("Leave Type:"), 0, 1);
        detailsGrid.add(new Label(request.getLeaveType()), 1, 1);

        detailsGrid.add(new Label("Start Date:"), 0, 2);
        detailsGrid.add(new Label(sdf.format(request.getStartDate())), 1, 2);

        detailsGrid.add(new Label("End Date:"), 0, 3);
        detailsGrid.add(new Label(sdf.format(request.getEndDate())), 1, 3);

        detailsGrid.add(new Label("Total Days:"), 0, 4);
        detailsGrid.add(new Label(String.valueOf(request.getTotalDays())), 1, 4);

        detailsGrid.add(new Label("Reason:"), 0, 5);
        Label reasonLabel = new Label(request.getReason());
        reasonLabel.setWrapText(true);
        reasonLabel.setMaxWidth(300);
        detailsGrid.add(reasonLabel, 1, 5);

        // Notes area
        Label notesLabel = new Label("Approval/Rejection Notes:");
        notesArea = new TextArea();
        notesArea.setPromptText("Enter your notes here...");
        notesArea.setPrefRowCount(4);
        notesArea.setMaxWidth(400);

        content.getChildren().addAll(
                new Label("Request Details:"),
                detailsGrid,
                new Separator(),
                notesLabel,
                notesArea
        );

        getDialogPane().setContent(content);

        // Add buttons
        ButtonType approveButton = new ButtonType("Approve", ButtonBar.ButtonData.OK_DONE);
        ButtonType rejectButton = new ButtonType("Reject", ButtonBar.ButtonData.OTHER);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        getDialogPane().getButtonTypes().addAll(approveButton, rejectButton, cancelButton);

        // Style the dialog
        getDialogPane().getStylesheets().add(getClass().getResource("/ui/dashboard.css").toExternalForm());
        getDialogPane().getStyleClass().add("dialog-pane");
    }

    public String getNotes() {
        return notesArea.getText();
    }

    public LeaveRequest getLeaveRequest() {
        return request;
    }

    public boolean isNotesEmpty() {
        return notesArea.getText().trim().isEmpty();
    }

    public void focusNotes() {
        notesArea.requestFocus();
    }

    public boolean processResult() {
        if (isApproval) {
            return dataStore.approveLeaveRequest(request.getId(), supervisor.getId(), getNotes());
        } else {
            return dataStore.rejectLeaveRequest(request.getId(), supervisor.getId(), getNotes());
        }
    }
}
