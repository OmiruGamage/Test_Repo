package au.edu.rmit.sept.webapp.review;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReviewService {

    private final ReviewDAO reviewDAO;
    private final RentalLookupDAO rentalLookupDAO;
    private final Clock clock;

    @Autowired
    public ReviewService(ReviewDAO reviewDAO, RentalLookupDAO rentalLookupDAO) {
        this(reviewDAO, rentalLookupDAO, Clock.systemDefaultZone());
    }

    ReviewService(ReviewDAO reviewDAO, RentalLookupDAO rentalLookupDAO, Clock clock) {
        this.reviewDAO = reviewDAO;
        this.rentalLookupDAO = rentalLookupDAO;
        this.clock = clock;
    }

    /**
     * Renters review the gear and the owner; owners review the renter.
     */
    public List<ReviewType> allowedTypes(CompletedRental rental, Long userId) {

        if (rental.getRenterId().equals(rental.getOwnerId())) {
            return List.of();
        }

        if (rental.getRenterId().equals(userId)) {
            return List.of(ReviewType.GEAR, ReviewType.OWNER);
        }

        if (rental.getOwnerId().equals(userId)) {
            return List.of(ReviewType.RENTER);
        }

        return List.of();
    }

    public CompletedRental getReviewableRental(Long rentalId, Long userId, ReviewType type) {

        CompletedRental rental = rentalLookupDAO
                .findCompletedById(rentalId, today())
                .orElseThrow(() -> new ReviewNotAllowedException(
                        "You can only leave a review once the rental has been completed"));

        if (!allowedTypes(rental, userId).contains(type)) {
            throw new ReviewNotAllowedException(
                    "You can't leave this review for this rental");
        }

        if (reviewDAO.exists(rentalId, userId, type)) {
            throw new ReviewNotAllowedException(
                    "You have already left this review");
        }

        return rental;
    }

    public Long submitReview(
            Long rentalId,
            Long reviewerId,
            ReviewType type,
            ReviewForm form) {

        if (form.getRating() == null || form.getRating() < 1 || form.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        CompletedRental rental = getReviewableRental(rentalId, reviewerId, type);

        Review review = new Review();
        review.setRentalId(rentalId);
        review.setReviewerId(reviewerId);
        review.setRevieweeId(
                type == ReviewType.RENTER ? rental.getRenterId() : rental.getOwnerId());
        review.setListingId(rental.getListingId());
        review.setType(type);
        review.setRating(form.getRating());
        review.setComment(trimmedOrNull(form.getComment()));

        try {
            return reviewDAO.save(review);
        } catch (DuplicateKeyException e) {
            throw new ReviewNotAllowedException("You have already left this review");
        }
    }

    public List<PendingReview> findPendingReviews(Long userId) {

        Map<Long, Set<ReviewType>> written = reviewDAO.findTypesWrittenBy(userId);
        List<PendingReview> pending = new ArrayList<>();

        for (CompletedRental rental : rentalLookupDAO.findCompletedForUser(userId, today())) {

            Set<ReviewType> done = written.getOrDefault(rental.getRentalId(), Set.of());

            for (ReviewType type : allowedTypes(rental, userId)) {
                if (!done.contains(type)) {
                    pending.add(new PendingReview(rental, type));
                }
            }
        }

        return pending;
    }

    public List<ReviewView> findReviewsWrittenBy(Long userId) {
        return reviewDAO.findReviewsWrittenBy(userId);
    }

    public RatingSummary gearRating(Long listingId) {
        return reviewDAO.summariseGear(listingId);
    }

    public List<ReviewView> gearReviews(Long listingId) {
        return reviewDAO.findGearReviews(listingId);
    }

    /** Every requested listing gets an entry; unreviewed listings map to an empty summary. */
    public Map<Long, RatingSummary> gearRatings(List<Long> listingIds) {

        Map<Long, RatingSummary> found = reviewDAO.summariseGearByListing(listingIds);
        Map<Long, RatingSummary> result = new HashMap<>();

        for (Long id : listingIds) {
            result.put(id, found.getOrDefault(id, RatingSummary.empty()));
        }

        return result;
    }

    public RatingSummary userRating(Long userId, ReviewType type) {
        requireUserType(type);
        return reviewDAO.summariseUser(userId, type);
    }

    public List<ReviewView> reviewsAbout(Long userId, ReviewType type) {
        requireUserType(type);
        return reviewDAO.findReviewsAbout(userId, type);
    }

    private static void requireUserType(ReviewType type) {
        if (type == ReviewType.GEAR) {
            throw new IllegalArgumentException("GEAR reviews are about listings, not users");
        }
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private static String trimmedOrNull(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
