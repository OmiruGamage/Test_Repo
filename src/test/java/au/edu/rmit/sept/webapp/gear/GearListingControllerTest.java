package au.edu.rmit.sept.webapp.gear;

import au.edu.rmit.sept.webapp.config.SecurityConfig;
import au.edu.rmit.sept.webapp.user.User;
import au.edu.rmit.sept.webapp.user.UserRole;
import au.edu.rmit.sept.webapp.user.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(GearListingController.class)
@Import(SecurityConfig.class)
class GearListingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GearListingService gearListingService;

    @MockitoBean
    private UserService userService;

    private static User owner() {

        return new User(
                7L,
                "Ada Lovelace",
                "ada@example.com",
                "encoded",
                UserRole.USER,
                true
        );
    }

    private static MockMultipartFile aJpegFile() {
        return new MockMultipartFile(
                "photos", "photo.jpg", "image/jpeg", new byte[] { 1, 2, 3 });
    }

    private static GearListing listingOwnedBy(Long ownerId) {

        return new GearListing(
                1L,
                ownerId,
                "Coleman 4-person tent",
                Category.CAMPING,
                GearCondition.GOOD,
                new BigDecimal("25.00"),
                "Brunswick",
                "3056",
                null,
                LocalDate.now().plusMonths(6),
                ListingStatus.PUBLISHED,
                null
        );
    }

    @Test
    void anAnonymousRequestForTheFormIsRedirectedToLogin() throws Exception {

        mockMvc.perform(get("/listings/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void anAnonymousSubmissionCreatesNothing() throws Exception {

        mockMvc.perform(post("/listings")
                        .with(csrf())
                        .param("title", "Coleman 4-person tent"))
                .andExpect(status().is3xxRedirection());

        verify(gearListingService, never()).create(any(), any());
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void aLoggedInUserGetsTheFormWithTheCategoryAndConditionOptions()
            throws Exception {

        mockMvc.perform(get("/listings/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("add-listing"))
                .andExpect(model().attributeExists("listingForm"))
                .andExpect(model().attribute("categories", Category.values()))
                .andExpect(model().attribute("conditions", GearCondition.values()));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void theFormDefaultsItsExpiryDateToSixMonthsAway() throws Exception {

        mockMvc.perform(get("/listings/new"))
                .andExpect(status().isOk())
                .andExpect(model().attribute(
                        "listingForm",
                        hasProperty(
                                "expiryDate",
                                equalTo(LocalDate.now().plusMonths(6))
                        )
                ));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void aValidSubmissionCreatesTheListingAgainstTheLoggedInOwner()
            throws Exception {

        when(userService.findByEmail("ada@example.com")).thenReturn(owner());
        when(gearListingService.create(any(), eq(7L))).thenReturn(42L);

        mockMvc.perform(multipart("/listings")
                        .file(aJpegFile())
                        .with(csrf())
                        .param("title", "Coleman 4-person tent")
                        .param("category", "CAMPING")
                        .param("condition", "GOOD")
                        .param("dailyRate", "25.00")
                        .param("pickupSuburb", "Brunswick")
                        .param("pickupPostcode", "3056")
                        .param("expiryDate",
                                LocalDate.now().plusMonths(3).toString())
                        .param("description", "Sleeps 4."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/listings/new?created"));

        verify(gearListingService).create(any(ListingForm.class), eq(7L));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void aSubmissionWithNoPhotosIsRejected() throws Exception {

        when(userService.findByEmail("ada@example.com")).thenReturn(owner());

        mockMvc.perform(multipart("/listings")
                        .with(csrf())
                        .param("title", "Coleman 4-person tent")
                        .param("category", "CAMPING")
                        .param("condition", "GOOD")
                        .param("dailyRate", "25.00")
                        .param("pickupSuburb", "Brunswick")
                        .param("pickupPostcode", "3056")
                        .param("expiryDate",
                                LocalDate.now().plusMonths(3).toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("add-listing"))
                .andExpect(model().attributeHasFieldErrors("listingForm", "photos"));

        verify(gearListingService, never()).create(any(), any());
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void anInvalidSubmissionRedisplaysTheFormAndCreatesNothing()
            throws Exception {

        mockMvc.perform(post("/listings")
                        .with(csrf())
                        .param("title", "")
                        .param("category", "CAMPING")
                        .param("condition", "GOOD")
                        .param("dailyRate", "-5.00")
                        .param("pickupSuburb", "Brunswick")
                        .param("pickupPostcode", "abcd")
                        .param("expiryDate",
                                LocalDate.now().plusMonths(3).toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("add-listing"))
                .andExpect(model().attributeHasFieldErrors(
                        "listingForm", "title", "dailyRate", "pickupPostcode"));

        verify(gearListingService, never()).create(any(), any());
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void aRejectedSubmissionKeepsTheInputThatWasValid() throws Exception {

        mockMvc.perform(post("/listings")
                        .with(csrf())
                        .param("title", "Coleman 4-person tent")
                        .param("category", "CAMPING")
                        .param("condition", "GOOD")
                        .param("dailyRate", "25.00")
                        .param("pickupSuburb", "Brunswick")
                        .param("pickupPostcode", "999")
                        .param("expiryDate",
                                LocalDate.now().plusMonths(3).toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute(
                        "listingForm",
                        hasProperty(
                                "title",
                                equalTo("Coleman 4-person tent")
                        )
                ));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void anExpiryDateBeyondSixMonthsIsRejected() throws Exception {

        mockMvc.perform(post("/listings")
                        .with(csrf())
                        .param("title", "Coleman 4-person tent")
                        .param("category", "CAMPING")
                        .param("condition", "GOOD")
                        .param("dailyRate", "25.00")
                        .param("pickupSuburb", "Brunswick")
                        .param("pickupPostcode", "3056")
                        .param("expiryDate",
                                LocalDate.now().plusMonths(6).plusDays(1)
                                        .toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors(
                        "listingForm", "expiryDate"));

        verify(gearListingService, never()).create(any(), any());
    }

    @Test
    void anAnonymousRequestForTheDetailViewIsRedirectedToLogin() throws Exception {

        mockMvc.perform(get("/listings/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void theOwnerViewingTheirOwnListingSeesNoRequestToRentControl()
            throws Exception {

        when(gearListingService.findAvailableById(1L))
                .thenReturn(Optional.of(listingOwnedBy(7L)));
        when(userService.findByEmail("ada@example.com")).thenReturn(owner());
        when(userService.findById(7L)).thenReturn(owner());

        mockMvc.perform(get("/listings/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("listing-detail"))
                .andExpect(model().attribute("isOwner", true))
                .andExpect(content().string(not(containsString("Request to Rent"))));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void aListingOwnedBySomeoneElseShowsTheRequestToRentControl()
            throws Exception {

        User someoneElse = new User(
                9L, "Grace Hopper", "grace@example.com", "encoded",
                UserRole.USER, true);

        when(gearListingService.findAvailableById(1L))
                .thenReturn(Optional.of(listingOwnedBy(9L)));
        when(userService.findByEmail("ada@example.com")).thenReturn(owner());
        when(userService.findById(9L)).thenReturn(someoneElse);

        mockMvc.perform(get("/listings/1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("isOwner", false))
                .andExpect(content().string(containsString("Request to Rent")));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void aNonexistentListingShowsTheUnavailableMessage() throws Exception {

        when(gearListingService.findAvailableById(999999L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/listings/999999"))
                .andExpect(status().isOk())
                .andExpect(view().name("listing-detail"))
                .andExpect(model().attributeDoesNotExist("listing"))
                .andExpect(content().string(containsString("no longer available")));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void anExpiredOrRemovedListingShowsTheUnavailableMessageEvenThoughTheRowExists()
            throws Exception {

        when(gearListingService.findAvailableById(1L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/listings/1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("listing"))
                .andExpect(content().string(containsString("no longer available")));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void anOwnerWithNoReviewsShowsNoRatingsYet() throws Exception {

        when(gearListingService.findAvailableById(1L))
                .thenReturn(Optional.of(listingOwnedBy(7L)));
        when(userService.findByEmail("ada@example.com")).thenReturn(owner());
        when(userService.findById(7L)).thenReturn(owner());

        mockMvc.perform(get("/listings/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No ratings yet")));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void aNullDescriptionIsOmittedRatherThanShownBlank() throws Exception {

        GearListing listing = listingOwnedBy(7L);
        listing.setDescription(null);

        when(gearListingService.findAvailableById(1L))
                .thenReturn(Optional.of(listing));
        when(userService.findByEmail("ada@example.com")).thenReturn(owner());
        when(userService.findById(7L)).thenReturn(owner());

        mockMvc.perform(get("/listings/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(">null<"))));
    }

    private static GearListing listingWithStatus(Long ownerId, ListingStatus status) {

        GearListing listing = listingOwnedBy(ownerId);
        listing.setStatus(status);

        return listing;
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void myGearListsTheOwnersExpiredAndRemovedListingsToo() throws Exception {

        when(userService.findByEmail("ada@example.com")).thenReturn(owner());
        when(gearListingService.findByOwnerId(7L)).thenReturn(List.of(
                listingWithStatus(7L, ListingStatus.PUBLISHED),
                listingWithStatus(7L, ListingStatus.EXPIRED),
                listingWithStatus(7L, ListingStatus.REMOVED)
        ));

        mockMvc.perform(get("/listings/mine"))
                .andExpect(status().isOk())
                .andExpect(view().name("my-gear"))
                .andExpect(model().attribute("listings", hasSize(3)));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void aPublishedListingOnMyGearShowsAnEditControl() throws Exception {

        when(userService.findByEmail("ada@example.com")).thenReturn(owner());
        when(gearListingService.findByOwnerId(7L))
                .thenReturn(List.of(listingWithStatus(7L, ListingStatus.PUBLISHED)));

        mockMvc.perform(get("/listings/mine"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Edit<")))
                .andExpect(content().string(not(containsString("can no longer be edited"))));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void anExpiredListingOnMyGearHidesEditAndShowsAnExplanation() throws Exception {

        when(userService.findByEmail("ada@example.com")).thenReturn(owner());
        when(gearListingService.findByOwnerId(7L))
                .thenReturn(List.of(listingWithStatus(7L, ListingStatus.EXPIRED)));

        mockMvc.perform(get("/listings/mine"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(">Edit<"))))
                .andExpect(content().string(containsString("can no longer be edited")));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void deletingARequiresConfirmationOnMyGear() throws Exception {

        when(userService.findByEmail("ada@example.com")).thenReturn(owner());
        when(gearListingService.findByOwnerId(7L))
                .thenReturn(List.of(listingWithStatus(7L, ListingStatus.PUBLISHED)));

        mockMvc.perform(get("/listings/mine"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("confirm(")));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void theOwnerGetsThePrepopulatedEditForm() throws Exception {

        when(userService.findByEmail("ada@example.com")).thenReturn(owner());
        when(gearListingService.findById(1L))
                .thenReturn(Optional.of(listingOwnedBy(7L)));

        mockMvc.perform(get("/listings/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("add-listing"))
                .andExpect(model().attribute("isEdit", true))
                .andExpect(model().attribute(
                        "listingForm",
                        hasProperty("title", equalTo("Coleman 4-person tent"))
                ));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void someoneElsesListingCannotBeEditedByThisUser() throws Exception {

        when(userService.findByEmail("ada@example.com")).thenReturn(owner());
        when(gearListingService.findById(1L))
                .thenReturn(Optional.of(listingOwnedBy(9L)));

        mockMvc.perform(get("/listings/1/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/listings/mine"));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void editingANonexistentListingRedirectsToMyGear() throws Exception {

        when(userService.findByEmail("ada@example.com")).thenReturn(owner());
        when(gearListingService.findById(999999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/listings/999999/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/listings/mine"));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void aValidUpdateSubmissionRedirectsToMyGear() throws Exception {

        when(userService.findByEmail("ada@example.com")).thenReturn(owner());
        when(gearListingService.update(eq(1L), any(ListingForm.class), eq(7L)))
                .thenReturn(true);

        mockMvc.perform(post("/listings/1")
                        .with(csrf())
                        .param("title", "Coleman 4-person tent")
                        .param("category", "CAMPING")
                        .param("condition", "GOOD")
                        .param("dailyRate", "25.00")
                        .param("pickupSuburb", "Brunswick")
                        .param("pickupPostcode", "3056")
                        .param("expiryDate",
                                LocalDate.now().plusMonths(3).toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/listings/mine?updated"));

        verify(gearListingService).update(eq(1L), any(ListingForm.class), eq(7L));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void updatingSomeoneElsesListingRedirectsWithoutUpdating() throws Exception {

        when(userService.findByEmail("ada@example.com")).thenReturn(owner());
        when(gearListingService.update(eq(1L), any(ListingForm.class), eq(7L)))
                .thenReturn(false);

        mockMvc.perform(post("/listings/1")
                        .with(csrf())
                        .param("title", "Coleman 4-person tent")
                        .param("category", "CAMPING")
                        .param("condition", "GOOD")
                        .param("dailyRate", "25.00")
                        .param("pickupSuburb", "Brunswick")
                        .param("pickupPostcode", "3056")
                        .param("expiryDate",
                                LocalDate.now().plusMonths(3).toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/listings/mine"));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void anInvalidUpdateSubmissionRedisplaysTheEditFormAndUpdatesNothing()
            throws Exception {

        when(userService.findByEmail("ada@example.com")).thenReturn(owner());

        mockMvc.perform(post("/listings/1")
                        .with(csrf())
                        .param("title", "")
                        .param("category", "CAMPING")
                        .param("condition", "GOOD")
                        .param("dailyRate", "25.00")
                        .param("pickupSuburb", "Brunswick")
                        .param("pickupPostcode", "3056")
                        .param("expiryDate",
                                LocalDate.now().plusMonths(3).toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("add-listing"))
                .andExpect(model().attribute("isEdit", true))
                .andExpect(model().attributeHasFieldErrors("listingForm", "title"));

        verify(gearListingService, never()).update(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void deletingOwnListingRedirectsToMyGear() throws Exception {

        when(userService.findByEmail("ada@example.com")).thenReturn(owner());

        mockMvc.perform(post("/listings/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/listings/mine?deleted"));

        verify(gearListingService).delete(1L, 7L);
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void anExpiredListingOnMyGearShowsARelistLink() throws Exception {

        when(userService.findByEmail("ada@example.com")).thenReturn(owner());
        when(gearListingService.findByOwnerId(7L))
                .thenReturn(List.of(listingWithStatus(7L, ListingStatus.EXPIRED)));

        mockMvc.perform(get("/listings/mine"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Relist<")));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void aPublishedListingOnMyGearShowsNoRelistLink() throws Exception {

        when(userService.findByEmail("ada@example.com")).thenReturn(owner());
        when(gearListingService.findByOwnerId(7L))
                .thenReturn(List.of(listingWithStatus(7L, ListingStatus.PUBLISHED)));

        mockMvc.perform(get("/listings/mine"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(">Relist<"))));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void relistPrepopulatesEveryFieldExceptIdExpiryStatusAndCreatedAt()
            throws Exception {

        when(userService.findByEmail("ada@example.com")).thenReturn(owner());
        when(gearListingService.findById(1L))
                .thenReturn(Optional.of(listingWithStatus(7L, ListingStatus.EXPIRED)));

        mockMvc.perform(get("/listings/1/relist"))
                .andExpect(status().isOk())
                .andExpect(view().name("add-listing"))
                .andExpect(model().attribute(
                        "listingForm",
                        hasProperty("title", equalTo("Coleman 4-person tent"))
                ))
                .andExpect(model().attribute(
                        "listingForm",
                        hasProperty("category", equalTo(Category.CAMPING))
                ))
                .andExpect(model().attribute(
                        "listingForm",
                        hasProperty("pickupSuburb", equalTo("Brunswick"))
                ))
                .andExpect(model().attribute(
                        "listingForm",
                        hasProperty("expiryDate",
                                equalTo(LocalDate.now().plusMonths(6)))
                ));

        verify(gearListingService, never()).update(any(), any(), any());
        verify(gearListingService, never()).delete(any(), any());
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void relistDoesNotSetTheIsEditFlagSoItSubmitsAsANewListing() throws Exception {

        when(userService.findByEmail("ada@example.com")).thenReturn(owner());
        when(gearListingService.findById(1L))
                .thenReturn(Optional.of(listingWithStatus(7L, ListingStatus.EXPIRED)));

        mockMvc.perform(get("/listings/1/relist"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Publish Listing")));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void someoneElsesListingCannotBeRelistedByThisUser() throws Exception {

        when(userService.findByEmail("ada@example.com")).thenReturn(owner());
        when(gearListingService.findById(1L))
                .thenReturn(Optional.of(listingWithStatus(9L, ListingStatus.EXPIRED)));

        mockMvc.perform(get("/listings/1/relist"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/listings/mine"));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void relistingANonexistentListingRedirectsToMyGear() throws Exception {

        when(userService.findByEmail("ada@example.com")).thenReturn(owner());
        when(gearListingService.findById(999999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/listings/999999/relist"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/listings/mine"));
    }

    @Test
    void anAnonymousRequestForAPhotoIsServedNotRedirected() throws Exception {

        ListingPhoto photo = new ListingPhoto(
                5L, 1L, "image/jpeg", new byte[] { 1, 2, 3 }, 0);

        when(gearListingService.findPhotoById(5L)).thenReturn(Optional.of(photo));

        mockMvc.perform(get("/listings/photos/5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(new byte[] { 1, 2, 3 }));
    }

    @Test
    void aNonexistentPhotoIdReturns404() throws Exception {

        when(gearListingService.findPhotoById(999999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/listings/photos/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void theDetailViewExposesTheListingsThumbnailPhotoId() throws Exception {

        when(gearListingService.findAvailableById(1L))
                .thenReturn(Optional.of(listingOwnedBy(7L)));
        when(userService.findByEmail("ada@example.com")).thenReturn(owner());
        when(userService.findById(7L)).thenReturn(owner());
        when(gearListingService.findFirstPhotoIds(List.of(1L)))
                .thenReturn(Map.of(1L, 5L));

        mockMvc.perform(get("/listings/1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("thumbnailPhotoId", 5L));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void myGearExposesAThumbnailsMapKeyedByListingId() throws Exception {

        when(userService.findByEmail("ada@example.com")).thenReturn(owner());
        when(gearListingService.findByOwnerId(7L))
                .thenReturn(List.of(listingWithStatus(7L, ListingStatus.PUBLISHED)));
        when(gearListingService.findFirstPhotoIds(List.of(1L)))
                .thenReturn(Map.of(1L, 9L));

        mockMvc.perform(get("/listings/mine"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("thumbnails", Map.of(1L, 9L)));
    }
}
