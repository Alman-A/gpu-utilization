package model;

public class TimeAllowance {
    private int allocated;
    private int used;
    private int reserved;

    public TimeAllowance(int allocated) {
        if(allocated <=0) {
            throw new IllegalArgumentException("Allocated minutes must be positive.");
        }
        this.allocated = allocated;
        used = 0;
        reserved = 0;
    }

    public TimeAllowance(int allocated, int used, int reserved) {

    if (allocated <= 0 || used < 0 || reserved < 0
            || (long) used + reserved > allocated) {
        throw new IllegalArgumentException("Invalid saved allowance.");
    }

    this.allocated = allocated;
    this.used = used;
    this.reserved = reserved;
}

    public int getAllocated() {
        return allocated;
    }

    public int getUsed() {
        return used;
    }

    public int getReserved() {
        return reserved;
    }

    public int getAvailable() {
        return allocated - used - reserved;
    }

    public void reserve(int minutes) {
        if(minutes <= 0) {
            throw new IllegalArgumentException("Reserved minutes must be positive.");

        }
        if (minutes > getAvailable()) {
            throw new IllegalArgumentException("Not enough available GPU minutes.");

        }
        reserved += minutes;
    }

    public void settle(int duration, int charged) {
        if(duration <=0 || charged < 0 || charged > duration) {
            throw new IllegalArgumentException("Invalid duration or charge.");

        }
        if (duration > reserved) {
            throw new IllegalStateException("Not enough reserved minutes to settle this job.");

        }
        reserved -= duration;
        used += charged;
    }
    public void grant(int minutes) {
        if(minutes <= 0) {
            throw new IllegalArgumentException("Granted minutes must be positive.");

        }

        if (minutes > Integer.MAX_VALUE - allocated) {
            throw new IllegalArgumentException("The allocation would exceed the integer limit.");

        }
        allocated += minutes;
    }
}
