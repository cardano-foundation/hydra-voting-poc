package org.cardanofoundation.hydrapoc.common;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.function.TxSigner;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "operator", name = "mnemonic")
public class MnemonicOperatorAccountprovider implements OperatorAccountProvider {
    private Account account;

    public MnemonicOperatorAccountprovider(@Value("${operator.mnemonic}") String operatorMnemonic) {
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
