public class RelaxationSuggestion {

    private RelaxationType type;
    private String explanation;
    private int penalty;

    public RelaxationSuggestion(RelaxationType type, String explanation, int penalty) {
        this.type = type;
        this.explanation = explanation;
        this.penalty = penalty;
    }

    public RelaxationType getType() {
        return type;
    }

    public String getExplanation() {
        return explanation;
    }

    public int getPenalty() {
        return penalty;
    }
}
