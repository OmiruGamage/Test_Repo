package au.edu.rmit.sept.webapp.gear;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GearListingServiceTest {

    private GearListingDAO gearListingDAO;
    private ListingPhotoDAO listingPhotoDAO;
    private GearListingService gearListingService;

    @BeforeEach
    void setUp() {
        gearListingDAO = mock(GearListingDAO.class);
        listingPhotoDAO = mock(ListingPhotoDAO.class);
        gearListingService = new GearListingService(gearListingDAO, listingPhotoDAO);
    }

    private static MockMultipartFile aJpegFile() {
        return new MockMultipartFile(
                "photos", "photo.jpg", "image/jpeg", new byte[] { 1, 2, 3 });
    }

    private static ListingForm validForm() {

        ListingForm form = new ListingForm();

        form.setTitle("Coleman 4-person tent");
        form.setCategory(Category.CAMPING);
        form.setCondition(GearCondition.GOOD);
        form.setDailyRate(new BigDecimal("25.00"));
        form.setPickupSuburb("Brunswick");
        form.setPickupPostcode("3056");
        form.setExpiryDate(LocalDate.now().plusMonths(6));
        form.setDescription("Sleeps 4, used twice, includes pegs and fly.");
        form.setPhotos(List.of(aJpegFile()));

        return form;
    }

    private GearListing capturedListing() {

        ArgumentCaptor<GearListing> captor =
                ArgumentCaptor.forClass(GearListing.class);

        org.mockito.Mockito.verify(gearListingDAO).save(captor.capture());

        return captor.getValue();
    }

    @Test
    void everyFormFieldIsMappedOntoTheListing() {

        when(gearListingDAO.save(any())).thenReturn(42L);

        gearListingService.create(validForm(), 7L);

        GearListing saved = capturedListing();

        assertThat(saved.getOwnerId()).isEqualTo(7L);
        assertThat(saved.getTitle()).isEqualTo("Coleman 4-person tent");
        assertThat(saved.getCategory()).isEqualTo(Category.CAMPING);
        assertThat(saved.getCondition()).isEqualTo(GearCondition.GOOD);
        assertThat(saved.getDailyRate()).isEqualTo(new BigDecimal("25.00"));
        assertThat(saved.getPickupSuburb()).isEqualTo("Brunswick");
        assertThat(saved.getPickupPostcode()).isEqualTo("3056");
        assertThat(saved.getDescription())
                .isEqualTo("Sleeps 4, used twice, includes pegs and fly.");
    }

    @Test
    void theNewListingIdIsReturnedToTheCaller() {

        when(gearListingDAO.save(any())).thenReturn(42L);

        Long id = gearListingService.create(validForm(), 7L);

        assertThat(id).isEqualTo(42L);
    }

    @Test
    void surroundingWhitespaceIsStrippedFromTextFields() {

        when(gearListingDAO.save(any())).thenReturn(1L);

        ListingForm form = validForm();
        form.setTitle("  Coleman 4-person tent  ");
        form.setPickupSuburb("  Brunswick  ");
        form.setPickupPostcode(" 3056 ");
        form.setDescription("  Sleeps 4.  ");

        gearListingService.create(form, 7L);

        GearListing saved = capturedListing();

        assertThat(saved.getTitle()).isEqualTo("Coleman 4-person tent");
        assertThat(saved.getPickupSuburb()).isEqualTo("Brunswick");
        assertThat(saved.getPickupPostcode()).isEqualTo("3056");
        assertThat(saved.getDescription()).isEqualTo("Sleeps 4.");
    }

    @Test
    void aNewListingIsPublishedAndKeepsItsExpiryDate() {

        when(gearListingDAO.save(any())).thenReturn(1L);

        LocalDate expiry = LocalDate.now().plusMonths(6);

        ListingForm form = validForm();
        form.setExpiryDate(expiry);

        gearListingService.create(form, 7L);

        GearListing saved = capturedListing();

        assertThat(saved.getStatus()).isEqualTo(ListingStatus.PUBLISHED);
        assertThat(saved.getExpiryDate()).isEqualTo(expiry);
    }

    @Test
    void aBlankDescriptionIsStoredAsNullRatherThanAnEmptyString() {

        when(gearListingDAO.save(any())).thenReturn(1L);

        ListingForm form = validForm();
        form.setDescription("   ");

        gearListingService.create(form, 7L);

        assertThat(capturedListing().getDescription()).isNull();
    }

    @Test
    void aWholeDollarRateIsNormalisedToTwoDecimalPlaces() {

        when(gearListingDAO.save(any())).thenReturn(1L);

        ListingForm form = validForm();
        form.setDailyRate(new BigDecimal("25"));

        gearListingService.create(form, 7L);

        assertThat(capturedListing().getDailyRate())
                .isEqualTo(new BigDecimal("25.00"));
    }

    @Test
    void eachPhotoIsSavedAgainstTheNewListingWithIncreasingDisplayOrder() {

        when(gearListingDAO.save(any())).thenReturn(42L);

        ListingForm form = validForm();
        form.setPhotos(List.of(aJpegFile(), aJpegFile(), aJpegFile()));

        gearListingService.create(form, 7L);

        verify(listingPhotoDAO).save(eq(42L), eq("image/jpeg"), any(), eq(0));
        verify(listingPhotoDAO).save(eq(42L), eq("image/jpeg"), any(), eq(1));
        verify(listingPhotoDAO).save(eq(42L), eq("image/jpeg"), any(), eq(2));
    }

    @Test
    void anEmptyFilePartAmongThePhotosIsSkipped() {

        when(gearListingDAO.save(any())).thenReturn(42L);

        MockMultipartFile emptyFile =
                new MockMultipartFile("photos", "", "image/jpeg", new byte[0]);

        ListingForm form = validForm();
        form.setPhotos(List.of(aJpegFile(), emptyFile));

        gearListingService.create(form, 7L);

        verify(listingPhotoDAO, times(1)).save(any(), any(), any(), anyInt());
        verify(listingPhotoDAO, never()).save(any(), any(), any(), eq(1));
    }

    @Test
    void findPhotoByIdDelegatesToTheDAO() {

        ListingPhoto photo = new ListingPhoto(
                5L, 42L, "image/jpeg", new byte[] { 1 }, 0);
        when(listingPhotoDAO.findById(5L)).thenReturn(Optional.of(photo));

        assertThat(gearListingService.findPhotoById(5L)).contains(photo);
    }

    @Test
    void findFirstPhotoIdsDelegatesToTheDAO() {

        Map<Long, Long> ids = Map.of(42L, 5L);
        when(listingPhotoDAO.findFirstPhotoIdsByListingIds(List.of(42L)))
                .thenReturn(ids);

        assertThat(gearListingService.findFirstPhotoIds(List.of(42L))).isSameAs(ids);
    }

    private static GearListing listingWithStatus(ListingStatus status) {

        GearListing listing = new GearListing();
        listing.setStatus(status);

        return listing;
    }

    @Test
    void aPublishedListingIsAvailable() {

        GearListing published = listingWithStatus(ListingStatus.PUBLISHED);
        when(gearListingDAO.findById(1L)).thenReturn(Optional.of(published));

        assertThat(gearListingService.findAvailableById(1L))
                .contains(published);
    }

    @Test
    void anExpiredListingIsNotAvailable() {

        when(gearListingDAO.findById(1L))
                .thenReturn(Optional.of(listingWithStatus(ListingStatus.EXPIRED)));

        assertThat(gearListingService.findAvailableById(1L)).isEmpty();
    }

    @Test
    void aRemovedListingIsNotAvailable() {

        when(gearListingDAO.findById(1L))
                .thenReturn(Optional.of(listingWithStatus(ListingStatus.REMOVED)));

        assertThat(gearListingService.findAvailableById(1L)).isEmpty();
    }

    @Test
    void aNonexistentListingIsNotAvailable() {

        when(gearListingDAO.findById(999999L)).thenReturn(Optional.empty());

        assertThat(gearListingService.findAvailableById(999999L)).isEmpty();
    }

    @Test
    void findByOwnerIdDelegatesToTheDAO() {

        List<GearListing> owned = List.of(listingWithStatus(ListingStatus.PUBLISHED));
        when(gearListingDAO.findByOwnerId(7L)).thenReturn(owned);

        assertThat(gearListingService.findByOwnerId(7L)).isSameAs(owned);
    }

    private static GearListing existingListing(Long id, Long ownerId) {

        GearListing listing = new GearListing();

        listing.setId(id);
        listing.setOwnerId(ownerId);
        listing.setTitle("Coleman 4-person tent");
        listing.setCategory(Category.CAMPING);
        listing.setCondition(GearCondition.GOOD);
        listing.setDailyRate(new BigDecimal("25.00"));
        listing.setPickupSuburb("Brunswick");
        listing.setPickupPostcode("3056");
        listing.setDescription("Sleeps 4.");
        listing.setExpiryDate(LocalDate.now().plusMonths(6));
        listing.setStatus(ListingStatus.PUBLISHED);

        return listing;
    }

    private GearListing capturedUpdate() {

        ArgumentCaptor<GearListing> captor =
                ArgumentCaptor.forClass(GearListing.class);

        verify(gearListingDAO).update(captor.capture());

        return captor.getValue();
    }

    @Test
    void updateReusesTheSameNormalisationAsCreate() {

        when(gearListingDAO.findById(1L))
                .thenReturn(Optional.of(existingListing(1L, 7L)));

        ListingForm form = validForm();
        form.setTitle("  Updated title  ");
        form.setDailyRate(new BigDecimal("40"));
        form.setDescription("   ");

        boolean updated = gearListingService.update(1L, form, 7L);

        assertThat(updated).isTrue();

        GearListing saved = capturedUpdate();
        assertThat(saved.getTitle()).isEqualTo("Updated title");
        assertThat(saved.getDailyRate()).isEqualTo(new BigDecimal("40.00"));
        assertThat(saved.getDescription()).isNull();
    }

    @Test
    void editingDoesNotChangeTheListingsExpiryDate() {

        GearListing existing = existingListing(1L, 7L);
        LocalDate originalExpiry = existing.getExpiryDate();

        when(gearListingDAO.findById(1L)).thenReturn(Optional.of(existing));

        ListingForm form = validForm();
        form.setExpiryDate(LocalDate.now().plusMonths(1));

        gearListingService.update(1L, form, 7L);

        assertThat(capturedUpdate().getExpiryDate()).isEqualTo(originalExpiry);
    }

    @Test
    void updatingSomeoneElsesListingDoesNothing() {

        when(gearListingDAO.findById(1L))
                .thenReturn(Optional.of(existingListing(1L, 7L)));

        boolean updated = gearListingService.update(1L, validForm(), 9L);

        assertThat(updated).isFalse();
        verify(gearListingDAO, never()).update(any());
    }

    @Test
    void updatingANonexistentListingDoesNothing() {

        when(gearListingDAO.findById(999999L)).thenReturn(Optional.empty());

        boolean updated = gearListingService.update(999999L, validForm(), 7L);

        assertThat(updated).isFalse();
        verify(gearListingDAO, never()).update(any());
    }

    @Test
    void deleteFlipsTheListingsStatusToRemoved() {

        when(gearListingDAO.findById(1L))
                .thenReturn(Optional.of(existingListing(1L, 7L)));

        boolean deleted = gearListingService.delete(1L, 7L);

        assertThat(deleted).isTrue();
        assertThat(capturedUpdate().getStatus()).isEqualTo(ListingStatus.REMOVED);
    }

    @Test
    void deletingSomeoneElsesListingDoesNothing() {

        when(gearListingDAO.findById(1L))
                .thenReturn(Optional.of(existingListing(1L, 7L)));

        boolean deleted = gearListingService.delete(1L, 9L);

        assertThat(deleted).isFalse();
        verify(gearListingDAO, never()).update(any());
    }
}
