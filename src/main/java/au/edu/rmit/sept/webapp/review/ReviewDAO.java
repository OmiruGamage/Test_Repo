package au.edu.rmit.sept.webapp.review;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class ReviewDAO {

    private static final String REVIEW_VIEW_QUERY = """
            SELECT rv.id, rv.review_type, rv.rating, rv.comment, rv.created_at,
                   rv.listing_id, g.title AS listing_title,
                   rv.reviewer_id, ur.name AS reviewer_name,
                   rv.reviewee_id, ue.name AS reviewee_name
            FROM reviews rv
            JOIN gear_listings g ON g.id = rv.listing_id
            JOIN users ur ON ur.id = rv.reviewer_id
            JOIN users ue ON ue.id = rv.reviewee_id
            """;

    private static final String AVERAGE_COLUMNS =
            "COUNT(*) AS review_count, AVG(CAST(rating AS DECIMAL(10,2))) AS average_rating";

    private static final RowMapper<ReviewView> VIEW_MAPPER = (rs, rowNum) ->
            new ReviewView(
                    rs.getLong("id"),
                    ReviewType.valueOf(rs.getString("review_type")),
                    rs.getInt("rating"),
                    rs.getString("comment"),
                    rs.getLong("listing_id"),
                    rs.getString("listing_title"),
                    rs.getLong("reviewer_id"),
                    rs.getString("reviewer_name"),
                    rs.getLong("reviewee_id"),
                    rs.getString("reviewee_name"),
                    rs.getTimestamp("created_at").toLocalDateTime()
            );

    private static final RowMapper<RatingSummary> SUMMARY_MAPPER = (rs, rowNum) ->
            RatingSummary.of(
                    rs.getLong("review_count"),
                    rs.getBigDecimal("average_rating")
            );

    private final JdbcTemplate jdbcTemplate;

    public ReviewDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long save(Review review) {

        String sql = """
                INSERT INTO reviews
                (rental_id, reviewer_id, reviewee_id, listing_id,
                 review_type, rating, comment)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {

            PreparedStatement ps = connection.prepareStatement(
                    sql,
                    new String[] { "id" }
            );

            ps.setLong(1, review.getRentalId());
            ps.setLong(2, review.getReviewerId());
            ps.setLong(3, review.getRevieweeId());
            ps.setLong(4, review.getListingId());
            ps.setString(5, review.getType().name());
            ps.setInt(6, review.getRating());

            if (review.getComment() == null) {
                ps.setNull(7, Types.VARCHAR);
            } else {
                ps.setString(7, review.getComment());
            }

            return ps;

        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    public boolean exists(Long rentalId, Long reviewerId, ReviewType type) {

        String sql = """
                SELECT COUNT(*)
                FROM reviews
                WHERE rental_id = ? AND reviewer_id = ? AND review_type = ?
                """;

        Integer count = jdbcTemplate.queryForObject(
                sql, Integer.class, rentalId, reviewerId, type.name());

        return count != null && count > 0;
    }

    /** rentalId -> review types this reviewer has already written for it. */
    public Map<Long, Set<ReviewType>> findTypesWrittenBy(Long reviewerId) {

        String sql = """
                SELECT rental_id, review_type
                FROM reviews
                WHERE reviewer_id = ?
                """;

        Map<Long, Set<ReviewType>> written = new HashMap<>();

        jdbcTemplate.query(
                sql,
                rs -> {
                    written.computeIfAbsent(
                            rs.getLong("rental_id"),
                            key -> EnumSet.noneOf(ReviewType.class)
                    ).add(ReviewType.valueOf(rs.getString("review_type")));
                },
                reviewerId
        );

        return written;
    }

    public List<ReviewView> findGearReviews(Long listingId) {

        String sql = REVIEW_VIEW_QUERY
                + " WHERE rv.review_type = 'GEAR' AND rv.listing_id = ?"
                + " ORDER BY rv.created_at DESC, rv.id DESC";

        return jdbcTemplate.query(sql, VIEW_MAPPER, listingId);
    }

    public List<ReviewView> findReviewsAbout(Long revieweeId, ReviewType type) {

        String sql = REVIEW_VIEW_QUERY
                + " WHERE rv.review_type = ? AND rv.reviewee_id = ?"
                + " ORDER BY rv.created_at DESC, rv.id DESC";

        return jdbcTemplate.query(sql, VIEW_MAPPER, type.name(), revieweeId);
    }

    public List<ReviewView> findReviewsWrittenBy(Long reviewerId) {

        String sql = REVIEW_VIEW_QUERY
                + " WHERE rv.reviewer_id = ?"
                + " ORDER BY rv.created_at DESC, rv.id DESC";

        return jdbcTemplate.query(sql, VIEW_MAPPER, reviewerId);
    }

    public RatingSummary summariseGear(Long listingId) {

        String sql = "SELECT " + AVERAGE_COLUMNS
                + " FROM reviews WHERE review_type = 'GEAR' AND listing_id = ?";

        return jdbcTemplate.queryForObject(sql, SUMMARY_MAPPER, listingId);
    }

    public RatingSummary summariseUser(Long revieweeId, ReviewType type) {

        String sql = "SELECT " + AVERAGE_COLUMNS
                + " FROM reviews WHERE review_type = ? AND reviewee_id = ?";

        return jdbcTemplate.queryForObject(sql, SUMMARY_MAPPER, type.name(), revieweeId);
    }

    /** listingId -> gear rating, only for listings that have at least one review. */
    public Map<Long, RatingSummary> summariseGearByListing(List<Long> listingIds) {

        if (listingIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = listingIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String sql = "SELECT listing_id, " + AVERAGE_COLUMNS
                + " FROM reviews"
                + " WHERE review_type = 'GEAR'"
                + " AND listing_id IN (" + placeholders + ")"
                + " GROUP BY listing_id";

        Map<Long, RatingSummary> summaries = new HashMap<>();

        jdbcTemplate.query(
                sql,
                rs -> {
                    summaries.put(
                            rs.getLong("listing_id"),
                            RatingSummary.of(
                                    rs.getLong("review_count"),
                                    rs.getBigDecimal("average_rating")
                            )
                    );
                },
                listingIds.toArray()
        );

        return summaries;
    }
}
