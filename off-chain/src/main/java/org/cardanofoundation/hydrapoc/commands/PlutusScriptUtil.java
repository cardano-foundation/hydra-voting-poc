package org.cardanofoundation.hydrapoc.commands;

import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.ByteString;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.transaction.spec.PlutusV2Script;
import com.bloxbean.cardano.client.util.HexUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PlutusScriptUtil {
    @Value("${voting.contract.compileCode}")
    private String voteBatchContractCompileCode;

    public PlutusV2Script getVoteBatcherContract() {
//        return PlutusV2Script.builder()
//                .cborHex(voteBatchContractCompileCode)
//                .build();

        //Do double encoding for aiken compileCode
        ByteString bs = new ByteString(HexUtil.decodeHexString(voteBatchContractCompileCode));
        try {
            String cborHex = HexUtil.encodeHexString(CborSerializationUtil.serialize(bs));
            return PlutusV2Script.builder()
                    .cborHex(cborHex)
                    .build();
        } catch (CborException e) {
            throw new RuntimeException(e);
        }
    }

    public String getVoteBatcherContractAddress() throws CborSerializationException {
        return AddressProvider.getEntAddress(getVoteBatcherContract(), Networks.testnet()).toBech32();
    }

}
