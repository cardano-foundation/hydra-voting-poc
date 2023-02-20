package org.cardanofoundation.hydrapoc.batch;

import com.bloxbean.cardano.client.plutus.annotation.Constr;
import com.bloxbean.cardano.client.plutus.annotation.PlutusField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
//@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@Constr(alternative = 0)
public class ResultDatum {
    @PlutusField
    @Builder.Default
    private long yes;

    @PlutusField
    @Builder.Default
    private long no;

    @PlutusField
    @Builder.Default
    private long abstain;

    public ResultDatum() {

    }
}
