package au.edu.rmit.sept.webapp.gear;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = WithinSixMonthsValidator.class)
public @interface WithinSixMonths {

    String message() default
            "Expiry date must be no more than six months from today";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
