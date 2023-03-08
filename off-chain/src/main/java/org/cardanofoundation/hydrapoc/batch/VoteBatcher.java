package org.cardanofoundation.hydrapoc.batch;

import com.bloxbean.cardano.aiken.tx.evaluator.TxEvaluator;
import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.TransactionProcessor;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.coinselection.impl.DefaultUtxoSelectionStrategyImpl;
import com.bloxbean.cardano.client.function.Output;
import com.bloxbean.cardano.client.function.TxBuilderContext;
import com.bloxbean.cardano.client.function.helper.BalanceTxBuilders;
import com.bloxbean.cardano.client.function.helper.CollateralBuilders;
import com.bloxbean.cardano.client.function.helper.InputBuilders;
import com.bloxbean.cardano.client.function.helper.ScriptCallContextProviders;
import com.bloxbean.cardano.client.function.helper.model.ScriptCallContext;
import com.bloxbean.cardano.client.plutus.api.PlutusObjectConverter;
import com.bloxbean.cardano.client.plutus.impl.DefaultPlutusObjectConverter;
import com.bloxbean.cardano.client.transaction.spec.CostMdls;
import com.bloxbean.cardano.client.transaction.spec.ExUnits;
import com.bloxbean.cardano.client.transaction.spec.Language;
import com.bloxbean.cardano.client.transaction.spec.RedeemerTag;
import com.bloxbean.cardano.client.transaction.util.CostModelUtil;
import com.bloxbean.cardano.client.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cardanofoundation.hydrapoc.batch.data.input.CreateVoteBatchRedeemer;
import org.cardanofoundation.hydrapoc.batch.data.output.ChallengeProposalDatum;
import org.cardanofoundation.hydrapoc.batch.data.output.ResultBatchDatum;
import org.cardanofoundation.hydrapoc.batch.data.output.ResultDatum;
import org.cardanofoundation.hydrapoc.commands.PlutusScriptUtil;
import org.cardanofoundation.hydrapoc.commands.TransactionUtil;
import org.cardanofoundation.hydrapoc.common.OperatorAccountProvider;
import org.cardanofoundation.merkle.core.MerkleTree;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static org.cardanofoundation.merkle.util.Hashing.sha2_256;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteBatcher {

    private final UtxoSupplier utxoSupplier;
    private final ProtocolParamsSupplier protocolParamsSupplier;
    private final TransactionProcessor transactionProcessor;
    private final OperatorAccountProvider operatorAccountProvider;
    private final TransactionUtil transactionUtil;
    private final PlutusScriptUtil plutusScriptUtil;
    private final VoteUtxoFinder voteUtxoFinder;

    private PlutusObjectConverter plutusObjectConverter = new DefaultPlutusObjectConverter();

    //TODO -- check collateral return when new utxo added during balanceTx
    public Optional<String> createAndPostBatchTransaction(int batchSize) throws Exception {
        val voteBatcherScript = plutusScriptUtil.getVoteBatcherContract();
        val voteBatcherScriptAddress = plutusScriptUtil.getVoteBatcherContractAddress();
        val sender = operatorAccountProvider.getOperatorAddress();

        log.info("Sender Address: " + sender);
        log.info("Script Address: " + voteBatcherScriptAddress);

        val utxoTuples = voteUtxoFinder.getUtxosWithVotes(batchSize);
        if (utxoTuples.size() == 0) {
            log.warn("No utxo found");
            return Optional.empty();
        }

        val resultBatchDatum = ResultBatchDatum.empty(0);
        val voteDatums = new ArrayList<>();

        for (val tuple : utxoTuples) {
            val voteDatum = tuple._2;
            voteDatums.add(voteDatum);

            val challengeProposalDatum = new ChallengeProposalDatum(voteDatum.getChallenge(), voteDatum.getProposal());

            var resultDatum = resultBatchDatum.get(challengeProposalDatum);
            if (resultDatum == null) {
                resultDatum = new ResultDatum();
                resultBatchDatum.add(challengeProposalDatum, resultDatum);
            }

            switch (voteDatum.getChoice()) {
                case 0:
                    resultDatum.setAbstain(resultDatum.getAbstain() + voteDatum.getVotingPower());
                    break;
                case 1:
                    resultDatum.setNo(resultDatum.getNo() + voteDatum.getVotingPower());
                    break;
                case 2:
                    resultDatum.setYes(resultDatum.getYes() + voteDatum.getVotingPower());
                    break;
                default:
                    log.warn("Invalid vote, " + voteDatum.getChoice());
            }
        }
        val mt = MerkleTree.createFromItems(voteDatums, vote -> sha2_256(plutusObjectConverter.toPlutusData(vote).serializeToBytes()));
        resultBatchDatum.setMerkleRootHash(mt.elementHash());

        log.info("############# Input Votes ############");
        log.info(JsonUtil.getPrettyJson(utxoTuples.stream().map(utxoVoteDatumTuple -> utxoVoteDatumTuple._2).toList()));
        log.info("########### Result Datum #############");
        log.info(JsonUtil.getPrettyJson(resultBatchDatum));

        // Build and post contract txn\
        val utxoSelectionStrategy = new DefaultUtxoSelectionStrategyImpl(utxoSupplier);
        val collateralUtxos = utxoSelectionStrategy.select(sender, new Amount(LOVELACE, adaToLovelace(10)), Collections.emptySet());

        // Build the expected output
        val outputDatum = plutusObjectConverter.toPlutusData(resultBatchDatum);
        val output = Output.builder()
                .address(voteBatcherScriptAddress)
                .datum(outputDatum)
                .inlineDatum(true)
                .assetName(LOVELACE)
                .qty(adaToLovelace(1))
                .build();

        val scriptUtxos = utxoTuples.stream().map(utxoVoteDatumTuple -> utxoVoteDatumTuple._1)
                .toList();

        val scriptCallContexts = scriptUtxos.stream().map(utxo -> ScriptCallContext
                .builder()
                .script(voteBatcherScript)
                .utxo(utxo)
                .exUnits(ExUnits.builder()  // Exact exUnits will be calculated later
                        .mem(BigInteger.valueOf(0))
                        .steps(BigInteger.valueOf(0))
                        .build())

                .redeemer(plutusObjectConverter.toPlutusData(CreateVoteBatchRedeemer.create(mt)))
                .redeemerTag(RedeemerTag.Spend).build())
                .toList();

        var txBuilder = output.outputBuilder()
                .buildInputs(InputBuilders.createFromUtxos(scriptUtxos, sender))
                .andThen(CollateralBuilders.collateralOutputs(sender, new ArrayList<>(collateralUtxos))); // CIP-40

        // Loop and add scriptCallContexts
        for (var scriptCallContext : scriptCallContexts) {
            txBuilder = txBuilder.andThen(ScriptCallContextProviders.createFromScriptCallContext(scriptCallContext));
        }

        txBuilder = txBuilder.andThen((context, txn) -> {
            val txEvaluator = new TxEvaluator();
            val costMdls = new CostMdls();
            costMdls.add(CostModelUtil.getCostModelFromProtocolParams(protocolParamsSupplier.getProtocolParams(), Language.PLUTUS_V2).orElseThrow());
            val evalReedemers = txEvaluator.evaluateTx(txn, context.getUtxos(), costMdls);

            val redeemers = txn.getWitnessSet().getRedeemers();
            for (val redeemer : redeemers) { //Update costs
                evalReedemers.stream().filter(evalReedemer -> evalReedemer.getIndex().equals(redeemer.getIndex()))
                        .findFirst()
                        .ifPresent(evalRedeemer -> redeemer.setExUnits(evalRedeemer.getExUnits()));
            }

            // Remove all scripts from witness and just add one
            txn.getWitnessSet().getPlutusV2Scripts().clear();
            txn.getWitnessSet().getPlutusV2Scripts().add(plutusScriptUtil.getVoteBatcherContract());
        })
        .andThen(BalanceTxBuilders.balanceTx(sender, 1));

        val txBuilderContext = TxBuilderContext.init(utxoSupplier, protocolParamsSupplier);
        val transaction = txBuilderContext.buildAndSign(txBuilder, operatorAccountProvider.getTxSigner());

        val result = transactionProcessor.submitTransaction(transaction.serialize());
        if (!result.isSuccessful()) {
            throw new RuntimeException("Transaction failed. " + result.getResponse());
        }

        log.info("Vote Batcher Transaction Id : " + result.getValue());

        transactionUtil.waitForTransaction(result);

        return Optional.of(result.getValue());
    }

}
