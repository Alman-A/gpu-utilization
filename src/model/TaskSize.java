package model;

public class TaskSize {
    private String name;
    private int duration;

    public TaskSize(String name) {
        if("Small".equals(name)){
            duration = 5;
        } else if ("Medium".equals(name)) {
            duration = 15;
        } else if ("Large".equals(name)) {
            duration = 30;
        } else if ("Very large".equals(name)){
            duration = 60;
        } else {
            throw new IllegalArgumentException("Invalid task size");
        }
        this.name = name;
        
    }

    public String getName() {
        return name;
    }

    public int getDuration() {
        return duration;
    }

    @Override 
    public String toString() {
        return name + " (" + duration + " minutes)";
        
    }
}
