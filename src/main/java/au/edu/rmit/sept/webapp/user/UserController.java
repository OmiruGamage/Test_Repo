package au.edu.rmit.sept.webapp.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String showRegistrationPage(Model model) {
        model.addAttribute(
                "registrationRequest",
                new RegistrationRequest()
        );

        return "register";
    }

    @PostMapping("/register")
    public String register(
            @ModelAttribute RegistrationRequest request,
            Model model) {

        try {
            userService.register(request);

            return "redirect:/login?registered";

        } catch (IllegalArgumentException e) {
            model.addAttribute(
                    "error",
                    e.getMessage()
            );

            return "register";
        }
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @GetMapping("/profile")
    public String showProfile(
            Authentication authentication,
            Model model) {

        User user = userService.findByEmail(
                authentication.getName()
        );

        model.addAttribute("user", user);

        return "profile";
    }

    @PostMapping("/account/deactivate")
    public String deactivateAccount(
            Authentication authentication,
            HttpServletRequest request)
            throws ServletException {

        userService.deactivate(
                authentication.getName()
        );

        request.logout();

        return "redirect:/";
    }
}