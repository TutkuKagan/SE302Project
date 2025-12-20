import java.util.Objects;

public class Slot {

    // Exam day index (1, 2, 3, ...)
    private int day;
    // Slot number inside that day
    private int index;
    // Time range string, e.g. "09:00-11:00"
    private String timeRange;

    public Slot(int day, int index, String timeRange) {
        this.day = day;
        this.index = index;
        this.timeRange = timeRange;
    }

    public int getDay() {
        return day;
    }

    public int getIndex() {
        return index;
    }

    public String getTimeRange() {
        return timeRange;
    }

    @Override
    public String toString() {
        return "Day " + day + ", Slot " + index + " (" + timeRange + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Slot)) return false;
        Slot slot = (Slot) o;
        return day == slot.day && index == slot.index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(day, index);

    }
}
