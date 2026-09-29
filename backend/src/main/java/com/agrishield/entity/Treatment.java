package com.agrishield.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "treatments", indexes = {
    @Index(name = "idx_treatments_composite_key", columnList = "composite_key"),
    @Index(name = "idx_treatments_animal_tag", columnList = "animal_tag_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Treatment {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "tx_id", unique = true, length = 64)
    private String txId;

    @Column(name = "composite_key", nullable = false, unique = true, length = 64)
    private String compositeKey;

    @Column(name = "animal_tag_id", nullable = false, length = 50)
    private String animalTagId;

    @Column(name = "medication_id", nullable = false, length = 50)
    private String medicationId;

    @Column(nullable = false, length = 50)
    private String dosage;

    @Column(name = "dosage_mg", nullable = false, precision = 10, scale = 2)
    private BigDecimal dosageMg;

    @Column(nullable = false, length = 50)
    private String route;

    @Column(name = "administered_at", nullable = false)
    private ZonedDateTime administeredAt;

    @Column(name = "safe_release_time", nullable = false)
    private ZonedDateTime safeReleaseTime;

    @Column(name = "vet_license_id", nullable = false, length = 50)
    private String vetLicenseId;

    @Column(name = "vet_signature", nullable = false, columnDefinition = "TEXT")
    private String vetSignature;

    @Column(name = "fabric_block_number")
    private Long fabricBlockNumber;

    @Column(name = "fabric_tx_hash", length = 128)
    private String fabricTxHash;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
    }
}
