package au.edu.rmit.sept.webapp.review;

import au.edu.rmit.sept.webapp.gear.GearListing;
import au.edu.rmit.sept.webapp.gear.GearListingService;
import au.edu.rmit.sept.webapp.gear.ListingStatus;
import au.edu.rmit.sept.webapp.user.User;
import au.edu.rmit.sept.webapp.user.UserService;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;
    private final GearListingService gearListingService;

    public ReviewController(
            ReviewService reviewService,
            UserService userService,
            GearListingService gearListingService) {

        this.reviewService = reviewService;
        this.userService = userService;
        this.gearListingService = gearListingService;
    }

    /** Completed rentals still waiting for a review, plus reviews already given. */
    @GetMapping
    public String myReviews(Authentication authentication, Model model) {

        Long userId = currentUserId(authentication);

        model.addAttribute("userId", userId);
        model.addAttribute("pendingReviews", reviewService.findPendingReviews(userId));
        model.addAttribute("writtenReviews", reviewService.findReviewsWrittenBy(userId));

        return "my-reviews";
    }

    @GetMapping("/rentals/{rentalId}/{type}")
    public String showReviewForm(
            @PathVariable Long rentalId,
            @PathVariable ReviewType type,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            CompletedRental rental = reviewService.getReviewableRental(
                    rentalId, currentUserId(authentication), type);

            model.addAttribute("reviewForm", new ReviewForm());
            populateForm(model, rental, type);

            return "review-form";

        } catch (ReviewNotAllowedException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/reviews";
        }
    }

    @PostMapping("/rentals/{rentalId}/{type}")
    public String submitReview(
            @PathVariable Long rentalId,
            @PathVariable ReviewType type,
            @Valid @ModelAttribute("reviewForm") ReviewForm reviewForm,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        Long userId = currentUserId(authentication);

        try {
            if (bindingResult.hasErrors()) {
                populateForm(model,
                        reviewService.getReviewableRental(rentalId, userId, type),
                        type);
                return "review-form";
            }

            reviewService.submitReview(rentalId, userId, type, reviewForm);

            redirectAttributes.addFlashAttribute("success", "Your review has been posted.");
            return "redirect:/reviews";

        } catch (ReviewNotAllowedException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/reviews";
        }
    }

    /** All gear reviews for one listing. Owners can also see reviews on expired/removed listings. */
    @GetMapping("/listings/{listingId}")
    public String listingReviews(
            @PathVariable Long listingId,
            Authentication authentication,
            Model model) {

        Optional<GearListing> listing = gearListingService.findById(listingId);
        Long userId = currentUserId(authentication);

        if (listing.isEmpty()) {
            return "redirect:/";
        }

        boolean isOwner = listing.get().getOwnerId().equals(userId);

        if (listing.get().getStatus() != ListingStatus.PUBLISHED && !isOwner) {
            return "redirect:/";
        }

        model.addAttribute("listing", listing.get());
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("gearRating", reviewService.gearRating(listingId));
        model.addAttribute("gearReviews", reviewService.gearReviews(listingId));

        return "listing-reviews";
    }

    /** Ratings a user has received as an owner and as a renter. */
    @GetMapping("/users/{userId}")
    public String userReviews(@PathVariable Long userId, Model model) {

        User user;

        try {
            user = userService.findById(userId);
        } catch (IllegalArgumentException e) {
            return "redirect:/";
        }

        model.addAttribute("reviewedUserName", user.getName());
        model.addAttribute("ownerRating", reviewService.userRating(userId, ReviewType.OWNER));
        model.addAttribute("renterRating", reviewService.userRating(userId, ReviewType.RENTER));
        model.addAttribute("ownerReviews", reviewService.reviewsAbout(userId, ReviewType.OWNER));
        model.addAttribute("renterReviews", reviewService.reviewsAbout(userId, ReviewType.RENTER));

        return "user-reviews";
    }

    private static void populateForm(Model model, CompletedRental rental, ReviewType type) {

        model.addAttribute("rental", rental);
        model.addAttribute("reviewType", type);
        model.addAttribute(
                "revieweeName",
                type == ReviewType.RENTER ? rental.getRenterName() : rental.getOwnerName());
    }

    private Long currentUserId(Authentication authentication) {
        return userService.findByEmail(authentication.getName()).getId();
    }
}
