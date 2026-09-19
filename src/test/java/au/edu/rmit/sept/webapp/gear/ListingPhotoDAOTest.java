package au.edu.rmit.sept.webapp.gear;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({ ListingPhotoDAO.class, GearListingDAO.class })
class ListingPhotoDAOTest {

    @Autowired
    private ListingPhotoDAO listingPhotoDAO;

    @Autowired
    private GearListingDAO gearListingDAO;

    private Long aListingId() {

        GearListing listing = new GearListing();

        listing.setOwnerId(1L);
        listing.setTitle("Coleman 4-person tent");
        listing.setCategory(Category.CAMPING);
        listing.setCondition(GearCondition.GOOD);
        listing.setDailyRate(new BigDecimal("25.00"));
        listing.setPickupSuburb("Brunswick");
        listing.setPickupPostcode("3056");
        listing.setExpiryDate(LocalDate.now().plusMonths(6));
        listing.setStatus(ListingStatus.PUBLISHED);

        return gearListingDAO.save(listing);
    }

    @Test
    void savedPhotoIsReturnedWithEveryFieldIntact() {

        Long listingId = aListingId();
        byte[] data = { 1, 2, 3, 4 };

        Long id = listingPhotoDAO.save(listingId, "image/jpeg", data, 0);

        ListingPhoto found = listingPhotoDAO.findById(id).orElseThrow();

        assertThat(found.getId()).isEqualTo(id);
        assertThat(found.getListingId()).isEqualTo(listingId);
        assertThat(found.getContentType()).isEqualTo("image/jpeg");
        assertThat(found.getData()).isEqualTo(data);
        assertThat(found.getDisplayOrder()).isZero();
    }

    @Test
    void unknownIdReturnsEmpty() {

        Optional<ListingPhoto> found = listingPhotoDAO.findById(999999L);

        assertThat(found).isEmpty();
    }

    @Test
    void findFirstPhotoIdsReturnsTheDisplayOrderZeroPhotoPerListing() {

        Long listingId = aListingId();

        Long firstPhotoId = listingPhotoDAO.save(
                listingId, "image/jpeg", new byte[] { 1 }, 0);
        listingPhotoDAO.save(listingId, "image/png", new byte[] { 2 }, 1);

        Map<Long, Long> firstPhotoIds =
                listingPhotoDAO.findFirstPhotoIdsByListingIds(List.of(listingId));

        assertThat(firstPhotoIds).containsEntry(listingId, firstPhotoId);
    }

    @Test
    void findFirstPhotoIdsCoversMultipleListingsInOneCall() {

        Long listingOneId = aListingId();
        Long listingTwoId = aListingId();

        Long firstPhotoOne = listingPhotoDAO.save(
                listingOneId, "image/jpeg", new byte[] { 1 }, 0);
        Long firstPhotoTwo = listingPhotoDAO.save(
                listingTwoId, "image/jpeg", new byte[] { 2 }, 0);

        Map<Long, Long> firstPhotoIds = listingPhotoDAO.findFirstPhotoIdsByListingIds(
                List.of(listingOneId, listingTwoId));

        assertThat(firstPhotoIds)
                .containsEntry(listingOneId, firstPhotoOne)
                .containsEntry(listingTwoId, firstPhotoTwo);
    }

    @Test
    void aListingWithNoPhotosHasNoEntryInTheMap() {

        Long listingId = aListingId();

        Map<Long, Long> firstPhotoIds =
                listingPhotoDAO.findFirstPhotoIdsByListingIds(List.of(listingId));

        assertThat(firstPhotoIds).doesNotContainKey(listingId);
    }

    @Test
    void anEmptyListingIdListReturnsAnEmptyMapWithoutQuerying() {

        assertThat(listingPhotoDAO.findFirstPhotoIdsByListingIds(List.of())).isEmpty();
    }
}
