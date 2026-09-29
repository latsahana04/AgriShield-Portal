package com.agrishield.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "medications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medication {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "drug_id", nullable = false, unique = true, length = 50)
    private String drugId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "withdrawal_hours", nullable = false)
    private Integer withdrawalHours;

    @Column(name = "mrl_limit", nullable = false, length = 100)
    private String mrlLimit;

    @Column(name = "standard_dose_mg_kg", nullable = false, precision = 8, scale = 2)
    private BigDecimal standardDoseMgKg;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
    }
}
