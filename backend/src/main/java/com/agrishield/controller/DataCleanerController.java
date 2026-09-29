package com.agrishield.controller;

import com.agrishield.dto.DataCleanerRequest;
import com.agrishield.dto.DataCleanerResponse;
import com.agrishield.service.DataCleanerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/data")
@RequiredArgsConstructor
public class DataCleanerController {

    private final DataCleanerService dataCleanerService;

    /**
     * Ingests bulk treatment records, applies SHA-256 composite key deduplication,
     * validates against vet_shifts duty rosters, and commits clean records to Fabric.
     */
    @PostMapping("/clean")
    public ResponseEntity<DataCleanerResponse> cleanBulkData(@RequestBody DataCleanerRequest request) {
        DataCleanerResponse response = dataCleanerService.processBulkIngestion(request);
        return ResponseEntity.ok(response);
    }
}
