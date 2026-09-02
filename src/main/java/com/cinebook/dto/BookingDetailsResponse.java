package com.cinebook.dto;

import com.cinebook.entity.enums.BookingStatus;
import com.cinebook.entity.enums.PaymentStatus;
import com.cinebook.entity.enums.ShowtimeStatus;
import com.cinebook.entity.enums.TicketStatus;
import com.cinebook.entity.enums.TicketType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record BookingDetailsResponse(
    String bookingReference,
    BookingStatus status,
    String customerName,
    String customerEmail,
    String customerPhone,
    BigDecimal totalAmount,
    OffsetDateTime createdAt,
    MovieSummary movie,
    ShowtimeSummary showtime,
    LocationSummary location,
    HallSummary hall,
    List<BookingDetailSeatResponse> seats,
    List<BookingDetailTicketResponse> tickets,
    PaymentSummary payment,
    List<RefundResponse> refunds
) {
    public record MovieSummary(
        Long id,
        String title,
        Integer durationMinutes,
        String language,
        String genre,
        String posterUrl,
        String ageRating
    ) {}

    public record ShowtimeSummary(
        Long id,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        ShowtimeStatus status
    ) {}

    public record LocationSummary(
        Long id,
        String name,
        String city
    ) {}

    public record HallSummary(
        Long id,
        String name
    ) {}

    public record BookingDetailSeatResponse(
        Long seatId,
        String sectionName,
        String rowLabel,
        String seatNumber,
        TicketType ticketType,
        BigDecimal price
    ) {}

    public record BookingDetailTicketResponse(
        String ticketNumber,
        TicketStatus status,
        OffsetDateTime issuedAt,
        OffsetDateTime usedAt
    ) {}

    public record PaymentSummary(
        Long paymentId,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String provider,
        OffsetDateTime createdAt
    ) {}
}
