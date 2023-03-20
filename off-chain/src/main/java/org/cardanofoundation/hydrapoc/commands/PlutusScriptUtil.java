package org.cardanofoundation.hydrapoc.commands;

import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.ByteString;
import com.bloxbean.cardano.aiken.tx.evaluator.InitialBudgetConfig;
import com.bloxbean.cardano.aiken.tx.evaluator.SlotConfig;
import com.bloxbean.cardano.aiken.tx.evaluator.TxEvaluator;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.api.exception.ApiRuntimeException;
import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.transaction.spec.CostMdls;
import com.bloxbean.cardano.client.transaction.spec.PlutusV2Script;
import com.bloxbean.cardano.client.transaction.spec.Redeemer;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import com.bloxbean.cardano.client.util.HexUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.val;
import org.aspectj.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static com.bloxbean.cardano.client.transaction.spec.Language.PLUTUS_V2;
import static com.bloxbean.cardano.client.transaction.util.CostModelUtil.getCostModelFromProtocolParams;

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

    public static List<Redeemer> evaluateExUnitsDef(Transaction txn,
                                                  Set<Utxo> utxos,
                                                  ProtocolParams protocolParams) {
        val txEvaluator = new TxEvaluator();
        val costMdls = new CostMdls();
        val costModelFromProtocolParams = getCostModelFromProtocolParams(protocolParams, PLUTUS_V2);
        costMdls.add(costModelFromProtocolParams.orElseThrow());

        return txEvaluator.evaluateTx(txn, utxos, costMdls);
    }

    public static List<Redeemer> evaluateExUnits(Transaction txn,
                                                 Set<Utxo> utxos,
                                                 ProtocolParams protocolParams) {
        val txMem = Long.valueOf(protocolParams.getMaxTxExMem());
        val txCpu = Long.valueOf(protocolParams.getMaxTxExSteps());

        val slot_length = 1000;
        val zero_slot = 0;
        val zero_time = 1660003200000L;

        try (SlotConfig slotConfig = new SlotConfig(slot_length, zero_slot, zero_time);
            val initialBudgetConfig = new InitialBudgetConfig(txMem, txCpu)) {
            val txEvaluator = new TxEvaluator(slotConfig, initialBudgetConfig);
            val costMdls = new CostMdls();
            val costModelFromProtocolParams = getCostModelFromProtocolParams(protocolParams, PLUTUS_V2);
            costMdls.add(costModelFromProtocolParams.orElseThrow());

            return txEvaluator.evaluateTx(txn, utxos, costMdls);
        } catch (IOException e) {
            throw new ApiRuntimeException(e);
        }
    }

}
