package au.edu.rmit.sept.webapp.gear;

public enum Category {

    CAMPING("Camping and hiking"),
    SPORTS("Sports equipment"),
    WATER_SPORTS("Water sports"),
    PHOTOGRAPHY("Cameras and photography"),
    MUSIC("Musical instruments"),
    TOOLS("Tools and DIY"),
    PARTY("Party and events"),
    OTHER("Other");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
