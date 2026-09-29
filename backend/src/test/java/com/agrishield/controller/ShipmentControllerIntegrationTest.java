package com.agrishield.controller;

import com.agrishield.dto.ShipmentPermitRequest;
import com.agrishield.exception.PermitDeniedException;
import com.agrishield.service.ShipmentGatekeeperService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ShipmentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShipmentGatekeeperService shipmentGatekeeperService;

    @Test
    @WithMockUser(username = "manager_john", roles = {"MANAGER"})
    @DisplayName("POST /api/v1/shipments/permit returns HTTP 400 when batch contains locked animal")
    void testShipmentPermitReturnsHttp400OnLockedAnimal() throws Exception {
        ShipmentPermitRequest request = ShipmentPermitRequest.builder()
                .animalTagIds(List.of("COW-311", "COW-804"))
                .destinationFacility("Regional Slaughterhouse")
                .build();

        when(shipmentGatekeeperService.evaluateAndIssuePermit(any(ShipmentPermitRequest.class)))
                .thenThrow(new PermitDeniedException(
                        "PERMIT DENIED — 1 animal(s) currently LOCKED under active drug withdrawal hold.",
                        List.of("COW-804")
                ));

        mockMvc.perform(post("/api/v1/shipments/permit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("PERMIT_DENIED_GATEKEEPER_VIOLATION"))
                .andExpect(jsonPath("$.nonCompliantLockedTags[0]").value("COW-804"));
    }
}
