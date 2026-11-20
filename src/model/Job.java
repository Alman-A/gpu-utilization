package model;

import java.time.LocalDateTime;
import java.time.Duration;

public class Job {

    private int jobId;
    private String studentId;
    private String title;
    private String applicationName;
    private String description;
    private TaskSize taskSize;
    private String status;

    private Integer assignedGpuId;
    private LocalDateTime submissionTime;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;

    private LocalDateTime cancellationTime;
    private String cancelledBy;
    private String cancellationReason;
    private String resultSummary;

    public Job(int jobId, String studentId, String title, String applicationName, String description, TaskSize taskSize, LocalDateTime submissionTime) {

        if (jobId <= 0 || taskSize == null || submissionTime == null) {
            throw new IllegalArgumentException("Invalid job details.");
        }

        this.jobId = jobId;
        this.studentId = requireText(studentId, "Student ID");
        this.title = requireText(title, "Title");
        this.applicationName = requireText(applicationName, "Application name"
        );
        this.description = requireText(description, "Description");
        this.taskSize = taskSize;
        this.submissionTime = submissionTime;

        status = JobStatus.QUEUED;
        assignedGpuId = null;
        startTime = null;
        finishTime = null;

        cancellationTime = null;
        cancelledBy = null;
        cancellationReason = null;
        resultSummary = "";
    }

    private static String requireText(String value, String field) {

        if (value == null || value.trim().equals("")) {
            throw new IllegalArgumentException(field + " is required.");
        }

        return value.trim();
    }

    public void start(int gpuId, LocalDateTime now) {

        if (!status.equals(JobStatus.QUEUED)) {
            throw new IllegalStateException("Only queued jobs can start.");
        }

        if (gpuId < 1 || gpuId > 4 || now == null|| now.isBefore(submissionTime)) {
            throw new IllegalArgumentException("Invalid GPU or start time.");
        }

        assignedGpuId = gpuId;
        startTime = now;
        status = JobStatus.RUNNING;
    }

    public void complete(LocalDateTime now) {

        if (!status.equals(JobStatus.RUNNING)) {
            throw new IllegalStateException("Only running jobs can complete.");
        }

        if (now == null
                || !now.equals(startTime.plusMinutes(getEstimatedDuration()))) {
            throw new IllegalArgumentException("Invalid completion time.");
        }

        finishTime = now;
        status = JobStatus.COMPLETED;

        resultSummary = "Simulated execution completed successfully; " + getEstimatedDuration() + " GPU minutes.";
    }

    public void cancel(LocalDateTime now, String actor, String reason) {

        if (!status.equals(JobStatus.QUEUED) && !status.equals(JobStatus.RUNNING)) {
            throw new IllegalStateException("This job cannot be cancelled.");
        }

        String validActor = requireText(actor, "Cancelled by");
        String validReason = requireText(reason, "Cancellation reason");

        if (now == null || now.isBefore(submissionTime)) {
            throw new IllegalArgumentException("Invalid cancellation time.");
        }

        int charge = getRuntime(now);

        if (status.equals(JobStatus.RUNNING)) {
            finishTime = now;
        }

        cancellationTime = now;
        cancelledBy = validActor;
        cancellationReason = validReason;
        status = JobStatus.CANCELLED;

        resultSummary = "Cancelled; " + charge + " GPU minutes charged.";
    }

    public int getRuntime(LocalDateTime now) {

        if (startTime == null) {
            return 0;
        }

        LocalDateTime end = finishTime;

        if (end == null) {
            end = now;
        }

        if (end == null || end.isBefore(startTime)) {
            throw new IllegalArgumentException("Invalid runtime end time.");
        }

        long minutes = Duration.between(startTime, end).toMinutes();

        if (minutes > getEstimatedDuration()) {
            throw new IllegalStateException("Runtime exceeds job duration.");
        }

        return (int) minutes;
    }

    public Integer getQueueWaitMinutes() {

        if (startTime == null) {
            return null;
        }

        long minutes = Duration.between(
            submissionTime, startTime
        ).toMinutes();

        if (minutes > Integer.MAX_VALUE) {
            throw new IllegalStateException("Queue wait exceeds the integer limit.");
        }

        return (int) minutes;
    }

    public int getCharge() {

        if (status.equals(JobStatus.COMPLETED)) {
            return getEstimatedDuration();
        }

        if (status.equals(JobStatus.CANCELLED) && startTime != null) {
            return getRuntime(finishTime);
        }

        return 0;
    }

    public int getJobId() { 
        return jobId; 
    }

    public String getDisplayId() { 
        return "J" + jobId; 
    }

    public String getStudentId() { 
        return studentId; 
    }

    public String getTitle() { 
        return title; 
    }

    public String getApplicationName() { 
        return applicationName; 
    }

    public String getDescription() { 
        return description; 
    }

    public TaskSize getTaskSize() { 
        return taskSize; 
    }

    public String getStatus() { 
        return status; 
    }

    public Integer getAssignedGpuId() { 
        return assignedGpuId; 
    }

    public LocalDateTime getSubmissionTime() { 
        return submissionTime; 
    }

    public LocalDateTime getStartTime() { 
        return startTime; 
    }

    public LocalDateTime getFinishTime() { 
        return finishTime; 
    }

    public LocalDateTime getCancellationTime() {
        return cancellationTime;
    }

    public String getCancelledBy() { 
        return cancelledBy; 
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public String getResultSummary() { 
        return resultSummary; 
    }

    public int getEstimatedDuration() {
        return taskSize.getDuration();
    }

    public int getWorkloadUnits() {
        return taskSize.getDuration();
    }
}