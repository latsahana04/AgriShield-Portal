package com.agrishield.controller;

import com.agrishield.dto.ShipmentPermitRequest;
import com.agrishield.dto.ShipmentPermitResponse;
import com.agrishield.service.ShipmentGatekeeperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentGatekeeperService shipmentGatekeeperService;

    /**
     * Deterministic Shipment Gatekeeper:
     * - Returns HTTP 200 with transport clearance certificate if ALL animals are SAFE.
     * - Returns HTTP 400 with non-compliant ear tag list if ANY animal is LOCKED.
     */
    @PostMapping("/permit")
    public ResponseEntity<ShipmentPermitResponse> requestShipmentPermit(
            @Valid @RequestBody ShipmentPermitRequest request) {
        ShipmentPermitResponse response = shipmentGatekeeperService.evaluateAndIssuePermit(request);
        return ResponseEntity.ok(response);
    }
}
