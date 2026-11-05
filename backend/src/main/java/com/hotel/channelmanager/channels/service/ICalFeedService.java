package com.hotel.channelmanager.channels.service;

import com.hotel.channelmanager.common.exception.ApiException;
import com.hotel.channelmanager.inventory.domain.ManualBlock;
import com.hotel.channelmanager.inventory.domain.ManualBlockRepository;
import com.hotel.channelmanager.property.domain.Property;
import com.hotel.channelmanager.property.domain.PropertyRepository;
import com.hotel.channelmanager.property.domain.RoomType;
import com.hotel.channelmanager.property.domain.RoomTypeRepository;
import com.hotel.channelmanager.reservations.domain.Reservation;
import com.hotel.channelmanager.reservations.domain.ReservationRepository;
import com.hotel.channelmanager.reservations.domain.ReservationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service to dynamically generate RFC 5545 compliant .ics calendar export feeds.
 * Allows remote OTAs (Airbnb, VRBO, etc.) to poll and import property availability.
 */
@Service
public class ICalFeedService {

    private static final DateTimeFormatter ICS_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter ICS_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private final PropertyRepository propertyRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final ReservationRepository reservationRepository;
    private final ManualBlockRepository manualBlockRepository;

    public ICalFeedService(
            PropertyRepository propertyRepository,
            RoomTypeRepository roomTypeRepository,
            ReservationRepository reservationRepository,
            ManualBlockRepository manualBlockRepository
    ) {
        this.propertyRepository = propertyRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.reservationRepository = reservationRepository;
        this.manualBlockRepository = manualBlockRepository;
    }

    /**
     * Generate RFC 5545 calendar feed string for a specific room type.
     */
    @Transactional(readOnly = true)
    public String generateRoomTypeIcsFeed(String propertyId, String roomTypeId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> ApiException.notFound("Property not found."));

        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> ApiException.notFound("Room type not found."));

        LocalDate cutoff = LocalDate.now().minusDays(14); // Include recent and upcoming dates

        List<Reservation> activeReservations = reservationRepository
                .findAllByPropertyIdAndRoomTypeIdAndStatusInAndCheckOutDateGreaterThanEqual(
                        propertyId,
                        roomTypeId,
                        List.of(ReservationStatus.CONFIRMED, ReservationStatus.MODIFIED),
                        cutoff
                );

        List<ManualBlock> activeBlocks = manualBlockRepository
                .findAllByPropertyIdAndRoomTypeIdAndEndDateGreaterThanEqual(propertyId, roomTypeId, cutoff);

        StringBuilder ics = new StringBuilder();
        String dtstamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(java.time.ZoneOffset.UTC).format(Instant.now());

        ics.append("BEGIN:VCALENDAR\r\n");
        ics.append("VERSION:2.0\r\n");
        ics.append("PRODID:-//Hotel Channel Manager//EN\r\n");
        ics.append("CALSCALE:GREGORIAN\r\n");
        ics.append("METHOD:PUBLISH\r\n");
        ics.append("X-WR-CALNAME:").append(sanitizeText(property.getName())).append(" - ").append(sanitizeText(roomType.getName())).append("\r\n");
        ics.append("X-WR-TIMEZONE:UTC\r\n");

        // 1. Confirmed Reservations
        for (Reservation res : activeReservations) {
            ics.append("BEGIN:VEVENT\r\n");
            ics.append("UID:res-").append(res.getId()).append("@hotelchannelmanager.com\r\n");
            ics.append("DTSTAMP:").append(dtstamp).append("\r\n");
            ics.append("DTSTART;VALUE=DATE:").append(res.getCheckInDate().format(ICS_DATE)).append("\r\n");
            // RFC 5545 DTEND for VALUE=DATE is non-inclusive (the checkout day)
            ics.append("DTEND;VALUE=DATE:").append(res.getCheckOutDate().format(ICS_DATE)).append("\r\n");
            ics.append("SUMMARY:Reserved (").append(res.getChannelType()).append(")\r\n");
            ics.append("DESCRIPTION:Booking #").append(res.getChannelReservationId()).append("\r\n");
            ics.append("STATUS:CONFIRMED\r\n");
            ics.append("END:VEVENT\r\n");
        }

        // 2. Manual Room Blocks
        for (ManualBlock block : activeBlocks) {
            ics.append("BEGIN:VEVENT\r\n");
            ics.append("UID:block-").append(block.getId()).append("@hotelchannelmanager.com\r\n");
            ics.append("DTSTAMP:").append(dtstamp).append("\r\n");
            ics.append("DTSTART;VALUE=DATE:").append(block.getStartDate().format(ICS_DATE)).append("\r\n");
            // End date of manual block is inclusive, so non-inclusive DTEND is plus 1 day
            ics.append("DTEND;VALUE=DATE:").append(block.getEndDate().plusDays(1).format(ICS_DATE)).append("\r\n");
            ics.append("SUMMARY:Closed for Sale (").append(sanitizeText(block.getReason())).append(")\r\n");
            ics.append("DESCRIPTION:Manual hold by hotel staff\r\n");
            ics.append("STATUS:CONFIRMED\r\n");
            ics.append("END:VEVENT\r\n");
        }

        ics.append("END:VCALENDAR\r\n");
        return ics.toString();
    }

    private String sanitizeText(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", " ")
                .replace("\r", "");
    }
}

