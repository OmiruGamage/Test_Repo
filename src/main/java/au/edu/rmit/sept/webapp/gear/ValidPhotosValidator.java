package au.edu.rmit.sept.webapp.gear;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

public class ValidPhotosValidator
        implements ConstraintValidator<ValidPhotos, List<MultipartFile>> {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png");

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private static final int MAX_PHOTOS = 5;

    @Override
    public boolean isValid(
            List<MultipartFile> photos,
            ConstraintValidatorContext context) {

        List<MultipartFile> nonEmpty = photos == null
                ? List.of()
                : photos.stream().filter(file -> !file.isEmpty()).toList();

        if (nonEmpty.isEmpty()) {
            return fail(context, "Please add at least one photo");
        }

        if (nonEmpty.size() > MAX_PHOTOS) {
            return fail(context, "You can add at most " + MAX_PHOTOS + " photos");
        }

        for (MultipartFile file : nonEmpty) {

            if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
                return fail(context, "Photos must be JPEG or PNG");
            }

            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                return fail(context, "Each photo must be 5MB or smaller");
            }
        }

        return true;
    }

    private static boolean fail(
            ConstraintValidatorContext context,
            String message) {

        context.disableDefaultConstraintViolation();

        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();

        return false;
    }
}
