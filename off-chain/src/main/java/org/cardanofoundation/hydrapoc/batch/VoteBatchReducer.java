package org.cardanofoundation.hydrapoc.batch;

import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.TransactionProcessor;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.coinselection.impl.LargestFirstUtxoSelectionStrategy;
import com.bloxbean.cardano.client.function.Output;
import com.bloxbean.cardano.client.function.TxBuilderContext;
import com.bloxbean.cardano.client.function.helper.CollateralBuilders;
import com.bloxbean.cardano.client.function.helper.InputBuilders;
import com.bloxbean.cardano.client.function.helper.ScriptCallContextProviders;
import com.bloxbean.cardano.client.function.helper.model.ScriptCallContext;
import com.bloxbean.cardano.client.plutus.api.PlutusObjectConverter;
import com.bloxbean.cardano.client.plutus.impl.DefaultPlutusObjectConverter;
import com.bloxbean.cardano.client.transaction.spec.ExUnits;
import com.bloxbean.cardano.client.transaction.spec.RedeemerTag;
import com.bloxbean.cardano.client.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cardanofoundation.hydrapoc.batch.data.input.ReduceVoteBatchRedeemer;
import org.cardanofoundation.hydrapoc.common.BalanceUtil;
import org.cardanofoundation.hydrapoc.common.OperatorAccountProvider;
import org.cardanofoundation.hydrapoc.util.PlutusScriptUtil;
import org.cardanofoundation.hydrapoc.util.TransactionUtil;
import org.cardanofoundation.list.HashedList;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static java.util.Collections.emptySet;
import static org.cardanofoundation.hydrapoc.batch.util.CountVoteUtil.groupResultBatchDatum;
import static org.cardanofoundation.util.Hashing.sha2_256;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteBatchReducer {

    private final UtxoSupplier utxoSupplier;
    private final ProtocolParamsSupplier protocolParamsSupplier;
    private final TransactionProcessor transactionProcessor;
    private final OperatorAccountProvider operatorAccountProvider;
    private final TransactionUtil transactionUtil;
    private final PlutusScriptUtil plutusScriptUtil;
    private final VoteUtxoFinder voteUtxoFinder;

    private PlutusObjectConverter plutusObjectConverter = new DefaultPlutusObjectConverter();

    //TODO -- check collateral return when new utxo added during balanceTx
    @Retryable(include = {RuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100))
    public Optional<String> postReduceBatchTransaction(int batchSize) throws Exception {
        val voteBatcherScript = plutusScriptUtil.getVoteBatcherContract();
        val voteBatcherScriptAddress = plutusScriptUtil.getVoteBatcherContractAddress();
        val sender = operatorAccountProvider.getOperatorAddress();
        log.info("Sender Address: " + sender);
        log.info("Script Address: " + voteBatcherScriptAddress);

        val utxoTuples = voteUtxoFinder.getUtxosWithVoteBatches(batchSize);

        if (utxoTuples.size() == 0) {
            log.warn("No utxo found");
            return Optional.empty();
        }

        val results = utxoTuples.stream().map(t -> t._2).toList();
        val mt = HashedList.create(results, r -> {
            return sha2_256(plutusObjectConverter.toPlutusData(r).serializeToBytes());
        });
        val batchHash = mt.hash();

        // Calculate group result batch datum
        val reduceVoteBatchDatum = groupResultBatchDatum(results);
        reduceVoteBatchDatum.setBatchHash(batchHash);

        log.info("############# Input Vote Batches ############");
        log.info(JsonUtil.getPrettyJson(results));
        log.info("########### Reduced Result Datum #############");
        log.info(JsonUtil.getPrettyJson(reduceVoteBatchDatum));

        // Build and post contract txn
        val utxoSelectionStrategy = new LargestFirstUtxoSelectionStrategy(utxoSupplier);
        val collateralUtxos = utxoSelectionStrategy.select(sender, new Amount(LOVELACE, adaToLovelace(10000)), emptySet());

        // Build the expected output
        val outputDatum = plutusObjectConverter.toPlutusData(reduceVoteBatchDatum);
        val output1 = Output.builder()
                .address(voteBatcherScriptAddress)
                .datum(outputDatum)
                .inlineDatum(true)
                .assetName(LOVELACE)
                .qty(adaToLovelace(1))
                .build();

//        val output2 = Output.builder()
//                .address(sender)
//                .assetName(LOVELACE)
//                .qty(adaToLovelace(2))
//                .build();

        val scriptUtxos = utxoTuples.stream().map(utxoVoteDatumTuple -> utxoVoteDatumTuple._1)
                .toList();

        val extraInputs = utxoSelectionStrategy.select(sender, new Amount(LOVELACE, adaToLovelace(2)), Set.of());

        List<Utxo> allInputs = new ArrayList<>();
        allInputs.addAll(scriptUtxos);
        allInputs.addAll(extraInputs);

        val scriptCallContexts = scriptUtxos.stream().map(utxo -> ScriptCallContext
                .builder()
                .script(voteBatcherScript)
                .utxo(utxo)
                .exUnits(ExUnits.builder()  // Exact exUnits will be calculated later
                        .mem(BigInteger.valueOf(0))
                        .steps(BigInteger.valueOf(0))
                        .build())
                .redeemer(plutusObjectConverter.toPlutusData(ReduceVoteBatchRedeemer.create(batchHash)))
                .redeemerTag(RedeemerTag.Spend).build())
                .toList();

        var txBuilder = output1.outputBuilder()
                .buildInputs(InputBuilders.createFromUtxos(allInputs, sender))
                //.andThen(output2.outputBuilder().buildInputs(InputBuilders.createFromSender(sender, sender)))
                .andThen(CollateralBuilders.collateralOutputs(sender, new ArrayList<>(collateralUtxos))); // CIP-40

        for (val scriptCallContext : scriptCallContexts) {
            txBuilder = txBuilder.andThen(ScriptCallContextProviders.createFromScriptCallContext(scriptCallContext));
        }

        txBuilder = txBuilder.andThen((context, txn) -> {
                    val protocolParams = protocolParamsSupplier.getProtocolParams();
                    val utxos = context.getUtxos();

                    val evalReedemers = PlutusScriptUtil.evaluateExUnits(txn, utxos, protocolParams);

                    val redeemers = txn.getWitnessSet().getRedeemers();
                    for (val redeemer : redeemers) { //Update costs
                        evalReedemers.stream().filter(evalReedemer -> evalReedemer.getIndex().equals(redeemer.getIndex()))
                                .findFirst()
                                .ifPresent(evalRedeemer -> {
                                    redeemer.setExUnits(evalRedeemer.getExUnits());
                                });
                    }

                    // Remove all scripts from witness and just add one
                    txn.getWitnessSet().getPlutusV2Scripts().clear();
                    txn.getWitnessSet().getPlutusV2Scripts().add(voteBatcherScript);
                })
                .andThen(BalanceUtil.balanceTx(sender, 1));

        val txBuilderContext = TxBuilderContext.init(utxoSupplier, protocolParamsSupplier);
        val transaction = txBuilderContext.buildAndSign(txBuilder, operatorAccountProvider.getTxSigner());

        log.info("Fee:{}", transaction.getBody().getFee());

        val result = transactionProcessor.submitTransaction(transaction.serialize());
        if (!result.isSuccessful()) {
            throw new RuntimeException("Transaction failed. " + result.getResponse());
        }

        log.info("Reduce Vote Batch Reducer Transaction Id : " + result.getValue());

        transactionUtil.waitForTransaction(result);

        return Optional.of(result.getValue());
    }

}
