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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cardanofoundation.hydrapoc.batch.data.input.ValidateVoteRedeemer;
import org.cardanofoundation.hydrapoc.batch.data.output.CategoryResultsDatum;
import org.cardanofoundation.hydrapoc.common.BalanceUtil;
import org.cardanofoundation.hydrapoc.common.OperatorAccountProvider;
import org.cardanofoundation.hydrapoc.util.PlutusScriptUtil;
import org.cardanofoundation.hydrapoc.util.TransactionUtil;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteValidator {

    private final UtxoSupplier utxoSupplier;
    private final ProtocolParamsSupplier protocolParamsSupplier;
    private final TransactionProcessor transactionProcessor;
    private final OperatorAccountProvider operatorAccountProvider;
    private final TransactionUtil transactionUtil;
    private final PlutusScriptUtil plutusScriptUtil;
    private final VoteUtxoFinder voteUtxoFinder;

    private final PlutusObjectConverter plutusObjectConverter = new DefaultPlutusObjectConverter();

    //TODO -- check collateral return when new utxo added during balanceTx
    @Retryable(include = {RuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100))
    public Optional<String> createVoteValidation(long category) throws Exception {
        val contract = plutusScriptUtil.getPlutusContract();
        val contractAddress = plutusScriptUtil.getContractAddress();
        val sender = operatorAccountProvider.getOperatorAddress();

        log.info("Sender Address: " + sender);
        log.info("contractAddress Address: " + contractAddress);

        val maybeGlobalCategoryResults = voteUtxoFinder.findFirstCategoryResults(category);
        val maybeFirstVote = voteUtxoFinder.findFirstVote(category);

        if (maybeGlobalCategoryResults.isEmpty() || maybeFirstVote.isEmpty()) {
            log.warn("No utxos found!");
            return Optional.empty();
        }

        val vote = maybeFirstVote.get()._2;
        val globalCategoryResults = maybeGlobalCategoryResults.orElseThrow()._2;

        val categoryResults = CategoryResultsDatum.copyOf(globalCategoryResults);
        categoryResults.getResults().compute(vote.getProposal(), (key, value) -> value == null ? 0 : value + 1);

        //Function<Object, byte[]> hash_fn = vote -> sha2_256(plutusObjectConverter.toPlutusData(vote).serializeToBytes());
        //val hashedList = HashedList.create(voteDatums, hash_fn);

        // val batchHash = hashedList.hash();
        //categoryResults.setBatchHash(batchHash);

        //log.info("Create vote batcbes - batchHash:" + HexUtil.encodeHexString(batchHash));

//        log.info(JsonUtil.getPrettyJson(categoryResults));

        // Build and post contract txn
        val utxoSelectionStrategy = new LargestFirstUtxoSelectionStrategy(utxoSupplier);
        val collateralUtxos = utxoSelectionStrategy.select(sender, new Amount(LOVELACE, adaToLovelace(100000)), emptySet());

        // Build the expected output
        val outputDatum = plutusObjectConverter.toPlutusData(categoryResults);
        val output = Output.builder()
                .address(contractAddress)
                .datum(outputDatum)
                .inlineDatum(true)
                .assetName(LOVELACE)
                .qty(adaToLovelace(1))
                .build();

        val scriptUtxos = maybeGlobalCategoryResults.stream().map(utxoVoteDatumTuple -> utxoVoteDatumTuple._1)
                .toList();

        val extraInputs = utxoSelectionStrategy.select(sender, new Amount(LOVELACE, adaToLovelace(2)), Set.of());

        List<Utxo> allInputs = new ArrayList<>();
        allInputs.addAll(scriptUtxos);
        allInputs.addAll(extraInputs);

        var txBuilder = output.outputBuilder()
                .buildInputs(InputBuilders.createFromUtxos(allInputs, sender))
                //.andThen(output2.outputBuilder().buildInputs(InputBuilders.createFromSender(sender, sender)))
                .andThen(CollateralBuilders.collateralOutputs(sender, new ArrayList<>(collateralUtxos))); // CIP-40

        val scriptCallContexts = scriptUtxos.stream().map(utxo -> ScriptCallContext
                        .builder()
                        .script(contract)
                        .utxo(utxo)
                        .exUnits(ExUnits.builder()  // Exact exUnits will be calculated later
                                .mem(BigInteger.valueOf(0))
                                .steps(BigInteger.valueOf(0))
                                .build()
                        )

                        //.redeemer(plutusObjectConverter.toPlutusData(ValidateVoteRedeemer.create(batchHash)))
                        .redeemer(plutusObjectConverter.toPlutusData(ValidateVoteRedeemer.create()))
                        .redeemerTag(RedeemerTag.Spend).build())
                .toList();

        for (var scriptCallContext : scriptCallContexts) {
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
            txn.getWitnessSet().getPlutusV2Scripts().add(contract);
        })
        .andThen(BalanceUtil.balanceTx(sender, 1));

        val txBuilderContext = TxBuilderContext.init(utxoSupplier, protocolParamsSupplier);
        val transaction = txBuilderContext.buildAndSign(txBuilder, operatorAccountProvider.getTxSigner());

        log.info("Fee:{} lovelaces", transaction.getBody().getFee());

        val result = transactionProcessor.submitTransaction(transaction.serialize());
        if (!result.isSuccessful()) {
            throw new RuntimeException("Transaction failed. " + result.getResponse());
        }

        log.info("Vote Batcher Transaction Id: " + result.getValue());

        transactionUtil.waitForTransaction(result);

        return Optional.of(result.getValue());
    }

}
