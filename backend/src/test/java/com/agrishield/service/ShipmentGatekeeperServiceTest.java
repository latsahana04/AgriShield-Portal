package com.agrishield.service;

import com.agrishield.dto.ShipmentPermitRequest;
import com.agrishield.dto.ShipmentPermitResponse;
import com.agrishield.entity.Animal;
import com.agrishield.entity.AnimalStatus;
import com.agrishield.exception.PermitDeniedException;
import com.agrishield.repository.AnimalRepository;
import com.agrishield.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentGatekeeperServiceTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AnimalMrlService animalMrlService;

    @Mock
    private FabricGatewayService fabricGatewayService;

    @InjectMocks
    private ShipmentGatekeeperService shipmentGatekeeperService;

    private Animal safeAnimal1;
    private Animal safeAnimal2;
    private Animal lockedAnimal;

    @BeforeEach
    void setUp() {
        safeAnimal1 = Animal.builder()
                .tagId("COW-311")
                .species("Bovine")
                .breed("Angus")
                .barn("Barn 3")
                .pen("Pen 1")
                .weightKg(new BigDecimal("700.0"))
                .status(AnimalStatus.SAFE)
                .build();

        safeAnimal2 = Animal.builder()
                .tagId("COW-312")
                .species("Bovine")
                .breed("Angus")
                .barn("Barn 3")
                .pen("Pen 2")
                .weightKg(new BigDecimal("680.0"))
                .status(AnimalStatus.SAFE)
                .build();

        lockedAnimal = Animal.builder()
                .tagId("COW-804")
                .species("Bovine")
                .breed("Holstein")
                .barn("Barn 1")
                .pen("Pen 12")
                .weightKg(new BigDecimal("620.0"))
                .status(AnimalStatus.LOCKED)
                .safeReleaseTime(ZonedDateTime.now().plusHours(24))
                .build();
    }

    @Test
    @DisplayName("Deterministic Gatekeeper: Denies permit with HTTP 400 when ANY animal in batch is LOCKED")
    void testPermitDeniedWhenAnyAnimalLocked() {
        // Arrange: requested batch of 3 animals, 1 of which is LOCKED
        List<String> tags = List.of("COW-311", "COW-804", "COW-312");
        when(animalRepository.findByTagIdIn(tags)).thenReturn(List.of(safeAnimal1, lockedAnimal, safeAnimal2));

        when(animalMrlService.evaluateDynamicStatus(eq(safeAnimal1), any())).thenReturn(AnimalStatus.SAFE);
        when(animalMrlService.evaluateDynamicStatus(eq(lockedAnimal), any())).thenReturn(AnimalStatus.LOCKED);
        when(animalMrlService.evaluateDynamicStatus(eq(safeAnimal2), any())).thenReturn(AnimalStatus.SAFE);

        ShipmentPermitRequest request = ShipmentPermitRequest.builder()
                .animalTagIds(tags)
                .destinationFacility("Processing Plant #1")
                .build();

        // Act & Assert
        PermitDeniedException exception = assertThrows(PermitDeniedException.class, () -> {
            shipmentGatekeeperService.evaluateAndIssuePermit(request);
        });

        assertTrue(exception.getMessage().contains("PERMIT DENIED"));
        assertEquals(List.of("COW-804"), exception.getLockedTagIds());

        // Verify violation logged to audit_logs
        verify(auditLogRepository, times(1)).save(any());
        // Verify Fabric permit was NOT broadcast
        verify(fabricGatewayService, never()).submitShipmentPermit(anyString(), anyList(), anyString());
    }

    @Test
    @DisplayName("Deterministic Gatekeeper: Approves permit and submits to Fabric when ALL animals are SAFE")
    void testPermitApprovedWhenAllAnimalsSafe() {
        // Arrange: requested batch of 2 animals, both are SAFE
        List<String> tags = List.of("COW-311", "COW-312");
        when(animalRepository.findByTagIdIn(tags)).thenReturn(List.of(safeAnimal1, safeAnimal2));

        when(animalMrlService.evaluateDynamicStatus(eq(safeAnimal1), any())).thenReturn(AnimalStatus.SAFE);
        when(animalMrlService.evaluateDynamicStatus(eq(safeAnimal2), any())).thenReturn(AnimalStatus.SAFE);

        when(fabricGatewayService.submitShipmentPermit(anyString(), anyList(), anyString()))
                .thenReturn("0x8a92f00b9912c0199d7bca883401fa92");

        ShipmentPermitRequest request = ShipmentPermitRequest.builder()
                .animalTagIds(tags)
                .destinationFacility("Processing Plant #1")
                .build();

        // Act
        ShipmentPermitResponse response = shipmentGatekeeperService.evaluateAndIssuePermit(request);

        // Assert
        assertNotNull(response);
        assertEquals("APPROVED", response.getStatus());
        assertEquals(2, response.getBatchSize());
        assertEquals(List.of("COW-311", "COW-312"), response.getApprovedTagIds());
        assertEquals("0x8a92f00b9912c0199d7bca883401fa92", response.getFabricTxHash());

        verify(fabricGatewayService, times(1)).submitShipmentPermit(anyString(), anyList(), anyString());
    }
}
