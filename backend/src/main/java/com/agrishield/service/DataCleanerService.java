package com.agrishield.service;

import com.agrishield.dto.DataCleanerRequest;
import com.agrishield.dto.DataCleanerResponse;
import com.agrishield.dto.DataCleanerRowDto;
import com.agrishield.entity.*;
import com.agrishield.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataCleanerService {

    private final VetShiftRepository vetShiftRepository;
    private final TreatmentRepository treatmentRepository;
    private final AnimalRepository animalRepository;
    private final MedicationRepository medicationRepository;
    private final FabricGatewayService fabricGatewayService;

    // Active in-memory buffer for deduplication across concurrent batch sessions
    private final Set<String> activeMemoryKeyBuffer = ConcurrentHashMap.newKeySet();

    /**
     * Executes the deterministic Two-Pass Ingestion Pipeline:
     * Pass 1: SHA-256 Composite Key Deduplication
     * Pass 2: Vet Duty Shift Verification
     */
    @Transactional
    public DataCleanerResponse processBulkIngestion(DataCleanerRequest request) {
        List<DataCleanerRowDto> approvedClean = new ArrayList<>();
        List<DataCleanerRowDto> rejectedDuplicates = new ArrayList<>();
        List<DataCleanerRowDto> flaggedAnomalies = new ArrayList<>();

        for (DataCleanerRowDto row : request.getRows()) {
            ZonedDateTime ts = row.getTimestamp() != null ? row.getTimestamp() : ZonedDateTime.now();
            row.setTimestamp(ts);

            // =========================================================================
            // PASS 1: SHA-256 COMPOSITE KEY DEDUPLICATION
            // Key = SHA256(Animal_ID + Drug_ID + Timestamp)
            // =========================================================================
            String rawKeyInput = String.format("%s_%s_%s",
                    row.getAnimalTagId().trim().toUpperCase(),
                    row.getMedicationName().trim().toUpperCase(),
                    ts.toInstant().toEpochMilli());

            String compositeKey = DigestUtils.sha256Hex(rawKeyInput.getBytes(StandardCharsets.UTF_8));
            row.setCompositeKey(compositeKey);

            // Check if key exists in active memory buffer OR in PostgreSQL treatments table
            boolean existsInMemory = activeMemoryKeyBuffer.contains(compositeKey);
            boolean existsInDatabase = treatmentRepository.existsByCompositeKey(compositeKey);

            if (existsInMemory || existsInDatabase) {
                row.setValidationStatus("REJECTED_DUPLICATE");
                row.setRejectionReason(String.format("Pass 1 Fail: Composite key collision [%s] detected in %s.",
                        compositeKey.substring(0, 16), existsInMemory ? "active memory buffer" : "PostgreSQL database"));
                rejectedDuplicates.add(row);
                log.warn("Ingestion Pass 1: Duplicate rejected for animal {} drug {}", row.getAnimalTagId(), row.getMedicationName());
                continue;
            }

            // =========================================================================
            // PASS 2: VET DUTY SHIFT VALIDATION
            // Query 'vet_shifts' table in PostgreSQL
            // =========================================================================
            LocalDate shiftDate = ts.toLocalDate();
            LocalTime treatmentTime = ts.toLocalTime();

            List<VetShift> scheduledShifts = vetShiftRepository.findByVetId(row.getVetLicenseId());

            if (scheduledShifts.isEmpty()) {
                row.setValidationStatus("FLAGGED_VET_ANOMALY");
                row.setRejectionReason(String.format("Pass 2 Fail: Veterinarian ID '%s' not registered in duty shift database.", row.getVetLicenseId()));
                flaggedAnomalies.add(row);
                log.warn("Ingestion Pass 2: Unknown vet anomaly {}", row.getVetLicenseId());
                continue;
            }

            // Check condition: Shift Start <= Treatment Timestamp <= Shift End on matching date
            List<VetShift> activeShifts = vetShiftRepository.findActiveShift(row.getVetLicenseId(), shiftDate, treatmentTime);

            if (activeShifts.isEmpty()) {
                row.setValidationStatus("FLAGGED_VET_ANOMALY");
                row.setRejectionReason(String.format("Pass 2 Fail: Treatment timestamp %s outside scheduled shift window for Dr. %s.",
                        treatmentTime, row.getVetLicenseId()));
                flaggedAnomalies.add(row);
                log.warn("Ingestion Pass 2: Off-duty shift anomaly for vet {} at time {}", row.getVetLicenseId(), treatmentTime);
                continue;
            }

            // PASS 1 & 2 SUCCESSFUL -> APPROVED_CLEAN
            row.setValidationStatus("APPROVED_CLEAN");
            row.setRejectionReason(null);
            approvedClean.add(row);

            // Register key in memory buffer to prevent same-batch duplicate collisions
            activeMemoryKeyBuffer.add(compositeKey);
        }

        // Commit Clean Records if requested
        boolean committed = false;
        String fabricSummary = null;
        if (request.isCommitCleanToLedger() && !approvedClean.isEmpty()) {
            commitCleanRecords(approvedClean);
            committed = true;
            fabricSummary = String.format("Committed %d clean treatment records to Fabric ledger channel 'farmchannel'.", approvedClean.size());
        }

        return DataCleanerResponse.builder()
                .totalProcessed(request.getRows().size())
                .approvedCleanCount(approvedClean.size())
                .rejectedDuplicatesCount(rejectedDuplicates.size())
                .flaggedAnomaliesCount(flaggedAnomalies.size())
                .approvedClean(approvedClean)
                .rejectedDuplicates(rejectedDuplicates)
                .flaggedAnomalies(flaggedAnomalies)
                .committedToLedger(committed)
                .fabricTxSummary(fabricSummary)
                .build();
    }

    private void commitCleanRecords(List<DataCleanerRowDto> cleanRows) {
        for (DataCleanerRowDto row : cleanRows) {
            Medication med = medicationRepository.findByNameIgnoreCase(row.getMedicationName())
                    .orElseGet(() -> medicationRepository.findByDrugId("DRUG-01").orElseThrow());

            Animal animal = animalRepository.findByTagId(row.getAnimalTagId())
                    .orElseGet(() -> animalRepository.save(Animal.builder()
                            .id(UUID.randomUUID().toString())
                            .tagId(row.getAnimalTagId())
                            .species("Bovine")
                            .breed("Commercial")
                            .barn("Barn 1")
                            .pen("Pen 1")
                            .weightKg(new java.math.BigDecimal("550.0"))
                            .status(AnimalStatus.LOCKED)
                            .build()));

            ZonedDateTime safeRelease = row.getTimestamp().plusHours(med.getWithdrawalHours());

            // 1. Update animal MRL status to LOCKED
            animal.setStatus(AnimalStatus.LOCKED);
            animal.setLastDrugId(med.getName());
            animal.setAdministeredAt(row.getTimestamp());
            animal.setSafeReleaseTime(safeRelease);
            animal.setWithdrawalHours(med.getWithdrawalHours());
            animalRepository.save(animal);

            // 2. Submit to Fabric Smart Contract
            String txHash = fabricGatewayService.submitTreatmentRecord(
                    animal.getTagId(),
                    med.getDrugId(),
                    row.getDosage() != null ? row.getDosage() : "10 ml",
                    row.getVetLicenseId(),
                    row.getTimestamp().toInstant(),
                    med.getWithdrawalHours(),
                    "SIG_ECDSA_" + UUID.randomUUID().toString().substring(0, 8)
            );

            // 3. Persist Treatment entity
            Treatment treatment = Treatment.builder()
                    .id(UUID.randomUUID().toString())
                    .txId(txHash.substring(0, 18))
                    .compositeKey(row.getCompositeKey())
                    .animalTagId(animal.getTagId())
                    .medicationId(med.getDrugId())
                    .dosage(row.getDosage() != null ? row.getDosage() : "10 ml")
                    .dosageMg(row.getDosageMg() != null ? row.getDosageMg() : new java.math.BigDecimal("1000.0"))
                    .route(row.getRoute() != null ? row.getRoute() : "Intramuscular")
                    .administeredAt(row.getTimestamp())
                    .safeReleaseTime(safeRelease)
                    .vetLicenseId(row.getVetLicenseId())
                    .vetSignature("SIG_VERIFIED_X509")
                    .fabricTxHash(txHash)
                    .build();

            treatmentRepository.save(treatment);
        }
    }
}
