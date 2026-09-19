package au.edu.rmit.sept.webapp.gear;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import(GearListingDAO.class)
class GearListingDAOTest {

    @Autowired
    private GearListingDAO gearListingDAO;

    private static GearListing listingWith(String postcode, String dailyRate) {

        GearListing listing = new GearListing();

        listing.setOwnerId(1L);
        listing.setTitle("Coleman 4-person tent");
        listing.setCategory(Category.CAMPING);
        listing.setCondition(GearCondition.GOOD);
        listing.setDailyRate(new BigDecimal(dailyRate));
        listing.setPickupSuburb("Brunswick");
        listing.setPickupPostcode(postcode);
        listing.setDescription("Sleeps 4, used twice, includes pegs and fly.");
        listing.setExpiryDate(LocalDate.now().plusMonths(6));
        listing.setStatus(ListingStatus.PUBLISHED);

        return listing;
    }

    @Test
    void savedListingIsReturnedWithEveryFieldIntact() {

        Long id = gearListingDAO.save(listingWith("3056", "25.00"));

        assertThat(id).isNotNull();

        GearListing found = gearListingDAO.findById(id).orElseThrow();

        assertThat(found.getId()).isEqualTo(id);
        assertThat(found.getOwnerId()).isEqualTo(1L);
        assertThat(found.getTitle()).isEqualTo("Coleman 4-person tent");
        assertThat(found.getCategory()).isEqualTo(Category.CAMPING);
        assertThat(found.getCondition()).isEqualTo(GearCondition.GOOD);
        assertThat(found.getPickupSuburb()).isEqualTo("Brunswick");
        assertThat(found.getDescription())
                .isEqualTo("Sleeps 4, used twice, includes pegs and fly.");
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void postcodeWithLeadingZeroSurvivesTheRoundTrip() {

        Long id = gearListingDAO.save(listingWith("0800", "25.00"));

        GearListing found = gearListingDAO.findById(id).orElseThrow();

        assertThat(found.getPickupPostcode()).isEqualTo("0800");
    }

    @Test
    void dailyRateKeepsItsCentsExactly() {

        Long id = gearListingDAO.save(listingWith("3056", "1234.56"));

        GearListing found = gearListingDAO.findById(id).orElseThrow();

        assertThat(found.getDailyRate()).isEqualTo(new BigDecimal("1234.56"));
    }

    @Test
    void smallestLegalRateIsStoredWithoutRounding() {

        Long id = gearListingDAO.save(listingWith("3056", "0.01"));

        GearListing found = gearListingDAO.findById(id).orElseThrow();

        assertThat(found.getDailyRate()).isEqualTo(new BigDecimal("0.01"));
    }

    @Test
    void expiryDateAndStatusSurviveTheRoundTrip() {

        LocalDate expiry = LocalDate.now().plusMonths(6);

        Long id = gearListingDAO.save(listingWith("3056", "25.00"));

        GearListing found = gearListingDAO.findById(id).orElseThrow();

        assertThat(found.getExpiryDate()).isEqualTo(expiry);
        assertThat(found.getStatus()).isEqualTo(ListingStatus.PUBLISHED);
    }

    @Test
    void anAbsentDescriptionIsStoredAsNull() {

        GearListing listing = listingWith("3056", "25.00");
        listing.setDescription(null);

        Long id = gearListingDAO.save(listing);

        assertThat(gearListingDAO.findById(id).orElseThrow().getDescription())
                .isNull();
    }

    @Test
    void unknownIdReturnsEmpty() {

        Optional<GearListing> found = gearListingDAO.findById(999999L);

        assertThat(found).isEmpty();
    }

    @Test
    void findByOwnerIdReturnsOnlyThatOwnersListings() {

        GearListing mine = listingWith("3056", "25.00");
        mine.setOwnerId(1L);

        GearListing someoneElses = listingWith("3056", "25.00");
        someoneElses.setOwnerId(2L);

        gearListingDAO.save(mine);
        gearListingDAO.save(someoneElses);

        List<GearListing> found = gearListingDAO.findByOwnerId(1L);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getOwnerId()).isEqualTo(1L);
    }

    @Test
    void findByOwnerIdIncludesExpiredAndRemovedListings() {

        GearListing expired = listingWith("3056", "25.00");
        expired.setOwnerId(1L);
        expired.setStatus(ListingStatus.EXPIRED);

        GearListing removed = listingWith("3056", "25.00");
        removed.setOwnerId(1L);
        removed.setStatus(ListingStatus.REMOVED);

        gearListingDAO.save(expired);
        gearListingDAO.save(removed);

        assertThat(gearListingDAO.findByOwnerId(1L)).hasSize(2);
    }

