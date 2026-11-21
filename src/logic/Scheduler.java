package logic;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;

import model.AccessRequest;
import model.GPU;
import model.Job;
import model.JobPrediction;
import model.JobStatus;
import model.RequestStatus;
import model.Student;
import model.TaskSize;

import storage.AppState;

public class Scheduler {

    private AppState state;

    public Scheduler(AppState state) {

        if (state == null) {
            throw new IllegalArgumentException("Application state is required.");
        }

        this.state = state;
    }

    public Job submit(Student student, String title,
                      String applicationName, String description,
                      TaskSize size) {

        requireClock();

        if (student == null || state.findStudent(student.getUniversityId()) != student) {
            throw new IllegalArgumentException("Student is not registered.");
        }

        AccessRequest request = state.findRequest(student.getUniversityId());

        if (request == null || !RequestStatus.APPROVED.equals(request.getStatus())) {
            throw new IllegalStateException("Student is not approved.");
        }

        requireText(title, "Title");
        requireText(applicationName, "Application name");
        requireText(description, "Description");

        if (size == null) {
            throw new IllegalArgumentException("A task size is required.");
        }

        int duration = size.getDuration();

        if (duration > student.getAllowance().getAvailable()) {
            throw new IllegalArgumentException("Not enough available GPU minutes.");
        }

        Job job = new Job(
            state.takeNextJobId(),
            student.getUniversityId(),
            title,
            applicationName,
            description,
            size,
            state.getClock().getCurrentTime()
        );

        student.getAllowance().reserve(duration);
        state.getJobs().add(job);

        dispatchWaitingJobs();

        return job;
    }

    public void advanceMinute() {

        requireClock();

        state.getClock().advanceMinute();

        LocalDateTime now = state.getClock().getCurrentTime();

        for (int i = 0; i < state.getGpus().length; i++) {

            GPU gpu = state.getGpus()[i];

            if (!gpu.isFree()) {

                Job job = state.findJob(gpu.getCurrentJobId());

                int runtime = job.getRuntime(now);

                if (runtime == job.getEstimatedDuration()) {

                    Student student = state.findStudent(
                        job.getStudentId()
                    );

                    requireReservation(student, job);

                    job.complete(now);
                    settleJob(student, job);
                    gpu.release();
                }
            }
        }

        dispatchWaitingJobs();
    }

    public void cancel(Job job, String actorUsername, String reason) {

        requireClock();

        if (job == null || state.findJob(job.getJobId()) != job) {
            throw new IllegalArgumentException("Job does not belong to this system."
            );
        }

        boolean running = JobStatus.RUNNING.equals(job.getStatus());

        if (!running && !JobStatus.QUEUED.equals(job.getStatus())) {
            throw new IllegalStateException("Only queued or running jobs can be cancelled."
            );
        }

        requireText(actorUsername, "Cancellation user");
        requireText(reason, "Cancellation reason");

        actorUsername = actorUsername.trim();

        // ApplicationService supplies the authenticated username.
        if (!"admin".equals(actorUsername)
                && !job.getStudentId().equals(actorUsername)) {
            throw new SecurityException("Students can cancel only their own jobs.");
        }

        Student student = state.findStudent(job.getStudentId());

        requireReservation(student, job);

        GPU gpu = null;

        if (running) {

            gpu = state.getGpus()[job.getAssignedGpuId() - 1];

            if (gpu.getCurrentJobId() == null
                    || gpu.getCurrentJobId() != job.getJobId()) {
                throw new IllegalStateException("Job and GPU assignment do not match.");
            }
        }

        job.cancel(
            state.getClock().getCurrentTime(),
            actorUsername,
            reason
        );

        settleJob(student, job);

        if (running) {
            gpu.release();
        }

        dispatchWaitingJobs();
    }

