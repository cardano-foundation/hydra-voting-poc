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
@Constr(alternative = 0)
public class CreateVoteBatchRedeemer {

    @PlutusField
    private MerkleElement merkleTree;

    public static CreateVoteBatchRedeemer create(MerkleElement merkleTree) {
        try {
            return new CreateVoteBatchRedeemer(merkleTree);
        } catch (Exception e) {
            throw new RuntimeException("Create failed", e);
        }
    }

}
