package com.agrishield.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentPermitResponse {
    private String permitId;
    private String status; // APPROVED or DENIED
    private int batchSize;
    private List<String> approvedTagIds;
    private List<String> violatingLockedTagIds;
    private String message;
    private ZonedDateTime issuedAt;
    private String authorizedBy;
    private String fabricTxHash;
    private String digitalClearanceSignature;
}
