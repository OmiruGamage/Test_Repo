package au.edu.rmit.sept.webapp.review;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Read-only access to completed rentals. A rental is completed when it was
 * accepted (or explicitly marked COMPLETED) and its end date has passed.
 */
@Repository
public class RentalLookupDAO {

    private static final String COMPLETED_RENTALS = """
            SELECT r.id AS rental_id, r.listing_id, g.title AS listing_title,
                   g.owner_id, o.name AS owner_name,
                   r.renter_id, rn.name AS renter_name,
                   r.start_date, r.end_date
            FROM rental_requests r
            JOIN gear_listings g ON g.id = r.listing_id
            JOIN users o ON o.id = g.owner_id
            JOIN users rn ON rn.id = r.renter_id
            WHERE r.status IN ('ACCEPTED', 'COMPLETED')
              AND r.end_date < ?
            """;

    private static final RowMapper<CompletedRental> ROW_MAPPER = (rs, rowNum) ->
            new CompletedRental(
                    rs.getLong("rental_id"),
                    rs.getLong("listing_id"),
                    rs.getString("listing_title"),
                    rs.getLong("owner_id"),
                    rs.getString("owner_name"),
                    rs.getLong("renter_id"),
                    rs.getString("renter_name"),
                    rs.getDate("start_date").toLocalDate(),
                    rs.getDate("end_date").toLocalDate()
            );

    private final JdbcTemplate jdbcTemplate;

    public RentalLookupDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<CompletedRental> findCompletedById(Long rentalId, LocalDate today) {

        String sql = COMPLETED_RENTALS + " AND r.id = ?";

        return jdbcTemplate.query(sql, ROW_MAPPER, Date.valueOf(today), rentalId)
                .stream()
                .findFirst();
    }

    public List<CompletedRental> findCompletedForUser(Long userId, LocalDate today) {

        String sql = COMPLETED_RENTALS
                + " AND (r.renter_id = ? OR g.owner_id = ?)"
                + " ORDER BY r.end_date DESC, r.id DESC";

        return jdbcTemplate.query(sql, ROW_MAPPER, Date.valueOf(today), userId, userId);
    }
}
