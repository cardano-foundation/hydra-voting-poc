package org.cardanofoundation.hydrapoc.batch.util;

import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.util.Tuple;
import org.cardanofoundation.hydrapoc.batch.data.output.ResultBatchDatum;
import org.cardanofoundation.hydrapoc.batch.data.output.ResultDatum;

import java.util.List;
import java.util.stream.Collectors;

public class CountVoteUtil {

    public static ResultBatchDatum groupResultBatchDatumTuples(List<Tuple<Utxo, ResultBatchDatum>> utxoTuples, long iteration) {
        return groupResultBatchDatum(utxoTuples.stream().map(t -> t._2).collect(Collectors.toList()), iteration);
    }

    public static ResultBatchDatum groupResultBatchDatum(List<ResultBatchDatum> resultBatchData, long iteration) {
        ResultBatchDatum groupResultBatchDatum = ResultBatchDatum.empty(iteration);

        for (ResultBatchDatum resultBatchDatum : resultBatchData) {
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
