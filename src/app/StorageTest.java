package app;

import java.io.IOException;
import java.time.LocalDateTime;

import model.AccessRequest;
import model.Job;
import model.JobStatus;
import model.Student;
import model.TaskSize;
import model.TimeAllowance;
import model.TimeGrant;

import storage.AppState;
import storage.DataStore;
import storage.FileDataStore;

public class StorageTest {

    public static void main(String[] args) throws IOException {

        AppState state = new AppState();

        LocalDateTime initial =
                LocalDateTime.of(2026, 11, 20, 10, 0);

        state.getClock().initialize(initial);

        // 1. Create an approved student with 100 minutes.
        AccessRequest request = new AccessRequest(
                "TEST001",
                "Test Student",
                "test@example.com",
                "0501234567",
                "test123");

        request.approve();
        state.getRequests().add(request);

        Student student = new Student(
                request.getUniversityId(),
                request.getName(),
                request.getEmail(),
                request.getPhone(),
                request.getPassword(),
                new TimeAllowance(100));

        state.getStudents().add(student);

        // 2. Complete a five-minute job on GPU 1.
        Job completed = new Job(
                state.takeNextJobId(),
                student.getUniversityId(),
                "Completed test",
                "Test application",
                "A small test job.",
                new TaskSize("Small"),
                state.getClock().getCurrentTime());

        state.getJobs().add(completed);
        student.getAllowance().reserve(5);

        completed.start(1, state.getClock().getCurrentTime());
        state.getGpus()[0].assign(completed.getJobId());

        for (int i = 0; i < 5; i++) {
            state.getClock().advanceMinute();
        }

        completed.complete(state.getClock().getCurrentTime());
        student.getAllowance().settle(5, 5);
        state.getGpus()[0].release();

        // 3. Start another job and leave it running.
        Job running = new Job(
                state.takeNextJobId(),
                student.getUniversityId(),
                "Running test",
                "Test application",
                "First line.\nSecond line.",
                new TaskSize("Medium"),
                state.getClock().getCurrentTime());

        state.getJobs().add(running);
        student.getAllowance().reserve(15);

        running.start(1, state.getClock().getCurrentTime());
        state.getGpus()[0].assign(running.getJobId());

        for (int i = 0; i < 2; i++) {
            state.getClock().advanceMinute();
        }

        // 4. Grant another 20 minutes.
        student.getAllowance().grant(20);

        state.getGrants().add(new TimeGrant(
                student.getUniversityId(),
                20,
                "Storage test grant",
                "admin",
                state.getClock().getCurrentTime()));

        // 5. Save, then load into a new AppState.
        DataStore store =
                new FileDataStore("data/storage-test.txt");

        store.save(state);

        AppState loaded = store.load();

        // 6. Check the number of restored records.
        if (loaded.getStudents().size() != 1
                || loaded.getRequests().size() != 1
                || loaded.getGrants().size() != 1
                || loaded.getJobs().size() != 2
                || loaded.getGpus().length != 4) {

            throw new IllegalStateException(
                    "FAIL: Saved records are missing.");
        }

        System.out.println("PASS: All records restored.");

        // 7. Check student login data.
        Student restored = loaded.findStudent("TEST001");

        if (restored == null
                || !restored.checkPassword("test123")) {

            throw new IllegalStateException(
                    "FAIL: Student login data changed.");
        }

        System.out.println("PASS: Student login data restored.");

        // 8. Check the allowance.
        TimeAllowance allowance = restored.getAllowance();

        if (allowance.getAllocated() != 120
                || allowance.getUsed() != 5
                || allowance.getReserved() != 15
                || allowance.getAvailable() != 100) {

            throw new IllegalStateException(
                    "FAIL: Allowance values changed.");
        }

        System.out.println(
                "PASS: Balances are 120 allocated, "
                + "5 used, 15 reserved, 100 available.");

        // 9. Check both clock timestamps.
        if (!initial.equals(loaded.getClock().getInitialTime())
                || !initial.plusMinutes(7).equals(
                        loaded.getClock().getCurrentTime())
                || loaded.getClock().getElapsedMinutes() != 7) {

            throw new IllegalStateException(
                    "FAIL: Clock values changed.");
        }

        System.out.println(
                "PASS: Clock restored to 10:07, "
                + "with 7 elapsed minutes.");

        // 10. Check completed and running jobs.
        Job restoredCompleted =
                loaded.findJob(completed.getJobId());

        Job restoredRunning =
                loaded.findJob(running.getJobId());

        if (restoredCompleted == null
                || restoredRunning == null
                || !JobStatus.COMPLETED.equals(
                        restoredCompleted.getStatus())
                || restoredCompleted.getCharge() != 5
                || !JobStatus.RUNNING.equals(
                        restoredRunning.getStatus())
                || restoredRunning.getRuntime(
                        loaded.getClock().getCurrentTime()) != 2
                || !running.getDescription().equals(
                        restoredRunning.getDescription())) {

            throw new IllegalStateException(
                    "FAIL: Job history or runtime changed.");
        }

        System.out.println(
                "PASS: Completed and running jobs restored correctly.");

        // 11. Check the GPU assignment and next job ID.
        if (loaded.getGpus()[0].getCurrentJobId() == null
                || loaded.getGpus()[0].getCurrentJobId()
                        != running.getJobId()
                || !loaded.getGpus()[1].isFree()
                || !loaded.getGpus()[2].isFree()
                || !loaded.getGpus()[3].isFree()
                || loaded.getNextJobNumber() != 103) {

            throw new IllegalStateException(
                    "FAIL: GPU assignment or next job ID changed.");
        }

        System.out.println(
                "PASS: GPU assignment and next job ID restored.");

        // 12. Repeat loading to detect extra charging or time advancement.
        store.save(loaded);

        AppState reloaded = store.load();

        if (reloaded.findStudent("TEST001")
                        .getAllowance().getUsed() != 5
                || !initial.plusMinutes(7).equals(
                        reloaded.getClock().getCurrentTime())) {

            throw new IllegalStateException(
                    "FAIL: Reload added a charge or advanced time.");
        }

        System.out.println(
                "PASS: Repeated loading adds no charge or simulated time.");

        System.out.println("ALL STORAGE TESTS PASSED.");
    }
}