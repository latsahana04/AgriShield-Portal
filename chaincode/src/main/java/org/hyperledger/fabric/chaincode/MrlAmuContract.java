package org.hyperledger.fabric.chaincode;

import com.owlike.genson.Genson;
import org.hyperledger.fabric.chaincode.model.ShipmentPermitRecord;
import org.hyperledger.fabric.chaincode.model.TreatmentRecord;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Contract(
        name = "MrlAmuContract",
        info = @Info(
                title = "AgriShield MRL & AMU Compliance Smart Contract",
                description = "Tamper-evident distributed ledger contract for livestock drug residue and clearance permit verification",
                version = "1.0",
                license = @License(name = "Apache 2.0 License")
        )
)
@Default
public class MrlAmuContract implements ContractInterface {

    private final Genson genson = new Genson();

    private enum MrlErrors {
        INVALID_ARGUMENT,
        TREATMENT_ALREADY_EXISTS,
        ANIMAL_LOCKED_HOLD,
        RECORD_NOT_FOUND
    }

    /**
     * 1. Records a clean veterinary treatment on the distributed ledger.
     * Computes safe release timestamp: T_safe = T_admin + (withdrawalHours * 3600s)
     * Saves JSON state to CouchDB.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public TreatmentRecord recordTreatment(
            final Context ctx,
            final String treatmentId,
            final String animalTagId,
            final String medicationId,
            final String vetLicenseId,
            final String timestamp,
            final int withdrawalHours,
            final String signature) {

        ChaincodeStub stub = ctx.getStub();

        // Parameter validations
        if (treatmentId == null || treatmentId.isBlank()) {
            throw new ChaincodeException("Treatment ID must be specified", MrlErrors.INVALID_ARGUMENT.toString());
        }
        if (animalTagId == null || animalTagId.isBlank()) {
            throw new ChaincodeException("Animal tag ID must be specified", MrlErrors.INVALID_ARGUMENT.toString());
        }
        if (withdrawalHours < 0) {
            throw new ChaincodeException("Withdrawal hours cannot be negative", MrlErrors.INVALID_ARGUMENT.toString());
        }

        // Check if treatment already exists
        String compositeKey = stub.createCompositeKey("TREATMENT", treatmentId).toString();
        byte[] existingState = stub.getState(compositeKey);
        if (existingState != null && existingState.length > 0) {
            throw new ChaincodeException("Treatment " + treatmentId + " already recorded on-chain", MrlErrors.TREATMENT_ALREADY_EXISTS.toString());
        }

        // Compute Safe Release Timestamp
        Instant adminTime = Instant.parse(timestamp);
        Instant safeReleaseTime = adminTime.plus(withdrawalHours, ChronoUnit.HOURS);
        String status = Instant.now().isBefore(safeReleaseTime) ? "LOCKED" : "SAFE";

        TreatmentRecord record = new TreatmentRecord(
                treatmentId,
                animalTagId,
                medicationId,
                vetLicenseId,
                timestamp,
                withdrawalHours,
                safeReleaseTime.toString(),
                signature,
                status
        );

        String json = genson.serialize(record);
        stub.putState(compositeKey, json.getBytes(StandardCharsets.UTF_8));

        // Also update latest animal status index
        String animalIndexKey = stub.createCompositeKey("ANIMAL_STATUS", animalTagId).toString();
        stub.putState(animalIndexKey, json.getBytes(StandardCharsets.UTF_8));

        // Emit Event
        stub.setEvent("TreatmentRecordedEvent", json.getBytes(StandardCharsets.UTF_8));

        return record;
    }

    /**
     * 2. Deterministically verifies that all animals in a proposed shipment batch are SAFE.
     * Emits an immutable shipment clearance certificate event if valid.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public ShipmentPermitRecord verifyShipmentPermit(
            final Context ctx,
            final String batchId,
            final List<String> animalTagIds) {

        ChaincodeStub stub = ctx.getStub();
        Instant now = Instant.now();
        List<String> lockedTags = new ArrayList<>();

        for (String tagId : animalTagIds) {
            String animalIndexKey = stub.createCompositeKey("ANIMAL_STATUS", tagId).toString();
            byte[] state = stub.getState(animalIndexKey);

            if (state != null && state.length > 0) {
                TreatmentRecord latestTx = genson.deserialize(new String(state, StandardCharsets.UTF_8), TreatmentRecord.class);
                Instant releaseTime = Instant.parse(latestTx.getSafeReleaseTimestamp());
                if (now.isBefore(releaseTime)) {
                    lockedTags.add(tagId);
                }
            }
        }

        // Zero-tolerance gatekeeper rule
        if (!lockedTags.isEmpty()) {
            throw new ChaincodeException(
                    "Shipment permit rejected: Animals currently LOCKED under withdrawal: " + lockedTags,
                    MrlErrors.ANIMAL_LOCKED_HOLD.toString()
            );
        }

        // Clearance Granted
        String callerMsp = ctx.getClientIdentity().getMSPID();
        ShipmentPermitRecord permit = new ShipmentPermitRecord(
                batchId,
                animalTagIds,
                "APPROVED",
                now.toString(),
                callerMsp
        );

        String permitJson = genson.serialize(permit);
        String permitKey = stub.createCompositeKey("PERMIT", batchId).toString();
        stub.putState(permitKey, permitJson.getBytes(StandardCharsets.UTF_8));

        // Emit Certificate Event
        stub.setEvent("ShipmentPermitClearedEvent", permitJson.getBytes(StandardCharsets.UTF_8));

        return permit;
    }

    /**
     * 3. Queries the complete immutable historical audit trail for a specific livestock ear tag.
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryTreatmentHistory(final Context ctx, final String animalTagId) {
        ChaincodeStub stub = ctx.getStub();
        String animalIndexKey = stub.createCompositeKey("ANIMAL_STATUS", animalTagId).toString();

        QueryResultsIterator<KeyModification> history = stub.getHistoryForKey(animalIndexKey);
        List<String> historyEntries = new ArrayList<>();

        for (KeyModification mod : history) {
            String entry = String.format("{\"txId\":\"%s\",\"timestamp\":\"%s\",\"isDelete\":%b,\"payload\":%s}",
                    mod.getTxId(),
                    mod.getTimestamp().toString(),
                    mod.isDeleted(),
                    new String(mod.getValue(), StandardCharsets.UTF_8));
            historyEntries.add(entry);
        }

        return "[" + String.join(",", historyEntries) + "]";
    }
}
