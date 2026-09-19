package au.edu.rmit.sept.webapp.gear;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class GearListingBrowseController {

    private final GearListingDAO gearListingDAO;
    private final ListingPhotoDAO listingPhotoDAO;

    public GearListingBrowseController(
            GearListingDAO gearListingDAO,
            ListingPhotoDAO listingPhotoDAO) {

        this.gearListingDAO = gearListingDAO;
        this.listingPhotoDAO = listingPhotoDAO;
    }

    @ModelAttribute("categories")
    public Category[] categories() {
        return Category.values();
    }

    @ModelAttribute("conditions")
    public GearCondition[] conditions() {
        return GearCondition.values();
    }

    @GetMapping("/")
    public String browse(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String pickupSuburb,
            @RequestParam(required = false) String pickupPostcode,
            @RequestParam(required = false) BigDecimal minRate,
            @RequestParam(required = false) BigDecimal maxRate,
            Model model) {

        SearchFilters filters = new SearchFilters();

        filters.setCategory(blankToNull(category) == null
                ? null : Category.valueOf(category));
        filters.setCondition(blankToNull(condition) == null
                ? null : GearCondition.valueOf(condition));
        filters.setPickupSuburb(blankToNull(pickupSuburb));
        filters.setPickupPostcode(blankToNull(pickupPostcode));
        filters.setMinRate(minRate);
        filters.setMaxRate(maxRate);

        List<GearListing> listings = gearListingDAO.search(filters);
        List<Long> listingIds = listings.stream().map(GearListing::getId).toList();

        model.addAttribute("filters", filters);
        model.addAttribute("listings", listings);
        model.addAttribute(
                "thumbnails",
                listingPhotoDAO.findFirstPhotoIdsByListingIds(listingIds)
        );

        return "browse";
    }

    private static String blankToNull(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
