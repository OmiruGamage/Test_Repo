package au.edu.rmit.sept.webapp.gear;

public enum GearCondition {

    NEW("New"),
    GOOD("Good"),
    FAIR("Fair");

    private final String label;

    GearCondition(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
