import java.util.ArrayList;
import java.util.List;

public class SchedulingResult {
    private Schedule schedule;
    private List<String> relaxations = new ArrayList<>();
    private int penaltyScore = 0;

    public SchedulingResult(Schedule schedule) {
        this.schedule = schedule;
    }

    public Schedule getSchedule() { return schedule; }

    public List<String> getRelaxations() { return relaxations; }

    public void addRelaxation(String explanation, int penalty) {
        this.relaxations.add(explanation);
        this.penaltyScore += penalty;
    }

    public int getPenaltyScore() { return penaltyScore; }
}