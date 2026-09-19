package au.edu.rmit.sept.webapp.admin;

import au.edu.rmit.sept.webapp.gear.ListingStatus;
import au.edu.rmit.sept.webapp.user.UserRole;
import au.edu.rmit.sept.webapp.user.UserService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.function.Supplier;

/**
 * Admin Tools. Access to /admin/** is restricted to ROLE_ADMIN in SecurityConfig,
 * so every endpoint here is protected at the endpoint, not just hidden in the UI.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;

    public AdminController(AdminService adminService, UserService userService) {
        this.adminService = adminService;
        this.userService = userService;
    }

    @ModelAttribute("roles")
    public UserRole[] roles() {
        return UserRole.values();
    }

    @ModelAttribute("statuses")
    public ListingStatus[] statuses() {
        return ListingStatus.values();
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("recentActions", adminService.findRecentActions());
        return "admin/dashboard";
    }

    // ---------- users ----------

    @GetMapping("/users")
    public String users(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String active,
            Model model) {

        String search = blankToNull(q);
        UserRole roleFilter = parseEnum(UserRole.class, role);
        Boolean activeFilter = blankToNull(active) == null ? null : Boolean.valueOf(active.trim());

        model.addAttribute("users", adminService.findUsers(search, roleFilter, activeFilter));
        model.addAttribute("q", search);
        model.addAttribute("selectedRole", roleFilter);
        model.addAttribute("selectedActive", activeFilter);

        return "admin/users";
    }

    @GetMapping("/users/{id}")
    public String userDetail(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            model.addAttribute("account", adminService.getUser(id));
        } catch (AdminActionException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/users";
        }

        model.addAttribute("listings", adminService.findListingsByOwner(id));
        model.addAttribute("actions", adminService.findActionsForUser(id));

        return "admin/user-detail";
    }

    @PostMapping("/users/{id}/deactivate")
    public String deactivateUser(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Long adminId = currentUserId(authentication);

        run(redirectAttributes, () -> {
            adminService.deactivateUser(adminId, id, reason);
            return "Account deactivated. Their published listings were removed.";
        });

        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/users/{id}/reactivate")
    public String reactivateUser(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Long adminId = currentUserId(authentication);

        run(redirectAttributes, () -> {
            adminService.reactivateUser(adminId, id, reason);
            return "Account reactivated.";
        });

        return "redirect:/admin/users/" + id;
    }

    // ---------- listings ----------

    @GetMapping("/listings")
    public String listings(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            Model model) {

        String search = blankToNull(q);
        ListingStatus statusFilter = parseEnum(ListingStatus.class, status);

        model.addAttribute("listings", adminService.findListings(search, statusFilter));
        model.addAttribute("q", search);
        model.addAttribute("selectedStatus", statusFilter);

        return "admin/listings";
    }

    @PostMapping("/listings/{id}/remove")
    public String removeListing(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String returnTo,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Long adminId = currentUserId(authentication);

        run(redirectAttributes, () -> {
            adminService.removeListing(adminId, id, reason);
            return "Listing removed.";
        });

        return "redirect:" + safeReturn(returnTo);
    }

    @PostMapping("/listings/{id}/restore")
    public String restoreListing(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String returnTo,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Long adminId = currentUserId(authentication);

        run(redirectAttributes, () -> {
            ListingStatus status = adminService.restoreListing(adminId, id, reason);
            return status == ListingStatus.PUBLISHED
                    ? "Listing restored and visible in search again."
                    : "Listing restored, but its expiry date has passed so it is now Expired.";
        });

        return "redirect:" + safeReturn(returnTo);
    }

    // ---------- history ----------

    @GetMapping("/actions")
    public String actions(Model model) {
        model.addAttribute("actions", adminService.findRecentActions());
        return "admin/actions";
    }

    // ---------- helpers ----------

    private static void run(RedirectAttributes redirectAttributes, Supplier<String> action) {
        try {
            redirectAttributes.addFlashAttribute("success", action.get());
        } catch (AdminActionException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
    }

    /** Only allow redirects back into the admin area. */
    static String safeReturn(String returnTo) {
        if (returnTo != null && returnTo.matches("^/admin/[A-Za-z0-9/]*$")) {
            return returnTo;
        }
        return "/admin/listings";
    }

    private Long currentUserId(Authentication authentication) {
        return userService.findByEmail(authentication.getName()).getId();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {

        String clean = blankToNull(value);

        if (clean == null) {
            return null;
        }

        try {
            return Enum.valueOf(type, clean);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
