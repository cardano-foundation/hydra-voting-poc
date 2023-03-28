package org.cardanofoundation.hydrapoc.batch.data.output;

import com.bloxbean.cardano.client.plutus.annotation.Constr;
import com.bloxbean.cardano.client.plutus.annotation.PlutusField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@Constr(alternative = 0)
@EqualsAndHashCode
public class ChallengeProposalDatum implements Comparable<ChallengeProposalDatum> {

    @PlutusField
    private long challenge;

    @PlutusField
    private long proposal;

    @Override
    public int compareTo(ChallengeProposalDatum o) {
        return Comparator.comparing(ChallengeProposalDatum::getChallenge)
                .thenComparing(ChallengeProposalDatum::getProposal)
                .compare(this, o);
    }

}
