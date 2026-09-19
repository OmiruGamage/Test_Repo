package au.edu.rmit.sept.webapp.admin;

import au.edu.rmit.sept.webapp.gear.ListingStatus;
import au.edu.rmit.sept.webapp.user.UserRole;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class AdminService {

    static final int MAX_REASON_LENGTH = 500;
    static final int RECENT_ACTION_LIMIT = 50;

    private final AdminDAO adminDAO;
    private final Clock clock;

    @Autowired
    public AdminService(AdminDAO adminDAO) {
        this(adminDAO, Clock.systemDefaultZone());
    }

    AdminService(AdminDAO adminDAO, Clock clock) {
        this.adminDAO = adminDAO;
        this.clock = clock;
    }

    // ---------- viewing ----------

    public List<AdminUserView> findUsers(String search, UserRole role, Boolean active) {
        return adminDAO.findUsers(search, role, active);
    }

    public AdminUserView getUser(Long userId) {
        return adminDAO.findUserById(userId)
                .orElseThrow(() -> new AdminActionException("That account does not exist"));
    }

    public List<AdminListingView> findListings(String search, ListingStatus status) {
        adminDAO.expireOverdueListings(today());
        return adminDAO.findListings(search, status);
    }

    public List<AdminListingView> findListingsByOwner(Long ownerId) {
        adminDAO.expireOverdueListings(today());
        return adminDAO.findListingsByOwner(ownerId);
    }

    public List<AdminActionView> findRecentActions() {
        return adminDAO.findRecentActions(RECENT_ACTION_LIMIT);
    }

    public List<AdminActionView> findActionsForUser(Long userId) {
        return adminDAO.findActionsForUser(userId);
    }

    // ---------- users ----------

    /**
     * Blocks the account from logging in, takes its published listings out of
     * search and closes any pending rental requests it is involved in.
     */
    @Transactional
    public void deactivateUser(Long adminId, Long userId, String reason) {

        String cleanReason = requireReason(reason);

        if (userId.equals(adminId)) {
            throw new AdminActionException("You can't deactivate your own account");
        }

        AdminUserView user = getUser(userId);

        if (user.getRole() == UserRole.ADMIN) {
            throw new AdminActionException("Administrator accounts can't be deactivated here");
        }

        if (!user.isActive()) {
            throw new AdminActionException("This account is already deactivated");
        }

        adminDAO.setUserActive(userId, false);
        adminDAO.removePublishedListingsByOwner(userId);
        adminDAO.closePendingRequestsForUser(userId);
        adminDAO.recordAction(adminId, AdminActionType.DEACTIVATE_USER, userId, cleanReason);
    }

    /** Lets the account log in again. Removed listings stay removed until restored one by one. */
    @Transactional
    public void reactivateUser(Long adminId, Long userId, String reason) {

        String cleanReason = requireReason(reason);
        AdminUserView user = getUser(userId);

        if (user.isActive()) {
            throw new AdminActionException("This account is already active");
        }

        adminDAO.setUserActive(userId, true);
        adminDAO.recordAction(adminId, AdminActionType.REACTIVATE_USER, userId, cleanReason);
    }

    // ---------- listings ----------

    @Transactional
    public void removeListing(Long adminId, Long listingId, String reason) {

        String cleanReason = requireReason(reason);
        AdminListingView listing = getListing(listingId);

        if (listing.getStatus() == ListingStatus.REMOVED) {
            throw new AdminActionException("This listing has already been removed");
        }

        adminDAO.setListingStatus(listingId, ListingStatus.REMOVED);
        adminDAO.closePendingRequestsForListing(listingId);
        adminDAO.recordAction(adminId, AdminActionType.REMOVE_LISTING, listingId, cleanReason);
    }

    /**
     * Restores a removed listing. It goes back to Published unless its expiry
     * date has passed, in which case it becomes Expired.
     */
    @Transactional
    public ListingStatus restoreListing(Long adminId, Long listingId, String reason) {

        String cleanReason = requireReason(reason);
        AdminListingView listing = getListing(listingId);

        if (listing.getStatus() != ListingStatus.REMOVED) {
            throw new AdminActionException("Only removed listings can be restored");
        }

        if (!listing.isOwnerActive()) {
            throw new AdminActionException(
                    "Reactivate the owner's account before restoring their listing");
        }

        ListingStatus newStatus = listing.getExpiryDate().isBefore(today())
                ? ListingStatus.EXPIRED
                : ListingStatus.PUBLISHED;

        adminDAO.setListingStatus(listingId, newStatus);
        adminDAO.recordAction(adminId, AdminActionType.RESTORE_LISTING, listingId, cleanReason);

        return newStatus;
    }

    private AdminListingView getListing(Long listingId) {
        return adminDAO.findListingById(listingId)
                .orElseThrow(() -> new AdminActionException("That listing does not exist"));
    }

    static String requireReason(String reason) {

        if (reason == null || reason.isBlank()) {
            throw new AdminActionException("Please give a reason for this action");
        }

        String trimmed = reason.trim();

        if (trimmed.length() > MAX_REASON_LENGTH) {
            throw new AdminActionException(
                    "Reason must be " + MAX_REASON_LENGTH + " characters or fewer");
        }

        return trimmed;
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
