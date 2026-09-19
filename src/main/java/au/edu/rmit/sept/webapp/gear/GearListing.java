package au.edu.rmit.sept.webapp.gear;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class GearListing {

    private Long id;
    private Long ownerId;
    private String title;
    private Category category;
    private GearCondition condition;
    private BigDecimal dailyRate;
    private String pickupSuburb;
    private String pickupPostcode;
    private String description;
    private LocalDate expiryDate;
    private ListingStatus status;
    private LocalDateTime createdAt;

    public GearListing() {
    }

    public GearListing(
            Long id,
            Long ownerId,
            String title,
            Category category,
            GearCondition condition,
            BigDecimal dailyRate,
            String pickupSuburb,
            String pickupPostcode,
            String description,
            LocalDate expiryDate,
            ListingStatus status,
            LocalDateTime createdAt) {

        this.id = id;
        this.ownerId = ownerId;
        this.title = title;
        this.category = category;
        this.condition = condition;
        this.dailyRate = dailyRate;
        this.pickupSuburb = pickupSuburb;
        this.pickupPostcode = pickupPostcode;
        this.description = description;
        this.expiryDate = expiryDate;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public GearCondition getCondition() {
        return condition;
    }

    public void setCondition(GearCondition condition) {
        this.condition = condition;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public void setDailyRate(BigDecimal dailyRate) {
        this.dailyRate = dailyRate;
    }

    public String getPickupSuburb() {
        return pickupSuburb;
    }

    public void setPickupSuburb(String pickupSuburb) {
        this.pickupSuburb = pickupSuburb;
    }

    public String getPickupPostcode() {
        return pickupPostcode;
    }

    public void setPickupPostcode(String pickupPostcode) {
        this.pickupPostcode = pickupPostcode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public void setStatus(ListingStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
