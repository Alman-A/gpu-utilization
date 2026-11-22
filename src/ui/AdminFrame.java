package ui;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.*;

import app.Main;
import logic.ApplicationService;
import model.*;

public class AdminFrame extends JFrame implements ActionListener {

    private Main navigation;
    private ApplicationService service;

    private JTextArea outputArea;
    private JTextField searchField, statusField;

    private JButton dashboardButton, requestsButton, studentsButton;
    private JButton jobsButton, approveButton, rejectButton;
    private JButton grantButton, cancelButton, advanceButton;
    private JButton saveButton, logoutButton;

    public AdminFrame(Main navigation, ApplicationService service) {

        if (service.getCurrentUser() == null
                || !service.getCurrentUser().getRole().equals("ADMIN")) {

            throw new SecurityException(
                    "Administrator login required.");
        }

        this.navigation = navigation;
        this.service = service;

        setTitle("GPU Management - Administrator");
        setSize(1100, 650);
        setLayout(new GridLayout(1, 2));

        JPanel controls = new JPanel();
        controls.setLayout(new GridLayout(11, 2));

        searchField = new JTextField();
        statusField = new JTextField();

        dashboardButton = new JButton("Dashboard");
        requestsButton = new JButton("View Requests");
        studentsButton = new JButton("Find Student");
        jobsButton = new JButton("View Jobs");

        approveButton = new JButton("Approve Request");
        rejectButton = new JButton("Reject Request");
        grantButton = new JButton("Grant Minutes");
        cancelButton = new JButton("Cancel Job");

        advanceButton = new JButton("Advance Minute");
        saveButton = new JButton("Retry Save");
        logoutButton = new JButton("Log Out");

        controls.add(new JLabel("Administrator"));
        controls.add(new JLabel("GPU Management"));

        controls.add(new JLabel("Job ID / title search:"));
        controls.add(searchField);

        controls.add(new JLabel("Status (blank = all):"));
        controls.add(statusField);

        controls.add(new JLabel("Jobs: QUEUED, RUNNING,"));
        controls.add(new JLabel("COMPLETED, CANCELLED"));

        controls.add(new JLabel("Requests: PENDING,"));
        controls.add(new JLabel("APPROVED, REJECTED"));

        controls.add(dashboardButton);
        controls.add(requestsButton);

        controls.add(studentsButton);
        controls.add(jobsButton);

        controls.add(approveButton);
        controls.add(rejectButton);

        controls.add(grantButton);
        controls.add(cancelButton);

        controls.add(advanceButton);
        controls.add(saveButton);

        controls.add(logoutButton);
        controls.add(new JLabel(""));

        outputArea = new JTextArea();
        outputArea.setEditable(false);

        add(controls);
        add(new JScrollPane(outputArea));

        dashboardButton.addActionListener(this);
        requestsButton.addActionListener(this);
        studentsButton.addActionListener(this);
        jobsButton.addActionListener(this);
        approveButton.addActionListener(this);
        rejectButton.addActionListener(this);
        grantButton.addActionListener(this);
        cancelButton.addActionListener(this);
        advanceButton.addActionListener(this);
        saveButton.addActionListener(this);
        logoutButton.addActionListener(this);

        showDashboard();
    }

    public void actionPerformed(ActionEvent e) {

        try {

            if (e.getSource() == dashboardButton) {
                showDashboard();

            } else if (e.getSource() == requestsButton) {
                showRequests();

            } else if (e.getSource() == studentsButton) {
                findStudent();

            } else if (e.getSource() == jobsButton) {
                showJobs();

            } else if (e.getSource() == approveButton) {
                approveRequest();

            } else if (e.getSource() == rejectButton) {
                rejectRequest();

            } else if (e.getSource() == grantButton) {
                grantMinutes();

            } else if (e.getSource() == cancelButton) {
                cancelJob();

            } else if (e.getSource() == advanceButton) {
                service.advanceMinute();
                showDashboard();

            } else if (e.getSource() == saveButton) {
                service.saveNow();
                showDashboard();

                JOptionPane.showMessageDialog(
                        this, "Changes saved.");

            } else if (e.getSource() == logoutButton) {
                service.logout();
                navigation.showLogin();
            }

        } catch (IOException exception) {

            showDashboard();

            JOptionPane.showMessageDialog(
                    this,
                    "Saving failed. The change may already be in memory.\n"
                    + "Click Retry Save before another action.\n"
                    + exception.getMessage());

        } catch (IllegalArgumentException exception) {

            JOptionPane.showMessageDialog(
                    this, exception.getMessage());

        } catch (IllegalStateException exception) {

            JOptionPane.showMessageDialog(
                    this, exception.getMessage());

        } catch (SecurityException exception) {

            JOptionPane.showMessageDialog(
                    this, exception.getMessage());
        }
    }

