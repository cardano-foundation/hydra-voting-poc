package org.cardanofoundation.hydrapoc.batch;

import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.common.OrderEnum;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.client.util.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydrapoc.commands.PlutusScriptUtil;
import org.cardanofoundation.hydrapoc.importvote.VoteDatum;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteUtxoFinder {
    private final UtxoSupplier utxoSupplier;
    private final PlutusScriptUtil plutusScriptUtil;

    public List<Tuple<Utxo, VoteDatum>> getUtxosWithVotes(int batchSize) {
        String voteBatchContractAddress = null;
        try {
            voteBatchContractAddress = plutusScriptUtil.getVoteBatcherContractAddress();
        } catch (CborSerializationException e) {
            log.error("Error", e);
            return Collections.EMPTY_LIST;
        }
        boolean isContinue = true;
        List<Tuple<Utxo, VoteDatum>> utxos = new ArrayList<>();
        while (isContinue) {
            List<Utxo> utxoList = utxoSupplier.getPage(voteBatchContractAddress, batchSize, 0, OrderEnum.asc);
            if (utxoList.size() == 0) {
                isContinue = false;
                continue;
            }

            List<Tuple<Utxo, VoteDatum>> utxoTuples = utxoList.stream()
                    .filter(utxo -> StringUtils.hasLength(utxo.getInlineDatum()))
                    .map(utxo -> {
                        Optional<VoteDatum> voteDatum = VoteDatum.deserialize(HexUtil.decodeHexString(utxo.getInlineDatum()));
                        return new Tuple<>(utxo, voteDatum.get());
                    })
                    .filter(utxoOptionalTuple -> utxoOptionalTuple._2 != null)
                    .collect(Collectors.toList());

            utxos.addAll(utxoTuples);
            if (utxoTuples.size() >= batchSize)
                isContinue = false;
        }

        log.info(utxos.toString());
        return utxos;
    }
}
