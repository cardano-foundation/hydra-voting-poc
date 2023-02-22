package org.cardanofoundation.hydrapoc.batch.util;

import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.util.Tuple;
import org.cardanofoundation.hydrapoc.batch.data.output.ResultBatchDatum;
import org.cardanofoundation.hydrapoc.batch.data.output.ResultDatum;

import java.util.List;

public class CountVoteUtil {

    public static ResultBatchDatum groupResultBatchDatum(List<Tuple<Utxo, ResultBatchDatum>> utxoTuples) {
        ResultBatchDatum groupResultBatchDatum = new ResultBatchDatum();
        for (Tuple<Utxo, ResultBatchDatum> tuple : utxoTuples) {
            ResultBatchDatum resultBatchDatum = tuple._2;

            resultBatchDatum.getResults().forEach((challengeProposalDatum, resultDatum) -> {
                ResultDatum groupResultDatum = groupResultBatchDatum.get(challengeProposalDatum);
                if (groupResultDatum != null) {
                    groupResultDatum.add(resultDatum);
                } else {
                    groupResultBatchDatum.add(challengeProposalDatum, resultDatum);
                }

            });
        }

        return groupResultBatchDatum;
    }
}
