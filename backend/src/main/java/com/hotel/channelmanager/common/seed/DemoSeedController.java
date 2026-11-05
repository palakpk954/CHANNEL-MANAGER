package com.hotel.channelmanager.common.seed;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/demo")
@Tag(name = "Demo Seed", description = "1-click demo environment generator with sample property, 90 days inventory, channels, and bookings")
public class DemoSeedController {

    private final DemoDataSeederService seederService;

    public DemoSeedController(DemoDataSeederService seederService) {
        this.seederService = seederService;
    }

    @PostMapping("/seed")
    @Operation(summary = "Seed complete demo environment (Grand Palace Resorts, 3 rooms, 2 rates, 90 days, mock channel)")
    public ResponseEntity<Map<String, Object>> seedDemoData() {
        seederService.seedDemoDataIfEmpty();
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Demo environment initialized successfully.",
                "demoProperty", "The Grand Palace Resort & Spa",
                "adminEmail", "admin@grandpalace.com",
                "defaultPassword", "Password123!"
        ));
    }
}

