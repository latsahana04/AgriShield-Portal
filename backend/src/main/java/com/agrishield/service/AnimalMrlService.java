package com.agrishield.service;

import com.agrishield.dto.AnimalScanResponse;
import com.agrishield.entity.Animal;
import com.agrishield.entity.AnimalStatus;
import com.agrishield.exception.ResourceNotFoundException;
import com.agrishield.repository.AnimalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnimalMrlService {

    private final AnimalRepository animalRepository;

    /**
     * Sub-10ms lookup returning dynamic MRL compliance status.
     * Evaluates dynamic status decay rule:
     * Status = (T_current < Safe Release Time) ? LOCKED : SAFE
     */
    @Transactional
    public AnimalScanResponse scanAnimalTag(String tagId) {
        long startTime = System.nanoTime();

        Animal animal = animalRepository.findByTagId(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Animal with ear tag ID '" + tagId + "' not found"));

        ZonedDateTime now = ZonedDateTime.now();
        AnimalStatus currentCalculatedStatus = evaluateDynamicStatus(animal, now);

        // Update database if status decayed from LOCKED to SAFE
        if (animal.getStatus() == AnimalStatus.LOCKED && currentCalculatedStatus == AnimalStatus.SAFE) {
            animal.setStatus(AnimalStatus.SAFE);
            animalRepository.save(animal);
            log.info("Animal {} automatically decayed to SAFE status.", animal.getTagId());
        }

        Long remainingSeconds = 0L;
        if (animal.getSafeReleaseTime() != null && animal.getSafeReleaseTime().isAfter(now)) {
            remainingSeconds = Math.max(0, Duration.between(now, animal.getSafeReleaseTime()).getSeconds());
        }

        long latencyMs = (System.nanoTime() - startTime) / 1_000_000;

        return AnimalScanResponse.builder()
                .tagId(animal.getTagId())
                .species(animal.getSpecies())
                .breed(animal.getBreed())
                .barn(animal.getBarn())
                .pen(animal.getPen())
                .weightKg(animal.getWeightKg())
                .status(currentCalculatedStatus)
                .permitEligible(currentCalculatedStatus == AnimalStatus.SAFE)
                .remainingWithdrawalSeconds(remainingSeconds)
                .safeReleaseTime(animal.getSafeReleaseTime())
                .lastDrugAdministered(animal.getLastDrugId())
                .administeredAt(animal.getAdministeredAt())
                .lookupLatencyMs(latencyMs)
                .build();
    }

    public AnimalStatus evaluateDynamicStatus(Animal animal, ZonedDateTime currentTime) {
        if (animal.getSafeReleaseTime() == null) {
            return AnimalStatus.SAFE;
        }
        return currentTime.isBefore(animal.getSafeReleaseTime()) ? AnimalStatus.LOCKED : AnimalStatus.SAFE;
    }
}
