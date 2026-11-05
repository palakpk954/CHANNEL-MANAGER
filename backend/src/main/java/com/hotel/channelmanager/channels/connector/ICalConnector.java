package com.hotel.channelmanager.channels.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.channelmanager.channels.domain.ChannelAccount;
import com.hotel.channelmanager.channels.domain.ChannelType;
import com.hotel.channelmanager.common.security.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Universal RFC 5545 iCal Two-Way Connector.
 * Enables automatic availability synchronization with Airbnb, VRBO, Booking.com iCal,
 * and any external vacation rental calendar.
 */
@Component
public class ICalConnector implements ChannelConnector {

    private static final Logger log = LoggerFactory.getLogger(ICalConnector.class);

    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private static final Pattern DATE_ONLY_PATTERN = Pattern.compile("(\\d{4})(\\d{2})(\\d{2})");
    private static final Pattern DATE_TIME_PATTERN = Pattern.compile("(\\d{4})(\\d{2})(\\d{2})T(\\d{2})(\\d{2})(\\d{2})Z?");

    public ICalConnector(EncryptionService encryptionService, ObjectMapper objectMapper) {
        this.encryptionService = encryptionService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.ICAL;
    }

    @Override
    public ChannelCapabilities getCapabilities() {
        // iCal feeds only exchange stay dates/bookings; no rates or min-stay rules
        return new ChannelCapabilities(false, 1, 60, java.util.Set.of());
    }

