package org.cardanofoundation.hydrapoc.importvote;

import com.bloxbean.cardano.client.exception.CborDeserializationException;
import com.bloxbean.cardano.client.plutus.annotation.Constr;
import com.bloxbean.cardano.client.plutus.annotation.PlutusField;
import com.bloxbean.cardano.client.transaction.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.transaction.spec.BytesPlutusData;
import com.bloxbean.cardano.client.transaction.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.transaction.spec.PlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Constr(alternative = 0)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
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

    public static Optional<VoteDatum> deserialize(byte[] datum) {
        try {
            PlutusData plutusData = PlutusData.deserialize(datum);
            if (!(plutusData instanceof ConstrPlutusData))
                return Optional.empty();

            ConstrPlutusData constr = (ConstrPlutusData) plutusData;
            if (constr.getData().getPlutusDataList().size() != 5)
                return Optional.empty();

            List<PlutusData> plutusDataList = constr.getData().getPlutusDataList();
            String voterKey = HexUtil.encodeHexString(((BytesPlutusData) plutusDataList.get(0)).getValue());
            long votingPower = ((BigIntPlutusData) plutusDataList.get(1)).getValue().longValue();
            long challenge = ((BigIntPlutusData) plutusDataList.get(2)).getValue().longValue();
            long proposal = ((BigIntPlutusData) plutusDataList.get(3)).getValue().longValue();
            int choice = ((BigIntPlutusData) plutusDataList.get(4)).getValue().intValue();

            return Optional.of(VoteDatum.builder()
                    .voterKey(voterKey)
                    .votingPower(votingPower)
                    .challenge(challenge)
                    .proposal(proposal)
                    .choice(choice)
                    .build());
        } catch (Exception e) {
            log.error("Error in deserialization (VoteDatum)", e);
            return Optional.empty();
        }
    }
}