    private void showDashboard() {

        outputArea.setText("ADMINISTRATOR DASHBOARD\n\n");

        if (service.isDirty()) {
            outputArea.append(
                    "UNSAVED CHANGES - click Retry Save\n\n");
        }

        if (service.isClockInitialized()) {
            outputArea.append(
                    "Simulation time: "
                    + service.getCurrentTime() + "\n");
        } else {
            outputArea.append("Simulation clock is not initialized.\n");
        }

        outputArea.append(
                "Busy GPUs: "
                + service.getBusyGpuCount() + " / 4\n");

        Double utilization = service.getUtilizationPercent();

        if (utilization == null) {
            outputArea.append("Utilization: N/A\n");
        } else {
            outputArea.append(
                    String.format(
                            "Utilization: %.1f%%\n", utilization));
        }

        ArrayList<Job> jobs = service.getVisibleJobs(null, null);
        GPU[] gpus = service.getGpus();

        outputArea.append("\nGPUs\n");

        for (int i = 0; i < gpus.length; i++) {

            GPU gpu = gpus[i];

            if (gpu.isFree()) {

                outputArea.append(
                        "GPU " + gpu.getId() + ": Available\n");

            } else {

                outputArea.append(
                        "GPU " + gpu.getId()
                        + ": Job J" + gpu.getCurrentJobId());

                for (int j = 0; j < jobs.size(); j++) {

                    Job job = jobs.get(j);

                    if (job.getJobId()
                            == gpu.getCurrentJobId().intValue()) {

                        int remaining = job.getEstimatedDuration()
                                - job.getRuntime(
                                        service.getCurrentTime());

                        outputArea.append(
                                "\nRemaining: " + remaining + " minutes"
                                + "\nExpected finish: "
                                + job.getStartTime().plusMinutes(
                                        job.getEstimatedDuration()));
                    }
                }

                outputArea.append("\n");
            }
        }

        int queued = 0;
        int running = 0;
        int completed = 0;
        int cancelled = 0;

        for (int i = 0; i < jobs.size(); i++) {

            String status = jobs.get(i).getStatus();

            if (status.equals(JobStatus.QUEUED)) {
                queued++;

            } else if (status.equals(JobStatus.RUNNING)) {
                running++;

            } else if (status.equals(JobStatus.COMPLETED)) {
                completed++;

            } else if (status.equals(JobStatus.CANCELLED)) {
                cancelled++;
            }
        }

        outputArea.append("\nQueued: " + queued);
        outputArea.append("\nRunning: " + running);
        outputArea.append("\nCompleted: " + completed);
        outputArea.append("\nCancelled: " + cancelled + "\n");

        outputArea.append("\nSTUDENT BALANCES\n");

        ArrayList<Student> students = service.getStudents();

        for (int i = 0; i < students.size(); i++) {
            displayStudent(students.get(i));
        }

        outputArea.append("\nQUEUE PREDICTIONS\n");

        ArrayList<JobPrediction> predictions =
                service.getVisiblePredictions();

        for (int i = 0; i < predictions.size(); i++) {

            JobPrediction prediction = predictions.get(i);

            outputArea.append(
                    "\nJ" + prediction.getJobId()
                    + " | Position: "
                    + prediction.getQueuePosition()
                    + "\nGPU: " + prediction.getPredictedGpuId()
                    + "\nWait: "
                    + prediction.getRemainingWaitMinutes()
                    + " minutes"
                    + "\nStart: " + prediction.getPredictedStart()
                    + "\nFinish: " + prediction.getPredictedFinish()
                    + "\n");

            for (int j = 0; j < jobs.size(); j++) {

                Job job = jobs.get(j);

                if (job.getJobId() == prediction.getJobId()) {
                    outputArea.append(
                            "Student: " + job.getStudentId()
                            + "\nApplication: "
                            + job.getApplicationName() + "\n");
                }
            }
        }

        outputArea.append("\nCOMPLETED JOBS\n");

        for (int i = 0; i < jobs.size(); i++) {

            if (jobs.get(i).getStatus().equals(JobStatus.COMPLETED)) {
                displayJob(jobs.get(i));
            }
        }

        outputArea.setCaretPosition(0);
    }

