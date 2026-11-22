package ui;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.time.*;
import java.time.format.*;
import java.util.ArrayList;

import javax.swing.*;

import logic.ApplicationService;
import model.*;

public class StudentFrame extends JFrame
        implements ActionListener, WindowListener {

    private ApplicationService service;

    private JLabel welcomeLabel, clockLabel;
    private JLabel allocatedLabel, usedLabel;
    private JLabel reservedLabel, availableLabel;
    private JLabel durationLabel, saveLabel;

    private JTextField titleField, applicationField;
    private JTextField searchField, cancelIdField;

    private JTextArea descriptionArea, jobsArea;

    private JComboBox<TaskSize> sizeBox;
    private JComboBox<String> statusBox;

    private JButton submitButton, cancelButton;
    private JButton refreshButton, retrySaveButton, logoutButton;

    private boolean loggedOut;

    private final DateTimeFormatter timeFormat =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public StudentFrame(ApplicationService service) {

        if (service == null) {
            throw new IllegalArgumentException("Application service is required.");
        }

        if (!(service.getCurrentUser() instanceof Student)) {
            throw new SecurityException("Only approved students can open this window.");
        }

        this.service = service;
        loggedOut = false;

        buildUi();
        refreshView();
    }

    private void buildUi() {

        setTitle("GPU Utilization Management - Student");
        setSize(1200, 800);
        setLocationRelativeTo(null);

        setLayout(new GridLayout(1, 2, 10, 10));

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(this);

        welcomeLabel = new JLabel();
        clockLabel = new JLabel();
        allocatedLabel = new JLabel();
        usedLabel = new JLabel();
        reservedLabel = new JLabel();
        availableLabel = new JLabel();

        JPanel balances = new JPanel(new GridLayout(6, 1));

        balances.add(welcomeLabel);
        balances.add(clockLabel);
        balances.add(allocatedLabel);
        balances.add(usedLabel);
        balances.add(reservedLabel);
        balances.add(availableLabel);

        titleField = new JTextField();
        applicationField = new JTextField();
        durationLabel = new JLabel();

        sizeBox = new JComboBox<TaskSize>(
            new TaskSize[] {
                new TaskSize("Small"),
                new TaskSize("Medium"),
                new TaskSize("Large"),
                new TaskSize("Very large")
            }
        );

        JPanel form = new JPanel(
            new GridLayout(4, 2, 5, 5)
        );

        form.add(new JLabel("Job title:"));
        form.add(titleField);

        form.add(new JLabel("Application name:"));
        form.add(applicationField);

        form.add(new JLabel("Task size:"));
        form.add(sizeBox);

        form.add(new JLabel("Description (enter below):"));
        form.add(durationLabel);

        descriptionArea = new JTextArea();
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        searchField = new JTextField();
        cancelIdField = new JTextField();

        statusBox = new JComboBox<String>(
            new String[] {
                "All",
                JobStatus.QUEUED,
                JobStatus.RUNNING,
                JobStatus.COMPLETED,
                JobStatus.CANCELLED
            }
        );

        submitButton = new JButton("Submit job");
        cancelButton = new JButton("Cancel job");
        refreshButton = new JButton("Refresh jobs");
        retrySaveButton = new JButton("Retry save");
        logoutButton = new JButton("Log out");

        saveLabel = new JLabel();

        JPanel controls = new JPanel(
            new GridLayout(6, 2, 5, 5)
        );

        controls.add(submitButton);
        controls.add(refreshButton);

        controls.add(new JLabel("Search ID/title (press Enter):"));
        controls.add(searchField);

        controls.add(new JLabel("Show status:"));
        controls.add(statusBox);

        controls.add(new JLabel("Job ID to cancel (e.g. J101):"));
        controls.add(cancelIdField);

        controls.add(cancelButton);
        controls.add(retrySaveButton);

        controls.add(logoutButton);
        controls.add(saveLabel);

        JPanel left = new JPanel(
            new GridLayout(4, 1, 5, 5)
        );

        left.add(balances);
        left.add(form);
        left.add(new JScrollPane(descriptionArea));
        left.add(controls);

        add(left);

        jobsArea = new JTextArea();
        jobsArea.setEditable(false);
        jobsArea.setLineWrap(true);
        jobsArea.setWrapStyleWord(true);

        add(new JScrollPane(jobsArea));

        submitButton.addActionListener(this);
        cancelButton.addActionListener(this);
        refreshButton.addActionListener(this);
        retrySaveButton.addActionListener(this);
        logoutButton.addActionListener(this);

        searchField.addActionListener(this);
        statusBox.addActionListener(this);
        sizeBox.addActionListener(this);
    }

    public void refreshView() {

        Student student = (Student) service.getCurrentUser();
        TimeAllowance allowance = service.getMyAllowance();

        welcomeLabel.setText(
            "Welcome, " + student.getName()
            + " | ID: " + student.getUniversityId()
        );

        clockLabel.setText(
            "Simulation time: " + format(service.getCurrentTime())
        );

        allocatedLabel.setText(
            "Allocated: " + allowance.getAllocated()
        );

        usedLabel.setText(
            "Used: " + allowance.getUsed()
        );

        reservedLabel.setText(
            "Reserved: " + allowance.getReserved()
        );

        availableLabel.setText(
            "Available: " + allowance.getAvailable()
        );

        TaskSize size = (TaskSize) sizeBox.getSelectedItem();

        durationLabel.setText(
            "Duration: " + size.getDuration() + " minutes"
        );

        String status = (String) statusBox.getSelectedItem();

        if ("All".equals(status)) {
            status = null;
        }

        ArrayList<Job> jobs = service.getVisibleJobs(
            searchField.getText(), status
        );

        ArrayList<JobPrediction> predictions =
            service.getVisiblePredictions();

        String text = "MY JOBS\n\n";

        if (jobs.size() == 0) {
            text += "No jobs match your search.";
        }

        for (int i = 0; i < jobs.size(); i++) {

            Job job = jobs.get(i);
            JobPrediction prediction = null;

            for (int j = 0; j < predictions.size(); j++) {

                if (predictions.get(j).getJobId()
                        == job.getJobId()) {

                    prediction = predictions.get(j);
                    break;
                }
            }

            text += describeJob(job, prediction) + "\n\n";
        }

        jobsArea.setText(text);
        jobsArea.setCaretPosition(0);

        boolean dirty = service.isDirty();

        submitButton.setEnabled(
            !dirty
            && service.isClockInitialized()
            && allowance.getAvailable() > 0
        );

        cancelButton.setEnabled(!dirty);
        retrySaveButton.setEnabled(dirty);

        if (dirty) {
            saveLabel.setText("Unsaved: use Retry save");
        } else {
            saveLabel.setText("Changes saved");
        }
    }

    private String describeJob(
            Job job, JobPrediction prediction) {

        String gpu = "-";

        if (job.getAssignedGpuId() != null) {
            gpu = "GPU" + job.getAssignedGpuId();
        }

        String queueWait = "-";

        if (job.getQueueWaitMinutes() != null) {
            queueWait = job.getQueueWaitMinutes() + " minutes";
        }

        String charge = "Not settled";

        if (JobStatus.COMPLETED.equals(job.getStatus())
                || JobStatus.CANCELLED.equals(job.getStatus())) {

            charge = job.getCharge() + " minutes";
        }

        String result = job.getResultSummary();

        if (result.equals("")) {
            result = "Not available yet";
        }

        String text =
            "Job: " + job.getDisplayId()
            + "\nTitle: " + job.getTitle()
            + "\nApplication: " + job.getApplicationName()
            + "\nDescription: " + job.getDescription()
            + "\nSize: " + job.getTaskSize()
            + "\nWorkload units: " + job.getWorkloadUnits()
            + "\nStatus: " + job.getStatus()
            + "\nActual GPU: " + gpu
            + "\nSubmitted: " + format(job.getSubmissionTime())
            + "\nActual start: " + format(job.getStartTime())
            + "\nActual finish: " + format(job.getFinishTime())
            + "\nGPU runtime: "
            + job.getRuntime(service.getCurrentTime()) + " minutes"
            + "\nActual queue wait: " + queueWait
            + "\nSettled charge: " + charge;

        if (JobStatus.RUNNING.equals(job.getStatus())) {

            text += "\nExpected finish: " + format(
                job.getStartTime().plusMinutes(
                    job.getEstimatedDuration()
                )
            );
        }

        if (prediction != null) {

            text +=
                "\nQueue position: " + prediction.getQueuePosition()
                + "\nPredicted GPU: GPU"
                + prediction.getPredictedGpuId()
                + "\nPredicted start: "
                + format(prediction.getPredictedStart())
                + "\nPredicted finish: "
                + format(prediction.getPredictedFinish())
                + "\nRemaining wait: "
                + prediction.getRemainingWaitMinutes() + " minutes";
        }

        if (JobStatus.CANCELLED.equals(job.getStatus())) {

            text +=
                "\nCancelled at: "
                + format(job.getCancellationTime())
                + "\nCancelled by: " + job.getCancelledBy()
                + "\nCancellation reason: "
                + job.getCancellationReason();
        }

        return text + "\nResult: " + result;
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        try {

            Object source = event.getSource();

            if (source == submitButton) {
                handleSubmit();

            } else if (source == cancelButton) {
                handleCancel();

            } else if (source == retrySaveButton) {
                service.saveNow();

            } else if (source == logoutButton) {
                handleLogout();
            }

        } catch (IOException exception) {

            if (event.getSource() == submitButton
                    && service.isDirty()) {
                clearForm();
            }

            if (event.getSource() == cancelButton
                    && service.isDirty()) {
                cancelIdField.setText("");
            }

            showError(
                "The change was applied but could not be saved.\n"
                + "Click Retry save instead of repeating the action.\n"
                + exception.getMessage()
            );

        } catch (RuntimeException exception) {

            showError(exception.getMessage());
        }

        if (service.getCurrentUser() instanceof Student) {
            refreshView();
        }
    }

    private void handleSubmit() throws IOException {

        if (titleField.getText().trim().equals("")
                || applicationField.getText().trim().equals("")
                || descriptionArea.getText().trim().equals("")) {

            throw new IllegalArgumentException(
                "Complete all job fields."
            );
        }

        TaskSize size = (TaskSize) sizeBox.getSelectedItem();

        int available = service.getMyAllowance().getAvailable();

        if (size.getDuration() > available) {
            throw new IllegalArgumentException(
                "This job needs " + size.getDuration()
                + " minutes. You have "
                + available + " available."
            );
        }

        int answer = JOptionPane.showConfirmDialog(
            this,
            "Submit " + titleField.getText().trim() + "?"
            + "\nDuration: " + size.getDuration() + " minutes"
            + "\nAvailable afterwards: "
            + (available - size.getDuration()),
            "Confirm job",
            JOptionPane.YES_NO_OPTION
        );

        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        service.submitJob(
            titleField.getText(),
            applicationField.getText(),
            descriptionArea.getText(),
            size
        );

        clearForm();

        JOptionPane.showMessageDialog(this, "Job accepted.");
    }

    private void handleCancel() throws IOException {

        String input = cancelIdField.getText().trim();

        if (input.length() > 0
                && (input.charAt(0) == 'J'
                || input.charAt(0) == 'j')) {

            input = input.substring(1);
        }

        int id;

        try {

            id = Integer.parseInt(input);

        } catch (NumberFormatException exception) {

            throw new IllegalArgumentException("Enter a job ID such as J101 or 101.");
        }

        ArrayList<Job> jobs = service.getVisibleJobs("", null);

        Job selected = null;

        for (int i = 0; i < jobs.size(); i++) {

            if (jobs.get(i).getJobId() == id) {
                selected = jobs.get(i);
                break;
            }
        }

        if (selected == null) {
            throw new IllegalArgumentException("No job belonging to you has that ID.");
        }

        if (!JobStatus.QUEUED.equals(selected.getStatus())
                && !JobStatus.RUNNING.equals(selected.getStatus())) {

            throw new IllegalStateException("Only queued or running jobs can be cancelled.");
        }

        int charge = selected.getRuntime(
            service.getCurrentTime()
        );

        String reason = JOptionPane.showInputDialog(
            this,
            "Cancel " + selected.getDisplayId() + "?"
            + "\nCharge: " + charge + " GPU minutes."
            + "\nEnter a reason, then press OK to cancel:",
            "Cancel job",
            JOptionPane.QUESTION_MESSAGE
        );

        if (reason != null) {
            service.cancelJob(id, reason);
            cancelIdField.setText("");
        }
    }

    private String format(LocalDateTime time) {

        if (time == null) {
            return "-";
        }

        return timeFormat.format(time);
    }

    private void clearForm() {

        titleField.setText("");
        applicationField.setText("");
        descriptionArea.setText("");
    }

    private void handleLogout() {

        service.logout();
        loggedOut = true;
        dispose();
    }

    public boolean isLoggedOut() {
        return loggedOut;
    }

    private void showError(String message) {

        JOptionPane.showMessageDialog(
            this,
            message,
            "Student window",
            JOptionPane.ERROR_MESSAGE
        );
    }

    @Override
    public void windowClosing(WindowEvent event) {

        try {
            handleLogout();

        } catch (RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

    @Override
    public void windowOpened(WindowEvent event) {
    }

    @Override
    public void windowClosed(WindowEvent event) {
    }

    @Override
    public void windowIconified(WindowEvent event) {
    }

    @Override
    public void windowDeiconified(WindowEvent event) {
    }

    @Override
    public void windowActivated(WindowEvent event) {
    }

    @Override
    public void windowDeactivated(WindowEvent event) {
    }
}