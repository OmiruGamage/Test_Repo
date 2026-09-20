package au.edu.rmit.sept.webapp.gear;

import au.edu.rmit.sept.webapp.review.ReviewService;
import au.edu.rmit.sept.webapp.review.ReviewType;
import au.edu.rmit.sept.webapp.user.UserService;

import jakarta.validation.Valid;
import jakarta.validation.groups.Default;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/listings")
public class GearListingController {

    private final GearListingService gearListingService;
    private final UserService userService;
    private final ReviewService reviewService;

    public GearListingController(
            GearListingService gearListingService,
            UserService userService,
            ReviewService reviewService) {

        this.gearListingService = gearListingService;
        this.userService = userService;
        this.reviewService = reviewService;
    }

    @ModelAttribute("categories")
    public Category[] categories() {
        return Category.values();
    }

    @ModelAttribute("conditions")
    public GearCondition[] conditions() {
        return GearCondition.values();
    }

    @GetMapping("/new")
    public String showAddListingForm(Model model) {

        ListingForm form = new ListingForm();
        form.setExpiryDate(LocalDate.now().plusMonths(6));

        model.addAttribute("listingForm", form);

        return "add-listing";
    }

    @PostMapping
    public String createListing(
            @Validated({Default.class, RequiresPhotos.class})
            @ModelAttribute("listingForm") ListingForm listingForm,
            BindingResult bindingResult,
            Authentication authentication) {

        if (bindingResult.hasErrors()) {
            return "add-listing";
        }

        Long ownerId = userService
                .findByEmail(authentication.getName())
                .getId();

        gearListingService.create(listingForm, ownerId);

        return "redirect:/listings/new?created";
    }

    @GetMapping("/{id}")
    public String showListingDetail(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {

        Optional<GearListing> listing = gearListingService.findAvailableById(id);

        if (listing.isEmpty()) {
            return "listing-detail";
        }

        Long currentUserId = userService
                .findByEmail(authentication.getName())
                .getId();

        Long listingId = listing.get().getId();

        model.addAttribute("listing", listing.get());
        model.addAttribute("owner", userService.findById(listing.get().getOwnerId()));
        model.addAttribute("isOwner", listing.get().getOwnerId().equals(currentUserId));
        model.addAttribute(
                "thumbnailPhotoId",
                gearListingService.findFirstPhotoIds(List.of(listingId)).get(listingId)
        );

        // Reviews and Ratings: gear rating, gear reviews and the owner's rating
        model.addAttribute("gearRating", reviewService.gearRating(listingId));
        model.addAttribute("gearReviews", reviewService.gearReviews(listingId));
        model.addAttribute(
                "ownerRating",
                reviewService.userRating(listing.get().getOwnerId(), ReviewType.OWNER)
        );

        return "listing-detail";
    }

    @GetMapping("/mine")
    public String showMyGear(Authentication authentication, Model model) {

        Long currentUserId = userService
                .findByEmail(authentication.getName())
                .getId();

        List<GearListing> listings = gearListingService.findByOwnerId(currentUserId);

        List<Long> listingIds = listings.stream().map(GearListing::getId).toList();

        model.addAttribute("listings", listings);
        model.addAttribute("thumbnails", gearListingService.findFirstPhotoIds(listingIds));
        model.addAttribute("ratings", reviewService.gearRatings(listingIds));

        return "my-gear";
    }

    @GetMapping("/photos/{photoId}")
    public ResponseEntity<byte[]> showPhoto(@PathVariable Long photoId) {

        return gearListingService.findPhotoById(photoId)
                .map(photo -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(photo.getContentType()))
                        .body(photo.getData()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/edit")
    public String showEditListingForm(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {

        Optional<GearListing> listing = gearListingService.findById(id);

        Long currentUserId = userService
                .findByEmail(authentication.getName())
                .getId();

        if (listing.isEmpty() || !listing.get().getOwnerId().equals(currentUserId)) {
            return "redirect:/listings/mine";
        }

        model.addAttribute("listingForm", toForm(listing.get()));
        model.addAttribute("isEdit", true);
        model.addAttribute("listingId", id);

        return "add-listing";
    }

    @PostMapping("/{id}")
    public String updateListing(
            @PathVariable Long id,
            @Valid @ModelAttribute("listingForm") ListingForm listingForm,
            BindingResult bindingResult,
            Authentication authentication,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("listingId", id);
            return "add-listing";
        }

        Long currentUserId = userService
                .findByEmail(authentication.getName())
                .getId();

        boolean updated = gearListingService.update(id, listingForm, currentUserId);

        if (!updated) {
            return "redirect:/listings/mine";
        }

        return "redirect:/listings/mine?updated";
    }

    @PostMapping("/{id}/delete")
    public String deleteListing(
            @PathVariable Long id,
            Authentication authentication) {

        Long currentUserId = userService
                .findByEmail(authentication.getName())
                .getId();

        gearListingService.delete(id, currentUserId);

        return "redirect:/listings/mine?deleted";
    }

    @GetMapping("/{id}/relist")
    public String showRelistForm(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {

        Optional<GearListing> listing = gearListingService.findById(id);

        Long currentUserId = userService
                .findByEmail(authentication.getName())
                .getId();

        if (listing.isEmpty() || !listing.get().getOwnerId().equals(currentUserId)) {
            return "redirect:/listings/mine";
        }

        ListingForm form = toForm(listing.get());
        form.setExpiryDate(LocalDate.now().plusMonths(6));

        model.addAttribute("listingForm", form);

        return "add-listing";
    }

    private static ListingForm toForm(GearListing listing) {

        ListingForm form = new ListingForm();

        form.setTitle(listing.getTitle());
        form.setCategory(listing.getCategory());
        form.setCondition(listing.getCondition());
        form.setDailyRate(listing.getDailyRate());
        form.setPickupSuburb(listing.getPickupSuburb());
        form.setPickupPostcode(listing.getPickupPostcode());
        form.setExpiryDate(listing.getExpiryDate());
        form.setDescription(listing.getDescription());

        return form;
    }
}