    @Test
    void updatePersistsEveryFieldIncludingExpiryAndStatus() {

        Long id = gearListingDAO.save(listingWith("3056", "25.00"));

        GearListing changed = gearListingDAO.findById(id).orElseThrow();
        changed.setTitle("Updated title");
        changed.setCategory(Category.SPORTS);
        changed.setCondition(GearCondition.FAIR);
        changed.setDailyRate(new BigDecimal("40.00"));
        changed.setPickupSuburb("Carlton");
        changed.setPickupPostcode("3000");
        changed.setDescription("Updated description.");
        changed.setExpiryDate(LocalDate.now().plusMonths(1));
        changed.setStatus(ListingStatus.EXPIRED);

        gearListingDAO.update(changed);

        GearListing found = gearListingDAO.findById(id).orElseThrow();

        assertThat(found.getTitle()).isEqualTo("Updated title");
        assertThat(found.getCategory()).isEqualTo(Category.SPORTS);
        assertThat(found.getCondition()).isEqualTo(GearCondition.FAIR);
        assertThat(found.getDailyRate()).isEqualTo(new BigDecimal("40.00"));
        assertThat(found.getPickupSuburb()).isEqualTo("Carlton");
        assertThat(found.getPickupPostcode()).isEqualTo("3000");
        assertThat(found.getDescription()).isEqualTo("Updated description.");
        assertThat(found.getExpiryDate()).isEqualTo(LocalDate.now().plusMonths(1));
        assertThat(found.getStatus()).isEqualTo(ListingStatus.EXPIRED);
    }

    @Test
    void updatingAnUnknownIdAffectsNoRows() {

        GearListing phantom = listingWith("3056", "25.00");
        phantom.setId(999999L);

        int rowsAffected = gearListingDAO.update(phantom);

        assertThat(rowsAffected).isZero();
    }

    private static GearListing aListing() {

        GearListing listing = listingWith("3056", "25.00");
        listing.setStatus(ListingStatus.PUBLISHED);

        return listing;
    }

    @Test
    void noFiltersReturnsAllPublishedListings() {

        gearListingDAO.save(aListing());
        gearListingDAO.save(aListing());

        assertThat(gearListingDAO.search(new SearchFilters())).hasSize(2);
    }

