package au.edu.rmit.sept.webapp.gear;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GearListingService {

    private final GearListingDAO gearListingDAO;
    private final ListingPhotoDAO listingPhotoDAO;

    public GearListingService(
            GearListingDAO gearListingDAO,
            ListingPhotoDAO listingPhotoDAO) {

        this.gearListingDAO = gearListingDAO;
        this.listingPhotoDAO = listingPhotoDAO;
    }

    public Optional<GearListing> findAvailableById(Long id) {

        return gearListingDAO.findById(id)
                .filter(listing -> listing.getStatus() == ListingStatus.PUBLISHED);
    }

    public Optional<GearListing> findById(Long id) {
        return gearListingDAO.findById(id);
    }

    public List<GearListing> findByOwnerId(Long ownerId) {
        return gearListingDAO.findByOwnerId(ownerId);
    }

    public Long create(ListingForm form, Long ownerId) {

        GearListing listing = new GearListing();

        listing.setOwnerId(ownerId);
        listing.setTitle(form.getTitle().trim());
        listing.setCategory(form.getCategory());
        listing.setCondition(form.getCondition());

        listing.setDailyRate(
                form.getDailyRate().setScale(2, RoundingMode.UNNECESSARY)
        );

        listing.setPickupSuburb(form.getPickupSuburb().trim());
        listing.setPickupPostcode(form.getPickupPostcode().trim());
        listing.setDescription(trimmedOrNull(form.getDescription()));
        listing.setExpiryDate(form.getExpiryDate());
        listing.setStatus(ListingStatus.PUBLISHED);

        Long id = gearListingDAO.save(listing);

        int displayOrder = 0;

        for (MultipartFile photo : form.getPhotos()) {

            if (photo.isEmpty()) {
                continue;
            }

            listingPhotoDAO.save(
                    id,
                    photo.getContentType(),
                    readBytes(photo),
                    displayOrder
            );

            displayOrder++;
        }

        return id;
    }

    public Optional<ListingPhoto> findPhotoById(Long id) {
        return listingPhotoDAO.findById(id);
    }

    public Map<Long, Long> findFirstPhotoIds(List<Long> listingIds) {
        return listingPhotoDAO.findFirstPhotoIdsByListingIds(listingIds);
    }

    private static byte[] readBytes(MultipartFile file) {

        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public boolean update(Long id, ListingForm form, Long ownerId) {

        Optional<GearListing> maybeListing = gearListingDAO.findById(id);

        if (maybeListing.isEmpty()
                || !maybeListing.get().getOwnerId().equals(ownerId)) {
            return false;
        }

        GearListing listing = maybeListing.get();

        listing.setTitle(form.getTitle().trim());
        listing.setCategory(form.getCategory());
        listing.setCondition(form.getCondition());

        listing.setDailyRate(
                form.getDailyRate().setScale(2, RoundingMode.UNNECESSARY)
        );

        listing.setPickupSuburb(form.getPickupSuburb().trim());
        listing.setPickupPostcode(form.getPickupPostcode().trim());
        listing.setDescription(trimmedOrNull(form.getDescription()));

        gearListingDAO.update(listing);

        return true;
    }

    public boolean delete(Long id, Long ownerId) {

        Optional<GearListing> maybeListing = gearListingDAO.findById(id);

        if (maybeListing.isEmpty()
                || !maybeListing.get().getOwnerId().equals(ownerId)) {
            return false;
        }

        GearListing listing = maybeListing.get();
        listing.setStatus(ListingStatus.REMOVED);

        gearListingDAO.update(listing);

        return true;
    }

    private static String trimmedOrNull(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
