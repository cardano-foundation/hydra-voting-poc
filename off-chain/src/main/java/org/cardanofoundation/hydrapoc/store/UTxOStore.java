package org.cardanofoundation.hydrapoc.store;

import org.cardanofoundation.hydra.core.model.UTXO;

import javax.validation.constraints.NotNull;
import java.util.Map;

public interface UTxOStore {

    @NotNull
    Map<String, UTXO> getLatestUTxO();

    void storeLatestUtxO(Map<String, UTXO> utxo);

}
