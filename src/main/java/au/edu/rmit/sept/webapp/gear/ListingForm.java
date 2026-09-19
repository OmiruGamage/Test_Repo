package au.edu.rmit.sept.webapp.gear;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ListingForm {

    @NotBlank(message = "Please enter a title")
    @Size(max = 120, message = "Title must be 120 characters or fewer")
    private String title;

    @NotNull(message = "Please choose a category")
    private Category category;

    @NotNull(message = "Please choose a condition")
    private GearCondition condition;

    @NotNull(message = "Please enter a daily rate")
    @DecimalMin(value = "0.01", message = "Daily rate must be at least 0.01 AUD")
    @Digits(integer = 8, fraction = 2,
            message = "Daily rate must have at most 2 decimal places")
    private BigDecimal dailyRate;

    @NotBlank(message = "Please enter a pickup suburb")
    @Size(max = 100, message = "Suburb must be 100 characters or fewer")
    private String pickupSuburb;

    @NotBlank(message = "Please enter a pickup postcode")
    @Pattern(regexp = "\\d{4}", message = "Postcode must be 4 digits")
    private String pickupPostcode;

    @NotNull(message = "Please choose an expiry date")
    @FutureOrPresent(message = "Expiry date cannot be in the past")
    @WithinSixMonths
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryDate;

    @Size(max = 1000, message = "Description must be 1000 characters or fewer")
    private String description;

    @ValidPhotos(groups = RequiresPhotos.class)
    private List<MultipartFile> photos;

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

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<MultipartFile> getPhotos() {
        return photos;
    }

    public void setPhotos(List<MultipartFile> photos) {
        this.photos = photos;
    }
}
