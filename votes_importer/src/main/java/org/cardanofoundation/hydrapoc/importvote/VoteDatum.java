package org.cardanofoundation.hydrapoc.importvote;

import com.bloxbean.cardano.client.plutus.annotation.Constr;
import com.bloxbean.cardano.client.plutus.annotation.PlutusField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Constr(alternative = 0)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteDatum {
    @PlutusField
    String voterKey;
    @PlutusField
    long votingPower;
    @PlutusField
    long challenge;
    @PlutusField
    long proposal;
    @PlutusField
    int choice;
}
