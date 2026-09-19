package au.edu.rmit.sept.webapp.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@Import(UserDAO.class)
class UserDAOTest {

    @Autowired
    private UserDAO userDAO;

    private static User userWith(String email) {

        User user = new User();

        user.setName("Ada Lovelace");
        user.setEmail(email);
        user.setPassword("{bcrypt}$2a$10$hashedvaluegoeshere");
        user.setRole(UserRole.USER);
        user.setActive(true);

        return user;
    }

    @Test
    void savedUserIsReturnedWithEveryFieldIntact() {

        userDAO.save(userWith("ada@example.com"));

        User found = userDAO.findByEmail("ada@example.com").orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getName()).isEqualTo("Ada Lovelace");
        assertThat(found.getEmail()).isEqualTo("ada@example.com");
        assertThat(found.getPassword())
                .isEqualTo("{bcrypt}$2a$10$hashedvaluegoeshere");
        assertThat(found.getRole()).isEqualTo(UserRole.USER);
        assertThat(found.isActive()).isTrue();
    }

    @Test
    void adminRoleSurvivesTheRoundTrip() {

        User admin = userWith("admin@example.com");
        admin.setRole(UserRole.ADMIN);

        userDAO.save(admin);

        assertThat(userDAO.findByEmail("admin@example.com").orElseThrow().getRole())
                .isEqualTo(UserRole.ADMIN);
    }

    @Test
    void findByEmailReturnsEmptyForAnUnknownAddress() {

        Optional<User> found = userDAO.findByEmail("nobody@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    void findByIdReturnsTheSameUserAsFindByEmail() {

        userDAO.save(userWith("ada@example.com"));
        Long id = userDAO.findByEmail("ada@example.com").orElseThrow().getId();

        User found = userDAO.findById(id).orElseThrow();

        assertThat(found.getEmail()).isEqualTo("ada@example.com");
        assertThat(found.getName()).isEqualTo("Ada Lovelace");
    }

    @Test
    void findByIdReturnsEmptyForAnUnknownId() {

        Optional<User> found = userDAO.findById(999999L);

        assertThat(found).isEmpty();
    }

    @Test
    void existsByEmailIsTrueOnlyAfterTheUserIsSaved() {

        assertThat(userDAO.existsByEmail("ada@example.com")).isFalse();

        userDAO.save(userWith("ada@example.com"));

        assertThat(userDAO.existsByEmail("ada@example.com")).isTrue();
    }

    @Test
    void theSameEmailCannotBeRegisteredTwice() {

        userDAO.save(userWith("ada@example.com"));

        assertThatThrownBy(() -> userDAO.save(userWith("ada@example.com")))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void updateActiveDeactivatesTheAccount() {

        userDAO.save(userWith("ada@example.com"));

        userDAO.updateActive("ada@example.com", false);

        assertThat(userDAO.findByEmail("ada@example.com").orElseThrow().isActive())
                .isFalse();
    }

    @Test
    void updateActiveCanReactivateTheAccount() {

        userDAO.save(userWith("ada@example.com"));
        userDAO.updateActive("ada@example.com", false);

        userDAO.updateActive("ada@example.com", true);

        assertThat(userDAO.findByEmail("ada@example.com").orElseThrow().isActive())
                .isTrue();
    }
}
