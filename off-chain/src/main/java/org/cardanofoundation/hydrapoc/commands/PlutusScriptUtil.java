package org.cardanofoundation.hydrapoc.commands;

import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.transaction.spec.PlutusV2Script;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PlutusScriptUtil {
    @Value("${voting.contract.compileCode}")
    private String voteBatchContractCompileCode;

    public PlutusV2Script getVoteBatcherContract() {
        return PlutusV2Script.builder()
                .cborHex(voteBatchContractCompileCode)
                .build();

    }

    public String getVoteBatcherContractAddress() throws CborSerializationException {
        return AddressProvider.getEntAddress(getVoteBatcherContract(), Networks.testnet()).toBech32();
    }

}
