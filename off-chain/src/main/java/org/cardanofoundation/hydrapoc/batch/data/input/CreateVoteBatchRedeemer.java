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
    private byte[] merkleTreeRootHash;

    public static CreateVoteBatchRedeemer create(byte[] merkleTreeRootHash) {
        if (merkleTreeRootHash.length != 32) {
            throw new IllegalArgumentException("Doesn't seem like a valid SHA2-256 hash");
        }

        try {
            return new CreateVoteBatchRedeemer(merkleTreeRootHash);
        } catch (Exception e) {
            throw new RuntimeException("Create failed", e);
        }
    }

}
