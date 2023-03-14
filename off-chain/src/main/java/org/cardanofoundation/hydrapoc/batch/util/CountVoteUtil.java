package org.cardanofoundation.hydrapoc.batch.util;

import org.cardanofoundation.hydrapoc.batch.data.output.ResultBatchDatum;
import org.cardanofoundation.hydrapoc.batch.data.output.ResultDatum;

import java.util.List;

public class CountVoteUtil {

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
