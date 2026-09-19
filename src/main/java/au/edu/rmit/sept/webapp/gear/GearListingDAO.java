package au.edu.rmit.sept.webapp.gear;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class GearListingDAO {

    private static final RowMapper<GearListing> ROW_MAPPER = (rs, rowNum) ->
            new GearListing(
                    rs.getLong("id"),
                    rs.getLong("owner_id"),
                    rs.getString("title"),
                    Category.valueOf(rs.getString("category")),
                    GearCondition.valueOf(rs.getString("gear_condition")),
                    rs.getBigDecimal("daily_rate"),
                    rs.getString("pickup_suburb"),
                    rs.getString("pickup_postcode"),
                    rs.getString("description"),
                    rs.getDate("expiry_date").toLocalDate(),
                    ListingStatus.valueOf(rs.getString("status")),
                    rs.getTimestamp("created_at").toLocalDateTime()
            );

    private final JdbcTemplate jdbcTemplate;

    public GearListingDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long save(GearListing listing) {

        String sql = """
                INSERT INTO gear_listings
                (owner_id, title, category, gear_condition, daily_rate,
                 pickup_suburb, pickup_postcode, description,
                 expiry_date, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {

            PreparedStatement ps = connection.prepareStatement(
                    sql,
                    new String[] { "id" }
            );

            ps.setLong(1, listing.getOwnerId());
            ps.setString(2, listing.getTitle());
            ps.setString(3, listing.getCategory().name());
            ps.setString(4, listing.getCondition().name());
            ps.setBigDecimal(5, listing.getDailyRate());
            ps.setString(6, listing.getPickupSuburb());
            ps.setString(7, listing.getPickupPostcode());
            ps.setString(8, listing.getDescription());
            ps.setDate(9, Date.valueOf(listing.getExpiryDate()));
            ps.setString(10, listing.getStatus().name());

            return ps;

        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    public Optional<GearListing> findById(Long id) {

        String sql = """
                SELECT id, owner_id, title, category, gear_condition, daily_rate,
                       pickup_suburb, pickup_postcode, description,
                       expiry_date, status, created_at
                FROM gear_listings
                WHERE id = ?
                """;

        return jdbcTemplate.query(sql, ROW_MAPPER, id)
                .stream()
                .findFirst()
                .map(this::applyExpiryTransition);
    }

    public List<GearListing> findByOwnerId(Long ownerId) {

        String sql = """
                SELECT id, owner_id, title, category, gear_condition, daily_rate,
                       pickup_suburb, pickup_postcode, description,
                       expiry_date, status, created_at
                FROM gear_listings
                WHERE owner_id = ?
                ORDER BY created_at DESC
                """;

        return jdbcTemplate.query(sql, ROW_MAPPER, ownerId)
                .stream()
                .map(this::applyExpiryTransition)
                .toList();
    }

    private GearListing applyExpiryTransition(GearListing listing) {

        if (listing.getStatus() == ListingStatus.PUBLISHED
                && listing.getExpiryDate().isBefore(LocalDate.now())) {

            listing.setStatus(ListingStatus.EXPIRED);
            update(listing);
        }

        return listing;
    }

    public int update(GearListing listing) {

        String sql = """
                UPDATE gear_listings
                SET title = ?, category = ?, gear_condition = ?, daily_rate = ?,
                    pickup_suburb = ?, pickup_postcode = ?, description = ?,
                    expiry_date = ?, status = ?
                WHERE id = ?
                """;

        return jdbcTemplate.update(
                sql,
                listing.getTitle(),
                listing.getCategory().name(),
                listing.getCondition().name(),
                listing.getDailyRate(),
                listing.getPickupSuburb(),
                listing.getPickupPostcode(),
                listing.getDescription(),
                Date.valueOf(listing.getExpiryDate()),
                listing.getStatus().name(),
                listing.getId()
        );
    }

    public List<GearListing> search(SearchFilters filters) {

        StringBuilder sql = new StringBuilder("""
                SELECT id, owner_id, title, category, gear_condition, daily_rate,
                       pickup_suburb, pickup_postcode, description,
                       expiry_date, status, created_at
                FROM gear_listings
                WHERE status = 'PUBLISHED'
                """);

        List<Object> params = new ArrayList<>();

        if (filters.getCategory() != null) {
            sql.append(" AND category = ?");
            params.add(filters.getCategory().name());
        }

        if (filters.getCondition() != null) {
            sql.append(" AND gear_condition = ?");
            params.add(filters.getCondition().name());
        }

        if (filters.getPickupSuburb() != null) {
            sql.append(" AND LOWER(pickup_suburb) = LOWER(?)");
            params.add(filters.getPickupSuburb());
        }

        if (filters.getPickupPostcode() != null) {
            sql.append(" AND pickup_postcode = ?");
            params.add(filters.getPickupPostcode());
        }

        if (filters.getMinRate() != null) {
            sql.append(" AND daily_rate >= ?");
            params.add(filters.getMinRate());
        }

        if (filters.getMaxRate() != null) {
            sql.append(" AND daily_rate <= ?");
            params.add(filters.getMaxRate());
        }

        sql.append(" ORDER BY created_at DESC");

        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray())
                .stream()
                .map(this::applyExpiryTransition)
                .filter(listing -> listing.getStatus() == ListingStatus.PUBLISHED)
                .toList();
    }
}
