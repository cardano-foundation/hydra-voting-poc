package org.cardanofoundation.hydrapoc.batch;

import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.common.OrderEnum;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.client.util.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cardanofoundation.hydrapoc.batch.data.output.ResultBatchDatum;
import org.cardanofoundation.hydrapoc.util.PlutusScriptUtil;
import org.cardanofoundation.hydrapoc.importvote.VoteDatum;
import org.cardanofoundation.hydrapoc.util.MoreComparators;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.cardanofoundation.hydrapoc.util.MoreComparators.createTxHashAndTransactionIndexComparator;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteUtxoFinder {

    private final UtxoSupplier utxoSupplier;
    private final PlutusScriptUtil plutusScriptUtil;

    public List<Tuple<Utxo, VoteDatum>> getUtxosWithVotes(int batchSize) {
        boolean isContinue = true;
        List<Tuple<Utxo, VoteDatum>> utxos = new ArrayList<>();
        int page = 0;

        while (isContinue) {
            List<Utxo> utxoList = utxoSupplier.getPage(getContractAddress(), batchSize, page++, OrderEnum.asc);
            if (utxoList.size() == 0) {
                isContinue = false;
                continue;
            }

            List<Tuple<Utxo, VoteDatum>> utxoTuples = utxoList.stream()
                    .filter(utxo -> StringUtils.hasLength(utxo.getInlineDatum()))
                    .map(utxo -> {
                        Optional<VoteDatum> voteDatum = VoteDatum.deserialize(HexUtil.decodeHexString(utxo.getInlineDatum()));
                        return new Tuple<>(utxo, voteDatum.orElse(null));
                    })
                    .filter(utxoOptionalTuple -> utxoOptionalTuple._2 != null)
                    .sorted(createTxHashAndTransactionIndexComparator())
                    .toList();

            utxos.addAll(utxoTuples);

            if (utxos.size() >= batchSize) {
                utxos = utxos.subList(0, batchSize);
                isContinue = false;
            }

        }

        return utxos;
    }

    private String getContractAddress() {
        String voteBatchContractAddress = null;
        try {
            voteBatchContractAddress = plutusScriptUtil.getVoteBatcherContractAddress();
        } catch (CborSerializationException e) {
            throw new RuntimeException(e);
        }
        return voteBatchContractAddress;
    }

    public List<Tuple<Utxo, ResultBatchDatum>> getUtxosWithVoteBatches(int batchSize) {
        var isContinue = true;
        List<Tuple<Utxo, ResultBatchDatum>> utxos = new ArrayList<>();
        int page = 0;
        while (isContinue) {
            List<Utxo> utxoList = utxoSupplier.getPage(getContractAddress(), batchSize, page++, OrderEnum.asc);
            if (utxoList.size() == 0) {
                isContinue = false;
                continue;
            }

            val utxoTuples = utxoList.stream()
                    .filter(utxo -> StringUtils.hasLength(utxo.getInlineDatum()))
                    .map(utxo -> {
                        Optional<ResultBatchDatum> resultBatchDatumOptional = ResultBatchDatum.deserialize(HexUtil.decodeHexString(utxo.getInlineDatum()));

                        return new Tuple<>(utxo, resultBatchDatumOptional.orElse(null));
                    })
                    .filter(utxoOptionalTuple -> utxoOptionalTuple._2 != null)
                    .sorted(createTxHashAndTransactionIndexComparator())
                    .toList();

            utxos.addAll(utxoTuples);

            if (utxos.size() >= batchSize) {
                utxos = utxos.subList(0, batchSize);
                isContinue = false;
            }

        }

        return utxos;
    }

}
