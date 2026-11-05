package com.hotel.channelmanager.property.service;

import com.hotel.channelmanager.property.web.dto.CreateRatePlanRequest;
import com.hotel.channelmanager.property.web.dto.CreateRoomTypeRequest;
import com.hotel.channelmanager.property.web.dto.CsvImportResultDto;
import com.hotel.channelmanager.property.web.dto.RoomTypeDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CsvImportService {

    private final RoomTypeService roomTypeService;
    private final RatePlanService ratePlanService;

    public CsvImportService(RoomTypeService roomTypeService, RatePlanService ratePlanService) {
        this.roomTypeService = roomTypeService;
        this.ratePlanService = ratePlanService;
    }

    @Transactional
    public CsvImportResultDto importCsv(String propertyId, InputStream inputStream, String actorId, String actorEmail) {
        List<CsvImportResultDto.RowError> errors = new ArrayList<>();
        int importedRooms = 0;
        int importedRates = 0;
        int rowNumber = 1;

        Map<String, String> roomCodeToIdMap = new HashMap<>();
        for (RoomTypeDto room : roomTypeService.getRoomTypes(propertyId)) {
            roomCodeToIdMap.put(room.code().toUpperCase(), room.id());
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            boolean headerSkipped = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    rowNumber++;
                    continue;
                }

                if (!headerSkipped) {
                    headerSkipped = true;
                    // Check if it's indeed header
                    if (line.toLowerCase().startsWith("type,") || line.toLowerCase().startsWith("entity,")) {
                        rowNumber++;
                        continue;
                    }
                }

                String[] tokens = line.split(",", -1);
                for (int i = 0; i < tokens.length; i++) {
                    tokens[i] = tokens[i].trim().replace("\"", "");
                }

                if (tokens.length < 3) {
                    errors.add(new CsvImportResultDto.RowError(rowNumber, "line", "Insufficient columns in row."));
                    rowNumber++;
                    continue;
                }

                String entityType = tokens[0].toUpperCase();

                if ("ROOM".equals(entityType)) {
                    // Expect: ROOM,code,name,maxOccupancy,totalUnits,bedType,amenities
                    if (tokens.length < 5) {
                        errors.add(new CsvImportResultDto.RowError(rowNumber, "ROOM", "Rooms require: code, name, maxOccupancy, totalUnits."));
                        rowNumber++;
                        continue;
                    }

                    String code = tokens[1];
                    String name = tokens[2];
                    int maxOccupancy;
                    int totalUnits;

                    try {
                        maxOccupancy = Integer.parseInt(tokens[3]);
                        totalUnits = Integer.parseInt(tokens[4]);
                    } catch (NumberFormatException e) {
                        errors.add(new CsvImportResultDto.RowError(rowNumber, "numbers", "Occupancy and units must be valid numbers."));
                        rowNumber++;
                        continue;
                    }

                    String bedType = tokens.length > 5 ? tokens[5] : "Double";
                    String amenities = tokens.length > 6 ? tokens[6] : "WiFi, AC";

                    try {
                        RoomTypeDto created = roomTypeService.createRoomType(
                                propertyId,
                                new CreateRoomTypeRequest(code, name, maxOccupancy, totalUnits, bedType, amenities),
                                actorId,
                                actorEmail
                        );
                        roomCodeToIdMap.put(created.code().toUpperCase(), created.id());
                        importedRooms++;
                    } catch (Exception e) {
                        errors.add(new CsvImportResultDto.RowError(rowNumber, "code", e.getMessage()));
                    }

                } else if ("RATE".equals(entityType)) {
                    // Expect: RATE,code,name,roomCode,mealPlan,cancellationPolicy
                    if (tokens.length < 6) {
                        errors.add(new CsvImportResultDto.RowError(rowNumber, "RATE", "Rates require: code, name, roomCode, mealPlan, cancellationPolicy."));
                        rowNumber++;
                        continue;
                    }

                    String code = tokens[1];
                    String name = tokens[2];
                    String roomCode = tokens[3].toUpperCase();
                    String mealPlan = tokens[4];
                    String cancellationPolicy = tokens[5];

                    String roomTypeId = roomCodeToIdMap.get(roomCode);
                    if (roomTypeId == null) {
                        errors.add(new CsvImportResultDto.RowError(rowNumber, "roomCode", "Referenced room code '" + roomCode + "' does not exist."));
                        rowNumber++;
                        continue;
                    }

                    try {
                        ratePlanService.createRatePlan(
                                propertyId,
                                new CreateRatePlanRequest(code, name, roomTypeId, mealPlan, cancellationPolicy, null, null, null),
                                actorId,
                                actorEmail
                        );
                        importedRates++;
                    } catch (Exception e) {
                        errors.add(new CsvImportResultDto.RowError(rowNumber, "code", e.getMessage()));
                    }

                } else {
                    errors.add(new CsvImportResultDto.RowError(rowNumber, "type", "Unknown entity type '" + entityType + "'. Must be ROOM or RATE."));
                }

                rowNumber++;
            }
        } catch (Exception e) {
            errors.add(new CsvImportResultDto.RowError(rowNumber, "file", "Failed to parse CSV file: " + e.getMessage()));
        }

        if (errors.isEmpty()) {
            return CsvImportResultDto.success(rowNumber - 1, importedRooms, importedRates);
        } else {
            return CsvImportResultDto.withErrors(rowNumber - 1, importedRooms, importedRates, errors);
        }
    }
}

