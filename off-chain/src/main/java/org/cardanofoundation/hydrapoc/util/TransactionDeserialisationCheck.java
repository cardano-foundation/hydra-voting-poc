package org.cardanofoundation.hydrapoc.util;

import com.bloxbean.cardano.client.exception.CborDeserializationException;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import com.bloxbean.cardano.client.util.HexUtil;

public class TransactionDeserialisationCheck {

    public static void main(String[] args) throws CborDeserializationException {
        var tx = Transaction.deserialize(HexUtil.decodeHexString("84a60081825820fe15eb46adbb14eefc7f366382006c415d7e9bc03499d3bc9ed528ca5f7d4a57080182a300581d70e37db487fbd58c45d059bcbf5cd6b1604d3bec16cf888f1395a4ebc4011a0011d28a028201d8185841d8799fa1d8799f1a125614ac1a002829e9ffd8799f1a008c6dc80000ff58205e371fd490fc09698042af01219446484dbea5fa9db55e42e1f3d145aec4dc4a00ff82583900bee45311d846fc0a8b6bc6c1d06cc212f577efa60086a9c26783c6eb1708ad5cfcd595d49f19695c86d335a339dc8d4130170e38a2cb0c9639b93102000d81825820b5f6312f937688af95106ba4d4fc0d7b71c19a7d5e1e0ea043c3cd82325ce815011082583900bee45311d846fc0a8b6bc6c1d06cc212f577efa60086a9c26783c6eb1708ad5cfcd595d49f19695c86d335a339dc8d4130170e38a2cb0c961b0000002e8fb22e45111a000f4240a20581840000d8799f58205e371fd490fc09698042af01219446484dbea5fa9db55e42e1f3d145aec4dc4aff8200000681583d583b0100003232323232323222253330064a22930b180080091129998030010a4c26600a6002600e0046660060066010004002ae695cdaab9f5742ae89f5f6"));

        System.out.println(tx);

        System.out.println("Inputs:");
        System.out.println(tx.getBody().getInputs());

        System.out.println("Outputs:");
        System.out.println(tx.getBody().getOutputs());
    }

}
