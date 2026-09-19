package au.edu.rmit.sept.webapp.review;

public enum ReviewType {

    GEAR("Gear condition"),
    OWNER("Owner reliability"),
    RENTER("Renter reliability");

    private final String label;

    ReviewType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
