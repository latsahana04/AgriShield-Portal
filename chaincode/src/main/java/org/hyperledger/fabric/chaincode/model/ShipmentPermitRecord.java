package org.hyperledger.fabric.chaincode.model;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.List;

@DataType()
public class ShipmentPermitRecord {

    @Property()
    private final String batchId;

    @Property()
    private final List<String> animalTagIds;

    @Property()
    private final String status; // APPROVED or REJECTED

    @Property()
    private final String timestamp;

    @Property()
    private final String endorserMsp;

    public ShipmentPermitRecord(
            @JsonProperty("batchId") String batchId,
            @JsonProperty("animalTagIds") List<String> animalTagIds,
            @JsonProperty("status") String status,
            @JsonProperty("timestamp") String timestamp,
            @JsonProperty("endorserMsp") String endorserMsp) {
        this.batchId = batchId;
        this.animalTagIds = animalTagIds;
        this.status = status;
        this.timestamp = timestamp;
        this.endorserMsp = endorserMsp;
    }

    public String getBatchId() { return batchId; }
    public List<String> getAnimalTagIds() { return animalTagIds; }
    public String getStatus() { return status; }
    public String getTimestamp() { return timestamp; }
    public String getEndorserMsp() { return endorserMsp; }
}
