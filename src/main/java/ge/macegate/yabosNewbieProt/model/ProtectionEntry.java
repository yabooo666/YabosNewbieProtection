package ge.macegate.yabosNewbieProt.model;

public final class ProtectionEntry {

    private String lastKnownName;
    private long remainingSeconds;

    public ProtectionEntry(String lastKnownName, long remainingSeconds) {
        this.lastKnownName = lastKnownName;
        this.remainingSeconds = Math.max(0L, remainingSeconds);
    }

    public String getLastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String lastKnownName) {
        if (lastKnownName != null && !lastKnownName.isBlank()) {
            this.lastKnownName = lastKnownName;
        }
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(long remainingSeconds) {
        this.remainingSeconds = Math.max(0L, remainingSeconds);
    }

    public void addSeconds(long amount) {
        this.remainingSeconds = Math.max(0L, this.remainingSeconds + amount);
    }

    public void subtractSeconds(long amount) {
        this.remainingSeconds = Math.max(0L, this.remainingSeconds - amount);
    }
}
