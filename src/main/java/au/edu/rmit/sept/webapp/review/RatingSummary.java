package au.edu.rmit.sept.webapp.review;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Average rating (1 decimal place) and number of reviews for a listing or user.
 */
public final class RatingSummary {

    private static final RatingSummary EMPTY = new RatingSummary(0, null);

    private final long count;
    private final BigDecimal average;

    private RatingSummary(long count, BigDecimal average) {
        this.count = count;
        this.average = average;
    }

    public static RatingSummary empty() {
        return EMPTY;
    }

    public static RatingSummary of(long count, BigDecimal average) {

        if (count <= 0 || average == null) {
            return EMPTY;
        }

        return new RatingSummary(count, average.setScale(1, RoundingMode.HALF_UP));
    }

    public long getCount() {
        return count;
    }

    public BigDecimal getAverage() {
        return average;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public String getDisplay() {

        if (isEmpty()) {
            return "No ratings yet";
        }

        return average.toPlainString() + " / 5 ("
                + count + (count == 1 ? " review)" : " reviews)");
    }
}
