package au.edu.rmit.sept.webapp.gear;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ListingFormValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void startValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void stopValidator() {
        factory.close();
    }

    private static ListingForm validForm() {

        ListingForm form = new ListingForm();

        form.setTitle("Coleman 4-person tent");
        form.setCategory(Category.CAMPING);
        form.setCondition(GearCondition.GOOD);
        form.setDailyRate(new BigDecimal("25.00"));
        form.setPickupSuburb("Brunswick");
        form.setPickupPostcode("3056");
        form.setExpiryDate(LocalDate.now().plusMonths(3));
        form.setDescription("Sleeps 4, used twice, includes pegs and fly.");

        return form;
    }

    private static Set<String> fieldsWithErrors(ListingForm form) {

        return validator.validate(form)
                .stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private static Set<String> fieldsWithErrors(ListingForm form, Class<?> group) {

        return validator.validate(form, group)
                .stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private static MockMultipartFile aJpegFile() {
        return new MockMultipartFile(
                "photos", "photo.jpg", "image/jpeg", new byte[] { 1, 2, 3 });
    }

    @Test
    void aCompletelyValidFormHasNoErrors() {
        assertThat(fieldsWithErrors(validForm())).isEmpty();
    }

    @Test
    void everyMandatoryFieldIsReportedWhenTheFormIsEmpty() {

        Set<String> errors = fieldsWithErrors(new ListingForm());

        assertThat(errors).containsExactlyInAnyOrder(
                "title",
                "category",
                "condition",
                "dailyRate",
                "pickupSuburb",
                "pickupPostcode",
                "expiryDate"
        );
    }

    @Test
    void anAbsentDescriptionIsAcceptedBecauseItIsOptional() {

        ListingForm form = validForm();
        form.setDescription(null);

        assertThat(fieldsWithErrors(form)).isEmpty();
    }

    @Test
    void threeBadFieldsProduceThreeSeparateErrorsNotOne() {

        ListingForm form = validForm();
        form.setCategory(null);
        form.setDailyRate(new BigDecimal("-5.00"));
        form.setPickupPostcode("abcde");

        assertThat(fieldsWithErrors(form))
                .containsExactlyInAnyOrder("category", "dailyRate", "pickupPostcode");
    }

    @ParameterizedTest
    @ValueSource(strings = { "0.01", "25.00", "1234.56", "99999999.99" })
    void acceptableDailyRates(String rate) {

        ListingForm form = validForm();
        form.setDailyRate(new BigDecimal(rate));

        assertThat(fieldsWithErrors(form)).doesNotContain("dailyRate");
    }

    @ParameterizedTest
    @ValueSource(strings = { "0.00", "-0.01", "-5.00", "0.009" })
    void rejectedDailyRates(String rate) {

        ListingForm form = validForm();
        form.setDailyRate(new BigDecimal(rate));

        assertThat(fieldsWithErrors(form)).contains("dailyRate");
    }

    @ParameterizedTest
    @ValueSource(strings = { "3000", "0800", "0000", "9999" })
    void acceptablePostcodes(String postcode) {

        ListingForm form = validForm();
        form.setPickupPostcode(postcode);

        assertThat(fieldsWithErrors(form)).doesNotContain("pickupPostcode");
    }

    @ParameterizedTest
    @ValueSource(strings = { "300", "30000", "abcd", "30 0", "3o56", " 3056" })
    void rejectedPostcodes(String postcode) {

        ListingForm form = validForm();
        form.setPickupPostcode(postcode);

        assertThat(fieldsWithErrors(form)).contains("pickupPostcode");
    }

    @Test
    void descriptionOfExactlyOneThousandCharactersIsAccepted() {

        ListingForm form = validForm();
        form.setDescription("x".repeat(1000));

        assertThat(fieldsWithErrors(form)).doesNotContain("description");
    }

    @Test
    void descriptionOfOneThousandAndOneCharactersIsRejected() {

        ListingForm form = validForm();
        form.setDescription("x".repeat(1001));

        assertThat(fieldsWithErrors(form)).contains("description");
    }

    @Test
    void titleOfExactlyOneHundredAndTwentyCharactersIsAccepted() {

        ListingForm form = validForm();
        form.setTitle("x".repeat(120));

        assertThat(fieldsWithErrors(form)).doesNotContain("title");
    }

    @Test
    void titleOfOneHundredAndTwentyOneCharactersIsRejected() {

        ListingForm form = validForm();
        form.setTitle("x".repeat(121));

        assertThat(fieldsWithErrors(form)).contains("title");
    }

    @Test
    void whitespaceOnlyTextFieldsAreRejected() {

        ListingForm form = validForm();
        form.setTitle("   ");
        form.setPickupSuburb("   ");

        assertThat(fieldsWithErrors(form))
                .contains("title", "pickupSuburb");
    }

    @Test
    void anExpiryDateExactlySixMonthsAwayIsAccepted() {

        ListingForm form = validForm();
        form.setExpiryDate(LocalDate.now().plusMonths(6));

        assertThat(fieldsWithErrors(form)).doesNotContain("expiryDate");
    }

    @Test
    void anExpiryDateOneDayBeyondSixMonthsIsRejected() {

        ListingForm form = validForm();
        form.setExpiryDate(LocalDate.now().plusMonths(6).plusDays(1));

        assertThat(fieldsWithErrors(form)).contains("expiryDate");
    }

    @Test
    void anExpiryDateOfTodayIsAccepted() {

        ListingForm form = validForm();
        form.setExpiryDate(LocalDate.now());

        assertThat(fieldsWithErrors(form)).doesNotContain("expiryDate");
    }

    @Test
    void anExpiryDateOfYesterdayIsRejected() {

        ListingForm form = validForm();
        form.setExpiryDate(LocalDate.now().minusDays(1));

        assertThat(fieldsWithErrors(form)).contains("expiryDate");
    }

    @Test
    void aMissingExpiryDateIsRejected() {

        ListingForm form = validForm();
        form.setExpiryDate(null);

        assertThat(fieldsWithErrors(form)).contains("expiryDate");
    }

    @Test
    void photosAreNotCheckedUnderTheDefaultGroupUsedByEditAndUpdate() {

        ListingForm form = validForm();
        form.setPhotos(null);

        assertThat(fieldsWithErrors(form)).doesNotContain("photos");
    }

    @Test
    void oneToFiveJpegOrPngPhotosPassTheRequiresPhotosGroup() {

        ListingForm form = validForm();
        form.setPhotos(List.of(aJpegFile()));

        assertThat(fieldsWithErrors(form, RequiresPhotos.class))
                .doesNotContain("photos");
    }

    @Test
    void noPhotosIsRejectedUnderTheRequiresPhotosGroup() {

        ListingForm form = validForm();
        form.setPhotos(List.of());

        assertThat(fieldsWithErrors(form, RequiresPhotos.class)).contains("photos");
    }

    @Test
    void aNullPhotosListIsRejectedUnderTheRequiresPhotosGroup() {

        ListingForm form = validForm();
        form.setPhotos(null);

        assertThat(fieldsWithErrors(form, RequiresPhotos.class)).contains("photos");
    }

    @Test
    void exactlyFivePhotosIsAccepted() {

        ListingForm form = validForm();

        form.setPhotos(IntStream.range(0, 5)
                .mapToObj(i -> (MultipartFile) aJpegFile())
                .toList());

        assertThat(fieldsWithErrors(form, RequiresPhotos.class))
                .doesNotContain("photos");
    }

    @Test
    void sixPhotosIsRejected() {

        ListingForm form = validForm();

        form.setPhotos(IntStream.range(0, 6)
                .mapToObj(i -> (MultipartFile) aJpegFile())
                .toList());

        assertThat(fieldsWithErrors(form, RequiresPhotos.class)).contains("photos");
    }

    @Test
    void aNonImageContentTypeIsRejected() {

        ListingForm form = validForm();

        form.setPhotos(List.of(new MockMultipartFile(
                "photos", "doc.pdf", "application/pdf", new byte[] { 1 })));

        assertThat(fieldsWithErrors(form, RequiresPhotos.class)).contains("photos");
    }

    @Test
    void aPhotoLargerThanFiveMegabytesIsRejected() {

        ListingForm form = validForm();

        form.setPhotos(List.of(new MockMultipartFile(
                "photos", "big.jpg", "image/jpeg",
                new byte[5 * 1024 * 1024 + 1])));

        assertThat(fieldsWithErrors(form, RequiresPhotos.class)).contains("photos");
    }

    @Test
    void aPhotoAtExactlyFiveMegabytesIsAccepted() {

        ListingForm form = validForm();

        form.setPhotos(List.of(new MockMultipartFile(
                "photos", "exact.jpg", "image/jpeg",
                new byte[5 * 1024 * 1024])));

        assertThat(fieldsWithErrors(form, RequiresPhotos.class))
                .doesNotContain("photos");
    }
}
