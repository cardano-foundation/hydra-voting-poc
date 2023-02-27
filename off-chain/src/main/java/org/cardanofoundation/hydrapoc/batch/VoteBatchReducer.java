package org.cardanofoundation.hydrapoc.batch;

import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.TransactionProcessor;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.api.TransactionService;
import com.bloxbean.cardano.client.coinselection.UtxoSelectionStrategy;
import com.bloxbean.cardano.client.coinselection.impl.DefaultUtxoSelectionStrategyImpl;
import com.bloxbean.cardano.client.function.Output;
import com.bloxbean.cardano.client.function.TxBuilder;
import com.bloxbean.cardano.client.function.TxBuilderContext;
import com.bloxbean.cardano.client.function.helper.BalanceTxBuilders;
import com.bloxbean.cardano.client.function.helper.CollateralBuilders;
import com.bloxbean.cardano.client.function.helper.InputBuilders;
import com.bloxbean.cardano.client.function.helper.ScriptCallContextProviders;
import com.bloxbean.cardano.client.function.helper.model.ScriptCallContext;
import com.bloxbean.cardano.client.plutus.api.PlutusObjectConverter;
import com.bloxbean.cardano.client.plutus.impl.DefaultPlutusObjectConverter;
import com.bloxbean.cardano.client.transaction.spec.*;
import com.bloxbean.cardano.client.transaction.util.CostModelUtil;
import com.bloxbean.cardano.client.util.JsonUtil;
import com.bloxbean.cardano.client.util.Tuple;
import com.bloxbean.cardano.tx.evaluator.TxEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydrapoc.batch.data.input.ReduceVoteBatchRedeemer;
import org.cardanofoundation.hydrapoc.batch.data.output.ResultBatchDatum;
import org.cardanofoundation.hydrapoc.commands.PlutusScriptUtil;
import org.cardanofoundation.hydrapoc.commands.TransactionUtil;
import org.cardanofoundation.hydrapoc.common.OperatorAccountProvider;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static org.cardanofoundation.hydrapoc.batch.util.CountVoteUtil.groupResultBatchDatum;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteBatchReducer {
    private final UtxoSupplier utxoSupplier;
    private final ProtocolParamsSupplier protocolParamsSupplier;
    private final TransactionProcessor transactionProcessor;
    private final TransactionService transactionService;
    private final OperatorAccountProvider operatorAccountProvider;
    private final TransactionUtil transactionUtil;
    private final PlutusScriptUtil plutusScriptUtil;
    private final VoteUtxoFinder voteUtxoFinder;

    private PlutusObjectConverter plutusObjectConverter = new DefaultPlutusObjectConverter();

    //TODO -- check collateral return when new utxo added during balanceTx
    public String postReduceBatchTransaction(int batchSize) throws Exception {
        PlutusV2Script voteBatcherScript = plutusScriptUtil.getVoteBatcherContract();
        String voteBatcherScriptAddress = plutusScriptUtil.getVoteBatcherContractAddress();
        String sender = operatorAccountProvider.getOperatorAddress();
        log.info("Sender Address: " + sender);
        log.info("Script Address: " + voteBatcherScriptAddress);

        List<Tuple<Utxo, ResultBatchDatum>> utxoTuples = voteUtxoFinder.getUtxosWithVoteBatches(batchSize);
        if (utxoTuples.size() == 0) {
            log.warn("No utxo found");
            return null;
        }

        //Calculate group result batch datum
        ResultBatchDatum reduceVoteBatchDatum = groupResultBatchDatum(utxoTuples);

        log.info("############# Input Vote Batches ############");
        log.info(JsonUtil.getPrettyJson(utxoTuples.stream().map(utxoVoteBatch -> utxoVoteBatch._2).collect(Collectors.toList())));
        log.info("########### Reduced Result Datum #############");
        log.info(JsonUtil.getPrettyJson(reduceVoteBatchDatum));

        // Build and post contract txn
        UtxoSelectionStrategy utxoSelectionStrategy = new DefaultUtxoSelectionStrategyImpl(utxoSupplier);
        Set<Utxo> collateralUtxos =
                utxoSelectionStrategy.select(sender, new Amount(LOVELACE, adaToLovelace(10)), Collections.emptySet());

        // Build the expected output
        PlutusData outputDatum = plutusObjectConverter.toPlutusData(reduceVoteBatchDatum);
        Output output = Output.builder()
                .address(voteBatcherScriptAddress)
                .datum(outputDatum)
                .inlineDatum(true)
                .assetName(LOVELACE)
                .qty(adaToLovelace(1))
                .build();

        List<Utxo> scriptUtxos = utxoTuples.stream().map(utxoVoteDatumTuple -> utxoVoteDatumTuple._1)
                .collect(Collectors.toList());

        List<ScriptCallContext> scriptCallContexts = scriptUtxos.stream().map(utxo -> ScriptCallContext
                .builder()
                .script(voteBatcherScript)
                .utxo(utxo)
                .exUnits(ExUnits.builder()  // Exact exUnits will be calculated later
                        .mem(BigInteger.valueOf(0))
                        .steps(BigInteger.valueOf(0))
                        .build())
                .redeemer(plutusObjectConverter.toPlutusData(ReduceVoteBatchRedeemer.create()))
                .redeemerTag(RedeemerTag.Spend).build()).collect(Collectors.toList());


        TxBuilder txBuilder = output.outputBuilder()
                .buildInputs(InputBuilders.createFromUtxos(scriptUtxos, sender))
                .andThen(CollateralBuilders.collateralOutputs(sender, new ArrayList<>(collateralUtxos))); //CIP-40

        //Loop and add scriptCallContexts
        for (var scriptCallContext : scriptCallContexts) {
            txBuilder = txBuilder.andThen(ScriptCallContextProviders.createFromScriptCallContext(scriptCallContext));
        }

        txBuilder = txBuilder.andThen((context, txn) -> {
                    TxEvaluator txEvaluator = new TxEvaluator(context.getUtxos());
                    CostMdls costMdls = new CostMdls();
                    costMdls.add(CostModelUtil.getCostModelFromProtocolParams(protocolParamsSupplier.getProtocolParams(), Language.PLUTUS_V2).orElseThrow());
                    List<Redeemer> evalReedemers = txEvaluator.evaluateTx(txn, costMdls);

                    List<Redeemer> redeemers = txn.getWitnessSet().getRedeemers();
                    for (Redeemer redeemer : redeemers) { // Update costs
                        evalReedemers.stream().filter(evalReedemer -> evalReedemer.getIndex().equals(redeemer.getIndex()))
                                .findFirst()
                                .ifPresent(evalRedeemer -> redeemer.setExUnits(evalRedeemer.getExUnits()));
                    }

                    // Remove all scripts from witness and just add one
                    txn.getWitnessSet().getPlutusV2Scripts().clear();
                    txn.getWitnessSet().getPlutusV2Scripts().add(plutusScriptUtil.getVoteBatcherContract());
                })
                .andThen(BalanceTxBuilders.balanceTx(sender, 1));

        TxBuilderContext txBuilderContext = TxBuilderContext.init(utxoSupplier, protocolParamsSupplier);
        Transaction transaction = txBuilderContext.buildAndSign(txBuilder, operatorAccountProvider.getTxSigner());

        Result<String> result = transactionProcessor.submitTransaction(transaction.serialize());
        if (!result.isSuccessful()) {
            throw new RuntimeException("Transaction failed. " + result.getResponse());
        }

        log.info("Reduce Vote Batch Reducer Transaction Id : " + result.getValue());

        transactionUtil.waitForTransaction(result);

        return result.getValue();
    }

}
