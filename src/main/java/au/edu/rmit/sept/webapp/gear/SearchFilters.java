package au.edu.rmit.sept.webapp.gear;

import java.math.BigDecimal;

public class SearchFilters {

    private Category category;
    private GearCondition condition;
    private String pickupSuburb;
    private String pickupPostcode;
    private BigDecimal minRate;
    private BigDecimal maxRate;

    public SearchFilters() {
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

    public BigDecimal getMinRate() {
        return minRate;
    }

    public void setMinRate(BigDecimal minRate) {
        this.minRate = minRate;
    }

    public BigDecimal getMaxRate() {
        return maxRate;
    }

    public void setMaxRate(BigDecimal maxRate) {
        this.maxRate = maxRate;
    }
}
