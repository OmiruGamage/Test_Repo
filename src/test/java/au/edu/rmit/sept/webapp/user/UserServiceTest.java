package au.edu.rmit.sept.webapp.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserDAO userDAO;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userDAO = mock(UserDAO.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userService = new UserService(userDAO, passwordEncoder);
    }

    private static RegistrationRequest request(String name, String email, String password) {

        RegistrationRequest request = new RegistrationRequest();

        request.setName(name);
        request.setEmail(email);
        request.setPassword(password);

        return request;
    }

    private User capturedUser() {

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDAO).save(captor.capture());

        return captor.getValue();
    }

    @Test
    void theRawPasswordIsNeverStored() {

        when(passwordEncoder.encode("hunter2")).thenReturn("ENCODED");

        userService.register(request("Ada", "ada@example.com", "hunter2"));

        User saved = capturedUser();

        assertThat(saved.getPassword()).isEqualTo("ENCODED");
        assertThat(saved.getPassword()).isNotEqualTo("hunter2");
    }

    @Test
    void newAccountsAreActiveAndGetTheDefaultRole() {

        when(passwordEncoder.encode(anyString())).thenReturn("ENCODED");

        userService.register(request("Ada", "ada@example.com", "hunter2"));

        User saved = capturedUser();

        assertThat(saved.getRole()).isEqualTo(UserRole.USER);
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void emailIsLowercasedAndTrimmedBeforeItIsStored() {

        when(passwordEncoder.encode(anyString())).thenReturn("ENCODED");

        userService.register(request("Ada", "  Ada@EXAMPLE.com  ", "hunter2"));

        assertThat(capturedUser().getEmail()).isEqualTo("ada@example.com");
    }

    @Test
    void nameIsTrimmedBeforeItIsStored() {

        when(passwordEncoder.encode(anyString())).thenReturn("ENCODED");

        userService.register(request("  Ada Lovelace  ", "ada@example.com", "hunter2"));

        assertThat(capturedUser().getName()).isEqualTo("Ada Lovelace");
    }

    @Test
    void registeringAnEmailThatAlreadyExistsIsRejected() {

        when(userDAO.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() ->
                userService.register(request("Ada", "ada@example.com", "hunter2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(userDAO, never()).save(any());
    }

    @Test
    void duplicateDetectionIgnoresCasingAndSurroundingSpaces() {

        when(userDAO.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() ->
                userService.register(request("Ada", "  ADA@Example.COM ", "hunter2")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findByEmailNormalisesTheAddressBeforeLookingItUp() {

        User user = new User();
        when(userDAO.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

        assertThat(userService.findByEmail("  ADA@Example.COM ")).isSameAs(user);
    }

    @Test
    void findByEmailThrowsWhenNobodyMatches() {

        when(userDAO.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByEmail("ghost@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void findByIdReturnsTheMatchingUser() {

        User user = new User();
        when(userDAO.findById(7L)).thenReturn(Optional.of(user));

        assertThat(userService.findById(7L)).isSameAs(user);
    }

    @Test
    void findByIdThrowsWhenNobodyMatches() {

        when(userDAO.findById(999999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(999999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void deactivateNormalisesTheAddressAndFlagsTheAccountInactive() {

        userService.deactivate("  ADA@Example.COM ");

        verify(userDAO).updateActive("ada@example.com", false);
    }
}
