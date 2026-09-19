package au.edu.rmit.sept.webapp.admin;

import au.edu.rmit.sept.webapp.gear.Category;
import au.edu.rmit.sept.webapp.gear.ListingStatus;
import au.edu.rmit.sept.webapp.user.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 19);
    private static final Long ADMIN_ID = 1L;
    private static final Long USER_ID = 2L;
    private static final Long LISTING_ID = 30L;

    @Mock
    private AdminDAO adminDAO;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-19T00:00:00Z"), ZoneOffset.UTC);
        adminService = new AdminService(adminDAO, clock);
    }

    @Test
    void deactivateRequiresReason() {
        assertThatThrownBy(() -> adminService.deactivateUser(ADMIN_ID, USER_ID, "   "))
                .isInstanceOf(AdminActionException.class)
                .hasMessageContaining("reason");

        verify(adminDAO, never()).setUserActive(anyLong(), anyBoolean());
    }

    @Test
    void reasonLongerThanLimitIsRejected() {
        String longReason = "x".repeat(AdminService.MAX_REASON_LENGTH + 1);

        assertThatThrownBy(() -> adminService.deactivateUser(ADMIN_ID, USER_ID, longReason))
                .isInstanceOf(AdminActionException.class);
    }

    @Test
    void adminCannotDeactivateThemself() {
        assertThatThrownBy(() -> adminService.deactivateUser(ADMIN_ID, ADMIN_ID, "test"))
                .isInstanceOf(AdminActionException.class);
    }

    @Test
    void otherAdminsCannotBeDeactivated() {
        when(adminDAO.findUserById(USER_ID)).thenReturn(Optional.of(user(UserRole.ADMIN, true)));

        assertThatThrownBy(() -> adminService.deactivateUser(ADMIN_ID, USER_ID, "test"))
                .isInstanceOf(AdminActionException.class);
    }

    @Test
    void deactivateBlocksAccountRemovesListingsAndLogsAction() {
        when(adminDAO.findUserById(USER_ID)).thenReturn(Optional.of(user(UserRole.USER, true)));

        adminService.deactivateUser(ADMIN_ID, USER_ID, "  Spam listings  ");

        verify(adminDAO).setUserActive(USER_ID, false);
        verify(adminDAO).removePublishedListingsByOwner(USER_ID);
        verify(adminDAO).closePendingRequestsForUser(USER_ID);
        verify(adminDAO).recordAction(ADMIN_ID, AdminActionType.DEACTIVATE_USER, USER_ID, "Spam listings");
    }

    @Test
    void reactivateRejectsActiveAccount() {
        when(adminDAO.findUserById(USER_ID)).thenReturn(Optional.of(user(UserRole.USER, true)));

        assertThatThrownBy(() -> adminService.reactivateUser(ADMIN_ID, USER_ID, "appeal"))
                .isInstanceOf(AdminActionException.class);
    }

    @Test
    void removeListingClosesPendingRequestsAndLogsAction() {
        when(adminDAO.findListingById(LISTING_ID))
                .thenReturn(Optional.of(listing(ListingStatus.PUBLISHED, TODAY.plusDays(5), true)));

        adminService.removeListing(ADMIN_ID, LISTING_ID, "Offensive photo");

        verify(adminDAO).setListingStatus(LISTING_ID, ListingStatus.REMOVED);
        verify(adminDAO).closePendingRequestsForListing(LISTING_ID);
        verify(adminDAO).recordAction(ADMIN_ID, AdminActionType.REMOVE_LISTING, LISTING_ID, "Offensive photo");
    }

    @Test
    void removedListingCannotBeRemovedAgain() {
        when(adminDAO.findListingById(LISTING_ID))
                .thenReturn(Optional.of(listing(ListingStatus.REMOVED, TODAY.plusDays(5), true)));

        assertThatThrownBy(() -> adminService.removeListing(ADMIN_ID, LISTING_ID, "again"))
                .isInstanceOf(AdminActionException.class);
    }

    @Test
    void restoreBeforeExpiryPublishesListing() {
        when(adminDAO.findListingById(LISTING_ID))
                .thenReturn(Optional.of(listing(ListingStatus.REMOVED, TODAY, true)));

        ListingStatus status = adminService.restoreListing(ADMIN_ID, LISTING_ID, "Removed by mistake");

        assertThat(status).isEqualTo(ListingStatus.PUBLISHED);
        verify(adminDAO).setListingStatus(LISTING_ID, ListingStatus.PUBLISHED);
    }

    @Test
    void restoreAfterExpiryMakesListingExpired() {
        when(adminDAO.findListingById(LISTING_ID))
                .thenReturn(Optional.of(listing(ListingStatus.REMOVED, TODAY.minusDays(1), true)));

        ListingStatus status = adminService.restoreListing(ADMIN_ID, LISTING_ID, "Appeal upheld");

        assertThat(status).isEqualTo(ListingStatus.EXPIRED);
        verify(adminDAO).setListingStatus(LISTING_ID, ListingStatus.EXPIRED);
    }

    @Test
    void cannotRestoreListingOfDeactivatedOwner() {
        when(adminDAO.findListingById(LISTING_ID))
                .thenReturn(Optional.of(listing(ListingStatus.REMOVED, TODAY.plusDays(5), false)));

        assertThatThrownBy(() -> adminService.restoreListing(ADMIN_ID, LISTING_ID, "reason"))
                .isInstanceOf(AdminActionException.class);
    }

    @Test
    void onlyRemovedListingsCanBeRestored() {
        when(adminDAO.findListingById(LISTING_ID))
                .thenReturn(Optional.of(listing(ListingStatus.PUBLISHED, TODAY.plusDays(5), true)));

        assertThatThrownBy(() -> adminService.restoreListing(ADMIN_ID, LISTING_ID, "reason"))
                .isInstanceOf(AdminActionException.class);
    }

    @Test
    void safeReturnOnlyAllowsAdminPaths() {
        assertThat(AdminController.safeReturn("/admin/users/5")).isEqualTo("/admin/users/5");
        assertThat(AdminController.safeReturn("https://evil.example")).isEqualTo("/admin/listings");
        assertThat(AdminController.safeReturn(null)).isEqualTo("/admin/listings");
    }

    private static AdminUserView user(UserRole role, boolean active) {
        return new AdminUserView(USER_ID, "Sam", "sam@test.com", role, active);
    }

    private static AdminListingView listing(ListingStatus status, LocalDate expiry, boolean ownerActive) {
        return new AdminListingView(
                LISTING_ID, "Tent", USER_ID, "Sam", ownerActive,
                Category.CAMPING, new BigDecimal("20.00"), expiry, status,
                LocalDateTime.of(2026, 9, 1, 10, 0));
    }
}
