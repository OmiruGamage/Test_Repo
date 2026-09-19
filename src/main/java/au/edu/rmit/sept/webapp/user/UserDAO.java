package au.edu.rmit.sept.webapp.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserDAO {

    private final JdbcTemplate jdbcTemplate;

    public UserDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByEmail(String email) {

        String sql = """
                SELECT id, name, email, password, role, active
                FROM users
                WHERE email = ?
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new User(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password"),
                        UserRole.valueOf(
                                rs.getString("role")
                        ),
                        rs.getBoolean("active")
                ),
                email
        ).stream().findFirst();
    }

    public Optional<User> findById(Long id) {

        String sql = """
                SELECT id, name, email, password, role, active
                FROM users
                WHERE id = ?
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new User(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password"),
                        UserRole.valueOf(
                                rs.getString("role")
                        ),
                        rs.getBoolean("active")
                ),
                id
        ).stream().findFirst();
    }

    public boolean existsByEmail(String email) {

        String sql = """
                SELECT COUNT(*)
                FROM users
                WHERE email = ?
                """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                email
        );

        return count != null && count > 0;
    }

    public void save(User user) {

        String sql = """
                INSERT INTO users
                (name, email, password, role, active)
                VALUES (?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.getRole().name(),
                user.isActive()
        );
    }

    public void updateActive(
            String email,
            boolean active) {

        String sql = """
                UPDATE users
                SET active = ?
                WHERE email = ?
                """;

        jdbcTemplate.update(
                sql,
                active,
                email
        );
    }
}