    private void showRequests() {

        String status = statusField.getText().trim().toUpperCase();

        if (status.equals("")) {
            status = null;

        } else if (!status.equals(RequestStatus.PENDING)
                && !status.equals(RequestStatus.APPROVED)
                && !status.equals(RequestStatus.REJECTED)) {

            throw new IllegalArgumentException(
                    "Use PENDING, APPROVED or REJECTED.");
        }

        ArrayList<AccessRequest> requests =
                service.getRequests(status);

        outputArea.setText("REGISTRATION REQUESTS\n");

        for (int i = 0; i < requests.size(); i++) {

            AccessRequest request = requests.get(i);

            outputArea.append(
                    "\nID: " + request.getUniversityId()
                    + "\nName: " + request.getName()
                    + "\nEmail: " + request.getEmail()
                    + "\nPhone: " + request.getPhone()
                    + "\nPassword: ********"
                    + "\nStatus: " + request.getStatus()
                    + "\n");
        }

        if (requests.size() == 0) {
            outputArea.append("\nNo matching requests.");
        }

        outputArea.setCaretPosition(0);
    }

    private void findStudent() {

        String id = JOptionPane.showInputDialog(
                this, "Enter university ID:");

        if (id == null) {
            return;
        }

        Student student = service.findStudent(id.trim());

        if (student == null) {
            throw new IllegalArgumentException(
                    "Student was not found.");
        }

        outputArea.setText("STUDENT DETAILS\n");

        displayStudent(student);

        outputArea.append(
                "\nEmail: " + student.getEmail()
                + "\nPhone: " + student.getPhone()
                + "\n\nSTUDENT JOBS\n");

        ArrayList<Job> jobs =
                service.getStudentJobs(student.getUniversityId());

        for (int i = 0; i < jobs.size(); i++) {
            displayJob(jobs.get(i));
        }

        if (jobs.size() == 0) {
            outputArea.append("\nNo jobs found.");
        }

        outputArea.setCaretPosition(0);
    }

    private void displayStudent(Student student) {

        TimeAllowance allowance = student.getAllowance();

        outputArea.append(
                "\n" + student.getUniversityId()
                + " - " + student.getName()
                + "\nAllocated: " + allowance.getAllocated()
                + " | Used: " + allowance.getUsed()
                + "\nReserved: " + allowance.getReserved()
                + " | Available: " + allowance.getAvailable()
                + "\n");
    }

    private void showJobs() {

        String query = searchField.getText().trim();
        String status = statusField.getText().trim().toUpperCase();

        if (status.equals("")) {
            status = null;

        } else if (!status.equals(JobStatus.QUEUED)
                && !status.equals(JobStatus.RUNNING)
                && !status.equals(JobStatus.COMPLETED)
                && !status.equals(JobStatus.CANCELLED)) {

            throw new IllegalArgumentException(
                    "Use QUEUED, RUNNING, COMPLETED or CANCELLED.");
        }

        ArrayList<Job> jobs =
                service.getVisibleJobs(query, status);

        outputArea.setText("JOBS\n");

        for (int i = 0; i < jobs.size(); i++) {
            displayJob(jobs.get(i));
        }

        if (jobs.size() == 0) {
            outputArea.append("\nNo matching jobs.");
        }

        outputArea.setCaretPosition(0);
    }

