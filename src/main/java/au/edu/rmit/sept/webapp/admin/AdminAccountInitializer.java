package au.edu.rmit.sept.webapp.admin;

import au.edu.rmit.sept.webapp.user.User;
import au.edu.rmit.sept.webapp.user.UserDAO;
import au.edu.rmit.sept.webapp.user.UserRole;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Admin accounts can't be self-registered, so one is provisioned on start-up
 * from configuration (gearhub.admin.*). Skipped if the password is not set.
 */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String name;

    public AdminAccountInitializer(
            UserDAO userDAO,
            PasswordEncoder passwordEncoder,
            @Value("${gearhub.admin.email:admin@gearhub.local}") String email,
            @Value("${gearhub.admin.password:}") String password,
            @Value("${gearhub.admin.name:GearHub Admin}") String name) {

        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    @Override
    public void run(ApplicationArguments args) {

        if (password == null || password.isBlank()) {
            log.warn("gearhub.admin.password is not set, so no admin account was created");
            return;
        }

        String normalisedEmail = email.trim().toLowerCase();

        if (userDAO.existsByEmail(normalisedEmail)) {
            return;
        }

        User admin = new User();
        admin.setName(name);
        admin.setEmail(normalisedEmail);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);

        userDAO.save(admin);

        log.info("Created admin account {}", normalisedEmail);
    }
}
