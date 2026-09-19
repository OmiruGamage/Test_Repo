package au.edu.rmit.sept.webapp.gear;

import au.edu.rmit.sept.webapp.config.SecurityConfig;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(GearListingBrowseController.class)
@Import(SecurityConfig.class)
class GearListingBrowseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GearListingDAO gearListingDAO;

    @MockitoBean
    private ListingPhotoDAO listingPhotoDAO;

    private static GearListing aListing() {

        return new GearListing(
                1L,
                7L,
                "Coleman 4-person tent",
                Category.CAMPING,
                GearCondition.GOOD,
                new BigDecimal("25.00"),
                "Brunswick",
                "3056",
                null,
                LocalDate.now().plusMonths(6),
                ListingStatus.PUBLISHED,
                null
        );
    }

    @Test
    void anAnonymousGetReturnsTheFullPublishedSet() throws Exception {

        when(gearListingDAO.search(any())).thenReturn(List.of(aListing()));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("browse"))
                .andExpect(model().attributeExists("listings"));
    }

    @Test
    void eachFilterParamNarrowsTheSearchFilters() throws Exception {

        ArgumentCaptor<SearchFilters> captor =
                ArgumentCaptor.forClass(SearchFilters.class);
        when(gearListingDAO.search(any())).thenReturn(List.of());

        mockMvc.perform(get("/")
                        .param("category", "SPORTS")
                        .param("condition", "FAIR")
                        .param("pickupSuburb", "Carlton")
                        .param("pickupPostcode", "3053")
                        .param("minRate", "10.00")
                        .param("maxRate", "50.00"))
                .andExpect(status().isOk());

        verify(gearListingDAO).search(captor.capture());

        SearchFilters filters = captor.getValue();

        assertThat(filters.getCategory()).isEqualTo(Category.SPORTS);
        assertThat(filters.getCondition()).isEqualTo(GearCondition.FAIR);
        assertThat(filters.getPickupSuburb()).isEqualTo("Carlton");
        assertThat(filters.getPickupPostcode()).isEqualTo("3053");
        assertThat(filters.getMinRate()).isEqualTo(new BigDecimal("10.00"));
        assertThat(filters.getMaxRate()).isEqualTo(new BigDecimal("50.00"));
    }

    @Test
    void anUnmatchedFilterCombinationReturnsAnEmptyListingsAttribute()
            throws Exception {

        when(gearListingDAO.search(any())).thenReturn(List.of());

        mockMvc.perform(get("/").param("category", "MUSIC"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("listings", empty()));
    }

    @Test
    void clearingFiltersRestoresTheFullListWithNullFilters() throws Exception {

        ArgumentCaptor<SearchFilters> captor =
                ArgumentCaptor.forClass(SearchFilters.class);
        when(gearListingDAO.search(any())).thenReturn(List.of());

        mockMvc.perform(get("/")
                        .param("category", "")
                        .param("condition", "")
                        .param("pickupSuburb", "")
                        .param("pickupPostcode", ""))
                .andExpect(status().isOk());

        verify(gearListingDAO).search(captor.capture());

        SearchFilters filters = captor.getValue();

        assertThat(filters.getCategory()).isNull();
        assertThat(filters.getCondition()).isNull();
        assertThat(filters.getPickupSuburb()).isNull();
        assertThat(filters.getPickupPostcode()).isNull();
    }

    @Test
    void resultsExposeAThumbnailsMapKeyedByListingId() throws Exception {

        when(gearListingDAO.search(any())).thenReturn(List.of(aListing()));
        when(listingPhotoDAO.findFirstPhotoIdsByListingIds(List.of(1L)))
                .thenReturn(Map.of(1L, 9L));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("thumbnails", Map.of(1L, 9L)));
    }
}
