package com.agrishield.dto;

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
public class DataCleanerRowDto {
    private String animalTagId;
    private String medicationName;
    private String dosage;
    private BigDecimal dosageMg;
    private String route;
    private ZonedDateTime timestamp;
    private String vetLicenseId;
    private String compositeKey;
    private String validationStatus; // APPROVED_CLEAN, REJECTED_DUPLICATE, FLAGGED_VET_ANOMALY
    private String rejectionReason;
}
