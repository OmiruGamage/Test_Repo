package au.edu.rmit.sept.webapp.review;

/**
 * A review the current user is allowed to write but has not written yet.
 */
public class PendingReview {

    private final CompletedRental rental;
    private final ReviewType type;

    public PendingReview(CompletedRental rental, ReviewType type) {
        this.rental = rental;
        this.type = type;
    }

    public CompletedRental getRental() {
        return rental;
    }

    public ReviewType getType() {
        return type;
    }
}
