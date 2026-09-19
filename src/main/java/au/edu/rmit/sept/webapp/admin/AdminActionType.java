package au.edu.rmit.sept.webapp.admin;

public enum AdminActionType {

    DEACTIVATE_USER("Deactivated account", Target.USER),
    REACTIVATE_USER("Reactivated account", Target.USER),
    REMOVE_LISTING("Removed listing", Target.LISTING),
    RESTORE_LISTING("Restored listing", Target.LISTING);

    public enum Target {
        USER,
        LISTING
    }

    private final String label;
    private final Target target;

    AdminActionType(String label, Target target) {
        this.label = label;
        this.target = target;
    }

    public String getLabel() {
        return label;
    }

    public Target getTarget() {
        return target;
    }
}
