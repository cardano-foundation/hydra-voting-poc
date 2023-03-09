package org.cardanofoundation.hydrapoc.common;

import com.bloxbean.cardano.client.function.TxSigner;

public interface OperatorAccountProvider {
    String getOperatorAddress();

    TxSigner getTxSigner();
}