    public ArrayList<Job> getQueuedJobs() {

        ArrayList<Job> queue = new ArrayList<Job>();

        for (int i = 0; i < state.getJobs().size(); i++) {

            Job job = state.getJobs().get(i);

            if (JobStatus.QUEUED.equals(job.getStatus())) {
                queue.add(job);
            }
        }

        // Selection sort: submission time, then numeric job ID.
        for (int i = 0; i < queue.size() - 1; i++) {

            int smallest = i;

            for (int j = i + 1; j < queue.size(); j++) {

                Job candidate = queue.get(j);
                Job first = queue.get(smallest);

                if (candidate.getSubmissionTime().isBefore(
                        first.getSubmissionTime())
                        || (candidate.getSubmissionTime().equals(
                            first.getSubmissionTime())
                        && candidate.getJobId() < first.getJobId())) {

                    smallest = j;
                }
            }

            Job temp = queue.get(i);

            queue.set(i, queue.get(smallest));
            queue.set(smallest, temp);
        }

        return queue;
    }

    public ArrayList<JobPrediction> predictQueue() {

        requireClock();

        LocalDateTime now = state.getClock().getCurrentTime();

        GPU[] gpus = state.getGpus();

        LocalDateTime[] freeAt = new LocalDateTime[gpus.length];

        for (int i = 0; i < gpus.length; i++) {

            if (gpus[i].isFree()) {

                freeAt[i] = now;

            } else {

                Job running = state.findJob(
                    gpus[i].getCurrentJobId()
                );

                freeAt[i] = running.getStartTime().plusMinutes(
                    running.getEstimatedDuration()
                );
            }
        }

        ArrayList<Job> queue = getQueuedJobs();

        ArrayList<JobPrediction> predictions =
            new ArrayList<JobPrediction>();

        for (int i = 0; i < queue.size(); i++) {

            int earliest = 0;

            for (int g = 1; g < gpus.length; g++) {

                if (freeAt[g].isBefore(freeAt[earliest])
                        || (freeAt[g].equals(freeAt[earliest])
                        && gpus[g].getId() < gpus[earliest].getId())) {

                    earliest = g;
                }
            }

            Job job = queue.get(i);

            LocalDateTime start = freeAt[earliest];

            LocalDateTime finish = start.plusMinutes(
                job.getEstimatedDuration()
            );

            long wait = Duration.between(now, start).toMinutes();

            if (wait < 0 || wait > Integer.MAX_VALUE) {
                throw new IllegalStateException("Predicted wait is outside the supported range.");
            }

            JobPrediction prediction = new JobPrediction(
                job.getJobId(),
                i + 1,
                gpus[earliest].getId(),
                (int) wait,
                start,
                finish
            );

            predictions.add(prediction);
            freeAt[earliest] = finish;
        }

        return predictions;
    }

    public int getBusyGpuCount() {

        int count = 0;

        for (int i = 0; i < state.getGpus().length; i++) {

            if (!state.getGpus()[i].isFree()) {
                count++;
            }
        }

        return count;
    }

    public Double getUtilizationPercent() {

        long elapsed = state.getClock().getElapsedMinutes();

        if (elapsed == 0) {
            return null;
        }

        long busyMinutes = 0;

        LocalDateTime now = state.getClock().getCurrentTime();

        for (int i = 0; i < state.getJobs().size(); i++) {

            busyMinutes += state.getJobs().get(i).getRuntime(now);
        }

        return 100.0 * busyMinutes / (4.0 * elapsed);
    }

    private void dispatchWaitingJobs() {

        ArrayList<Job> queue = getQueuedJobs();

        int next = 0;

        for (int i = 0;
                i < state.getGpus().length && next < queue.size();
                i++) {

            GPU gpu = state.getGpus()[i];

            if (gpu.isFree()) {

                Job job = queue.get(next);

                job.start(
                    gpu.getId(),
                    state.getClock().getCurrentTime()
                );

                gpu.assign(job.getJobId());

                next++;
            }
        }
    }

    private void settleJob(Student student, Job job) {

        student.getAllowance().settle(
            job.getEstimatedDuration(),
            job.getCharge()
        );
    }

    private void requireReservation(Student student, Job job) {

        if (student == null
                || student.getAllowance().getReserved()
                    < job.getEstimatedDuration()) {

            throw new IllegalStateException("The job reservation is missing.");
        }
    }

    private void requireClock() {

        if (!state.getClock().isInitialized()) {
            throw new IllegalStateException("Initialize the simulation clock first.");
        }
    }

    private void requireText(String value, String field) {

        if (value == null || value.trim().equals("")) {
            throw new IllegalArgumentException(field + " is required.");
        }
    }
}