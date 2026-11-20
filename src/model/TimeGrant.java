package model;

import java.time.LocalDateTime;

public class TimeGrant {

    private final String studentId;
    private final int minutes;
    private final String reason;
    private final String grantedBy;
    private final LocalDateTime time;

    public TimeGrant(String studentId, int minutes, String reason,
                     String grantedBy, LocalDateTime time) {

        if (studentId == null || studentId.trim().length() == 0) {
            throw new IllegalArgumentException("Student ID cannot be blank.");
        }

        if (minutes <= 0) {
            throw new IllegalArgumentException("Granted minutes must be greater than zero.");
        }

        if (reason == null || reason.trim().length() == 0) {
            throw new IllegalArgumentException( "A reason is required for granting time.");
        }

        if (grantedBy == null || grantedBy.trim().length() == 0) {
            throw new IllegalArgumentException("Administrator username cannot be blank.");
        }

        if (time == null) {
            throw new IllegalArgumentException("Grant time cannot be null.");
        }

        this.studentId = studentId.trim();
        this.minutes = minutes;
        this.reason = reason.trim();
        this.grantedBy = grantedBy.trim();
        this.time = time;
    }

    public String getStudentId() {
        return studentId;
    }

    public int getMinutes() {
        return minutes;
    }

    public String getReason() {
        return reason;
    }

    public String getGrantedBy() {
        return grantedBy;
    }

    public LocalDateTime getTime() {
        return time;
    }
}