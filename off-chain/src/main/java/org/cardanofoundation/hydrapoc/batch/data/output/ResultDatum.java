package org.cardanofoundation.hydrapoc.batch.data.output;

import com.bloxbean.cardano.client.plutus.annotation.Constr;
import com.bloxbean.cardano.client.plutus.annotation.PlutusField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@Slf4j
@Constr(alternative = 0)
public class ResultDatum {

    @PlutusField
    private long yes;

    @PlutusField
    private long no;

    @PlutusField
    private long abstain;

    public ResultDatum() {
    }

    /**
     * Add values in resultDatum to this {@link ResultDatum}
     * @param resultDatum
     */
    public void add(@NonNull ResultDatum resultDatum) {
        this.yes = this.yes + resultDatum.getYes();
        this.no = this.no + resultDatum.getNo();
        this.abstain = this.abstain + resultDatum.getAbstain();
    }

}
