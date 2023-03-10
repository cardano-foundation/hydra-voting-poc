package org.cardanofoundation.hydrapoc.suppliers;

import com.bloxbean.cardano.client.api.model.ProtocolParams;
import org.cardanofoundation.hydrapoc.hydra.suppliers.HydraProtocolParamsSupplier;
import org.junit.jupiter.api.Test;

class HydraProtocolParamsSupplierTest {

    @Test
    void getProtocolParams() {
        HydraProtocolParamsSupplier protocolParamsSupplier = new HydraProtocolParamsSupplier();
        ProtocolParams protocolParams = protocolParamsSupplier.getProtocolParams();
        System.out.println(protocolParams);
    }
}
