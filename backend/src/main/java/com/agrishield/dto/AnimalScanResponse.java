package com.agrishield.dto;

import com.agrishield.entity.AnimalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnimalScanResponse {
    private String tagId;
    private String species;
    private String breed;
    private String barn;
    private String pen;
    private BigDecimal weightKg;
    private AnimalStatus status;
    private boolean permitEligible;
    private Long remainingWithdrawalSeconds;
    private ZonedDateTime safeReleaseTime;
    private String lastDrugAdministered;
    private ZonedDateTime administeredAt;
    private String prescribingVet;
    private long lookupLatencyMs;
}
