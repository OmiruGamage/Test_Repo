package au.edu.rmit.sept.webapp.review;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 19);
    private static final Long OWNER_ID = 1L;
    private static final Long RENTER_ID = 2L;
    private static final Long STRANGER_ID = 3L;
    private static final Long RENTAL_ID = 10L;
    private static final Long LISTING_ID = 20L;

    @Mock
    private ReviewDAO reviewDAO;

    @Mock
    private RentalLookupDAO rentalLookupDAO;

    private ReviewService reviewService;

    private final CompletedRental rental = new CompletedRental(
            RENTAL_ID, LISTING_ID, "Tent", OWNER_ID, "Olivia",
            RENTER_ID, "Ravi", TODAY.minusDays(7), TODAY.minusDays(2));

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-19T00:00:00Z"), ZoneOffset.UTC);
        reviewService = new ReviewService(reviewDAO, rentalLookupDAO, clock);
    }

    @Test
    void renterCanReviewGearAndOwner() {
        assertThat(reviewService.allowedTypes(rental, RENTER_ID))
                .containsExactly(ReviewType.GEAR, ReviewType.OWNER);
    }

    @Test
    void ownerCanOnlyReviewRenter() {
        assertThat(reviewService.allowedTypes(rental, OWNER_ID))
                .containsExactly(ReviewType.RENTER);
    }

    @Test
    void outsiderCannotReview() {
        assertThat(reviewService.allowedTypes(rental, STRANGER_ID)).isEmpty();
    }

    @Test
    void rejectsRentalThatIsNotCompleted() {
        when(rentalLookupDAO.findCompletedById(RENTAL_ID, TODAY)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                reviewService.getReviewableRental(RENTAL_ID, RENTER_ID, ReviewType.GEAR))
                .isInstanceOf(ReviewNotAllowedException.class)
                .hasMessageContaining("completed");
    }

    @Test
    void ownerCannotWriteGearReviewOfOwnListing() {
        when(rentalLookupDAO.findCompletedById(RENTAL_ID, TODAY)).thenReturn(Optional.of(rental));

        assertThatThrownBy(() ->
                reviewService.getReviewableRental(RENTAL_ID, OWNER_ID, ReviewType.GEAR))
                .isInstanceOf(ReviewNotAllowedException.class);
    }

    @Test
    void rejectsSecondReviewOfSameType() {
        when(rentalLookupDAO.findCompletedById(RENTAL_ID, TODAY)).thenReturn(Optional.of(rental));
        when(reviewDAO.exists(RENTAL_ID, RENTER_ID, ReviewType.GEAR)).thenReturn(true);

        assertThatThrownBy(() ->
                reviewService.getReviewableRental(RENTAL_ID, RENTER_ID, ReviewType.GEAR))
                .isInstanceOf(ReviewNotAllowedException.class)
                .hasMessageContaining("already");
    }

    @Test
    void gearReviewIsAboutOwnerAndListing() {
        when(rentalLookupDAO.findCompletedById(RENTAL_ID, TODAY)).thenReturn(Optional.of(rental));
        when(reviewDAO.exists(RENTAL_ID, RENTER_ID, ReviewType.GEAR)).thenReturn(false);
        when(reviewDAO.save(any(Review.class))).thenReturn(99L);

        Long id = reviewService.submitReview(
                RENTAL_ID, RENTER_ID, ReviewType.GEAR, form(4, "  Great tent  "));

        ArgumentCaptor<Review> saved = ArgumentCaptor.forClass(Review.class);
        verify(reviewDAO).save(saved.capture());

        assertThat(id).isEqualTo(99L);
        assertThat(saved.getValue().getReviewerId()).isEqualTo(RENTER_ID);
        assertThat(saved.getValue().getRevieweeId()).isEqualTo(OWNER_ID);
        assertThat(saved.getValue().getListingId()).isEqualTo(LISTING_ID);
        assertThat(saved.getValue().getRating()).isEqualTo(4);
        assertThat(saved.getValue().getComment()).isEqualTo("Great tent");
    }

    @Test
    void renterReviewIsAboutRenter() {
        when(rentalLookupDAO.findCompletedById(RENTAL_ID, TODAY)).thenReturn(Optional.of(rental));
        when(reviewDAO.exists(RENTAL_ID, OWNER_ID, ReviewType.RENTER)).thenReturn(false);
        when(reviewDAO.save(any(Review.class))).thenReturn(1L);

        reviewService.submitReview(RENTAL_ID, OWNER_ID, ReviewType.RENTER, form(5, "   "));

        ArgumentCaptor<Review> saved = ArgumentCaptor.forClass(Review.class);
        verify(reviewDAO).save(saved.capture());

        assertThat(saved.getValue().getRevieweeId()).isEqualTo(RENTER_ID);
        assertThat(saved.getValue().getComment()).isNull();
    }

    @Test
    void rejectsRatingOutsideOneToFive() {
        assertThatThrownBy(() ->
                reviewService.submitReview(RENTAL_ID, RENTER_ID, ReviewType.GEAR, form(6, null)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(reviewDAO, never()).save(any());
    }

    @Test
    void duplicateInsertIsReportedAsAlreadyReviewed() {
        when(rentalLookupDAO.findCompletedById(RENTAL_ID, TODAY)).thenReturn(Optional.of(rental));
        when(reviewDAO.exists(RENTAL_ID, RENTER_ID, ReviewType.OWNER)).thenReturn(false);
        when(reviewDAO.save(any(Review.class))).thenThrow(new DuplicateKeyException("dup"));

        assertThatThrownBy(() ->
                reviewService.submitReview(RENTAL_ID, RENTER_ID, ReviewType.OWNER, form(3, null)))
                .isInstanceOf(ReviewNotAllowedException.class);
    }

    @Test
    void pendingReviewsLeaveOutReviewsAlreadyWritten() {
        when(rentalLookupDAO.findCompletedForUser(RENTER_ID, TODAY)).thenReturn(List.of(rental));
        when(reviewDAO.findTypesWrittenBy(RENTER_ID))
                .thenReturn(Map.of(RENTAL_ID, EnumSet.of(ReviewType.GEAR)));

        List<PendingReview> pending = reviewService.findPendingReviews(RENTER_ID);

        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getType()).isEqualTo(ReviewType.OWNER);
    }

    @Test
    void gearRatingsGiveEveryListingAnEntry() {
        when(reviewDAO.summariseGearByListing(List.of(1L, 2L)))
                .thenReturn(Map.of(1L, RatingSummary.of(2, new java.math.BigDecimal("4.5"))));

        Map<Long, RatingSummary> ratings = reviewService.gearRatings(List.of(1L, 2L));

        assertThat(ratings.get(1L).getDisplay()).isEqualTo("4.5 / 5 (2 reviews)");
        assertThat(ratings.get(2L).isEmpty()).isTrue();
    }

    @Test
    void userRatingRejectsGearType() {
        assertThatThrownBy(() -> reviewService.userRating(OWNER_ID, ReviewType.GEAR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ReviewForm form(Integer rating, String comment) {
        ReviewForm form = new ReviewForm();
        form.setRating(rating);
        form.setComment(comment);
        return form;
    }
}
