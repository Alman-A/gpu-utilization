package storage;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Scanner;

import model.AccessRequest;
import model.GPU;
import model.Job;
import model.JobStatus;
import model.RequestStatus;
import model.Student;
import model.TaskSize;
import model.TimeAllowance;
import model.TimeGrant;

public class FileDataStore implements DataStore {

    private File file;

    public FileDataStore(String path) {
        if (path == null || path.trim().length() == 0) {
            throw new IllegalArgumentException("A file path is required.");
        }
        file = new File(path).getAbsoluteFile();
    }

    @Override
    public void save(AppState state) throws IOException {
        try {
            validate(state);

            File folder = file.getParentFile();

            if (!folder.isDirectory() && !folder.mkdirs()) {
                throw new IOException("Cannot create the data folder.");
            }

            PrintWriter out = new PrintWriter(file, "UTF-8");

            try {
                out.println("GPU_TEXT_2");
                out.println(state.getNextJobNumber());

                writeTime(out, state.getClock().getInitialTime());
                writeTime(out, state.getClock().getCurrentTime());

                // Save registration requests.
                out.println(state.getRequests().size());

                for (AccessRequest r : state.getRequests()) {
                    writeText(out, r.getUniversityId());
                    writeText(out, r.getName());
                    writeText(out, r.getEmail());
                    writeText(out, r.getPhone());
                    writeText(out, r.getPassword());
                    writeText(out, r.getStatus());
                }

                // Contact details and passwords remain in the requests.
                out.println(state.getStudents().size());

                for (Student s : state.getStudents()) {
                    writeText(out, s.getUniversityId());

                    out.println(s.getAllowance().getAllocated());
                    out.println(s.getAllowance().getUsed());
                    out.println(s.getAllowance().getReserved());
                }

                // Save extra time grants.
                out.println(state.getGrants().size());

                for (TimeGrant g : state.getGrants()) {
                    writeText(out, g.getStudentId());
                    out.println(g.getMinutes());
                    writeText(out, g.getReason());
                    writeText(out, g.getGrantedBy());
                    writeTime(out, g.getTime());
                }

                // Save jobs and their GPU assignments.
                out.println(state.getJobs().size());

                for (Job j : state.getJobs()) {
                    out.println(j.getJobId());

                    writeText(out, j.getStudentId());
                    writeText(out, j.getTitle());
                    writeText(out, j.getApplicationName());
                    writeText(out, j.getDescription());
                    writeText(out, j.getTaskSize().getName());

                    writeTime(out, j.getSubmissionTime());
                    writeText(out, j.getStatus());

                    int gpuId = 0;

                    if (j.getAssignedGpuId() != null) {
                        gpuId = j.getAssignedGpuId();
                    }

                    out.println(gpuId);

                    writeTime(out, j.getStartTime());
                    writeTime(out, j.getFinishTime());
                    writeTime(out, j.getCancellationTime());

                    writeText(out, j.getCancelledBy());
                    writeText(out, j.getCancellationReason());
                    writeText(out, j.getResultSummary());
                }

                out.println("END");

            } finally {
                out.close();
            }

            if (out.checkError()) {
                throw new IOException("Writing failed.");
            }

        } catch (RuntimeException e) {
            throw new IOException(
                    "Cannot save " + file + ": " + e.getMessage(), e);

        } catch (IOException e) {
            throw new IOException(
                    "Cannot save " + file + ": " + e.getMessage(), e);
        }
    }

    @Override
    public AppState load() throws IOException {
        if (!file.exists()) {
            return new AppState();
        }

        try (Scanner in = new Scanner(file, "UTF-8")) {

            require(in.nextLine().equals("GPU_TEXT_2"),"Wrong file format.");

            AppState state = new AppState();

            int nextId = readInt(in);
            LocalDateTime initial = readTime(in);
            LocalDateTime current = readTime(in);

            if (initial != null || current != null) {
                state.getClock().restore(initial, current);
            }

            // Load requests.
            int count = readCount(in);

            for (int i = 0; i < count; i++) {
                AccessRequest r = new AccessRequest(
                        readText(in),
                        readText(in),
                        readText(in),
                        readText(in),
                        readText(in));

                String status = readText(in);

                require(state.findRequest(r.getUniversityId()) == null,"Duplicate request ID.");

                if (RequestStatus.APPROVED.equals(status)) {
                    r.approve();

                } else if (RequestStatus.REJECTED.equals(status)) {
                    r.reject();

                } else {
                    require(RequestStatus.PENDING.equals(status),
                            "Bad request status.");
                }

                state.getRequests().add(r);
            }

            // Load approved students.
            count = readCount(in);

            for (int i = 0; i < count; i++) {
                String id = readText(in);
                AccessRequest r = state.findRequest(id);

                require(r != null && RequestStatus.APPROVED.equals(r.getStatus()),"Student has no approved request.");

                require(state.findStudent(id) == null,
                        "Duplicate student ID.");

                TimeAllowance allowance = new TimeAllowance(
                        readInt(in),
                        readInt(in),
                        readInt(in));

                Student student = new Student(
                        id,
                        r.getName(),
                        r.getEmail(),
                        r.getPhone(),
                        r.getPassword(),
                        allowance);

                state.getStudents().add(student);
            }

            // Load grants.
            count = readCount(in);

            for (int i = 0; i < count; i++) {
                TimeGrant grant = new TimeGrant(
                        readText(in),
                        readInt(in),
                        readText(in),
                        readText(in),
                        readTime(in));

                state.getGrants().add(grant);
            }

            // Load jobs and restore busy GPUs.
            count = readCount(in);

            for (int i = 0; i < count; i++) {
                Job job = readJob(in);

                require(state.findJob(job.getJobId()) == null,
                        "Duplicate job ID.");

                state.getJobs().add(job);

                if (JobStatus.RUNNING.equals(job.getStatus())) {
                    int index = job.getAssignedGpuId() - 1;
                    state.getGpus()[index].assign(job.getJobId());
                }
            }

            require(in.nextLine().equals("END") && !in.hasNextLine(),
                    "Incomplete file or unexpected extra data.");

            if (in.ioException() != null) {
                throw in.ioException();
            }

            state.restoreNextJobNumber(nextId);

            validate(state);

            return state;

        } catch (RuntimeException e) {
            throw new IOException(
                    "Invalid data in " + file + ": " + e.getMessage(), e);

        } catch (IOException e) {
            throw new IOException(
                    "Cannot load " + file + ": " + e.getMessage(), e);
        }
    }

