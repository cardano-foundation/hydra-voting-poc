package org.cardanofoundation.hydrapoc.batch.data.output;

import com.bloxbean.cardano.client.plutus.annotation.Constr;
import com.bloxbean.cardano.client.plutus.annotation.PlutusField;
import com.bloxbean.cardano.client.transaction.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.transaction.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.transaction.spec.MapPlutusData;
import com.bloxbean.cardano.client.transaction.spec.PlutusData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Constr(alternative = 0)
@Data
@AllArgsConstructor
@Slf4j
public class CategoryResultsDatum {

    @PlutusField
    private long category;

    @PlutusField
    private Map<Long, Long> results;

//    @PlutusField
//    private byte[] batchHash = new byte[0];

    public static CategoryResultsDatum copyOf(CategoryResultsDatum categoryResultsDatum) {
        return new CategoryResultsDatum(categoryResultsDatum.category, new HashMap<>(categoryResultsDatum.getResults()));
    }

    public static CategoryResultsDatum empty(final long category) {
        return new CategoryResultsDatum(category, new HashMap<>());
    }

    public void add(Long proposal, Long result) {
        results.put(proposal, result);
    }

    public Long get(Long proposal) {
        return results.get(proposal);
    }

//    public void setBatchHash(@Nullable byte[] batchHash) {
//        if (batchHash == null) {
//            this.batchHash = new byte[0];
//        } else {
//            this.batchHash = batchHash;
//        }
//    }

    public static Optional<CategoryResultsDatum> deserialize(byte[] datum) {
        try {
            ConstrPlutusData constr = (ConstrPlutusData) PlutusData.deserialize(datum);
            List<PlutusData> list = constr.getData().getPlutusDataList();
//            if (list.size() == 0) {
//                return Optional.of(ResultBatchDatum.empty());
//            }

            BigIntPlutusData categoryPlutusData = (BigIntPlutusData) list.get(0);
            MapPlutusData resultsPDMap = (MapPlutusData) list.get(1);

            Iterator<Map.Entry<PlutusData, PlutusData>> entries = resultsPDMap.getMap().entrySet().iterator();

            CategoryResultsDatum resultBatchDatum = CategoryResultsDatum.empty(categoryPlutusData.getValue().longValue());
            //resultBatchDatum.setBatchHash(bytesPD.getValue());

            while (entries.hasNext()) {
                Map.Entry<PlutusData, PlutusData> entry = entries.next();

                BigIntPlutusData proposalPlutusData = (BigIntPlutusData) entry.getKey();
                BigIntPlutusData resultsPlutusData = (BigIntPlutusData)entry.getValue();

                resultBatchDatum.add(proposalPlutusData.getValue().longValue(), resultsPlutusData.getValue().longValue());
            }

            return Optional.of(resultBatchDatum);
        } catch (Exception e) {
            log.trace("Error in deserialization (VoteDatum)", e);
            return Optional.empty();
        }
    }
}
