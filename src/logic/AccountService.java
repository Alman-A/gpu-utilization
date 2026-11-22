package logic;

import java.time.LocalDateTime;

import model.AccessRequest;
import model.Administrator;
import model.RequestStatus;
import model.Student;
import model.TimeAllowance;
import model.TimeGrant;
import model.User;
import storage.AppState;

public class AccountService {

    private final AppState state;

    public AccountService(AppState state) {

        if (state == null) {
            throw new IllegalArgumentException(
                    "Application state cannot be null.");
        }

        this.state = state;
    }

    public User authenticate(String username, String password) {

        username = requireText(username, "Username");
        requirePassword(password);

        Administrator admin = new Administrator();

        if (admin.getUsername().equals(username)) {

            if (admin.checkPassword(password)) {
                return admin;
            }

            throw new IllegalArgumentException(
                    "Incorrect username or password.");
        }

        Student student = state.findStudent(username);

        if (student != null) {

            if (student.checkPassword(password)) {
                return student;
            }

            throw new IllegalArgumentException(
                    "Incorrect username or password.");
        }

        AccessRequest request = state.findRequest(username);

        if (request != null) {

            if (RequestStatus.PENDING.equals(request.getStatus())) {
                throw new IllegalStateException(
                        "Your registration is pending approval.");
            }

            if (RequestStatus.REJECTED.equals(request.getStatus())) {
                throw new IllegalStateException(
                        "Your registration has been rejected.");
            }
        }

        throw new IllegalArgumentException(
                "Incorrect username or password.");
    }

    public AccessRequest register(String universityId, String name,
                                  String email, String phone,
                                  String password) {

        universityId = requireText(universityId, "University ID");
        name = requireText(name, "Name");
        email = requireText(email, "Email");
        phone = requireText(phone, "Phone");

        requirePassword(password);

        if ("admin".equalsIgnoreCase(universityId)) {
            throw new IllegalArgumentException(
                    "The username admin is reserved.");
        }

        if (state.findStudent(universityId) != null
                || state.findRequest(universityId) != null) {

            throw new IllegalArgumentException(
                    "This university ID is already registered.");
        }

        if (!validEmail(email)) {
            throw new IllegalArgumentException(
                    "Enter a valid email address.");
        }

        if (!validPhone(phone)) {
            throw new IllegalArgumentException(
                    "Enter a valid phone number.");
        }

        AccessRequest request = new AccessRequest(
                universityId, name, email, phone, password);

        state.getRequests().add(request);

        return request;
    }

    public Student approve(String universityId, int initialMinutes) {

        universityId = requireText(universityId, "University ID");

        if (initialMinutes <= 0) {
            throw new IllegalArgumentException(
                    "Initial minutes must be positive.");
        }

        AccessRequest request = requirePendingRequest(universityId);

        if (state.findStudent(universityId) != null) {
            throw new IllegalStateException(
                    "A student account already exists for this ID.");
        }

        TimeAllowance allowance = new TimeAllowance(initialMinutes);

        Student student = new Student(
                request.getUniversityId(),
                request.getName(),
                request.getEmail(),
                request.getPhone(),
                request.getPassword(),
                allowance);

        // Validate the account before changing shared data.
        state.getStudents().add(student);
        request.approve();

        return student;
    }

    public void reject(String universityId) {

        universityId = requireText(universityId, "University ID");

        AccessRequest request = requirePendingRequest(universityId);
        request.reject();
    }

    public void grantTime(String universityId, int minutes,
                          String reason, String actorUsername,
                          LocalDateTime now) {

        universityId = requireText(universityId, "University ID");
        reason = requireText(reason, "Reason");
        actorUsername = requireText(actorUsername, "Actor username");

        if (minutes <= 0) {
            throw new IllegalArgumentException(
                    "Granted minutes must be positive.");
        }

        if (now == null) {
            throw new IllegalArgumentException(
                    "Simulated time cannot be null.");
        }

        Student student = state.findStudent(universityId);

        if (student == null) {
            throw new IllegalArgumentException(
                    "No approved student exists with this university ID.");
        }

        TimeGrant grant = new TimeGrant(
                universityId, minutes, reason, actorUsername, now);

        // grant() checks overflow before changing the allocation.
        student.getAllowance().grant(minutes);
        state.getGrants().add(grant);
    }

    private AccessRequest requirePendingRequest(String universityId) {

        AccessRequest request = state.findRequest(universityId);

        if (request == null) {
            throw new IllegalArgumentException(
                    "No registration request exists for this university ID.");
        }

        if (!RequestStatus.PENDING.equals(request.getStatus())) {
            throw new IllegalStateException(
                    "This registration request has already been processed.");
        }

        return request;
    }

    private String requireText(String value, String fieldName) {

        if (value == null || value.trim().equals("")) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be blank.");
        }

        return value.trim();
    }

    private void requirePassword(String password) {

        if (password == null || password.trim().equals("")) {
            throw new IllegalArgumentException(
                    "Password cannot be blank.");
        }

        // The original password is not trimmed or changed.
    }

    private boolean validEmail(String email) {

        int atPosition = -1;

        for (int i = 0; i < email.length(); i++) {

            char character = email.charAt(i);

            if (character <= ' ') {
                return false;
            }

            if (character == '@') {

                if (atPosition != -1) {
                    return false;
                }

                atPosition = i;
            }
        }

        return atPosition > 0 && atPosition < email.length() - 1;
    }

    private boolean validPhone(String phone) {

        boolean containsDigit = false;

        for (int i = 0; i < phone.length(); i++) {

            char character = phone.charAt(i);

            if (character >= '0' && character <= '9') {
                containsDigit = true;

            } else if (character == '+' && i == 0) {
                // Allow a plus sign only at the beginning.

            } else if (character == ' ' || character == '-'
                    || character == '(' || character == ')'
                    || character == '.') {
                // Allow common phone-number formatting.

            } else {
                return false;
            }
        }

        return containsDigit;
    }
}