    private Job readJob(Scanner in) throws IOException {
        int id = readInt(in);

        Job job = new Job(
                id,
                readText(in),
                readText(in),
                readText(in),
                readText(in),
                new TaskSize(readText(in)),
                readTime(in));

        String status = readText(in);
        int gpuId = readInt(in);

        LocalDateTime start = readTime(in);
        LocalDateTime finish = readTime(in);
        LocalDateTime cancellation = readTime(in);

        String actor = readText(in);
        String reason = readText(in);
        String result = readText(in);

        if (start != null) {
            job.start(gpuId, start);
        } else {
            require(gpuId == 0,
                    "Unstarted job has a GPU.");
        }

        if (JobStatus.COMPLETED.equals(status)) {
            job.complete(finish);

        } else if (JobStatus.CANCELLED.equals(status)) {
            job.cancel(cancellation, actor, reason);
        }

        require(job.getStatus().equals(status)
                        && same(job.getFinishTime(), finish)
                        && same(job.getCancellationTime(), cancellation)
                        && same(job.getCancelledBy(), actor)
                        && same(job.getCancellationReason(), reason)
                        && same(job.getResultSummary(), result),
                "Invalid job history: " + id);

        return job;
    }

    private void validate(AppState state) throws IOException {
        require(state != null,"Missing application state.");

        LocalDateTime initial = state.getClock().getInitialTime();
        LocalDateTime now = state.getClock().getCurrentTime();

        require((initial == null) == (now == null),"Invalid clock.");

        if (now != null) {
            require(!now.isBefore(initial),"Invalid clock order.");
        }

        // Check requests and account activation.
        for (int i = 0; i < state.getRequests().size(); i++) {
            AccessRequest r = state.getRequests().get(i);

            require(!"admin".equals(r.getUniversityId()),"Reserved university ID.");

            for (int k = 0; k < i; k++) {
                require(!r.getUniversityId().equals(
                                state.getRequests().get(k).getUniversityId()),
                        "Duplicate request ID.");
            }

            require(RequestStatus.APPROVED.equals(r.getStatus())
                            == (state.findStudent(r.getUniversityId()) != null),
                    "Request/account mismatch.");
        }

        // Check students and allowance balances.
        for (int i = 0; i < state.getStudents().size(); i++) {
            Student s = state.getStudents().get(i);

            for (int k = 0; k < i; k++) {
                require(!s.getUniversityId().equals(state.getStudents().get(k).getUniversityId()),"Duplicate student ID.");
            }

            AccessRequest r = state.findRequest(s.getUniversityId());

            require(r != null
                            && RequestStatus.APPROVED.equals(r.getStatus())
                            && s.getName().equals(r.getName())
                            && s.getEmail().equals(r.getEmail())
                            && s.getPhone().equals(r.getPhone())
                            && s.checkPassword(r.getPassword()),
                    "Student does not match registration.");

            long used = 0;
            long reserved = 0;
            long grants = 0;

            for (Job j : state.getJobs()) {
                if (s.getUniversityId().equals(j.getStudentId())) {

                    if (JobStatus.QUEUED.equals(j.getStatus())
                            || JobStatus.RUNNING.equals(j.getStatus())) {
                        reserved += j.getEstimatedDuration();
                    } else {
                        used += j.getCharge();
                    }
                }
            }

            for (TimeGrant g : state.getGrants()) {
                if (s.getUniversityId().equals(g.getStudentId())) {
                    grants += g.getMinutes();
                }
            }

            TimeAllowance a = s.getAllowance();

            require(a.getUsed() == used
                            && a.getReserved() == reserved
                            && used >= 0
                            && reserved >= 0
                            && used + reserved <= a.getAllocated()
                            && a.getAllocated() - grants > 0,
                    "Invalid allowance: " + s.getUniversityId());
        }

        require(state.getNextJobNumber() >= 101,
                "Invalid next job number.");

        // Check job IDs, times and running assignments.
        for (int i = 0; i < state.getJobs().size(); i++) {
            Job j = state.getJobs().get(i);

            for (int k = 0; k < i; k++) {
                require(j.getJobId() != state.getJobs().get(k).getJobId(),
                        "Duplicate job ID.");
            }

            require(state.findStudent(j.getStudentId()) != null
                            && j.getJobId() < state.getNextJobNumber(),
                    "Invalid job owner or ID.");

            checkTime(j.getSubmissionTime(), initial, now);

            if (j.getStartTime() != null) {
                checkTime(j.getStartTime(), j.getSubmissionTime(), now);
            }

            if (j.getFinishTime() != null) {
                checkTime(j.getFinishTime(), j.getStartTime(), now);
            }

            if (JobStatus.RUNNING.equals(j.getStatus())) {
                require(j.getStartTime()
                                .plusMinutes(j.getEstimatedDuration())
                                .isAfter(now),
                        "A running job is overdue.");

                require(Integer.valueOf(j.getJobId()).equals(
                                state.getGpus()[j.getAssignedGpuId() - 1]
                                        .getCurrentJobId()),
                        "Job/GPU mismatch.");
            }

            if (JobStatus.CANCELLED.equals(j.getStatus())) {
                checkTime(
                        j.getCancellationTime(),
                        j.getSubmissionTime(),
                        now);

                require("admin".equals(j.getCancelledBy())
                                || j.getStudentId().equals(j.getCancelledBy()),
                        "Invalid cancellation actor.");
            }
        }

        for (TimeGrant g : state.getGrants()) {
            require(state.findStudent(g.getStudentId()) != null
                            && "admin".equals(g.getGrantedBy()),
                    "Invalid grant.");

            checkTime(g.getTime(), initial, now);
        }

        // Check all four GPUs.
        require(state.getGpus().length == 4,
                "Four GPUs are required.");

        for (int i = 0; i < 4; i++) {
            GPU gpu = state.getGpus()[i];

            require(gpu.getId() == i + 1,
                    "Invalid GPU ID.");

            if (gpu.isFree()) {
                for (Job j : state.getJobs()) {
                    require(!JobStatus.QUEUED.equals(j.getStatus()),
                            "Free GPU with a waiting job.");
                }

            } else {
                Job j = state.findJob(gpu.getCurrentJobId());

                require(j != null
                                && JobStatus.RUNNING.equals(j.getStatus())
                                && Integer.valueOf(i + 1).equals(
                                        j.getAssignedGpuId()),
                        "Invalid GPU assignment.");
            }
        }
    }

