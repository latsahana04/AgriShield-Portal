package com.agrishield.service;

import com.agrishield.dto.ShipmentPermitRequest;
import com.agrishield.dto.ShipmentPermitResponse;
import com.agrishield.entity.Animal;
import com.agrishield.entity.AnimalStatus;
import com.agrishield.entity.AuditLog;
import com.agrishield.exception.PermitDeniedException;
import com.agrishield.exception.ResourceNotFoundException;
import com.agrishield.repository.AnimalRepository;
import com.agrishield.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentGatekeeperService {

    private final AnimalRepository animalRepository;
    private final AuditLogRepository auditLogRepository;
    private final AnimalMrlService animalMrlService;
    private final FabricGatewayService fabricGatewayService;

    /**
     * Deterministic Gatekeeper Rule Engine:
     * Status = exists a in {A_1..A_N} (Status(a) == LOCKED) ? DENIED (HTTP 400) : APPROVED (HTTP 200)
     */
    @Transactional
    public ShipmentPermitResponse evaluateAndIssuePermit(ShipmentPermitRequest request) {
        List<String> requestedTags = request.getAnimalTagIds();
        List<Animal> animals = animalRepository.findByTagIdIn(requestedTags);

        if (animals.size() < requestedTags.size()) {
            List<String> foundTags = animals.stream().map(Animal::getTagId).toList();
            List<String> missing = requestedTags.stream().filter(t -> !foundTags.contains(t)).toList();
            throw new ResourceNotFoundException("One or more ear tags not registered in database: " + missing);
        }

        ZonedDateTime now = ZonedDateTime.now();
        List<String> lockedTags = new ArrayList<>();
        List<String> safeTags = new ArrayList<>();

        for (Animal animal : animals) {
            AnimalStatus dynamicStatus = animalMrlService.evaluateDynamicStatus(animal, now);
            if (dynamicStatus == AnimalStatus.LOCKED) {
                lockedTags.add(animal.getTagId());
            } else {
                safeTags.add(animal.getTagId());
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = auth != null ? auth.getName() : "FARM_MANAGER";

        // =========================================================================
        // DETERMINISTIC GATE EVALUATION
        // =========================================================================
        if (!lockedTags.isEmpty()) {
            // Log violation event in audit_logs table
            auditLogRepository.save(AuditLog.builder()
                    .id(UUID.randomUUID().toString())
                    .action("SHIPMENT_PERMIT_BLOCKED")
                    .details(String.format("Gatekeeper denied permit for batch of %d animals. Violating locked tags: %s",
                            requestedTags.size(), String.join(", ", lockedTags)))
                    .actorUsername(actor)
                    .actorRole("MANAGER")
                    .status("DENIED")
                    .timestamp(now)
                    .build());

            log.warn("GATEKEEPER DENIAL: Batch contained {} locked animals under withdrawal: {}", lockedTags.size(), lockedTags);

            throw new PermitDeniedException(
                    String.format("PERMIT DENIED — %d animal(s) currently LOCKED under active drug withdrawal hold. Food chain transport blocked.",
                            lockedTags.size()),
                    lockedTags
            );
        }

        // ALL N ANIMALS SAFE — PERMIT CLEARANCE GRANTED
        String permitId = "PERMIT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Commit Signed Certificate to Hyperledger Fabric Distributed Ledger
        String fabricTxHash = fabricGatewayService.submitShipmentPermit(permitId, safeTags, actor);

        auditLogRepository.save(AuditLog.builder()
                .id(UUID.randomUUID().toString())
                .action("SHIPMENT_PERMIT_APPROVED")
                .details(String.format("Gatekeeper approved permit %s for %d animals. Fabric TX: %s", permitId, safeTags.size(), fabricTxHash))
                .actorUsername(actor)
                .actorRole("MANAGER")
                .status("APPROVED")
                .timestamp(now)
                .build());

        log.info("GATEKEEPER CLEARANCE GRANTED: Batch permit {} issued for {} safe animals.", permitId, safeTags.size());

        return ShipmentPermitResponse.builder()
                .permitId(permitId)
                .status("APPROVED")
                .batchSize(safeTags.size())
                .approvedTagIds(safeTags)
                .violatingLockedTagIds(List.of())
                .message(String.format("ALL %d ANIMALS SAFE — PERMIT CLEARANCE GRANTED.", safeTags.size()))
                .issuedAt(now)
                .authorizedBy(actor)
                .fabricTxHash(fabricTxHash)
                .digitalClearanceSignature("X509_SEAL_" + UUID.randomUUID().toString().substring(0, 16).toUpperCase())
                .build();
    }
}
