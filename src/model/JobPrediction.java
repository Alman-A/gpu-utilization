package model;

import java.time.LocalDateTime;

public class JobPrediction {

    private final int jobId;
    private final int queuePosition;
    private final int predictedGpuId;
    private final int remainingWaitMinutes;
    private final LocalDateTime predictedStart;
    private final LocalDateTime predictedFinish;

    public JobPrediction(int jobId, int queuePosition,
                         int predictedGpuId, int remainingWaitMinutes,
                         LocalDateTime predictedStart,
                         LocalDateTime predictedFinish) {

        if (jobId <= 0 || queuePosition <= 0) {
            throw new IllegalArgumentException("Job ID and queue position must be positive.");
        }

        if (predictedGpuId < 1 || predictedGpuId > 4) {
            throw new IllegalArgumentException("Predicted GPU ID must be between 1 and 4.");
        }

        if (remainingWaitMinutes < 0) {
            throw new IllegalArgumentException("Remaining wait cannot be negative.");
        }

        if (predictedStart == null || predictedFinish == null) {
            throw new IllegalArgumentException("Both predicted timestamps are required.");
        }

        if (!predictedFinish.isAfter(predictedStart)) {
            throw new IllegalArgumentException("Predicted finish must be after predicted start.");
        }

        this.jobId = jobId;
        this.queuePosition = queuePosition;
        this.predictedGpuId = predictedGpuId;
        this.remainingWaitMinutes = remainingWaitMinutes;
        this.predictedStart = predictedStart;
        this.predictedFinish = predictedFinish;
    }

    public int getJobId() {
        return jobId;
    }

    public int getQueuePosition() {
        return queuePosition;
    }

    public int getPredictedGpuId() {
        return predictedGpuId;
    }

    public int getRemainingWaitMinutes() {
        return remainingWaitMinutes;
    }

    public LocalDateTime getPredictedStart() {
        return predictedStart;
    }

    public LocalDateTime getPredictedFinish() {
        return predictedFinish;
    }
}