    @Test
    void categoryFilterNarrowsToThatCategory() {

        GearListing camping = aListing();
        GearListing sports = aListing();
        sports.setCategory(Category.SPORTS);

        gearListingDAO.save(camping);
        gearListingDAO.save(sports);

        SearchFilters filters = new SearchFilters();
        filters.setCategory(Category.SPORTS);

        List<GearListing> found = gearListingDAO.search(filters);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getCategory()).isEqualTo(Category.SPORTS);
    }

    @Test
    void conditionFilterNarrowsToThatCondition() {

        GearListing good = aListing();
        GearListing fair = aListing();
        fair.setCondition(GearCondition.FAIR);

        gearListingDAO.save(good);
        gearListingDAO.save(fair);

        SearchFilters filters = new SearchFilters();
        filters.setCondition(GearCondition.FAIR);

        List<GearListing> found = gearListingDAO.search(filters);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getCondition()).isEqualTo(GearCondition.FAIR);
    }

    @Test
    void suburbFilterIsCaseInsensitive() {

        GearListing brunswick = aListing();
        GearListing carlton = aListing();
        carlton.setPickupSuburb("Carlton");

        gearListingDAO.save(brunswick);
        gearListingDAO.save(carlton);

        SearchFilters filters = new SearchFilters();
        filters.setPickupSuburb("BRUNSWICK");

        List<GearListing> found = gearListingDAO.search(filters);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getPickupSuburb()).isEqualTo("Brunswick");
    }

    @Test
    void postcodeFilterNarrowsToThatPostcode() {

        GearListing here = aListing();
        GearListing elsewhere = aListing();
        elsewhere.setPickupPostcode("3000");

        gearListingDAO.save(here);
        gearListingDAO.save(elsewhere);

        SearchFilters filters = new SearchFilters();
        filters.setPickupPostcode("3056");

        List<GearListing> found = gearListingDAO.search(filters);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getPickupPostcode()).isEqualTo("3056");
    }

    @Test
    void filtersCombineWithAndNotOr() {

        GearListing matchesBoth = aListing();
        matchesBoth.setCategory(Category.SPORTS);
        matchesBoth.setCondition(GearCondition.FAIR);

        GearListing matchesOnlyCategory = aListing();
        matchesOnlyCategory.setCategory(Category.SPORTS);

        GearListing matchesOnlyCondition = aListing();
        matchesOnlyCondition.setCondition(GearCondition.FAIR);

        gearListingDAO.save(matchesBoth);
        gearListingDAO.save(matchesOnlyCategory);
        gearListingDAO.save(matchesOnlyCondition);

        SearchFilters filters = new SearchFilters();
        filters.setCategory(Category.SPORTS);
        filters.setCondition(GearCondition.FAIR);

        List<GearListing> found = gearListingDAO.search(filters);

        assertThat(found).hasSize(1);
    }

    @Test
    void anExpiredListingNeverAppearsRegardlessOfFilters() {

        GearListing expired = aListing();
        expired.setStatus(ListingStatus.EXPIRED);

        gearListingDAO.save(expired);

        assertThat(gearListingDAO.search(new SearchFilters())).isEmpty();
    }

    @Test
    void aRemovedListingNeverAppearsRegardlessOfFilters() {

        GearListing removed = aListing();
        removed.setStatus(ListingStatus.REMOVED);

        gearListingDAO.save(removed);

        assertThat(gearListingDAO.search(new SearchFilters())).isEmpty();
    }

    @Test
    void rateExactlyAtMinIsIncluded() {

        GearListing listing = aListing();
        listing.setDailyRate(new BigDecimal("25.00"));
        gearListingDAO.save(listing);

        SearchFilters filters = new SearchFilters();
        filters.setMinRate(new BigDecimal("25.00"));

        assertThat(gearListingDAO.search(filters)).hasSize(1);
    }

    @Test
    void rateJustBelowMinIsExcluded() {

        GearListing listing = aListing();
        listing.setDailyRate(new BigDecimal("24.99"));
        gearListingDAO.save(listing);

        SearchFilters filters = new SearchFilters();
        filters.setMinRate(new BigDecimal("25.00"));

        assertThat(gearListingDAO.search(filters)).isEmpty();
    }

    @Test
    void rateExactlyAtMaxIsIncluded() {

        GearListing listing = aListing();
        listing.setDailyRate(new BigDecimal("25.00"));
        gearListingDAO.save(listing);

        SearchFilters filters = new SearchFilters();
        filters.setMaxRate(new BigDecimal("25.00"));

        assertThat(gearListingDAO.search(filters)).hasSize(1);
    }

    @Test
    void rateJustAboveMaxIsExcluded() {

        GearListing listing = aListing();
        listing.setDailyRate(new BigDecimal("25.01"));
        gearListingDAO.save(listing);

        SearchFilters filters = new SearchFilters();
        filters.setMaxRate(new BigDecimal("25.00"));

        assertThat(gearListingDAO.search(filters)).isEmpty();
    }

    @Test
    void aListingPastItsExpiryDateIsReturnedAsExpiredOnTheNextRead() {

        GearListing listing = aListing();
        listing.setExpiryDate(LocalDate.now().minusDays(1));

        Long id = gearListingDAO.save(listing);

        assertThat(gearListingDAO.findById(id).orElseThrow().getStatus())
                .isEqualTo(ListingStatus.EXPIRED);
    }

    @Test
    void aListingExpiringTodayIsStillPublished() {

        GearListing listing = aListing();
        listing.setExpiryDate(LocalDate.now());

        Long id = gearListingDAO.save(listing);

        assertThat(gearListingDAO.findById(id).orElseThrow().getStatus())
                .isEqualTo(ListingStatus.PUBLISHED);
    }

    @Test
    void aRemovedListingIsNotFlippedToExpiredByAPastExpiryDate() {

        GearListing listing = aListing();
        listing.setExpiryDate(LocalDate.now().minusDays(1));
        listing.setStatus(ListingStatus.REMOVED);

        Long id = gearListingDAO.save(listing);

        assertThat(gearListingDAO.findById(id).orElseThrow().getStatus())
                .isEqualTo(ListingStatus.REMOVED);
    }

    @Test
    void findByOwnerIdAlsoAppliesTheExpiryTransitionButKeepsTheListing() {

        GearListing listing = aListing();
        listing.setOwnerId(1L);
        listing.setExpiryDate(LocalDate.now().minusDays(1));

        gearListingDAO.save(listing);

        List<GearListing> found = gearListingDAO.findByOwnerId(1L);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getStatus()).isEqualTo(ListingStatus.EXPIRED);
    }

    @Test
    void searchExcludesAListingPastItsExpiryDateEvenIfStillMarkedPublished() {

        GearListing listing = aListing();
        listing.setExpiryDate(LocalDate.now().minusDays(1));

        gearListingDAO.save(listing);

        assertThat(gearListingDAO.search(new SearchFilters())).isEmpty();
    }
}
