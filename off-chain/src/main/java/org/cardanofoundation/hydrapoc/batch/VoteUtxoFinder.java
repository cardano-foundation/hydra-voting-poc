package org.cardanofoundation.hydrapoc.batch;

import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.util.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cardanofoundation.hydrapoc.batch.data.output.CategoryResultsDatum;
import org.cardanofoundation.hydrapoc.importvote.VoteDatum;
import org.cardanofoundation.hydrapoc.util.PlutusScriptUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.client.api.common.OrderEnum.asc;
import static com.bloxbean.cardano.client.util.HexUtil.decodeHexString;
import static org.cardanofoundation.hydrapoc.util.MoreComparators.createTxHashAndTransactionIndexComparator;
import static org.springframework.util.StringUtils.hasLength;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteUtxoFinder {

    private final UtxoSupplier utxoSupplier;

    private final PlutusScriptUtil plutusScriptUtil;

    public Optional<Tuple<Utxo, VoteDatum>> findFirstVote(final long category) {
        boolean isContinue = true;

        int batchSize = 10;
        int page = 0;

        Optional<Tuple<Utxo, VoteDatum>> foundCategoryResults = Optional.empty();
        while (isContinue) {
            List<Utxo> utxoList = utxoSupplier.getPage(getContractAddress(), batchSize, page++, asc);
            if (utxoList.size() == 0) {
                isContinue = false;
                continue;
            }

            foundCategoryResults = utxoList.stream()
                    .filter(utxo -> {
                        // does this UTxO have InlineDatum
                        if (!hasLength(utxo.getInlineDatum())) {
                            return false;
                        }
                        var inlineDatumBytes = decodeHexString(utxo.getInlineDatum());
                        var maybeVoteDatum = VoteDatum.deserialize(inlineDatumBytes);
                        if (maybeVoteDatum.isEmpty()) {
                            return false;
                        }

                        return maybeVoteDatum.orElseThrow().getCategory() == category;
                    })
                    .map(utxo -> {
                        val inlineDatumHex = utxo.getInlineDatum();
                        val inlineDatumBytes = decodeHexString(inlineDatumHex);
                        val voteDatum = VoteDatum.deserialize(inlineDatumBytes).orElseThrow();

                        return new Tuple<>(utxo, voteDatum);
                    })
                    .sorted(createTxHashAndTransactionIndexComparator())
                    .findFirst();

            if (foundCategoryResults.isPresent()) {
                isContinue = false;
            }
        }

        return foundCategoryResults;

    }

    // we may consider some random implementation, e.g. finding first random category results

    public Optional<Tuple<Utxo, CategoryResultsDatum>> findFirstCategoryResults(final long category) {
        boolean isContinue = true;

        int batchSize = 10;
        int page = 0;

        Optional<Tuple<Utxo, CategoryResultsDatum>> foundCategoryResults = Optional.empty();
        while (isContinue) {
            List<Utxo> utxoList = utxoSupplier.getPage(getContractAddress(), batchSize, page++, asc);
            if (utxoList.size() == 0) {
                isContinue = false;
                continue;
            }

            foundCategoryResults = utxoList.stream()
                    .filter(utxo -> {
                        // does this UTxO have InlineDatum
                        if (!hasLength(utxo.getInlineDatum())) {
                            return false;
                        }
                        var inlineDatumBytes = decodeHexString(utxo.getInlineDatum());
                        var maybeCategoryResults = CategoryResultsDatum.deserialize(inlineDatumBytes);
                        if (maybeCategoryResults.isEmpty()) {
                            return false;
                        }

                        return maybeCategoryResults.orElseThrow().getCategory() == category;
                    })
                    .map(utxo -> {
                        val inlineDatumHex = utxo.getInlineDatum();
                        val inlineDatumBytes = decodeHexString(inlineDatumHex);
                        val categoryResultsDatum = CategoryResultsDatum.deserialize(inlineDatumBytes).orElseThrow();

                        return new Tuple<>(utxo, categoryResultsDatum);
                    })
                    .sorted(createTxHashAndTransactionIndexComparator())
                    .findFirst();

            if (foundCategoryResults.isPresent()) {
                isContinue = false;
            }
        }

        return foundCategoryResults;
    }

    private String getContractAddress() {
        String voteBatchContractAddress = null;
        try {
            voteBatchContractAddress = plutusScriptUtil.getContractAddress();
        } catch (CborSerializationException e) {
            throw new RuntimeException(e);
        }
        return voteBatchContractAddress;
    }

}
