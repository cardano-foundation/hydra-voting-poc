package org.cardanofoundation.hydrapoc.hydra.util;

import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.TransactionProcessor;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.coinselection.impl.LargestFirstUtxoSelectionStrategy;
import com.bloxbean.cardano.client.function.TxBuilder;
import com.bloxbean.cardano.client.function.TxBuilderContext;
import com.bloxbean.cardano.client.function.TxOutputBuilder;
import com.bloxbean.cardano.client.function.helper.BalanceTxBuilders;
import com.bloxbean.cardano.client.function.helper.InputBuilders;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import com.bloxbean.cardano.client.transaction.spec.TransactionOutput;
import com.bloxbean.cardano.client.transaction.spec.Value;
import com.bloxbean.cardano.client.util.HexUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydrapoc.commands.PlutusScriptUtil;
import org.cardanofoundation.hydrapoc.commands.TransactionUtil;
import org.cardanofoundation.hydrapoc.common.OperatorAccountProvider;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;

@Component
@RequiredArgsConstructor
@Slf4j
public class FuelTransaction {
    private final UtxoSupplier utxoSupplier;
    private final ProtocolParamsSupplier protocolParamsSupplier;
    private final TransactionProcessor transactionProcessor;
    private final OperatorAccountProvider operatorAccountProvider;
    private final TransactionUtil transactionUtil;
    private final PlutusScriptUtil plutusScriptUtil;

    private final String FUEL_DATA_HASH = "a654fb60d21c1fed48db2c320aa6df9737ec0204c0ba53b9b94a09fb40e757f3";

    public String fuel(List<String> nodeOperators) throws Exception {
        return createTransactionWithDatum(nodeOperators);
    }

    private String createTransactionWithDatum(List<String> receivers) throws Exception {
        String sender = operatorAccountProvider.getOperatorAddress();
        log.info("Sender Address: " + sender);

        //Create a empty output builder
        TxOutputBuilder txOutputBuilder = (context, outputs) -> {
        };

        for (String receiver : receivers) {
            txOutputBuilder = txOutputBuilder.and((context, outputs) -> {
                TransactionOutput transactionOutput = TransactionOutput.builder()
                        .address(receiver)
                        .value(Value
                                .builder()
                                .coin(adaToLovelace(100))
                                .build()
                        )
                        .datumHash(HexUtil.decodeHexString(FUEL_DATA_HASH))
                        .build();
                outputs.add(transactionOutput);
            });
        }

        //Create txInputs and balance tx
        TxBuilder txBuilder = txOutputBuilder
                .buildInputs(InputBuilders.createFromSender(sender, sender))
                .andThen(BalanceTxBuilders.balanceTx(sender));

        TxBuilderContext txBuilderContext = TxBuilderContext.init(utxoSupplier, protocolParamsSupplier);
        txBuilderContext.setUtxoSelectionStrategy(new LargestFirstUtxoSelectionStrategy(utxoSupplier));
        Transaction transaction = txBuilderContext
                .buildAndSign(txBuilder, operatorAccountProvider.getTxSigner());

        Result<String> result = transactionProcessor.submitTransaction(transaction.serialize());
        if (!result.isSuccessful())
            throw new RuntimeException("Transaction failed. " + result.getResponse());
        else
            log.info("Import Transaction Id : " + result.getValue());

        transactionUtil.waitForTransaction(result);
        return result.getValue();
    }
}
