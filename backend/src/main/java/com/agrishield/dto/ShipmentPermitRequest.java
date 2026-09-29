package com.agrishield.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentPermitRequest {
    @NotEmpty(message = "Animal batch tags list cannot be empty")
    private List<String> animalTagIds;

    private String destinationFacility;
    private String carrierName;
}
