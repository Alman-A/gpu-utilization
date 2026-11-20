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
}