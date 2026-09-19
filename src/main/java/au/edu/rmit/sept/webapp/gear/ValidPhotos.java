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
@Constraint(validatedBy = ValidPhotosValidator.class)
public @interface ValidPhotos {

    String message() default "Please add 1 to 5 JPEG or PNG photos, 5MB or smaller each";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
