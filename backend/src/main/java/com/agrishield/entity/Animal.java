package com.agrishield.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "animals", indexes = {
    @Index(name = "idx_animals_tag_id", columnList = "tag_id"),
    @Index(name = "idx_animals_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Animal {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "tag_id", nullable = false, unique = true, length = 50)
    private String tagId;

    @Column(nullable = false, length = 50)
    private String species;

    @Column(nullable = false, length = 50)
    private String breed;

    @Column(nullable = false, length = 50)
    private String barn;

    @Column(nullable = false, length = 50)
    private String pen;

    @Column(name = "weight_kg", nullable = false, precision = 8, scale = 2)
    private BigDecimal weightKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnimalStatus status;

    @Column(name = "last_drug_id", length = 50)
    private String lastDrugId;

    @Column(name = "administered_at")
    private ZonedDateTime administeredAt;

    @Column(name = "safe_release_time")
    private ZonedDateTime safeReleaseTime;

    @Column(name = "withdrawal_hours")
    private Integer withdrawalHours;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
        if (status == null) {
            status = AnimalStatus.SAFE;
        }
    }
}
