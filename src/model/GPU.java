package model;

public class GPU {
    private int id;
    private Integer currentJobId;

    public GPU(int id) {
        if(id < 1 || id > 4) {
            throw new IllegalArgumentException("GPU ID must be between 1 and 4.");

        }
        this.id = id;
        currentJobId = null;
    }

    public int getID() {
        return id;
    }

    public Integer getCurrentJobID() {
        return currentJobId;
    }

    public boolean isFree() {
        return currentJobId == null;
    }

    public void assign(int jobId) {
        if (jobId <= 0) {
            throw new IllegalArgumentException("Job ID must be positive.");

        }
        if (!isFree()) {
            throw new IllegalStateException("This GPU is already running a job.");

        }
        currentJobId = jobId;
    }

    public void release() {
        if (isFree()){
            throw new IllegalStateException("This GPU is already free.");
        }
        currentJobId = null;
    }
}
