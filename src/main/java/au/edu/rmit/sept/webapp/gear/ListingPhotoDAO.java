package au.edu.rmit.sept.webapp.gear;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ListingPhotoDAO {

    private static final RowMapper<ListingPhoto> ROW_MAPPER = (rs, rowNum) ->
            new ListingPhoto(
                    rs.getLong("id"),
                    rs.getLong("listing_id"),
                    rs.getString("content_type"),
                    rs.getBytes("data"),
                    rs.getInt("display_order")
            );

    private final JdbcTemplate jdbcTemplate;

    public ListingPhotoDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long save(
            Long listingId,
            String contentType,
            byte[] data,
            int displayOrder) {

        String sql = """
                INSERT INTO listing_photos
                (listing_id, content_type, data, display_order)
                VALUES (?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {

            PreparedStatement ps = connection.prepareStatement(
                    sql,
                    new String[] { "id" }
            );

            ps.setLong(1, listingId);
            ps.setString(2, contentType);
            ps.setBytes(3, data);
            ps.setInt(4, displayOrder);

            return ps;

        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    public Optional<ListingPhoto> findById(Long id) {

        String sql = """
                SELECT id, listing_id, content_type, data, display_order
                FROM listing_photos
                WHERE id = ?
                """;

        return jdbcTemplate.query(sql, ROW_MAPPER, id)
                .stream()
                .findFirst();
    }

    public Map<Long, Long> findFirstPhotoIdsByListingIds(List<Long> listingIds) {

        if (listingIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = listingIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String sql = """
                SELECT id, listing_id
                FROM listing_photos
                WHERE display_order = 0
                AND listing_id IN (%s)
                """.formatted(placeholders);

        Map<Long, Long> firstPhotoIdsByListingId = new HashMap<>();

        jdbcTemplate.query(
                sql,
                rs -> {
                    firstPhotoIdsByListingId.put(
                            rs.getLong("listing_id"),
                            rs.getLong("id")
                    );
                },
                listingIds.toArray()
        );

        return firstPhotoIdsByListingId;
    }
}
