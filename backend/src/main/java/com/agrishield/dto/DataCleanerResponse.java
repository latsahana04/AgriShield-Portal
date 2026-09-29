package com.agrishield.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataCleanerResponse {
    private int totalProcessed;
    private int approvedCleanCount;
    private int rejectedDuplicatesCount;
    private int flaggedAnomaliesCount;
    private List<DataCleanerRowDto> approvedClean;
    private List<DataCleanerRowDto> rejectedDuplicates;
    private List<DataCleanerRowDto> flaggedAnomalies;
    private boolean committedToLedger;
    private String fabricTxSummary;
}
