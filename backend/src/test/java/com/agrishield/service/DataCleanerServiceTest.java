package com.agrishield.service;

import com.agrishield.dto.DataCleanerRequest;
import com.agrishield.dto.DataCleanerResponse;
import com.agrishield.dto.DataCleanerRowDto;
import com.agrishield.entity.VetShift;
import com.agrishield.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataCleanerServiceTest {

    @Mock
    private VetShiftRepository vetShiftRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private MedicationRepository medicationRepository;

    @Mock
    private FabricGatewayService fabricGatewayService;

    @InjectMocks
    private DataCleanerService dataCleanerService;

    private DataCleanerRowDto validRow;

    @BeforeEach
    void setUp() {
        validRow = DataCleanerRowDto.builder()
                .animalTagId("COW-804")
                .medicationName("Penicillin G Procaine")
                .dosage("10 ml")
                .dosageMg(new BigDecimal("1000.00"))
                .route("Intramuscular")
                .vetLicenseId("VET-USA-4402")
                .timestamp(ZonedDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Pass 1: Correctly deduplicates records with identical SHA-256 composite keys")
    void testCompositeKeyDeduplication() {
        // Arrange: mock that composite key already exists in DB
        when(treatmentRepository.existsByCompositeKey(anyString())).thenReturn(true);

        DataCleanerRequest request = DataCleanerRequest.builder()
                .rows(List.of(validRow))
                .commitCleanToLedger(false)
                .build();

        // Act
        DataCleanerResponse response = dataCleanerService.processBulkIngestion(request);

        // Assert
        assertEquals(1, response.getTotalProcessed());
        assertEquals(0, response.getApprovedCleanCount());
        assertEquals(1, response.getRejectedDuplicatesCount());
        assertEquals("REJECTED_DUPLICATE", response.getRejectedDuplicates().get(0).getValidationStatus());
        assertTrue(response.getRejectedDuplicates().get(0).getRejectionReason().contains("Pass 1 Fail: Composite key collision"));

        verify(vetShiftRepository, never()).findActiveShift(any(), any(), any());
    }

    @Test
    @DisplayName("Pass 2: Flags off-duty treatment attempts when outside scheduled shift hours")
    void testRejectionDuringOffDutyShiftHours() {
        // Arrange: key is unique
        when(treatmentRepository.existsByCompositeKey(anyString())).thenReturn(false);

        // Vet is registered in DB
        VetShift scheduledShift = VetShift.builder()
                .shiftId("SH-101")
                .vetId("VET-USA-4402")
                .vetName("Dr. Sarah Lin")
                .shiftDate(LocalDate.now())
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(17, 0))
                .build();

        when(vetShiftRepository.findByVetId("VET-USA-4402")).thenReturn(List.of(scheduledShift));

        // But treatment timestamp is outside shift window (e.g. 03:00 AM) -> returns empty active shifts
        when(vetShiftRepository.findActiveShift(eq("VET-USA-4402"), any(LocalDate.class), any(LocalTime.class)))
                .thenReturn(Collections.emptyList());

        DataCleanerRequest request = DataCleanerRequest.builder()
                .rows(List.of(validRow))
                .commitCleanToLedger(false)
                .build();

        // Act
        DataCleanerResponse response = dataCleanerService.processBulkIngestion(request);

        // Assert
        assertEquals(1, response.getTotalProcessed());
        assertEquals(0, response.getApprovedCleanCount());
        assertEquals(1, response.getFlaggedAnomaliesCount());
        assertEquals("FLAGGED_VET_ANOMALY", response.getFlaggedAnomalies().get(0).getValidationStatus());
        assertTrue(response.getFlaggedAnomalies().get(0).getRejectionReason().contains("Pass 2 Fail: Treatment timestamp"));
    }
}
