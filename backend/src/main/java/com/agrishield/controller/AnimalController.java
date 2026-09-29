package com.agrishield.controller;

import com.agrishield.dto.AnimalScanResponse;
import com.agrishield.service.AnimalMrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/animals")
@RequiredArgsConstructor
public class AnimalController {

    private final AnimalMrlService animalMrlService;

    /**
     * Sub-10ms RFID scanner lookup endpoint returning real-time MRL compliance
     */
    @GetMapping("/scan/{tagId}")
    public ResponseEntity<AnimalScanResponse> scanTag(@PathVariable String tagId) {
        AnimalScanResponse response = animalMrlService.scanAnimalTag(tagId);
        return ResponseEntity.ok(response);
    }
}
