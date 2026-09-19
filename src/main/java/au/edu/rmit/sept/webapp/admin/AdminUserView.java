package au.edu.rmit.sept.webapp.admin;

import au.edu.rmit.sept.webapp.user.UserRole;

/** User row for the admin area. Never carries the password hash. */
public class AdminUserView {

    private final Long id;
    private final String name;
    private final String email;
    private final UserRole role;
    private final boolean active;

    public AdminUserView(Long id, String name, String email, UserRole role, boolean active) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }
}
