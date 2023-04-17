package org.cardanofoundation.hydrapoc.hydra.suppliers;

import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "hydra", name = "ws.url")
public class HydraProtocolParamsSupplier implements ProtocolParamsSupplier {

    private final ObjectMapper mapper = new ObjectMapper();

    private final JsonNode protoParamsJson;

    public HydraProtocolParamsSupplier() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("hydra/protocol-parameters.json")) {
            protoParamsJson = mapper.readTree(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ProtocolParams getProtocolParams() {

        ProtocolParams protocolParams = new ProtocolParams();
        protocolParams.setCollateralPercent(new BigDecimal(protoParamsJson.get("collateralPercentage").asText()));
        String utxoCostPerByte = protoParamsJson.get("utxoCostPerByte").asText();
        String utxoCostPerWord = protoParamsJson.get("utxoCostPerWord").asText();

        protocolParams.setCoinsPerUtxoSize(utxoCostPerByte);
        protocolParams.setCoinsPerUtxoWord(utxoCostPerWord);
        protocolParams.setMinFeeA(1);
        protocolParams.setMinFeeB(1);
        protocolParams.setPriceMem(new BigDecimal(protoParamsJson.get("executionUnitPrices").get("priceMemory").asText()));
        protocolParams.setPriceStep(new BigDecimal(protoParamsJson.get("executionUnitPrices").get("priceSteps").asText()));
        protocolParams.setMaxTxExMem(protoParamsJson.get("maxTxExecutionUnits").get("memory").asText());
        protocolParams.setMaxTxExSteps(protoParamsJson.get("maxTxExecutionUnits").get("steps").asText());

        Map<String, Long> costModel1 = costModelFor("PlutusScriptV1");
        Map<String, Long> costModel2 = costModelFor("PlutusScriptV2");
        protocolParams.setCostModels(Map.of("PlutusV1", costModel1, "PlutusV2", costModel2));

        return protocolParams;
    }

    private Map<String, Long> costModelFor(String lang) {
        JsonNode plutusV2CostJson = protoParamsJson.get("costModels").get(lang);
        Iterator<String> opsIter = plutusV2CostJson.fieldNames();
        Map<String, Long> costModel = new HashMap<>();
        while (opsIter.hasNext()) {
            String op = opsIter.next();
            costModel.put(op, plutusV2CostJson.get(op).asLong());
        }

        return costModel;
    }

}
