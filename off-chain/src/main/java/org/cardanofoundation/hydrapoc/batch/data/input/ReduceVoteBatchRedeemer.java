package org.cardanofoundation.hydrapoc.batch.data.input;

import com.bloxbean.cardano.client.plutus.annotation.Constr;
import com.bloxbean.cardano.client.plutus.annotation.PlutusField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Constr(alternative = 1)
public class ReduceVoteBatchRedeemer {

    @PlutusField
    private byte[] merkleTreeRootHash;

    @PlutusField
    private long iteration;

    public static ReduceVoteBatchRedeemer create(byte[] merkleTreeRootHash, long iteration) {
        if (merkleTreeRootHash.length != 32) {
            throw new IllegalArgumentException("Doesn't seem like a valid SHA2-256 hash");
        }
        if (iteration < 0) {
            throw new IllegalArgumentException("iteration cannot be negative");
        }

        try {
            return new ReduceVoteBatchRedeemer(merkleTreeRootHash, iteration);
        } catch (Exception e) {
            throw new RuntimeException("Create failed", e);
        }
    }
}
