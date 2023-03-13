package org.cardanofoundation.hydrapoc.batch.data.input;

import com.bloxbean.cardano.client.plutus.annotation.Constr;
import com.bloxbean.cardano.client.plutus.annotation.PlutusField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cardanofoundation.merkle.core.MerkleElement;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Constr(alternative = 1)
public class ReduceVoteBatchRedeemer {

    @PlutusField
    private MerkleElement merkleTree;

    @PlutusField
    private long iteration;

    public static ReduceVoteBatchRedeemer create(MerkleElement merkleTree, long iteration) {
        try {
            return new ReduceVoteBatchRedeemer(merkleTree, iteration);
        } catch (Exception e) {
            throw new RuntimeException("Create failed", e);
        }
    }
}
