package com.cinebook.service;

import com.cinebook.dto.*;
import com.cinebook.entity.*;
import com.cinebook.entity.enums.*;
import com.cinebook.exception.ResourceNotFoundException;
import com.cinebook.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminManagementServiceTest {

    @Mock private MovieRepository movieRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private HallRepository hallRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private SeatRepository seatRepository;
    @Mock private ShowtimeRepository showtimeRepository;
    @Mock private ShowtimeSeatRepository showtimeSeatRepository;
    @Mock private PricingRuleRepository pricingRuleRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private RefundService refundService;
    @Mock private TicketRepository ticketRepository;

    private MovieService movieService;
    private LocationService locationService;
    private HallService hallService;
    private SectionService sectionService;
    private SeatService seatService;
    private ShowtimeService showtimeService;
    private PricingRuleService pricingRuleService;

    @BeforeEach
    void setUp() {
        movieService = new MovieService(movieRepository, showtimeRepository);
        locationService = new LocationService(locationRepository, hallRepository);
        hallService = new HallService(hallRepository, locationRepository, showtimeRepository, sectionRepository);
        sectionService = new SectionService(sectionRepository, hallRepository);
        seatService = new SeatService(seatRepository, sectionRepository);
        showtimeService = new ShowtimeService(
                showtimeRepository,
                bookingRepository,
                paymentRepository,
                refundService,
                ticketRepository,
                showtimeSeatRepository,
                movieRepository,
                hallRepository,
                seatRepository
        );
        pricingRuleService = new PricingRuleService(pricingRuleRepository, showtimeRepository, sectionRepository);
    }

    // MOVIES
    @Test
    void test01_createMovie() {
        MovieCreateRequest req = new MovieCreateRequest("Avatar", "Sci-Fi epic", 162, "English", "Sci-Fi", "poster.jpg", "trailer.mp4", LocalDate.of(2009, 12, 18), "PG-13");
        when(movieRepository.save(any())).thenAnswer(inv -> {
            Movie m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        Movie movie = movieService.createMovie(req);

        assertNotNull(movie.getId());
        assertEquals("Avatar", movie.getTitle());
        assertEquals(162, movie.getDurationMinutes());
    }

    @Test
    void test02_updateMovie() {
        Movie movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Old Title");
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(movieRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MovieUpdateRequest updateReq = new MovieUpdateRequest("New Title", "Desc", 120, "English", "Action", "p.jpg", "t.mp4", LocalDate.now(), "R");
        Movie updated = movieService.updateMovie(1L, updateReq);

        assertEquals("New Title", updated.getTitle());
    }

    @Test
    void test03_nonexistentMovieReturns404() {
        when(movieRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> movieService.getMovieById(999L));
    }

    @Test
    void test04_preventUnsafeDeletionOfReferencedMovie() {
        Movie movie = new Movie();
        movie.setId(1L);
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(showtimeRepository.existsByMovieId(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> movieService.deleteMovie(1L));
    }

    // LOCATIONS
    @Test
    void test05_createLocation() {
        LocationCreateRequest req = new LocationCreateRequest("Cineplex Colombo", "123 Main St", "Colombo");
        when(locationRepository.save(any())).thenAnswer(inv -> {
            Location l = inv.getArgument(0);
            l.setId(1L);
            return l;
        });

        Location loc = locationService.createLocation(req);

        assertEquals("Cineplex Colombo", loc.getName());
        assertEquals("Colombo", loc.getCity());
    }

    @Test
    void test06_updateLocation() {
        Location loc = new Location();
        loc.setId(1L);
        loc.setName("Old Name");
        when(locationRepository.findById(1L)).thenReturn(Optional.of(loc));
        when(locationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Location locUpdated = locationService.updateLocation(1L, new LocationUpdateRequest("New Name", "Addr", "City"));
        assertEquals("New Name", locUpdated.getName());
    }

    @Test
    void test07_nonexistentLocationReturns404() {
        when(locationRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> locationService.getLocationById(999L));
    }

    @Test
    void test31_preventUnsafeDeletionOfLocationWithHalls() {
        Location loc = new Location();
        loc.setId(1L);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(loc));
        when(hallRepository.existsByLocationId(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> locationService.deleteLocation(1L));
    }

    // HALLS
    @Test
    void test08_createHallUnderValidLocation() {
        Location loc = new Location();
        loc.setId(1L);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(loc));
        when(hallRepository.save(any())).thenAnswer(inv -> {
            Hall h = inv.getArgument(0);
            h.setId(10L);
            return h;
        });

        Hall hall = hallService.createHall(1L, new HallCreateRequest("IMAX 1"));
        assertEquals("IMAX 1", hall.getName());
        assertEquals(1L, hall.getLocation().getId());
    }

    @Test
    void test09_invalidLocationReturns404WhenCreatingHall() {
        when(locationRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> hallService.createHall(999L, new HallCreateRequest("Hall 1")));
    }

    @Test
    void test10_updateHall() {
        Hall hall = new Hall();
        hall.setId(10L);
        hall.setName("Old Hall");
        when(hallRepository.findById(10L)).thenReturn(Optional.of(hall));
        when(hallRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Hall updated = hallService.updateHall(10L, new HallUpdateRequest("New Hall"));
        assertEquals("New Hall", updated.getName());
    }

    @Test
    void test32_preventUnsafeDeletionOfHallWithShowtimesOrSections() {
        Hall hall = new Hall();
        hall.setId(10L);
        when(hallRepository.findById(10L)).thenReturn(Optional.of(hall));
        when(showtimeRepository.existsByHallId(10L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> hallService.deleteHall(10L));
    }

    // SECTIONS
    @Test
    void test11_createSectionUnderValidHall() {
        Hall hall = new Hall();
        hall.setId(10L);
        when(hallRepository.findById(10L)).thenReturn(Optional.of(hall));
        when(sectionRepository.save(any())).thenAnswer(inv -> {
            Section s = inv.getArgument(0);
            s.setId(20L);
            return s;
        });

        Section section = sectionService.createSection(10L, new SectionCreateRequest("Ground Floor", SectionType.GROUND));
        assertEquals("Ground Floor", section.getName());
        assertEquals(SectionType.GROUND, section.getType());
    }

    @Test
    void test12_invalidHallReturns404WhenCreatingSection() {
        when(hallRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> sectionService.createSection(999L, new SectionCreateRequest("VIP", SectionType.VIP)));
    }

    // SEATS
    @Test
    void test13_createSeat() {
        Section section = new Section();
        section.setId(20L);
        when(sectionRepository.findById(20L)).thenReturn(Optional.of(section));
        when(seatRepository.existsBySectionIdAndRowLabelAndSeatNumber(20L, "A", "1")).thenReturn(false);
        when(seatRepository.save(any())).thenAnswer(inv -> {
            Seat s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });

        Seat seat = seatService.createSeat(20L, new SeatCreateRequest("A", "1", SeatType.STANDARD, 1, 1, true));
        assertEquals("A", seat.getRowLabel());
        assertEquals("1", seat.getSeatNumber());
        assertTrue(seat.getIsActive());
    }

    @Test
    void test14_invalidSectionReturns404WhenCreatingSeat() {
        when(sectionRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> seatService.createSeat(999L, new SeatCreateRequest("A", "1", SeatType.STANDARD, 1, 1, true)));
    }

    @Test
    void test15_duplicateSeatReturns409() {
        Section section = new Section();
        section.setId(20L);
        when(sectionRepository.findById(20L)).thenReturn(Optional.of(section));
        when(seatRepository.existsBySectionIdAndRowLabelAndSeatNumber(20L, "A", "1")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> seatService.createSeat(20L, new SeatCreateRequest("A", "1", SeatType.STANDARD, 1, 1, true)));
    }

    // SHOWTIMES
    @Test
    void test16_createValidShowtimeAndInitializeSeats() {
        Movie movie = new Movie(); movie.setId(1L);
        Hall hall = new Hall(); hall.setId(10L);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime start = now.plusHours(1);
        OffsetDateTime end = now.plusHours(3);

        Seat activeSeat1 = new Seat(); activeSeat1.setId(101L); activeSeat1.setIsActive(true);
        Seat activeSeat2 = new Seat(); activeSeat2.setId(102L); activeSeat2.setIsActive(true);

        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(hallRepository.findById(10L)).thenReturn(Optional.of(hall));
        when(showtimeRepository.findOverlappingShowtimes(eq(10L), any(), any())).thenReturn(List.of());
        when(showtimeRepository.save(any())).thenAnswer(inv -> {
            Showtime st = inv.getArgument(0);
            st.setId(50L);
            return st;
        });
        when(seatRepository.findBySectionHallIdAndIsActiveTrue(10L)).thenReturn(List.of(activeSeat1, activeSeat2));

        ShowtimeResponse response = showtimeService.createShowtime(new ShowtimeCreateRequest(1L, 10L, start, end));

        assertEquals(50L, response.id());
        assertEquals(ShowtimeStatus.SCHEDULED, response.status());
        verify(showtimeSeatRepository, times(2)).save(any());
    }

    @Test
    void test17_invalidMovieReturns404() {
        when(movieRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> showtimeService.createShowtime(new ShowtimeCreateRequest(999L, 10L, OffsetDateTime.now(), OffsetDateTime.now().plusHours(2))));
    }

    @Test
    void test18_invalidHallReturns404() {
        Movie movie = new Movie(); movie.setId(1L);
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(hallRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> showtimeService.createShowtime(new ShowtimeCreateRequest(1L, 999L, OffsetDateTime.now(), OffsetDateTime.now().plusHours(2))));
    }

    @Test
    void test19_endTimeBeforeOrEqualStartTimeReturns400() {
        Movie movie = new Movie(); movie.setId(1L);
        Hall hall = new Hall(); hall.setId(10L);
        OffsetDateTime now = OffsetDateTime.now();

        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(hallRepository.findById(10L)).thenReturn(Optional.of(hall));

        assertThrows(IllegalArgumentException.class, () -> showtimeService.createShowtime(new ShowtimeCreateRequest(1L, 10L, now.plusHours(2), now.plusHours(1))));
    }

    @Test
    void test20_overlappingShowtimeInSameHallReturns409() {
        Movie movie = new Movie(); movie.setId(1L);
        Hall hall = new Hall(); hall.setId(10L);
        OffsetDateTime now = OffsetDateTime.now();

        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(hallRepository.findById(10L)).thenReturn(Optional.of(hall));
        when(showtimeRepository.findOverlappingShowtimes(eq(10L), any(), any())).thenReturn(List.of(new Showtime()));

        assertThrows(IllegalStateException.class, () -> showtimeService.createShowtime(new ShowtimeCreateRequest(1L, 10L, now.plusHours(1), now.plusHours(3))));
    }

    @Test
    void test21_sameTimeInDifferentHallIsAllowed() {
        Movie movie = new Movie(); movie.setId(1L);
        Hall hall2 = new Hall(); hall2.setId(20L);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime start = now.plusHours(1);
        OffsetDateTime end = now.plusHours(3);

        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(hallRepository.findById(20L)).thenReturn(Optional.of(hall2));
        when(showtimeRepository.findOverlappingShowtimes(eq(20L), any(), any())).thenReturn(List.of());
        when(showtimeRepository.save(any())).thenAnswer(inv -> {
            Showtime st = inv.getArgument(0);
            st.setId(51L);
            return st;
        });

        ShowtimeResponse response = showtimeService.createShowtime(new ShowtimeCreateRequest(1L, 20L, start, end));
        assertEquals(51L, response.id());
    }

    @Test
    void test22_and_23_inactivePhysicalSeatsAreNotInitialized() {
        Movie movie = new Movie(); movie.setId(1L);
        Hall hall = new Hall(); hall.setId(10L);
        OffsetDateTime start = OffsetDateTime.now().plusHours(1);
        OffsetDateTime end = OffsetDateTime.now().plusHours(3);

        Seat activeSeat = new Seat(); activeSeat.setId(101L); activeSeat.setIsActive(true);

        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(hallRepository.findById(10L)).thenReturn(Optional.of(hall));
        when(showtimeRepository.findOverlappingShowtimes(eq(10L), any(), any())).thenReturn(List.of());
        when(showtimeRepository.save(any())).thenAnswer(inv -> {
            Showtime st = inv.getArgument(0);
            st.setId(50L);
            return st;
        });
        when(seatRepository.findBySectionHallIdAndIsActiveTrue(10L)).thenReturn(List.of(activeSeat));

        showtimeService.createShowtime(new ShowtimeCreateRequest(1L, 10L, start, end));

        verify(showtimeSeatRepository, times(1)).save(any());
    }

    // PRICING
    @Test
    void test24_createPricingRule() {
        Hall hall = new Hall(); hall.setId(10L);
        Showtime showtime = new Showtime(); showtime.setId(50L); showtime.setHall(hall);
        Section section = new Section(); section.setId(20L); section.setHall(hall);

        when(showtimeRepository.findById(50L)).thenReturn(Optional.of(showtime));
        when(sectionRepository.findById(20L)).thenReturn(Optional.of(section));
        when(pricingRuleRepository.existsByShowtimeIdAndSectionIdAndTicketType(50L, 20L, TicketType.ADULT)).thenReturn(false);
        when(pricingRuleRepository.save(any())).thenAnswer(inv -> {
            PricingRule pr = inv.getArgument(0);
            pr.setId(300L);
            return pr;
        });

        PricingRuleResponse response = pricingRuleService.createPricingRule(50L, new PricingRuleCreateRequest(20L, TicketType.ADULT, new BigDecimal("1200.00")));
        assertEquals(300L, response.id());
        assertEquals(new BigDecimal("1200.00"), response.price());
    }

    @Test
    void test25_invalidShowtimeReturns404WhenCreatingPricing() {
        when(showtimeRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> pricingRuleService.createPricingRule(999L, new PricingRuleCreateRequest(20L, TicketType.ADULT, new BigDecimal("1000.00"))));
    }

    @Test
    void test26_invalidSectionReturns404WhenCreatingPricing() {
        Showtime showtime = new Showtime(); showtime.setId(50L);
        when(showtimeRepository.findById(50L)).thenReturn(Optional.of(showtime));
        when(sectionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> pricingRuleService.createPricingRule(50L, new PricingRuleCreateRequest(999L, TicketType.ADULT, new BigDecimal("1000.00"))));
    }

    @Test
    void test27_sectionFromDifferentHallReturns400() {
        Hall hall1 = new Hall(); hall1.setId(10L);
        Hall hall2 = new Hall(); hall2.setId(20L);
        Showtime showtime = new Showtime(); showtime.setId(50L); showtime.setHall(hall1);
        Section section = new Section(); section.setId(20L); section.setHall(hall2);

        when(showtimeRepository.findById(50L)).thenReturn(Optional.of(showtime));
        when(sectionRepository.findById(20L)).thenReturn(Optional.of(section));

        assertThrows(IllegalArgumentException.class, () -> pricingRuleService.createPricingRule(50L, new PricingRuleCreateRequest(20L, TicketType.ADULT, new BigDecimal("1000.00"))));
    }

    @Test
    void test28_priceLessOrEqualZeroReturns400() {
        assertThrows(IllegalArgumentException.class, () -> pricingRuleService.createPricingRule(50L, new PricingRuleCreateRequest(20L, TicketType.ADULT, new BigDecimal("0.00"))));
    }

    @Test
    void test29_duplicatePricingRuleReturns409() {
        Hall hall = new Hall(); hall.setId(10L);
        Showtime showtime = new Showtime(); showtime.setId(50L); showtime.setHall(hall);
        Section section = new Section(); section.setId(20L); section.setHall(hall);

        when(showtimeRepository.findById(50L)).thenReturn(Optional.of(showtime));
        when(sectionRepository.findById(20L)).thenReturn(Optional.of(section));
        when(pricingRuleRepository.existsByShowtimeIdAndSectionIdAndTicketType(50L, 20L, TicketType.ADULT)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> pricingRuleService.createPricingRule(50L, new PricingRuleCreateRequest(20L, TicketType.ADULT, new BigDecimal("1200.00"))));
    }

    @Test
    void test30_updatePricingRule() {
        PricingRule rule = new PricingRule();
        rule.setId(300L);
        rule.setPrice(new BigDecimal("1000.00"));

        when(pricingRuleRepository.findById(300L)).thenReturn(Optional.of(rule));
        when(pricingRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PricingRuleResponse response = pricingRuleService.updatePricingRule(300L, new PricingRuleUpdateRequest(new BigDecimal("1500.00")));
        assertEquals(new BigDecimal("1500.00"), response.price());
    }
}