    @Override
    public ConnectionResult testConnection(ChannelCredentials credentials) {
        String url = credentials.getIcalUrl();
        if (url == null || url.isBlank()) {
            return ConnectionResult.error("iCal URL is required.");
        }
        String normalized = normalizeUrl(url);
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            return ConnectionResult.error("Invalid iCal URL. Must start with http://, https://, or webcal://");
        }
        return ConnectionResult.ok("iCal calendar URL format is valid: " + normalized);
    }

    @Override
    public MappingOptions fetchMappingOptions(ChannelAccount account) {
        List<MappingOptions.ChannelRoomOption> rooms = List.of(
                new MappingOptions.ChannelRoomOption("ical_calendar", "iCal Universal Calendar Feed")
        );
        List<MappingOptions.ChannelRateOption> rates = List.of(
                new MappingOptions.ChannelRateOption("ical_standard", "Standard iCal Rate", "ical_calendar")
        );
        return new MappingOptions(rooms, rates);
    }

    @Override
    public SyncResult pushAvailability(ChannelAccount account, List<AvailabilityUpdate> updates) {
        // Outbound iCal does not push to external APIs; OTAs pull from our exported .ics URL
        return SyncResult.ok("ical", "Outbound iCal calendar feed dynamically updated.");
    }

    @Override
    public SyncResult pushRates(ChannelAccount account, List<RateUpdate> updates) {
        return SyncResult.ok("ical", "iCal does not support rate updates.");
    }

    @Override
    public SyncResult pushRestrictions(ChannelAccount account, List<RestrictionUpdate> updates) {
        return SyncResult.ok("ical", "iCal does not support restriction updates.");
    }

    @Override
    public List<InboundReservation> pullReservations(ChannelAccount account, Instant since) {
        try {
            ChannelCredentials credentials = extractCredentials(account);
            if (credentials.getIcalUrl() == null || credentials.getIcalUrl().isBlank()) {
                log.warn("Cannot pull reservations for iCal account {}: no URL configured.", account.getId());
                return List.of();
            }

            String url = normalizeUrl(credentials.getIcalUrl());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "HotelChannelManager-iCal/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return parseIcs(response.body());
            } else {
                log.error("Failed to fetch remote iCal feed. HTTP status: {}", response.statusCode());
                return List.of();
            }
        } catch (Exception e) {
            log.error("Error pulling reservations from iCal account {}: {}", account.getId(), e.getMessage());
            return List.of();
        }
    }

    @Override
    public void acknowledge(ChannelAccount account, String channelReservationId) {
        // No acknowledgement required for iCal protocol
    }

    /**
     * Parse RFC 5545 iCalendar content into InboundReservation records.
     */
    public List<InboundReservation> parseIcs(String icsContent) {
        List<InboundReservation> reservations = new ArrayList<>();
        if (icsContent == null || icsContent.isBlank()) {
            return reservations;
        }

        // Unfold multiline headers per RFC 5545 (line beginning with space/tab continues previous line)
        String unfolded = icsContent.replaceAll("\r?\n[ \t]", "");
        String[] lines = unfolded.split("\r?\n");

        boolean inEvent = false;
        String uid = null;
        String summary = null;
        String description = null;
        LocalDate startDate = null;
        LocalDate endDate = null;
        String status = "CONFIRMED";

        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.equalsIgnoreCase("BEGIN:VEVENT")) {
                inEvent = true;
                uid = null;
                summary = null;
                description = null;
                startDate = null;
                endDate = null;
                status = "CONFIRMED";
            } else if (line.equalsIgnoreCase("END:VEVENT")) {
                if (inEvent && startDate != null && endDate != null) {
                    if (uid == null || uid.isBlank()) {
                        uid = "ical-" + Math.abs((startDate.toString() + endDate.toString() + summary).hashCode());
                    }
                    if (summary == null || summary.isBlank()) {
                        summary = "iCal Guest";
                    }

                    reservations.add(new InboundReservation(
                            uid,
                            "ICAL",
                            summary,
                            null,
                            null,
                            startDate,
                            endDate,
                            1,
                            0L,
                            "INR",
                            "ical_calendar",
                            "ical_standard",
                            status,
                            Instant.now()
                    ));
                }
                inEvent = false;
            } else if (inEvent) {
                if (line.startsWith("UID:")) {
                    uid = line.substring(4).trim();
                } else if (line.startsWith("SUMMARY:")) {
                    summary = line.substring(8).trim();
                } else if (line.startsWith("DESCRIPTION:")) {
                    description = line.substring(12).trim();
                } else if (line.startsWith("STATUS:")) {
                    String s = line.substring(7).trim();
                    if ("CANCELLED".equalsIgnoreCase(s)) {
                        status = "CANCELLED";
                    }
                } else if (line.startsWith("DTSTART")) {
                    startDate = parseDate(line);
                } else if (line.startsWith("DTEND")) {
                    endDate = parseDate(line);
                }
            }
        }

        return reservations;
    }

    private LocalDate parseDate(String line) {
        int colonIdx = line.indexOf(':');
        if (colonIdx == -1) return null;
        String val = line.substring(colonIdx + 1).trim();

        Matcher dtMatcher = DATE_TIME_PATTERN.matcher(val);
        if (dtMatcher.find()) {
            int year = Integer.parseInt(dtMatcher.group(1));
            int month = Integer.parseInt(dtMatcher.group(2));
            int day = Integer.parseInt(dtMatcher.group(3));
            return LocalDate.of(year, month, day);
        }

        Matcher dateMatcher = DATE_ONLY_PATTERN.matcher(val);
        if (dateMatcher.find()) {
            int year = Integer.parseInt(dateMatcher.group(1));
            int month = Integer.parseInt(dateMatcher.group(2));
            int day = Integer.parseInt(dateMatcher.group(3));
            return LocalDate.of(year, month, day);
        }

        return null;
    }

    private String normalizeUrl(String url) {
        String trimmed = url.trim();
        if (trimmed.startsWith("webcal://")) {
            return "https://" + trimmed.substring(9);
        }
        return trimmed;
    }

    private ChannelCredentials extractCredentials(ChannelAccount account) {
        try {
            String decrypted = encryptionService.decrypt(account.getEncryptedCredentials());
            return objectMapper.readValue(decrypted, ChannelCredentials.class);
        } catch (Exception e) {
            log.error("Failed to decrypt credentials for account {}", account.getId(), e);
            return new ChannelCredentials();
        }
    }
}

