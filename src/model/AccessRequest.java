public class AccessRequest {

    private String universityId;
    private String name;
    private String email;
    private String phone;
    private String password;
    private String status;

    public AccessRequest(String universityId, String name,
                         String email, String phone, String password) {

        if (universityId == null
                || universityId.trim().length() == 0) {
            throw new IllegalArgumentException(
                    "University ID cannot be blank.");
        }

        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException(
                    "Name cannot be blank.");
        }

        if (email == null || email.trim().length() == 0) {
            throw new IllegalArgumentException(
                    "Email cannot be blank.");
        }

        if (phone == null || phone.trim().length() == 0) {
            throw new IllegalArgumentException(
                    "Phone cannot be blank.");
        }

        if (password == null || password.trim().length() == 0) {
            throw new IllegalArgumentException(
                    "Password cannot be blank.");
        }

        this.universityId = universityId.trim();
        this.name = name.trim();
        this.email = email.trim();
        this.phone = phone.trim();
        this.password = password;
        this.status = RequestStatus.PENDING;
    }

    public String getUniversityId() {
        return universityId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPassword() {
        return password;
    }

    public String getStatus() {
        return status;
    }

    public void approve() {

        if (!RequestStatus.PENDING.equals(status)) {
            throw new IllegalStateException(
                    "Only pending requests can be approved.");
        }

        status = RequestStatus.APPROVED;
    }

    public void reject() {

        if (!RequestStatus.PENDING.equals(status)) {
            throw new IllegalStateException(
                    "Only pending requests can be rejected.");
        }

        status = RequestStatus.REJECTED;
    }
}