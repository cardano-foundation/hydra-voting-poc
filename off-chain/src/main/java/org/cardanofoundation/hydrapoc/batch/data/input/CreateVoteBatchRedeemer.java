package org.cardanofoundation.hydrapoc.batch.data.input;

import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.client.crypto.KeyGenUtil;
import com.bloxbean.cardano.client.plutus.annotation.Constr;
import com.bloxbean.cardano.client.plutus.annotation.PlutusField;
import com.bloxbean.cardano.client.util.HexUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Constr(alternative = 0)
public class CreateVoteBatchRedeemer {

    @PlutusField
    private String id;

    public static CreateVoteBatchRedeemer create() {
        try {
            byte[] bytes = KeyGenUtil.generateKey().getVkey().getBytes(); //any random bytes
            String id = HexUtil.encodeHexString(Blake2bUtil.blake2bHash224(bytes));
            return new CreateVoteBatchRedeemer(id);
        } catch (Exception e) {
            throw new RuntimeException("Create failed", e);
        }
    }
}
