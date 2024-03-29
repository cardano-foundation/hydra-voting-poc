package org.cardanofoundation.hydrapoc.hydra.suppliers;

import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.common.OrderEnum;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.transaction.spec.PlutusData;
import com.bloxbean.cardano.client.transaction.spec.serializers.PlutusDataJsonConverter;
import com.bloxbean.cardano.client.util.Tuple;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cardanofoundation.hydra.core.model.HydraState;
import org.cardanofoundation.hydrapoc.hydra.HydraClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "hydra", name = "ws.url")
@Slf4j
public class HydraUtxoSupplier implements UtxoSupplier {

    private final HydraClient hydraClient;

    @Override
    public List<Utxo> getPage(String address, Integer nrOfItems, Integer page, OrderEnum order) {
        if (hydraClient.getHydraState() != HydraState.Open) {
            return List.of();
        }
        // no paging support in hydra
        if (page >= 1) {
            return List.of();
        }

        try {
            val utxo = hydraClient.getUTxOStore().getLatestUTxO();

            val utxos = utxo.entrySet()
                    .stream().filter(utxoEntry -> utxoEntry.getValue().getAddress().equals(address))
                    .map(utxoEntry -> new Tuple<>(StringUtils.split(utxoEntry.getKey(), "#"), utxoEntry.getValue()))
                    .map(tuple -> Utxo.builder()
                            .txHash(tuple._1[0])
                            .outputIndex(Integer.parseInt(tuple._1[1]))
                            .address(address)
                            .amount(tuple._2.getValue().entrySet()
                                    .stream()
                                    .map(entry -> new Amount(entry.getKey(), entry.getValue()))
                                    .toList())
                            .dataHash(tuple._2.getDatumhash())
                            .inlineDatum(convertInlineDatum(tuple._2.getInlineDatum()))
                            .referenceScriptHash(tuple._2.getReferenceScript())
                            .build())
                    .toList();

            return utxos;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String convertInlineDatum(JsonNode inlineDatum) {
        if (inlineDatum == null || inlineDatum instanceof NullNode)
            return null;

        try {
            PlutusData plutusData = PlutusDataJsonConverter.toPlutusData(inlineDatum);

            return plutusData.serializeToHex();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to convert inlineDatum to PlutusData");
        }
    }
}
