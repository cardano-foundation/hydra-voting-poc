package org.cardanofoundation.hydrapoc.batch.data.input;

import com.bloxbean.cardano.client.plutus.annotation.Constr;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Constr(alternative = 0)
public class ValidateVoteRedeemer {

//    @PlutusField
//    private byte[] batchHash;

    //public static ValidateVoteRedeemer create(byte[] batchHash) {
    public static ValidateVoteRedeemer create() {
        return new ValidateVoteRedeemer();
//        if (batchHash.length != 32) {
//            throw new IllegalArgumentException("Doesn't seem like a valid SHA2-256 hash");
//        }
//
//        try {
//            return new ValidateVoteRedeemer(batchHash);
//        } catch (Exception e) {
//            throw new RuntimeException("Create failed", e);
//        }
    }

}
