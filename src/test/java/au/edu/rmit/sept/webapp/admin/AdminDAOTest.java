package au.edu.rmit.sept.webapp.admin;

import au.edu.rmit.sept.webapp.gear.ListingStatus;
import au.edu.rmit.sept.webapp.user.UserRole;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.sql.Date;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AdminDAOTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 19);

    private EmbeddedDatabase database;
    private JdbcTemplate jdbc;
    private AdminDAO adminDAO;

    private Long adminId;
    private Long ownerId;
    private Long renterId;

    @BeforeEach
    void setUp() {
        database = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:schema.sql")
                .build();

        jdbc = new JdbcTemplate(database);
        adminDAO = new AdminDAO(jdbc);

        adminId = insertUser("Ada Admin", "admin@test.com", "ADMIN");
        ownerId = insertUser("Olivia Owner", "olivia@test.com", "USER");
        renterId = insertUser("Ravi Renter", "ravi@test.com", "USER");
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    void searchMatchesNameOrEmailCaseInsensitively() {
        assertThat(adminDAO.findUsers("OLIVIA", null, null)).hasSize(1);
        assertThat(adminDAO.findUsers("@test.com", null, null)).hasSize(3);
        assertThat(adminDAO.findUsers(null, UserRole.ADMIN, null)).hasSize(1);
    }

    @Test
    void activeFilterReflectsDeactivation() {
        adminDAO.setUserActive(ownerId, false);

        assertThat(adminDAO.findUsers(null, null, false))
                .extracting(AdminUserView::getId)
                .containsExactly(ownerId);
    }

    @Test
    void deactivationUpdatesOnlyPublishedListingsAndPendingRequests() {
        Long published = insertListing(ownerId, "PUBLISHED", TODAY.plusDays(10));
        Long expired = insertListing(ownerId, "EXPIRED", TODAY.minusDays(1));
        Long pending = insertRental(published, renterId, "PENDING");
        Long accepted = insertRental(published, renterId, "ACCEPTED");

        adminDAO.removePublishedListingsByOwner(ownerId);
        adminDAO.closePendingRequestsForUser(ownerId);

        assertThat(adminDAO.findListingById(published).orElseThrow().getStatus())
                .isEqualTo(ListingStatus.REMOVED);
        assertThat(adminDAO.findListingById(expired).orElseThrow().getStatus())
                .isEqualTo(ListingStatus.EXPIRED);
        assertThat(rentalStatus(pending)).isEqualTo("CLOSED");
        assertThat(rentalStatus(accepted)).isEqualTo("ACCEPTED");
    }

    @Test
    void overdueListingsAreExpired() {
        Long overdue = insertListing(ownerId, "PUBLISHED", TODAY.minusDays(1));
        Long current = insertListing(ownerId, "PUBLISHED", TODAY);

        adminDAO.expireOverdueListings(TODAY);

        assertThat(adminDAO.findListingById(overdue).orElseThrow().getStatus())
                .isEqualTo(ListingStatus.EXPIRED);
        assertThat(adminDAO.findListingById(current).orElseThrow().getStatus())
                .isEqualTo(ListingStatus.PUBLISHED);
    }

    @Test
    void actionHistoryShowsTargetNamesAndListingActionsForOwner() {
        Long listing = insertListing(ownerId, "PUBLISHED", TODAY.plusDays(10));

        adminDAO.recordAction(adminId, AdminActionType.REMOVE_LISTING, listing, "Offensive");
        adminDAO.recordAction(adminId, AdminActionType.DEACTIVATE_USER, ownerId, "Repeat offender");

        assertThat(adminDAO.findActionsForUser(ownerId))
                .extracting(AdminActionView::getTargetLabel)
                .containsExactlyInAnyOrder("Tent", "Olivia Owner");
        assertThat(adminDAO.findActionsForUser(renterId)).isEmpty();
        assertThat(adminDAO.findRecentActions(10)).hasSize(2);
    }

    private Long insertUser(String name, String email, String role) {
        jdbc.update("INSERT INTO users (name, email, password, role) VALUES (?, ?, 'x', ?)",
                name, email, role);
        return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    private Long insertListing(Long owner, String status, LocalDate expiry) {
        jdbc.update("""
                INSERT INTO gear_listings
                (owner_id, title, category, gear_condition, daily_rate,
                 pickup_suburb, pickup_postcode, expiry_date, status)
                VALUES (?, 'Tent', 'CAMPING', 'GOOD', 20.00, 'Carlton', '3053', ?, ?)
                """, owner, Date.valueOf(expiry), status);
        return jdbc.queryForObject("SELECT MAX(id) FROM gear_listings", Long.class);
    }

    private Long insertRental(Long listing, Long renter, String status) {
        jdbc.update("""
                INSERT INTO rental_requests (listing_id, renter_id, start_date, end_date, status)
                VALUES (?, ?, ?, ?, ?)
                """, listing, renter, Date.valueOf(TODAY.plusDays(1)), Date.valueOf(TODAY.plusDays(3)), status);
        return jdbc.queryForObject("SELECT MAX(id) FROM rental_requests", Long.class);
    }

    private String rentalStatus(Long id) {
        return jdbc.queryForObject("SELECT status FROM rental_requests WHERE id = ?", String.class, id);
    }
}
