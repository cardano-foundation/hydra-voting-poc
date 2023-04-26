package org.cardanofoundation.hydrapoc.importvote;

import com.bloxbean.cardano.client.plutus.annotation.Constr;
import com.bloxbean.cardano.client.plutus.annotation.PlutusField;
import com.bloxbean.cardano.client.transaction.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.transaction.spec.BytesPlutusData;
import com.bloxbean.cardano.client.transaction.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.transaction.spec.PlutusData;
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
    byte[] voterKey;

    @PlutusField
    long category;

    @PlutusField
    long proposal;

    public static Optional<VoteDatum> deserialize(byte[] datum) {
        try {
            PlutusData plutusData = PlutusData.deserialize(datum);
            if (!(plutusData instanceof ConstrPlutusData constr))
                return Optional.empty();

            if (constr.getData().getPlutusDataList().size() != 5)
                return Optional.empty();

            List<PlutusData> plutusDataList = constr.getData().getPlutusDataList();
            byte[] voterKey = ((BytesPlutusData) plutusDataList.get(0)).getValue();
            long category = ((BigIntPlutusData) plutusDataList.get(2)).getValue().longValue();
            long proposal = ((BigIntPlutusData) plutusDataList.get(3)).getValue().longValue();

            return Optional.of(VoteDatum.builder()
                    .voterKey(voterKey)
                    .category(category)
                    .proposal(proposal)
                    .build());
        } catch (Exception e) {
            log.trace("Error in deserialization (VoteDatum)", e);
            return Optional.empty();
        }
    }

}
