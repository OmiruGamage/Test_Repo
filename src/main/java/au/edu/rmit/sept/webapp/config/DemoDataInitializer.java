package au.edu.rmit.sept.webapp.config;

import au.edu.rmit.sept.webapp.gear.Category;
import au.edu.rmit.sept.webapp.gear.GearCondition;
import au.edu.rmit.sept.webapp.gear.GearListing;
import au.edu.rmit.sept.webapp.gear.GearListingDAO;
import au.edu.rmit.sept.webapp.gear.ListingStatus;
import au.edu.rmit.sept.webapp.user.User;
import au.edu.rmit.sept.webapp.user.UserDAO;
import au.edu.rmit.sept.webapp.user.UserRole;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

/**
 * Demo data for running the app before the booking feature exists.
 * Only active with the "demo" profile:
 *   mvn spring-boot:run -Dspring-boot.run.profiles=demo
 *
 * Creates an owner and a renter, two listings, one completed rental
 * (so both sides can leave reviews) and one pending request
 * (so admin removal can be seen closing it).
 */
@Component
@Profile("demo")
public class DemoDataInitializer implements ApplicationRunner {

    static final String DEMO_PASSWORD = "password123";

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final UserDAO userDAO;
    private final GearListingDAO gearListingDAO;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(
            UserDAO userDAO,
            GearListingDAO gearListingDAO,
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder) {

        this.userDAO = userDAO;
        this.gearListingDAO = gearListingDAO;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {

        if (userDAO.existsByEmail("olivia@demo.gearhub")) {
            return;
        }

        Long ownerId = createUser("Olivia Owner", "olivia@demo.gearhub");
        Long renterId = createUser("Ravi Renter", "ravi@demo.gearhub");

        LocalDate today = LocalDate.now();

        Long tentId = createListing(ownerId, "2-person hiking tent", Category.CAMPING,
                GearCondition.GOOD, "25.00", "Lightweight, sets up in five minutes.");
        Long guitarId = createListing(ownerId, "Acoustic guitar", Category.MUSIC,
                GearCondition.FAIR, "15.00", null);

        // Finished last week: Ravi can rate the tent and Olivia; Olivia can rate Ravi.
        createRental(tentId, renterId, today.minusDays(7), today.minusDays(2), "ACCEPTED");

        // Still waiting on Olivia: closed automatically if an admin removes the listing.
        createRental(guitarId, renterId, today.plusDays(3), today.plusDays(5), "PENDING");

        log.info("Demo data loaded. Log in as olivia@demo.gearhub or ravi@demo.gearhub "
                + "with password '{}'", DEMO_PASSWORD);
    }

    private Long createUser(String name, String email) {

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        user.setRole(UserRole.USER);
        user.setActive(true);

        userDAO.save(user);

        return userDAO.findByEmail(email).orElseThrow().getId();
    }

    private Long createListing(
            Long ownerId,
            String title,
            Category category,
            GearCondition condition,
            String dailyRate,
            String description) {

        GearListing listing = new GearListing();
        listing.setOwnerId(ownerId);
        listing.setTitle(title);
        listing.setCategory(category);
        listing.setCondition(condition);
        listing.setDailyRate(new BigDecimal(dailyRate));
        listing.setPickupSuburb("Carlton");
        listing.setPickupPostcode("3053");
        listing.setDescription(description);
        listing.setExpiryDate(LocalDate.now().plusMonths(3));
        listing.setStatus(ListingStatus.PUBLISHED);

        return gearListingDAO.save(listing);
    }

    private void createRental(
            Long listingId,
            Long renterId,
            LocalDate start,
            LocalDate end,
            String status) {

        jdbcTemplate.update(
                "INSERT INTO rental_requests (listing_id, renter_id, start_date, end_date, status)"
                        + " VALUES (?, ?, ?, ?, ?)",
                listingId, renterId, Date.valueOf(start), Date.valueOf(end), status);
    }
}
