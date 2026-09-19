package au.edu.rmit.sept.webapp.review;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Runs the real SQL against an in-memory H2 database built from schema.sql. */
class ReviewDAOTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 19);

    private EmbeddedDatabase database;
    private JdbcTemplate jdbc;
    private ReviewDAO reviewDAO;
    private RentalLookupDAO rentalLookupDAO;

    private Long ownerId;
    private Long renterId;
    private Long listingId;
    private Long completedRentalId;
    private Long futureRentalId;
    private Long pendingRentalId;

    @BeforeEach
    void setUp() {
        database = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:schema.sql")
                .build();

        jdbc = new JdbcTemplate(database);
        reviewDAO = new ReviewDAO(jdbc);
        rentalLookupDAO = new RentalLookupDAO(jdbc);

        ownerId = insertUser("Olivia", "owner@test.com");
        renterId = insertUser("Ravi", "renter@test.com");
        listingId = insertListing(ownerId, "Tent");

        completedRentalId = insertRental(listingId, renterId, TODAY.minusDays(10), TODAY.minusDays(1), "ACCEPTED");
        futureRentalId = insertRental(listingId, renterId, TODAY.minusDays(1), TODAY.plusDays(3), "ACCEPTED");
        pendingRentalId = insertRental(listingId, renterId, TODAY.minusDays(10), TODAY.minusDays(5), "PENDING");
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    void onlyAcceptedRentalsThatHaveEndedAreCompleted() {
        assertThat(rentalLookupDAO.findCompletedById(completedRentalId, TODAY)).isPresent();
        assertThat(rentalLookupDAO.findCompletedById(futureRentalId, TODAY)).isEmpty();
        assertThat(rentalLookupDAO.findCompletedById(pendingRentalId, TODAY)).isEmpty();
    }

    @Test
    void completedRentalsAreFoundForBothParties() {
        assertThat(rentalLookupDAO.findCompletedForUser(renterId, TODAY)).hasSize(1);
        assertThat(rentalLookupDAO.findCompletedForUser(ownerId, TODAY)).hasSize(1);
    }

    @Test
    void gearRatingAveragesAllGearReviews() {
        Long secondRental = insertRental(listingId, renterId, TODAY.minusDays(30), TODAY.minusDays(20), "ACCEPTED");

        reviewDAO.save(review(completedRentalId, renterId, ownerId, ReviewType.GEAR, 5));
        reviewDAO.save(review(secondRental, renterId, ownerId, ReviewType.GEAR, 4));
        reviewDAO.save(review(completedRentalId, renterId, ownerId, ReviewType.OWNER, 1));

        RatingSummary summary = reviewDAO.summariseGear(listingId);

        assertThat(summary.getCount()).isEqualTo(2);
        assertThat(summary.getAverage()).isEqualByComparingTo("4.5");
        assertThat(reviewDAO.findGearReviews(listingId)).hasSize(2);
    }

    @Test
    void userRatingOnlyCountsThatReviewType() {
        reviewDAO.save(review(completedRentalId, renterId, ownerId, ReviewType.OWNER, 3));
        reviewDAO.save(review(completedRentalId, ownerId, renterId, ReviewType.RENTER, 5));

        assertThat(reviewDAO.summariseUser(ownerId, ReviewType.OWNER).getAverage())
                .isEqualByComparingTo("3.0");
        assertThat(reviewDAO.summariseUser(renterId, ReviewType.RENTER).getAverage())
                .isEqualByComparingTo("5.0");
        assertThat(reviewDAO.summariseUser(renterId, ReviewType.OWNER).isEmpty()).isTrue();
    }

    @Test
    void sameReviewCannotBeSavedTwice() {
        reviewDAO.save(review(completedRentalId, renterId, ownerId, ReviewType.GEAR, 5));

        assertThat(reviewDAO.exists(completedRentalId, renterId, ReviewType.GEAR)).isTrue();
        assertThatThrownBy(() ->
                reviewDAO.save(review(completedRentalId, renterId, ownerId, ReviewType.GEAR, 2)))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void summariesByListingOnlyIncludeReviewedListings() {
        Long otherListing = insertListing(ownerId, "Kayak");
        reviewDAO.save(review(completedRentalId, renterId, ownerId, ReviewType.GEAR, 4));

        Map<Long, RatingSummary> summaries =
                reviewDAO.summariseGearByListing(List.of(listingId, otherListing));

        assertThat(summaries).containsOnlyKeys(listingId);
    }

    @Test
    void writtenTypesAreGroupedByRental() {
        reviewDAO.save(review(completedRentalId, renterId, ownerId, ReviewType.GEAR, 4));
        reviewDAO.save(review(completedRentalId, renterId, ownerId, ReviewType.OWNER, 4));

        assertThat(reviewDAO.findTypesWrittenBy(renterId).get(completedRentalId))
                .containsExactlyInAnyOrder(ReviewType.GEAR, ReviewType.OWNER);
    }

    private Review review(Long rentalId, Long reviewerId, Long revieweeId, ReviewType type, int rating) {
        Review review = new Review();
        review.setRentalId(rentalId);
        review.setReviewerId(reviewerId);
        review.setRevieweeId(revieweeId);
        review.setListingId(listingId);
        review.setType(type);
        review.setRating(rating);
        return review;
    }

    private Long insertUser(String name, String email) {
        jdbc.update("INSERT INTO users (name, email, password) VALUES (?, ?, 'x')", name, email);
        return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    private Long insertListing(Long ownerId, String title) {
        jdbc.update("""
                INSERT INTO gear_listings
                (owner_id, title, category, gear_condition, daily_rate,
                 pickup_suburb, pickup_postcode, expiry_date)
                VALUES (?, ?, 'CAMPING', 'GOOD', 20.00, 'Carlton', '3053', ?)
                """, ownerId, title, Date.valueOf(TODAY.plusMonths(3)));
        return jdbc.queryForObject("SELECT MAX(id) FROM gear_listings", Long.class);
    }

    private Long insertRental(Long listingId, Long renterId, LocalDate start, LocalDate end, String status) {
        jdbc.update("""
                INSERT INTO rental_requests (listing_id, renter_id, start_date, end_date, status)
                VALUES (?, ?, ?, ?, ?)
                """, listingId, renterId, Date.valueOf(start), Date.valueOf(end), status);
        return jdbc.queryForObject("SELECT MAX(id) FROM rental_requests", Long.class);
    }
}
