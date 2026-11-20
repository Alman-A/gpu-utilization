package model;

public class Administrator extends User {

    public Administrator() {
        super("admin", "gpu2026");
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }
}