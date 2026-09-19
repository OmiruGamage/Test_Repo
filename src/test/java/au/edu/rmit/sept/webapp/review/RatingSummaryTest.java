package au.edu.rmit.sept.webapp.review;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RatingSummaryTest {

    @Test
    void noReviewsShowsNoRatingsYet() {
        assertThat(RatingSummary.of(0, null).getDisplay()).isEqualTo("No ratings yet");
        assertThat(RatingSummary.empty().isEmpty()).isTrue();
    }

    @Test
    void averageIsRoundedToOneDecimalPlace() {
        RatingSummary summary = RatingSummary.of(3, new BigDecimal("4.6667"));

        assertThat(summary.getAverage()).isEqualByComparingTo("4.7");
        assertThat(summary.getDisplay()).isEqualTo("4.7 / 5 (3 reviews)");
    }

    @Test
    void singleReviewUsesSingularWording() {
        assertThat(RatingSummary.of(1, new BigDecimal("5")).getDisplay())
                .isEqualTo("5.0 / 5 (1 review)");
    }
}
