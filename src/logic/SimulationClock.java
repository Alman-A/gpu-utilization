package logic;

import java.time.LocalDateTime;
import java.time.Duration;

public class SimulationClock {

    private LocalDateTime initialTime;
    private LocalDateTime currentTime;

    public SimulationClock() {
        initialTime = null;
        currentTime = null;
    }

    public boolean isInitialized() {
        return currentTime != null;
    }

    public void initialize(LocalDateTime start) {

        if (isInitialized()) {
            throw new IllegalStateException(
                "The simulation clock is already initialized."
            );
        }

        if (start == null) {
            throw new IllegalArgumentException(
                "A starting date and time must be provided."
            );
        }

        initialTime = start.withSecond(0).withNano(0);
        currentTime = initialTime;
    }

    public LocalDateTime getInitialTime() {
        return initialTime;
    }

    public LocalDateTime getCurrentTime() {
        return currentTime;
    }

    public void advanceMinute() {

        if (!isInitialized()) {
            throw new IllegalStateException(
                "Initialize the simulation clock first."
            );
        }

        currentTime = currentTime.plusMinutes(1);
    }

    public long getElapsedMinutes() {

        if (!isInitialized()) {
            return 0;
        }

        return Duration.between(
            initialTime, currentTime
        ).toMinutes();
    }
    public void restore(LocalDateTime initial, LocalDateTime current) {

    if (isInitialized()) {
        throw new IllegalStateException(
                "The simulation clock is already initialized.");
    }

    if (initial == null || current == null) {
        throw new IllegalArgumentException(
                "Both saved timestamps must be provided.");
    }

    if (current.isBefore(initial)) {
        throw new IllegalArgumentException(
                "Current time cannot be before the initial time.");
    }

    if (initial.getSecond() != 0 || initial.getNano() != 0
            || current.getSecond() != 0 || current.getNano() != 0) {
        throw new IllegalArgumentException(
                "Saved timestamps must use whole minutes.");
    }

    initialTime = initial;
    currentTime = current;
}
}