package au.edu.rmit.sept.webapp.gear;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class WithinSixMonthsValidator
        implements ConstraintValidator<WithinSixMonths, LocalDate> {

    @Override
    public boolean isValid(
            LocalDate value,
            ConstraintValidatorContext context) {

        if (value == null) {
            return true;
        }

        return !value.isAfter(LocalDate.now().plusMonths(6));
    }
}