    private void checkTime(
            LocalDateTime time,
            LocalDateTime first,
            LocalDateTime last) throws IOException {

        require(time != null
                        && first != null
                        && last != null
                        && !time.isBefore(first)
                        && !time.isAfter(last)
                        && time.getSecond() == 0
                        && time.getNano() == 0,
                "Invalid simulated timestamp.");
    }

    private void require(boolean valid, String message)
            throws IOException {

        if (!valid) {
            throw new IOException(message);
        }
    }

    private boolean same(Object a, Object b) {
        if (a == null) {
            return b == null;
        }

        return a.equals(b);
    }

    private int readInt(Scanner in) {
        return Integer.parseInt(in.nextLine());
    }

    private int readCount(Scanner in) throws IOException {
        int count = readInt(in);

        require(count >= 0,
                "Negative record count.");

        return count;
    }

    private void writeTime(PrintWriter out, LocalDateTime time) {
        if (time == null) {
            writeText(out, null);
        } else {
            writeText(out, time.toString());
        }
    }

    private LocalDateTime readTime(Scanner in) throws IOException {
        String text = readText(in);

        if (text == null) {
            return null;
        }

        return LocalDateTime.parse(text);
    }

    // N means null. S marks a String.
    // Escaping keeps multiline text inside one saved line.
    private void writeText(PrintWriter out, String text) {
        if (text == null) {
            out.println("N");
        } else {
            out.println("S" + text.replace("\\", "\\\\")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r"));
        }
    }

    private String readText(Scanner in) throws IOException {
        String line = in.nextLine();

        if (line.equals("N")) {
            return null;
        }

        require(line.length() > 0 && line.charAt(0) == 'S',
                "Invalid text field.");

        String text = "";

        for (int i = 1; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '\\') {
                i++;

                require(i < line.length(),
                        "Incomplete text field.");

                c = line.charAt(i);

                if (c == 'n') {
                    text += '\n';

                } else if (c == 'r') {
                    text += '\r';

                } else {
                    require(c == '\\',
                            "Invalid text escape.");

                    text += '\\';
                }

            } else {
                text += c;
            }
        }

        return text;
    }
}