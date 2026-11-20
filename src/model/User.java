public abstract class User {

    private String username;
    private String password;

    protected User(String username, String password) {

        if (username == null || username.trim().length() == 0) {
            throw new IllegalArgumentException("Username cannot be blank.");
        }

        if (password == null || password.trim().length() == 0) {
            throw new IllegalArgumentException("Password cannot be blank.");
        }

        this.username = username.trim();
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public boolean checkPassword(String input) {

        if (input == null) {
            return false;
        }

        return password.equals(input);
    }

    public abstract String getRole();
}