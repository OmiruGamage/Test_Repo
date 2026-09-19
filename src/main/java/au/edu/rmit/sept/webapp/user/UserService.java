package au.edu.rmit.sept.webapp.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserDAO userDao;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserDAO UserDao,
            PasswordEncoder passwordEncoder) {

        this.userDao = UserDao;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(RegistrationRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        if (userDao.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists"
            );
        }

        User user = new User();

        user.setName(request.getName().trim());
        user.setEmail(email);

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        user.setRole(UserRole.USER);
        user.setActive(true);

        userDao.save(user);
    }

    public User findByEmail(String email) {

        return userDao
                .findByEmail(
                        email.trim().toLowerCase()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );
    }

    public User findById(Long id) {

        return userDao
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );
    }

    public void deactivate(String email) {

        String normalizedEmail =
                email.trim().toLowerCase();

        userDao.updateActive(
                normalizedEmail,
                false
        );
    }
}
