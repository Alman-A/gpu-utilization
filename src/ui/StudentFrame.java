package ui;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.table.*;
import logic.ApplicationService;
import model.*;

public class StudentFrame extends JFrame
        implements ActionListener, WindowListener {

    private ApplicationService service;

    private JLabel welcomeLabel;
    private JLabel clockLabel;

    private JLabel allocatedLabel;
    private JLabel usedLabel;
    private JLabel reservedLabel;
    private JLabel availableLabel;

    private JLabel durationLabel;
    private JLabel saveLabel;

    private JTextField titleField;
    private JTextField applicationField;
    private JTextField searchField;

    private JTextArea descriptionArea;

    private JComboBox<TaskSize> sizeBox;
    private JComboBox<String> statusBox;

    private JButton submitButton;
    private JButton cancelButton;
    private JButton detailsButton;
    private JButton refreshButton;
    private JButton retrySaveButton;
    private JButton logoutButton;

    private JTable jobsTable;
    private DefaultTableModel jobsModel;

    private ArrayList<Job> displayedJobs;
    private ArrayList<JobPrediction> predictions;

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

        displayedJobs = new ArrayList<Job>();
        predictions = new ArrayList<JobPrediction>();

        loggedOut = false;

        buildUi();
        refreshView();
    }

    private void buildUi() {

        setTitle("GPU Utilization Management - Student");
        setSize(1250, 700);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout(10, 10));

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(this);

        JPanel header = new JPanel(new GridLayout(2, 1));

        welcomeLabel = new JLabel();
        clockLabel = new JLabel();

        header.add(welcomeLabel);
        header.add(clockLabel);

        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Home and submit", buildHomePanel());
        tabs.addTab("My jobs", buildJobsPanel());

        add(tabs, BorderLayout.CENTER);

        refreshButton = new JButton("Refresh");
        retrySaveButton = new JButton("Retry save");
        logoutButton = new JButton("Log out");
        saveLabel = new JLabel();

        JPanel footer = new JPanel(
            new FlowLayout(FlowLayout.LEFT)
        );

        footer.add(refreshButton);
        footer.add(retrySaveButton);
        footer.add(logoutButton);
        footer.add(saveLabel);

        add(footer, BorderLayout.SOUTH);

        submitButton.addActionListener(this);
        cancelButton.addActionListener(this);
        detailsButton.addActionListener(this);

        refreshButton.addActionListener(this);
        retrySaveButton.addActionListener(this);
        logoutButton.addActionListener(this);

        searchField.addActionListener(this);
        statusBox.addActionListener(this);
        sizeBox.addActionListener(this);
    }

    private JPanel buildHomePanel() {

        JPanel home = new JPanel(
            new BorderLayout(10, 10)
        );

        home.setBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );

        allocatedLabel = new JLabel();
        usedLabel = new JLabel();
        reservedLabel = new JLabel();
        availableLabel = new JLabel();

        JPanel balances = new JPanel(
            new GridLayout(1, 4, 10, 10)
        );

        balances.add(allocatedLabel);
        balances.add(usedLabel);
        balances.add(reservedLabel);
        balances.add(availableLabel);

        home.add(balances, BorderLayout.NORTH);

        titleField = new JTextField();
        applicationField = new JTextField();

        descriptionArea = new JTextArea(6, 40);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        sizeBox = new JComboBox<TaskSize>(
            new TaskSize[] {
                new TaskSize("Small"),
                new TaskSize("Medium"),
                new TaskSize("Large"),
                new TaskSize("Very large")
            }
        );

        JPanel fields = new JPanel(
            new GridLayout(3, 2, 8, 8)
        );

        fields.add(new JLabel("Job title:"));
        fields.add(titleField);

        fields.add(new JLabel("Application name:"));
        fields.add(applicationField);

        fields.add(new JLabel("Task size:"));
        fields.add(sizeBox);

        JPanel description = new JPanel(
            new BorderLayout()
        );

        description.add(
            new JLabel("Description:"),
            BorderLayout.NORTH
        );

        description.add(
            new JScrollPane(descriptionArea),
            BorderLayout.CENTER
        );

        durationLabel = new JLabel();
        submitButton = new JButton("Submit job");

        JPanel submit = new JPanel(
            new FlowLayout(FlowLayout.LEFT)
        );

        submit.add(durationLabel);
        submit.add(submitButton);

        JPanel form = new JPanel(
            new BorderLayout(10, 10)
        );

        form.setBorder(
            BorderFactory.createTitledBorder("Submit a new job")
        );

        form.add(fields, BorderLayout.NORTH);
        form.add(description, BorderLayout.CENTER);
        form.add(submit, BorderLayout.SOUTH);

        home.add(form, BorderLayout.CENTER);

        return home;
    }

    private JPanel buildJobsPanel() {

        JPanel panel = new JPanel(
            new BorderLayout(8, 8)
        );

        searchField = new JTextField(18);

        statusBox = new JComboBox<String>(
            new String[] {
                "All",
                JobStatus.QUEUED,
                JobStatus.RUNNING,
                JobStatus.COMPLETED,
                JobStatus.CANCELLED
            }
        );

        detailsButton = new JButton("View details");
        cancelButton = new JButton("Cancel selected job");

        JPanel controls = new JPanel(
            new FlowLayout(FlowLayout.LEFT)
        );

        controls.add(
            new JLabel("Job ID or title (press Enter):")
        );

        controls.add(searchField);
        controls.add(statusBox);
        controls.add(detailsButton);
        controls.add(cancelButton);

        panel.add(controls, BorderLayout.NORTH);

        jobsModel = new DefaultTableModel(
            new String[] {
                "Job",
                "Title",
                "Application",
                "Size",
                "Status",
                "GPU",
                "Submitted",
                "Start",
                "Finish",
                "Queue position",
                "Wait left"
            },
            0
        );

        jobsTable = new JTable(jobsModel);

        jobsTable.setDefaultEditor(Object.class, null);

        jobsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        jobsTable.setRowHeight(24);

        int[] widths = {
            65, 150, 110, 90, 85, 60,
            145, 155, 155, 95, 95
        };

        for (int i = 0; i < widths.length; i++) {
            jobsTable.getColumnModel()
                .getColumn(i)
                .setPreferredWidth(widths[i]);
        }

        panel.add(
            new JScrollPane(jobsTable),
            BorderLayout.CENTER
        );

        panel.add(
            new JLabel(
                "~ means predicted/expected. "
                + "Select a job for full history and results."
            ),
            BorderLayout.SOUTH
        );

        return panel;
    }

    public void refreshView() {

        Student student = (Student) service.getCurrentUser();

        TimeAllowance allowance = service.getMyAllowance();

        welcomeLabel.setText("Welcome, " + student.getName() + " | Student ID: " + student.getUniversityId());

        clockLabel.setText("Simulation time: " + format(service.getCurrentTime()));

        allocatedLabel.setText("Allocated: " + allowance.getAllocated());

        usedLabel.setText("Used: " + allowance.getUsed());

        reservedLabel.setText("Reserved: " + allowance.getReserved());

        availableLabel.setText("Available: " + allowance.getAvailable());

        TaskSize size = (TaskSize) sizeBox.getSelectedItem();

        durationLabel.setText("Duration: " + size.getDuration() + " GPU minutes");

        Integer selectedId = null;

        int selected = jobsTable.getSelectedRow();

        if (selected >= 0) {

            int index = jobsTable.convertRowIndexToModel(selected);

            selectedId = displayedJobs.get(index).getJobId();
        }

        String filter = (String) statusBox.getSelectedItem();

        if ("All".equals(filter)) {
            filter = null;
        }

        displayedJobs = service.getVisibleJobs(
            searchField.getText(), filter
        );

        predictions = service.getVisiblePredictions();

        jobsModel.setRowCount(0);

        for (int i = 0; i < displayedJobs.size(); i++) {

            Job job = displayedJobs.get(i);

            JobPrediction prediction = predictionFor(
                job.getJobId()
            );

            String start = format(job.getStartTime());
            String finish = format(job.getFinishTime());

            String position = "-";
            String wait = "-";
            String gpu = "-";

            if (job.getAssignedGpuId() != null) {
                gpu = "GPU" + job.getAssignedGpuId();
            }

            if (JobStatus.QUEUED.equals(job.getStatus()) && prediction != null) {
                start = "~ " + format(
                    prediction.getPredictedStart()
                );

                finish = "~ " + format(
                    prediction.getPredictedFinish()
                );

                position = "" + prediction.getQueuePosition();

                wait = prediction.getRemainingWaitMinutes() + " min";

            } else if (JobStatus.RUNNING.equals(job.getStatus())) {

                finish = "~ " + format(
                    job.getStartTime().plusMinutes(
                        job.getEstimatedDuration()
                    )
                );
            }

            jobsModel.addRow(
                new Object[] {
                    job.getDisplayId(),
                    job.getTitle(),
                    job.getApplicationName(),
                    job.getTaskSize().getName(),
                    job.getStatus(),
                    gpu,
                    format(job.getSubmissionTime()),
                    start,
                    finish,
                    position,
                    wait
                }
            );

            if (selectedId != null && selectedId == job.getJobId()) {

                int viewRow = jobsTable.convertRowIndexToView(i);

                jobsTable.setRowSelectionInterval(
                    viewRow, viewRow
                );
            }
        }

        boolean dirty = service.isDirty();

        submitButton.setEnabled(
            !dirty
            && service.isClockInitialized()
            && allowance.getAvailable() > 0
        );

        cancelButton.setEnabled(
            !dirty && !displayedJobs.isEmpty()
        );

        detailsButton.setEnabled(
            !displayedJobs.isEmpty()
        );

        retrySaveButton.setEnabled(dirty);

        if (dirty) {
            saveLabel.setText("Unsaved changes - click Retry save.");
        } else {
            saveLabel.setText("Changes saved.");
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        try {

            Object source = event.getSource();

            if (source == submitButton) {
                handleSubmit();

            } else if (source == cancelButton) {
                handleCancel();

            } else if (source == detailsButton) {
                showDetails();

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

            showError("The change exists in memory but could not be saved.\n"
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

            throw new IllegalArgumentException("Complete all job fields.");
        }

        TaskSize size = (TaskSize) sizeBox.getSelectedItem();

        int available = service.getMyAllowance().getAvailable();

        if (size.getDuration() > available) {
            throw new IllegalArgumentException(
                "This job needs " + size.getDuration()
                + " minutes, but you have only "
                + available + " available."
            );
        }

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Submit: " + titleField.getText().trim()
            + "\nDuration: " + size.getDuration() + " minutes"
            + "\nAvailable after submission: "
            + (available - size.getDuration()),
            "Confirm job",
            JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        service.submitJob(
            titleField.getText(),
            applicationField.getText(),
            descriptionArea.getText(),
            size
        );

        clearForm();

        JOptionPane.showMessageDialog(
            this, "Job accepted."
        );
    }

    private void handleCancel() throws IOException {

        Job job = selectedJob();

        if (!JobStatus.QUEUED.equals(job.getStatus())
                && !JobStatus.RUNNING.equals(job.getStatus())) {

            throw new IllegalStateException("Only queued or running jobs can be cancelled.");
        }

        int charge = job.getRuntime(
            service.getCurrentTime()
        );

        String reason = JOptionPane.showInputDialog(
            this,
            "Cancel " + job.getDisplayId() + "?"
            + "\nCharge: " + charge + " GPU minutes."
            + "\nEnter a reason, then press OK to cancel:",
            "Cancel job",
            JOptionPane.QUESTION_MESSAGE
        );

        if (reason != null) {
            service.cancelJob(job.getJobId(), reason);
        }
    }

    private void showDetails() {

        Job job = selectedJob();

        JobPrediction prediction = predictionFor(
            job.getJobId()
        );

        String charge = "Not settled";

        if (JobStatus.COMPLETED.equals(job.getStatus())
                || JobStatus.CANCELLED.equals(job.getStatus())) {

            charge = job.getCharge() + " minutes";
        }

        String wait = "-";

        if (job.getQueueWaitMinutes() != null) {
            wait = job.getQueueWaitMinutes() + " minutes";
        }

        String gpu = "-";

        if (job.getAssignedGpuId() != null) {
            gpu = "GPU" + job.getAssignedGpuId();
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
            + "\nExecution runtime: "
            + job.getRuntime(service.getCurrentTime()) + " minutes"
            + "\nActual queue wait: " + wait
            + "\nSettled charge: " + charge;

        if (prediction != null) {

            text +=
                "\n\nQueue position: " + prediction.getQueuePosition()
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
                "\n\nCancelled at: "
                + format(job.getCancellationTime())
                + "\nCancelled by: " + job.getCancelledBy()
                + "\nReason: " + job.getCancellationReason();
        }

        text += "\n\nResult: " + job.getResultSummary();

        JTextArea area = new JTextArea(text, 22, 65);

        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);

        JOptionPane.showMessageDialog(
            this,
            new JScrollPane(area),
            "Job details",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private Job selectedJob() {

        int row = jobsTable.getSelectedRow();

        if (row < 0) {
            throw new IllegalArgumentException(
                "Select a job first."
            );
        }

        return displayedJobs.get(
            jobsTable.convertRowIndexToModel(row)
        );
    }

    private JobPrediction predictionFor(int jobId) {

        for (int i = 0; i < predictions.size(); i++) {

            if (predictions.get(i).getJobId() == jobId) {
                return predictions.get(i);
            }
        }

        return null;
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