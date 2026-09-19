package au.edu.rmit.sept.webapp.gear;

public class ListingPhoto {

    private Long id;
    private Long listingId;
    private String contentType;
    private byte[] data;
    private int displayOrder;

    public ListingPhoto() {
    }

    public ListingPhoto(
            Long id,
            Long listingId,
            String contentType,
            byte[] data,
            int displayOrder) {

        this.id = id;
        this.listingId = listingId;
        this.contentType = contentType;
        this.data = data;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getListingId() {
        return listingId;
    }

    public void setListingId(Long listingId) {
        this.listingId = listingId;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
