package com.agrishield.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

@Entity
@Table(name = "vet_shifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VetShift {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "shift_id", nullable = false, unique = true, length = 50)
    private String shiftId;

    @Column(name = "vet_id", nullable = false, length = 50)
    private String vetId;

    @Column(name = "vet_name", nullable = false, length = 100)
    private String vetName;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "barn_assigned", length = 100)
    private String barnAssigned;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
    }
}
