package au.edu.rmit.sept.webapp.config;

import au.edu.rmit.sept.webapp.user.User;
import au.edu.rmit.sept.webapp.user.UserDAO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring Security only checks "disabled" at login time. This signs out anyone
 * whose account an admin has deactivated while they were already logged in.
 */
@Component
public class ActiveAccountInterceptor implements HandlerInterceptor {

    private final UserDAO userDAO;

    public ActiveAccountInterceptor(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return true;
        }

        boolean active = userDAO
                .findByEmail(authentication.getName().trim().toLowerCase())
                .map(User::isActive)
                .orElse(false);

        if (active) {
            return true;
        }

        request.logout();
        response.sendRedirect(request.getContextPath() + "/login?deactivated");

        return false;
    }
}
