package storage;

import java.util.ArrayList;

import logic.SimulationClock;

import model.AccessRequest;
import model.GPU;
import model.Job;
import model.Student;
import model.TimeGrant;

public class AppState {

    private ArrayList<Student> students;
    private ArrayList<AccessRequest> requests;
    private ArrayList<TimeGrant> grants;
    private ArrayList<Job> jobs;

    private GPU[] gpus;
    private SimulationClock clock;

    private int nextJobNumber;

    public AppState() {

        students = new ArrayList<Student>();
        requests = new ArrayList<AccessRequest>();
        grants = new ArrayList<TimeGrant>();
        jobs = new ArrayList<Job>();

        gpus = new GPU[4];

        for (int i = 0; i < gpus.length; i++) {
            gpus[i] = new GPU(i + 1);
        }

        clock = new SimulationClock();

        nextJobNumber = 101;
    }

    public ArrayList<Student> getStudents() {
        return students;
    }

    public ArrayList<AccessRequest> getRequests() {
        return requests;
    }

    public ArrayList<TimeGrant> getGrants() {
        return grants;
    }

    public ArrayList<Job> getJobs() {
        return jobs;
    }

    public GPU[] getGpus() {
        return gpus;
    }

    public SimulationClock getClock() {
        return clock;
    }

    public Student findStudent(String universityId) {

        if (universityId == null) {
            return null;
        }

        universityId = universityId.trim();

        for (int i = 0; i < students.size(); i++) {

            Student student = students.get(i);

            if (student.getUniversityId().equals(universityId)) {
                return student;
            }
        }

        return null;
    }

    public AccessRequest findRequest(String universityId) {

        if (universityId == null) {
            return null;
        }

        universityId = universityId.trim();

        for (int i = 0; i < requests.size(); i++) {

            AccessRequest request = requests.get(i);

            if (request.getUniversityId().equals(universityId)) {
                return request;
            }
        }

        return null;
    }

    public Job findJob(int jobId) {

        for (int i = 0; i < jobs.size(); i++) {

            Job job = jobs.get(i);

            if (job.getJobId() == jobId) {
                return job;
            }
        }

        return null;
    }

    public int getNextJobNumber() {
        return nextJobNumber;
    }

    public int takeNextJobId() {

        if (nextJobNumber == Integer.MAX_VALUE) {
            throw new IllegalStateException("No more job IDs can be generated.");
        }

        int jobId = nextJobNumber;

        nextJobNumber++;

        return jobId;
    }

    public void restoreNextJobNumber(int savedNextJobNumber) {

        if (savedNextJobNumber < nextJobNumber) {
            throw new IllegalArgumentException("The next job number cannot move backwards.");
        }

        for (int i = 0; i < jobs.size(); i++) {

            if (savedNextJobNumber <= jobs.get(i).getJobId()) {
                throw new IllegalArgumentException("The next job number must exceed every saved job ID.");
            }
        }

        nextJobNumber = savedNextJobNumber;
    }
}
