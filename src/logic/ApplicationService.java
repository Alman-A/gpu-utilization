package logic;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;

import model.AccessRequest;
import model.GPU;
import model.Job;
import model.JobPrediction;
import model.Student;
import model.TaskSize;
import model.TimeAllowance;
import model.User;

import storage.AppState;
import storage.DataStore;

public class ApplicationService {

    private DataStore store;
    private AppState state;
    private Scheduler scheduler;
    private AccountService accounts;

    private User currentUser;
    private boolean dirty;

    public ApplicationService(DataStore store) throws IOException {

        if (store == null) {
            throw new IllegalArgumentException("A data store is required.");
        }

        this.store = store;
        state = store.load();

        if (state == null) {
            throw new IOException("The data store returned no application state.");
        }

        scheduler = new Scheduler(state);
        accounts = new AccountService(state);

        currentUser = null;
        dirty = false;
    }

    public User login(String username, String password) {

        requireSavedState();

        currentUser = null;
        currentUser = accounts.authenticate(username, password);

        return currentUser;
    }

    public void logout() {

        requireSavedState();
        currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public AccessRequest register(String id, String name, String email, String phone, String password) throws IOException {

        requireSavedState();

        if (currentUser != null) {
            throw new SecurityException("Log out before registering an account.");
        }

        AccessRequest request = accounts.register(
            id, name, email, phone, password
        );

        saveNow();

        return request;
    }

    public void initializeClock(LocalDateTime start)
            throws IOException {

        requireAdmin();
        requireSavedState();

        state.getClock().initialize(start);

        saveNow();
    }

    public Student approveRequest(String id, int initialMinutes)
            throws IOException {

        requireAdmin();
        requireSavedState();

        Student student = accounts.approve(id, initialMinutes);

        saveNow();

        return student;
    }

    public void rejectRequest(String id) throws IOException {

        requireAdmin();
        requireSavedState();

        accounts.reject(id);

        saveNow();
    }

    public void grantTime(String id, int minutes, String reason)
            throws IOException {

        requireAdmin();
        requireSavedState();

        if (!isClockInitialized()) {
            throw new IllegalStateException("Initialize the simulation clock first.");
        }

        accounts.grantTime(
            id,
            minutes,
            reason,
            currentUser.getUsername(),
            getCurrentTime()
        );

        saveNow();
    }

    public Job submitJob(String title, String applicationName,
                         String description, TaskSize size)
            throws IOException {

        Student student = requireStudent();

        requireSavedState();

        Job job = scheduler.submit(
            student, title, applicationName, description, size
        );

        saveNow();

        return job;
    }

    public void cancelJob(int jobId, String reason)
            throws IOException {

        requireLoggedIn();
        requireSavedState();

        Job job = state.findJob(jobId);

        if (job == null) {
            throw new IllegalArgumentException("Job was not found.");
        }

        if (!"ADMIN".equals(currentUser.getRole())
                && !job.getStudentId().equals(
                    currentUser.getUsername())) {

            throw new SecurityException("Students can cancel only their own jobs.");
        }

        scheduler.cancel(
            job, currentUser.getUsername(), reason
        );

        saveNow();
    }

    public void advanceMinute() throws IOException {

        requireAdmin();
        requireSavedState();

        scheduler.advanceMinute();

        saveNow();
    }

    public ArrayList<Job> getVisibleJobs(String query, String status) {

        requireLoggedIn();

        ArrayList<Job> result = new ArrayList<Job>();

        String search = "";

        if (query != null) {
            search = query.trim().toLowerCase();
        }

        for (int i = 0; i < state.getJobs().size(); i++) {

            Job job = state.getJobs().get(i);

            // Students can see only their own jobs.
            if (!"ADMIN".equals(currentUser.getRole())&& !job.getStudentId().equals(currentUser.getUsername())) {
                continue;
            }

            if (status != null && !status.equals(job.getStatus())) {
                continue;
            }

            if (!job.getDisplayId().toLowerCase().contains(search) && !job.getTitle().toLowerCase().contains(search)) {
                continue;
            }

            result.add(job);
        }

        return result;
    }

    public ArrayList<JobPrediction> getVisiblePredictions() {

        requireLoggedIn();

        ArrayList<JobPrediction> result =
            new ArrayList<JobPrediction>();

        if (!isClockInitialized()) {
            return result;
        }

        ArrayList<JobPrediction> all = scheduler.predictQueue();

        for (int i = 0; i < all.size(); i++) {

            JobPrediction prediction = all.get(i);

            Job job = state.findJob(prediction.getJobId());

            if ("ADMIN".equals(currentUser.getRole()) || job.getStudentId().equals(currentUser.getUsername())) {
                result.add(prediction);
            }
        }

        return result;
    }

    public TimeAllowance getMyAllowance() {

        TimeAllowance allowance = requireStudent().getAllowance();
        return new TimeAllowance(
            allowance.getAllocated(),
            allowance.getUsed(),
            allowance.getReserved()
        );
    }

    public ArrayList<AccessRequest> getRequests(String status) {

        requireAdmin();

        ArrayList<AccessRequest> result =
            new ArrayList<AccessRequest>();

        for (int i = 0; i < state.getRequests().size(); i++) {

            AccessRequest request = state.getRequests().get(i);

            if (status == null || status.equals(request.getStatus())) {
                result.add(request);
            }
        }

        return result;
    }

    public ArrayList<Student> getStudents() {

        requireAdmin();

        ArrayList<Student> result = new ArrayList<Student>();

        for (int i = 0; i < state.getStudents().size(); i++) {
            result.add(state.getStudents().get(i));
        }

        return result;
    }

    public Student findStudent(String id) {

        requireAdmin();

        return state.findStudent(id);
    }

    public ArrayList<Job> getStudentJobs(String id) {

        requireAdmin();

        Student student = state.findStudent(id);

        if (student == null) {
            throw new IllegalArgumentException("Student was not found.");
        }

        ArrayList<Job> result = new ArrayList<Job>();

        for (int i = 0; i < state.getJobs().size(); i++) {

            Job job = state.getJobs().get(i);

            if (student.getUniversityId().equals(job.getStudentId())) {
                result.add(job);
            }
        }

        return result;
    }

    public GPU[] getGpus() {

        requireAdmin();

        GPU[] original = state.getGpus();
        GPU[] copy = new GPU[original.length];

        for (int i = 0; i < original.length; i++) {

            copy[i] = new GPU(original[i].getId());

            if (!original[i].isFree()) {
                copy[i].assign(original[i].getCurrentJobId());
            }
        }

        return copy;
    }

    public int getBusyGpuCount() {

        requireAdmin();

        return scheduler.getBusyGpuCount();
    }

    public Double getUtilizationPercent() {

        requireAdmin();

        return scheduler.getUtilizationPercent();
    }

    public boolean isClockInitialized() {
        return state.getClock().isInitialized();
    }

    public LocalDateTime getCurrentTime() {
        return state.getClock().getCurrentTime();
    }

    public void saveNow() throws IOException {

        dirty = true;

        store.save(state);

        dirty = false;
    }

    public boolean isDirty() {
        return dirty;
    }

    private void requireLoggedIn() {

        if (currentUser == null) {
            throw new SecurityException("Sign in first.");
        }
    }

    private void requireAdmin() {

        requireLoggedIn();

        if (!"ADMIN".equals(currentUser.getRole())) {
            throw new SecurityException("Only the administrator can do this.");
        }
    }

    private Student requireStudent() {

        requireLoggedIn();

        if (!(currentUser instanceof Student)) {
            throw new SecurityException("Only approved students can do this.");
        }

        return (Student) currentUser;
    }

    private void requireSavedState() {

        if (dirty) {
            throw new IllegalStateException("Changes are unsaved. Retry saving before another action.");
        }
    }
}