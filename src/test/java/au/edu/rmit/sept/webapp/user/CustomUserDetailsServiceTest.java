package au.edu.rmit.sept.webapp.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    private UserDAO userDAO;
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {
        userDAO = mock(UserDAO.class);
        customUserDetailsService = new CustomUserDetailsService(userDAO);
    }

    private static User user(UserRole role, boolean active) {

        User user = new User();

        user.setId(1L);
        user.setName("Ada Lovelace");
        user.setEmail("ada@example.com");
        user.setPassword("{bcrypt}$2a$10$hashedvaluegoeshere");
        user.setRole(role);
        user.setActive(active);

        return user;
    }

    @Test
    void theStoredHashIsHandedToSpringSecurityUnchanged() {

        when(userDAO.findByEmail("ada@example.com"))
                .thenReturn(Optional.of(user(UserRole.USER, true)));

        UserDetails details =
                customUserDetailsService.loadUserByUsername("ada@example.com");

        assertThat(details.getUsername()).isEqualTo("ada@example.com");
        assertThat(details.getPassword())
                .isEqualTo("{bcrypt}$2a$10$hashedvaluegoeshere");
    }

    @Test
    void anOrdinaryUserGetsTheUserAuthority() {

        when(userDAO.findByEmail(anyString()))
                .thenReturn(Optional.of(user(UserRole.USER, true)));

        UserDetails details =
                customUserDetailsService.loadUserByUsername("ada@example.com");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void anAdminGetsTheAdminAuthority() {

        when(userDAO.findByEmail(anyString()))
                .thenReturn(Optional.of(user(UserRole.ADMIN, true)));

        UserDetails details =
                customUserDetailsService.loadUserByUsername("admin@example.com");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void anActiveAccountIsEnabled() {

        when(userDAO.findByEmail(anyString()))
                .thenReturn(Optional.of(user(UserRole.USER, true)));

        assertThat(customUserDetailsService
                .loadUserByUsername("ada@example.com")
                .isEnabled())
                .isTrue();
    }

    @Test
    void aDeactivatedAccountCannotSignIn() {

        when(userDAO.findByEmail(anyString()))
                .thenReturn(Optional.of(user(UserRole.USER, false)));

        assertThat(customUserDetailsService
                .loadUserByUsername("ada@example.com")
                .isEnabled())
                .isFalse();
    }

    @Test
    void anUnknownEmailIsReportedAsNotFound() {

        when(userDAO.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                customUserDetailsService.loadUserByUsername("ghost@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void theEmailIsNormalisedBeforeTheLookup() {

        when(userDAO.findByEmail("ada@example.com"))
                .thenReturn(Optional.of(user(UserRole.USER, true)));

        assertThat(customUserDetailsService
                .loadUserByUsername("  ADA@Example.COM ")
                .getUsername())
                .isEqualTo("ada@example.com");
    }
}
