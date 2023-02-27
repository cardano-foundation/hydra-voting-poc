package org.cardanofoundation.hydrapoc.commands;

import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.ByteString;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.transaction.spec.PlutusV2Script;
import com.bloxbean.cardano.client.util.HexUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.aspectj.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

@Component
public class PlutusScriptUtil {

    private String voteBatchContractCompileCode;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() throws IOException {
        var plutusFileAsString = FileUtil.readAsString(new File("../on-chain/plutus.json"));
        var validatorsNode =  ((ArrayNode)objectMapper.readTree(plutusFileAsString).get("validators"));
        this.voteBatchContractCompileCode = validatorsNode.get(0).get("compiledCode").asText();
    }

    public PlutusV2Script getVoteBatcherContract() {
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
