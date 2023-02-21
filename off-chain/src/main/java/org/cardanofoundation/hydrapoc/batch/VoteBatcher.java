package org.cardanofoundation.hydrapoc.batch;

import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.TransactionProcessor;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.api.TransactionService;
import com.bloxbean.cardano.client.backend.model.EvaluationResult;
import com.bloxbean.cardano.client.coinselection.UtxoSelectionStrategy;
import com.bloxbean.cardano.client.coinselection.impl.DefaultUtxoSelectionStrategyImpl;
import com.bloxbean.cardano.client.exception.CborSerializationException;
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
import com.bloxbean.cardano.client.util.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydrapoc.commands.PlutusScriptUtil;
import org.cardanofoundation.hydrapoc.commands.TransactionUtil;
import org.cardanofoundation.hydrapoc.common.OperatorAccountProvider;
import org.cardanofoundation.hydrapoc.importvote.VoteDatum;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteBatcher {
    private final UtxoSupplier utxoSupplier;
    private final ProtocolParamsSupplier protocolParamsSupplier;
    private final TransactionProcessor transactionProcessor;
    private final TransactionService transactionService;
    private final OperatorAccountProvider operatorAccountProvider;
    private final TransactionUtil transactionUtil;
    private final PlutusScriptUtil plutusScriptUtil;
    private final VoteUtxoFinder voteUtxoFinder;

    private PlutusObjectConverter plutusObjectConverter = new DefaultPlutusObjectConverter();

    public String createAndPostBatchTransaction(int batchSize) throws Exception {
        PlutusV2Script voteBatcherScript = plutusScriptUtil.getVoteBatcherContract();
        String voteBatcherScriptAddress = plutusScriptUtil.getVoteBatcherContractAddress();
        String sender = operatorAccountProvider.getOperatorAddress();
        log.info("Sender Address: " + sender);
        log.info("Script Address: " + voteBatcherScriptAddress);

        List<Tuple<Utxo, VoteDatum>> utxoTuples = voteUtxoFinder.getUtxosWithVotes(batchSize);
        if (utxoTuples.size() == 0) {
            log.warn("No utxo found");
            return null;
        }

        ResultBatchDatum resultBatchDatum = new ResultBatchDatum();
        for (Tuple<Utxo, VoteDatum> tuple : utxoTuples) {
            VoteDatum voteDatum = tuple._2;
            ChallengeProposalDatum challengeProposalDatum =
                    new ChallengeProposalDatum(voteDatum.getChallenge(), voteDatum.getProposal());

            ResultDatum resultDatum = resultBatchDatum.get(challengeProposalDatum);
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

        log.info(resultBatchDatum.toString());

        //Build and post contract txn\
        UtxoSelectionStrategy utxoSelectionStrategy = new DefaultUtxoSelectionStrategyImpl(utxoSupplier);
        Set<Utxo> collateralUtxos =
                utxoSelectionStrategy.select(sender, new Amount(LOVELACE, adaToLovelace(10)), Collections.emptySet());

        //Build the expected output
        PlutusData outputDatum = plutusObjectConverter.toPlutusData(resultBatchDatum);
        Output output = Output.builder()
                .address(voteBatcherScriptAddress)
                .datum(outputDatum)
                .inlineDatum(true)
                .assetName(LOVELACE)
                .qty(adaToLovelace(1))
                .build();

        List<Utxo> scriptUtxos = utxoTuples.stream().map(utxoVoteDatumTuple -> utxoVoteDatumTuple._1)
                .collect(Collectors.toList());

        ScriptCallContext scriptCallContext = ScriptCallContext
                .builder()
                .script(voteBatcherScript)
                .exUnits(ExUnits.builder()  //Exact exUnits will be calculated later
                        .mem(BigInteger.valueOf(500000))
                        .steps(BigInteger.valueOf(150000000))
                        .build())
                .redeemer(plutusObjectConverter.toPlutusData(CreateVoteBatchRedeemer.create()))
                .redeemerTag(RedeemerTag.Spend).build();

        TxBuilder txBuilder = output.outputBuilder()
                .buildInputs(InputBuilders.createFromUtxos(scriptUtxos))
                .andThen(CollateralBuilders.collateralOutputs(sender, new ArrayList<>(collateralUtxos))) //CIP-40
                .andThen(ScriptCallContextProviders.createFromScriptCallContext(scriptCallContext))
                .andThen((context, txn) -> {
                    //Calculate ExUnit. It should be done before balanceTx for accurate fee calculation
                    //update estimate ExUnits
//                    ExUnits estimatedExUnits;
//                    try {
//                        estimatedExUnits = evaluateExUnits(txn);
//                        txn.getWitnessSet().getRedeemers().get(0).setExUnits(estimatedExUnits);
//                    } catch (Exception e) {
//                        throw new ApiRuntimeException("Script cost evaluation failed", e);
//                    }

//                    txn.getWitnessSet().getPlutusV2Scripts().clear();
                })
                .andThen(BalanceTxBuilders.balanceTx(sender, 1));

        Transaction transaction = TxBuilderContext.init(utxoSupplier, protocolParamsSupplier)
                .buildAndSign(txBuilder, operatorAccountProvider.getTxSigner());

        Result<String> result = transactionProcessor.submitTransaction(transaction.serialize());
        if (!result.isSuccessful())
            throw new RuntimeException("Transaction failed. " + result.getResponse());
        else
            log.info("Import Transaction Id : " + result.getValue());

        transactionUtil.waitForTransaction(result);

        return result.getValue();
    }

    private ExUnits evaluateExUnits(Transaction transaction) throws ApiException, CborSerializationException {
        Result<List<EvaluationResult>> evalResults = transactionService.evaluateTx(transaction.serialize());
        if (evalResults.isSuccessful()) {
            return evalResults.getValue().get(0).getExUnits();
        } else {
            return null;
        }
    }

}
