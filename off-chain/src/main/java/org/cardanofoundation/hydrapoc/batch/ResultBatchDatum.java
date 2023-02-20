package org.cardanofoundation.hydrapoc.batch;

import com.bloxbean.cardano.client.plutus.annotation.Constr;
import com.bloxbean.cardano.client.plutus.annotation.PlutusField;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Constr(alternative = 0)
@Data
public class ResultBatchDatum {
    @PlutusField
    private Map<ChallengeProposalDatum, ResultDatum> results = new HashMap<>();

    public void add(ChallengeProposalDatum challengeProposal, ResultDatum result) {
        results.put(challengeProposal, result);
    }

    public ResultDatum get(ChallengeProposalDatum challengeProposal) {
        return results.get(challengeProposal);
    }
}
