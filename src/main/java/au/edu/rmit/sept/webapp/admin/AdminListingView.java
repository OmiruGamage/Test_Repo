package au.edu.rmit.sept.webapp.admin;

import au.edu.rmit.sept.webapp.gear.Category;
import au.edu.rmit.sept.webapp.gear.ListingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Listing row for the admin area, joined with its owner. */
public class AdminListingView {

    private final Long id;
    private final String title;
    private final Long ownerId;
    private final String ownerName;
    private final boolean ownerActive;
    private final Category category;
    private final BigDecimal dailyRate;
    private final LocalDate expiryDate;
    private final ListingStatus status;
    private final LocalDateTime createdAt;

    public AdminListingView(
            Long id,
            String title,
            Long ownerId,
            String ownerName,
            boolean ownerActive,
            Category category,
            BigDecimal dailyRate,
            LocalDate expiryDate,
            ListingStatus status,
            LocalDateTime createdAt) {

        this.id = id;
        this.title = title;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.ownerActive = ownerActive;
        this.category = category;
        this.dailyRate = dailyRate;
        this.expiryDate = expiryDate;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public boolean isOwnerActive() {
        return ownerActive;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