    private void displayJob(Job job) {

        outputArea.append(
                "\n" + job.getDisplayId()
                + " - " + job.getTitle()
                + "\nStudent: " + job.getStudentId()
                + "\nApplication: " + job.getApplicationName()
                + "\nDescription: " + job.getDescription()
                + "\nTask size: " + job.getTaskSize()
                + "\nStatus: " + job.getStatus()
                + "\nDuration: "
                + job.getEstimatedDuration() + " minutes"
                + "\nGPU: " + job.getAssignedGpuId()
                + "\nSubmitted: " + job.getSubmissionTime()
                + "\nStarted: " + job.getStartTime()
                + "\nFinished: " + job.getFinishTime()
                + "\nQueue wait: " + job.getQueueWaitMinutes()
                + "\nRuntime: "
                + job.getRuntime(service.getCurrentTime())
                + "\nCharged minutes: " + job.getCharge()
                + "\nResult: " + job.getResultSummary()
                + "\n");

        if (job.getStatus().equals(JobStatus.CANCELLED)) {

            outputArea.append(
                    "Cancelled at: " + job.getCancellationTime()
                    + "\nCancelled by: " + job.getCancelledBy()
                    + "\nReason: " + job.getCancellationReason()
                    + "\n");
        }
    }

    private void approveRequest() throws IOException {

        String id = JOptionPane.showInputDialog(
                this, "University ID to approve:");

        if (id == null) {
            return;
        }

        String input = JOptionPane.showInputDialog(
                this, "Initial minutes:", "100");

        if (input == null) {
            return;
        }

        int minutes = readPositiveNumber(input);

        service.approveRequest(id.trim(), minutes);

        showDashboard();

        JOptionPane.showMessageDialog(
                this, "Request approved.");
    }

    private void rejectRequest() throws IOException {

        String id = JOptionPane.showInputDialog(
                this, "University ID to reject:");

        if (id == null) {
            return;
        }

        service.rejectRequest(id.trim());

        showDashboard();

        JOptionPane.showMessageDialog(
                this, "Request rejected.");
    }

    private void grantMinutes() throws IOException {

        String id = JOptionPane.showInputDialog(
                this, "University ID:");

        if (id == null) {
            return;
        }

        String input = JOptionPane.showInputDialog(
                this, "Minutes to grant:");

        if (input == null) {
            return;
        }

        int minutes = readPositiveNumber(input);

        String reason = JOptionPane.showInputDialog(
                this, "Reason for granting minutes:");

        if (reason == null) {
            return;
        }

        service.grantTime(id.trim(), minutes, reason);

        showDashboard();

        JOptionPane.showMessageDialog(
                this, "Minutes granted.");
    }

    private void cancelJob() throws IOException {

        String input = JOptionPane.showInputDialog(
                this, "Enter numeric job ID, for example 101:");

        if (input == null) {
            return;
        }

        int id = readPositiveNumber(input);

        ArrayList<Job> jobs =
                service.getVisibleJobs(null, null);

        Job selectedJob = null;

        for (int i = 0; i < jobs.size(); i++) {

            if (jobs.get(i).getJobId() == id) {
                selectedJob = jobs.get(i);
            }
        }

        if (selectedJob == null) {
            throw new IllegalArgumentException(
                    "Job was not found.");
        }

        if (!selectedJob.getStatus().equals(JobStatus.QUEUED)
                && !selectedJob.getStatus().equals(JobStatus.RUNNING)) {

            throw new IllegalStateException(
                    "Only queued or running jobs can be cancelled.");
        }

        int charge = 0;

        if (selectedJob.getStatus().equals(JobStatus.RUNNING)) {
            charge = selectedJob.getRuntime(
                    service.getCurrentTime());
        }

        String reason = JOptionPane.showInputDialog(
                this,
                "Cancelling " + selectedJob.getDisplayId()
                + " will charge " + charge + " minutes.\n"
                + "Enter a reason to confirm, or press Cancel:");

        if (reason == null) {
            return;
        }

        service.cancelJob(id, reason);

        showDashboard();

        JOptionPane.showMessageDialog(
                this, "Job cancelled.");
    }

    private int readPositiveNumber(String input) {

        int number;

        try {
            number = Integer.parseInt(input.trim());

        } catch (NumberFormatException exception) {

            throw new IllegalArgumentException(
                    "Enter a positive whole number.");
        }

        if (number <= 0) {
            throw new IllegalArgumentException(
                    "Enter a positive whole number.");
        }

        return number;
    }
}
