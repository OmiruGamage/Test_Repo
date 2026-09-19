package au.edu.rmit.sept.webapp.admin;

import au.edu.rmit.sept.webapp.gear.Category;
import au.edu.rmit.sept.webapp.gear.ListingStatus;
import au.edu.rmit.sept.webapp.user.UserRole;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AdminDAO {

    private static final String USER_QUERY = """
            SELECT id, name, email, role, active
            FROM users
            """;

    private static final String LISTING_QUERY = """
            SELECT g.id, g.title, g.owner_id, u.name AS owner_name,
                   u.active AS owner_active, g.category, g.daily_rate,
                   g.expiry_date, g.status, g.created_at
            FROM gear_listings g
            JOIN users u ON u.id = g.owner_id
            """;

    private static final String ACTION_QUERY = """
            SELECT a.id, a.action_type, a.target_id, a.reason, a.created_at,
                   ad.name AS admin_name,
                   COALESCE(tu.name, tl.title) AS target_label
            FROM admin_actions a
            JOIN users ad ON ad.id = a.admin_id
            LEFT JOIN users tu
                   ON a.target_type = 'USER' AND tu.id = a.target_id
            LEFT JOIN gear_listings tl
                   ON a.target_type = 'LISTING' AND tl.id = a.target_id
            """;

    private static final RowMapper<AdminUserView> USER_MAPPER = (rs, rowNum) ->
            new AdminUserView(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    UserRole.valueOf(rs.getString("role")),
                    rs.getBoolean("active")
            );

    private static final RowMapper<AdminListingView> LISTING_MAPPER = (rs, rowNum) ->
            new AdminListingView(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getLong("owner_id"),
                    rs.getString("owner_name"),
                    rs.getBoolean("owner_active"),
                    Category.valueOf(rs.getString("category")),
                    rs.getBigDecimal("daily_rate"),
                    rs.getDate("expiry_date").toLocalDate(),
                    ListingStatus.valueOf(rs.getString("status")),
                    rs.getTimestamp("created_at").toLocalDateTime()
            );

    private static final RowMapper<AdminActionView> ACTION_MAPPER = (rs, rowNum) ->
            new AdminActionView(
                    rs.getLong("id"),
                    rs.getString("admin_name"),
                    AdminActionType.valueOf(rs.getString("action_type")),
                    rs.getLong("target_id"),
                    rs.getString("target_label"),
                    rs.getString("reason"),
                    rs.getTimestamp("created_at").toLocalDateTime()
            );

    private final JdbcTemplate jdbcTemplate;

    public AdminDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ---------- users ----------

    public List<AdminUserView> findUsers(String search, UserRole role, Boolean active) {

        StringBuilder sql = new StringBuilder(USER_QUERY).append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (search != null) {
            String like = "%" + search.toLowerCase() + "%";
            sql.append(" AND (LOWER(name) LIKE ? OR LOWER(email) LIKE ?)");
            params.add(like);
            params.add(like);
        }

        if (role != null) {
            sql.append(" AND role = ?");
            params.add(role.name());
        }

        if (active != null) {
            sql.append(" AND active = ?");
            params.add(active);
        }

        sql.append(" ORDER BY LOWER(name), id");

        return jdbcTemplate.query(sql.toString(), USER_MAPPER, params.toArray());
    }

    public Optional<AdminUserView> findUserById(Long id) {

        return jdbcTemplate.query(USER_QUERY + " WHERE id = ?", USER_MAPPER, id)
                .stream()
                .findFirst();
    }

    public int setUserActive(Long userId, boolean active) {
        return jdbcTemplate.update("UPDATE users SET active = ? WHERE id = ?", active, userId);
    }

    // ---------- listings ----------

    public List<AdminListingView> findListings(String search, ListingStatus status) {

        StringBuilder sql = new StringBuilder(LISTING_QUERY).append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (search != null) {
            String like = "%" + search.toLowerCase() + "%";
            sql.append(" AND (LOWER(g.title) LIKE ? OR LOWER(u.name) LIKE ?)");
            params.add(like);
            params.add(like);
        }

        if (status != null) {
            sql.append(" AND g.status = ?");
            params.add(status.name());
        }

        sql.append(" ORDER BY g.created_at DESC, g.id DESC");

        return jdbcTemplate.query(sql.toString(), LISTING_MAPPER, params.toArray());
    }

    public List<AdminListingView> findListingsByOwner(Long ownerId) {

        return jdbcTemplate.query(
                LISTING_QUERY + " WHERE g.owner_id = ? ORDER BY g.created_at DESC, g.id DESC",
                LISTING_MAPPER,
                ownerId);
    }

    public Optional<AdminListingView> findListingById(Long id) {

        return jdbcTemplate.query(LISTING_QUERY + " WHERE g.id = ?", LISTING_MAPPER, id)
                .stream()
                .findFirst();
    }

    public int setListingStatus(Long listingId, ListingStatus status) {

        return jdbcTemplate.update(
                "UPDATE gear_listings SET status = ? WHERE id = ?",
                status.name(),
                listingId);
    }

    public int removePublishedListingsByOwner(Long ownerId) {

        return jdbcTemplate.update(
                "UPDATE gear_listings SET status = 'REMOVED'"
                        + " WHERE owner_id = ? AND status = 'PUBLISHED'",
                ownerId);
    }

    /** Brings stored statuses up to date so the admin list matches what users see. */
    public int expireOverdueListings(LocalDate today) {

        return jdbcTemplate.update(
                "UPDATE gear_listings SET status = 'EXPIRED'"
                        + " WHERE status = 'PUBLISHED' AND expiry_date < ?",
                Date.valueOf(today));
    }

    // ---------- rental requests ----------

    public int closePendingRequestsForListing(Long listingId) {

        return jdbcTemplate.update(
                "UPDATE rental_requests SET status = 'CLOSED'"
                        + " WHERE status = 'PENDING' AND listing_id = ?",
                listingId);
    }

    public int closePendingRequestsForUser(Long userId) {

        return jdbcTemplate.update(
                "UPDATE rental_requests SET status = 'CLOSED'"
                        + " WHERE status = 'PENDING'"
                        + " AND (renter_id = ?"
                        + "      OR listing_id IN (SELECT id FROM gear_listings WHERE owner_id = ?))",
                userId,
                userId);
    }

    // ---------- action history ----------

    public void recordAction(Long adminId, AdminActionType type, Long targetId, String reason) {

        jdbcTemplate.update(
                "INSERT INTO admin_actions (admin_id, action_type, target_type, target_id, reason)"
                        + " VALUES (?, ?, ?, ?, ?)",
                adminId,
                type.name(),
                type.getTarget().name(),
                targetId,
                reason);
    }

    public List<AdminActionView> findRecentActions(int limit) {

        return jdbcTemplate.query(
                ACTION_QUERY + " ORDER BY a.created_at DESC, a.id DESC LIMIT ?",
                ACTION_MAPPER,
                limit);
    }

    /** Actions on the user themself and on any of their listings. */
    public List<AdminActionView> findActionsForUser(Long userId) {

        return jdbcTemplate.query(
                ACTION_QUERY
                        + " WHERE (a.target_type = 'USER' AND a.target_id = ?)"
                        + " OR (a.target_type = 'LISTING' AND tl.owner_id = ?)"
                        + " ORDER BY a.created_at DESC, a.id DESC",
                ACTION_MAPPER,
                userId,
                userId);
    }
}
