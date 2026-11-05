package com.hotel.channelmanager.property.web.dto;

import java.util.List;

public record CsvImportResultDto(
        int totalRows,
        int importedRoomsCount,
        int importedRatesCount,
        int failedRowsCount,
        List<RowError> errors,
        String message
) {
    public record RowError(
            int rowNumber,
            String field,
            String errorMessage
    ) {}

    public static CsvImportResultDto success(int total, int rooms, int rates) {
        return new CsvImportResultDto(
                total,
                rooms,
                rates,
                0,
                List.of(),
                "Import completed successfully. " + rooms + " room types and " + rates + " price plans were added."
        );
    }

    public static CsvImportResultDto withErrors(int total, int rooms, int rates, List<RowError> errors) {
        String msg = errors.size() + " rows contain errors. Please review the errors, correct them and upload the file again.";
        return new CsvImportResultDto(total, rooms, rates, errors.size(), errors, msg);
    }
}

