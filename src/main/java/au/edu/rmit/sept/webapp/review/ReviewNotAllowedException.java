package au.edu.rmit.sept.webapp.review;

public class ReviewNotAllowedException extends RuntimeException {

    public ReviewNotAllowedException(String message) {
        super(message);
    }
}
