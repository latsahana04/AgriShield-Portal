package org.hyperledger.fabric.chaincode.model;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

@DataType()
public class TreatmentRecord {

    @Property()
    private final String treatmentId;

    @Property()
    private final String animalTagId;

    @Property()
    private final String medicationId;

    @Property()
    private final String vetLicenseId;

    @Property()
    private final String administeredTimestamp;

    @Property()
    private final int withdrawalHours;

    @Property()
    private final String safeReleaseTimestamp;

    @Property()
    private final String signature;

    @Property()
    private final String status; // LOCKED or SAFE

    public TreatmentRecord(
            @JsonProperty("treatmentId") String treatmentId,
            @JsonProperty("animalTagId") String animalTagId,
            @JsonProperty("medicationId") String medicationId,
            @JsonProperty("vetLicenseId") String vetLicenseId,
            @JsonProperty("administeredTimestamp") String administeredTimestamp,
            @JsonProperty("withdrawalHours") int withdrawalHours,
            @JsonProperty("safeReleaseTimestamp") String safeReleaseTimestamp,
            @JsonProperty("signature") String signature,
            @JsonProperty("status") String status) {
        this.treatmentId = treatmentId;
        this.animalTagId = animalTagId;
        this.medicationId = medicationId;
        this.vetLicenseId = vetLicenseId;
        this.administeredTimestamp = administeredTimestamp;
        this.withdrawalHours = withdrawalHours;
        this.safeReleaseTimestamp = safeReleaseTimestamp;
        this.signature = signature;
        this.status = status;
    }

    public String getTreatmentId() { return treatmentId; }
    public String getAnimalTagId() { return animalTagId; }
    public String getMedicationId() { return medicationId; }
    public String getVetLicenseId() { return vetLicenseId; }
    public String getAdministeredTimestamp() { return administeredTimestamp; }
    public int getWithdrawalHours() { return withdrawalHours; }
    public String getSafeReleaseTimestamp() { return safeReleaseTimestamp; }
    public String getSignature() { return signature; }
    public String getStatus() { return status; }
}
