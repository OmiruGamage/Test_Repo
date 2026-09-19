package au.edu.rmit.sept.webapp.review;

import java.time.LocalDate;

/**
 * A rental that has finished and can therefore be reviewed by either party.
 */
public class CompletedRental {

    private final Long rentalId;
    private final Long listingId;
    private final String listingTitle;
    private final Long ownerId;
    private final String ownerName;
    private final Long renterId;
    private final String renterName;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public CompletedRental(
            Long rentalId,
            Long listingId,
            String listingTitle,
            Long ownerId,
            String ownerName,
            Long renterId,
            String renterName,
            LocalDate startDate,
            LocalDate endDate) {

        this.rentalId = rentalId;
        this.listingId = listingId;
        this.listingTitle = listingTitle;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.renterId = renterId;
        this.renterName = renterName;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getRentalId() {
        return rentalId;
    }

    public Long getListingId() {
        return listingId;
    }

    public String getListingTitle() {
        return listingTitle;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Long getRenterId() {
        return renterId;
    }

    public String getRenterName() {
        return renterName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
