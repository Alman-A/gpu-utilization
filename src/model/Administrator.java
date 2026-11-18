public class Administrator extends User {

    private static final long serialVersionUID = 1L;

    public Administrator() {
        super("admin", "gpu2026");
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }
}