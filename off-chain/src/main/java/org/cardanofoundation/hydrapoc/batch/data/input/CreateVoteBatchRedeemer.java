package org.cardanofoundation.hydrapoc.batch.data.input;

import com.bloxbean.cardano.client.plutus.annotation.Constr;
import com.bloxbean.cardano.client.plutus.annotation.PlutusField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Constr(alternative = 0)
public class CreateVoteBatchRedeemer {

    @PlutusField
    private byte[] batchHash;

    public static CreateVoteBatchRedeemer create(byte[] batchHash) {
        if (batchHash.length != 32) {
            throw new IllegalArgumentException("Doesn't seem like a valid SHA2-256 hash");
        }

        try {
            return new CreateVoteBatchRedeemer(batchHash);
        } catch (Exception e) {
            throw new RuntimeException("Create failed", e);
        }
    }

}
