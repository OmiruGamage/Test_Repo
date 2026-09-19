package au.edu.rmit.sept.webapp.review;

import java.time.LocalDateTime;

/**
 * Read-only view of a review joined with the names needed to display it.
 */
public class ReviewView {

    private final Long id;
    private final ReviewType type;
    private final int rating;
    private final String comment;
    private final Long listingId;
    private final String listingTitle;
    private final Long reviewerId;
    private final String reviewerName;
    private final Long revieweeId;
    private final String revieweeName;
    private final LocalDateTime createdAt;

    public ReviewView(
            Long id,
            ReviewType type,
            int rating,
            String comment,
            Long listingId,
            String listingTitle,
            Long reviewerId,
            String reviewerName,
            Long revieweeId,
            String revieweeName,
            LocalDateTime createdAt) {

        this.id = id;
        this.type = type;
        this.rating = rating;
        this.comment = comment;
        this.listingId = listingId;
        this.listingTitle = listingTitle;
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.revieweeId = revieweeId;
        this.revieweeName = revieweeName;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public ReviewType getType() {
        return type;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public Long getListingId() {
        return listingId;
    }

    public String getListingTitle() {
        return listingTitle;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public Long getRevieweeId() {
        return revieweeId;
    }

    public String getRevieweeName() {
        return revieweeName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
