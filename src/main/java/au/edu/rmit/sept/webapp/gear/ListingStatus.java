package au.edu.rmit.sept.webapp.gear;

public enum ListingStatus {

    PUBLISHED("Published"),
    EXPIRED("Expired"),
    REMOVED("Removed");

    private final String label;

    ListingStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
