public class Student extends User {

    private static final long serialVersionUID = 1L;

    private String name;
    private String email;
    private String phone;
    private TimeAllowance allowance;

    public Student(String universityId, String name, String email,
                   String phone, String password,
                   TimeAllowance allowance) {

        super(universityId, password);

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be blank.");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be blank.");
        }

        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone cannot be blank.");
        }

        if (allowance == null) {
            throw new IllegalArgumentException(
                    "Time allowance cannot be null.");
        }

        this.name = name.trim();
        this.email = email.trim();
        this.phone = phone.trim();
        this.allowance = allowance;
    }

    public String getUniversityId() {
        return getUsername();
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

    public TimeAllowance getAllowance() {
        return allowance;
    }

    @Override
    public String getRole() {
        return "STUDENT";
    }
}