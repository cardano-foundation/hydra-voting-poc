package org.cardanofoundation.hydrapoc.batch.data.output;

import com.bloxbean.cardano.client.plutus.annotation.Constr;
import com.bloxbean.cardano.client.plutus.annotation.PlutusField;
import com.bloxbean.cardano.client.transaction.spec.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.*;

@Constr(alternative = 0)
@Data
@AllArgsConstructor
@Slf4j
public class ResultBatchDatum {

    @PlutusField
    private Map<ChallengeProposalDatum, ResultDatum> results;

    @PlutusField
    private byte[] batchHash = new byte[0];

    public static ResultBatchDatum empty() {
        return new ResultBatchDatum(new HashMap<>(), new byte[0]);
    }

    public void add(ChallengeProposalDatum challengeProposal, ResultDatum result) {
        results.put(challengeProposal, result);
    }

    public ResultDatum get(ChallengeProposalDatum challengeProposal) {
        return results.get(challengeProposal);
    }

    public void setBatchHash(@Nullable byte[] batchHash) {
        if (batchHash == null) {
            this.batchHash = new byte[0];
        } else {
            this.batchHash = batchHash;
        }
    }

    public static Optional<ResultBatchDatum> deserialize(byte[] datum) {
        try {
            ConstrPlutusData constr = (ConstrPlutusData) PlutusData.deserialize(datum);
            List<PlutusData> list = constr.getData().getPlutusDataList();
            if (list.size() == 0) {
                return Optional.of(ResultBatchDatum.empty());
            }

            MapPlutusData resultsPDMap = (MapPlutusData) list.get(0);
            BytesPlutusData bytesPD = (BytesPlutusData) list.get(1);

            Iterator<Map.Entry<PlutusData, PlutusData>> entries = resultsPDMap.getMap().entrySet().iterator();

            ResultBatchDatum resultBatchDatum = ResultBatchDatum.empty();
            resultBatchDatum.setBatchHash(bytesPD.getValue());

            while (entries.hasNext()) {
                Map.Entry<PlutusData, PlutusData> entry = entries.next();
                ConstrPlutusData challengeProposalData = (ConstrPlutusData)entry.getKey();
                ConstrPlutusData resultData = (ConstrPlutusData)entry.getValue();

                //ChallengeProposalDatum
                long challenge = ((BigIntPlutusData)challengeProposalData.getData().getPlutusDataList().get(0)).getValue().longValue();
                long proposal = ((BigIntPlutusData)challengeProposalData.getData().getPlutusDataList().get(1)).getValue().longValue();
                ChallengeProposalDatum challengeProposalDatum = new ChallengeProposalDatum(challenge, proposal);

                //ResultDatum
                long yes = ((BigIntPlutusData)resultData.getData().getPlutusDataList().get(0)).getValue().longValue();
                long no = ((BigIntPlutusData)resultData.getData().getPlutusDataList().get(1)).getValue().longValue();
                long abstain = ((BigIntPlutusData)resultData.getData().getPlutusDataList().get(2)).getValue().longValue();

                ResultDatum resultDatum = new ResultDatum(yes, no, abstain);

                resultBatchDatum.add(challengeProposalDatum, resultDatum);
            }

            return Optional.of(resultBatchDatum);
        } catch (Exception e) {
            log.trace("Error in deserialization (VoteDatum)", e);
            return Optional.empty();
        }
    }
}
