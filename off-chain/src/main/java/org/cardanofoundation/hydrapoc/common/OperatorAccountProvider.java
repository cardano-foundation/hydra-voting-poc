package org.cardanofoundation.hydrapoc.common;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.function.TxSigner;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OperatorAccountProvider {
    private Account account;

    public OperatorAccountProvider(@Value("${operator.mnemonic}") String operatorMnemonic) {
        account = new Account(Networks.testnet(), operatorMnemonic);
    }

    public TxSigner getTxSigner() {
        return new TxSigner() {
            @Override
            public Transaction sign(Transaction transaction) {
                return account.sign(transaction);
            }
        };
    }

    public String getOperatorAddress() {
        return account.baseAddress();
    }
}
