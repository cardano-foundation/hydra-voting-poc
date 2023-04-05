package org.cardanofoundation.hydrapoc.importvote;

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
import com.bloxbean.cardano.client.function.helper.MinAdaCheckers;
import com.bloxbean.cardano.client.plutus.api.PlutusObjectConverter;
import com.bloxbean.cardano.client.plutus.impl.DefaultPlutusObjectConverter;
import com.bloxbean.cardano.client.transaction.spec.PlutusData;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import com.bloxbean.cardano.client.transaction.spec.TransactionOutput;
import com.bloxbean.cardano.client.transaction.spec.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cardanofoundation.hydrapoc.util.PlutusScriptUtil;
import org.cardanofoundation.hydrapoc.util.TransactionUtil;
import org.cardanofoundation.hydrapoc.common.OperatorAccountProvider;
import org.cardanofoundation.hydrapoc.model.Vote;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteImporter {

    private final UtxoSupplier utxoSupplier;
    private final ProtocolParamsSupplier protocolParamsSupplier;
    private final TransactionProcessor transactionProcessor;
    private final OperatorAccountProvider operatorAccountProvider;
    private final TransactionUtil transactionUtil;
    private final PlutusScriptUtil plutusScriptUtil;

    private final static PlutusObjectConverter plutusObjectConverter = new DefaultPlutusObjectConverter();

    @Retryable(include = {RuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100))
    public String importVotes(Collection<Vote> votes) throws Exception {
        return createTransactionWithDatum(votes);
    }

    private String createTransactionWithDatum(Collection<Vote> votes) throws Exception {
        val voteDatumList = votes.stream()
                .map(vote -> VoteDatum.builder()
                        .voterKey(vote.getVoterKey())
                        .votingPower(vote.getVotingPower())
                        .challenge(vote.getChallenge())
                        .proposal(vote.getProposal())
                        .choice(vote.getChoice().toValue())
                        .build()
                ).toList();

        String sender = operatorAccountProvider.getOperatorAddress();
        log.info("Sender Address: " + sender);
        String voteBatchContractAddress = plutusScriptUtil.getVoteBatcherContractAddress();
        log.info("Contract Address: " + voteBatchContractAddress);

        //Create a empty output builder
        TxOutputBuilder txOutputBuilder = (context, outputs) -> {};

        //Iterate through voteDatumLists and create TransactionOutputs
        for (VoteDatum voteDatum : voteDatumList) {
            PlutusData datum = plutusObjectConverter.toPlutusData(voteDatum);
            txOutputBuilder = txOutputBuilder.and((context, outputs) -> {
                TransactionOutput transactionOutput = TransactionOutput.builder()
                        .address(voteBatchContractAddress)
                        .value(Value
                                .builder()
                                .coin(adaToLovelace(BigDecimal.ZERO))
                                .build()
                        )
                        .inlineDatum(datum)
                        .build();

                BigInteger additionalLoveLace = MinAdaCheckers.minAdaChecker().apply(context, transactionOutput);
                transactionOutput.setValue(transactionOutput.getValue().plus(new Value(additionalLoveLace, null)));

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
        if (!result.isSuccessful()) {
            throw new RuntimeException("Transaction failed. " + result.getResponse());
        }

        transactionUtil.waitForTransaction(result);

        return result.getValue();
    }

}
