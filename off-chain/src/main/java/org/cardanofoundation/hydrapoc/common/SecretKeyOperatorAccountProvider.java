package org.cardanofoundation.hydrapoc.common;

import com.bloxbean.cardano.client.crypto.KeyGenUtil;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.function.TxSigner;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import org.cardanofoundation.hydrapoc.hydra.util.AddressUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "operator", name = "secretkey")
public class SecretKeyOperatorAccountProvider implements OperatorAccountProvider{
    private SecretKey secretKey;
    private VerificationKey verificationKey;

    public SecretKeyOperatorAccountProvider(@Value("${operator.secretkey}") String secretKeyCbor) {
        try {
            this.secretKey = new SecretKey(secretKeyCbor);
            this.verificationKey = KeyGenUtil.getPublicKeyFromPrivateKey(secretKey);
        } catch (CborSerializationException e) {
            throw new RuntimeException("Could not get verification key from secret key", e);
        }
    }
    public String getOperatorAddress() {
        try {
            return AddressUtil.getAddressFromVerificationKey(verificationKey.getCborHex());
        } catch (Exception e) {
            throw new RuntimeException("Unable to get address from verification key", e);
        }
       // return "addr_test1vr2jvlvw62kv82x8gn0pewn6n5r82m6zxxn6c7vp04t9avs3wgpxv";
    }

    public TxSigner getTxSigner() {
        //SecretKey secretKey = new SecretKey("5820dd8dc6144991612984503a883d0d0a781ca9ab1c6f5e3f658736b160407fafcc");
        return SignerProviders.signerFrom(secretKey);
    }
}
