package org.cardanofoundation.hydrapoc.batch.data.input;

import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.client.crypto.KeyGenUtil;
import com.bloxbean.cardano.client.plutus.annotation.Constr;
import com.bloxbean.cardano.client.plutus.annotation.PlutusField;
import com.bloxbean.cardano.client.util.HexUtil;
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
    private String id;

    @PlutusField
    private MerkleElement merkleTree;

    @PlutusField
    private long iteration;

    public static ReduceVoteBatchRedeemer create(MerkleElement merkleTree, long iteration) {
        try {
            byte[] bytes = KeyGenUtil.generateKey().getVkey().getBytes(); // any random bytes
            String id = HexUtil.encodeHexString(Blake2bUtil.blake2bHash224(bytes));
            return new ReduceVoteBatchRedeemer(id, merkleTree, iteration);
        } catch (Exception e) {
            throw new RuntimeException("Create failed", e);
        }
    }
}
