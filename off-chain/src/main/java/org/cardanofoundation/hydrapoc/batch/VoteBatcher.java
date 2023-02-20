package org.cardanofoundation.hydrapoc.batch;

import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.TransactionProcessor;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.plutus.api.PlutusObjectConverter;
import com.bloxbean.cardano.client.plutus.impl.DefaultPlutusObjectConverter;
import com.bloxbean.cardano.client.util.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydrapoc.common.OperatorAccountProvider;
import org.cardanofoundation.hydrapoc.importvote.VoteDatum;
import org.cardanofoundation.hydrapoc.util.TransactionUtil;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteBatcher {
    private final UtxoSupplier utxoSupplier;
    private final ProtocolParamsSupplier protocolParamsSupplier;
    private final TransactionProcessor transactionProcessor;
    private final OperatorAccountProvider operatorAccountProvider;
    private final TransactionUtil transactionUtil;
    private final VoteUtxoFinder voteUtxoFinder;

    @org.springframework.beans.factory.annotation.Value("${voting.batch.contract}")
    private String voteBatchContractAddress;

    private PlutusObjectConverter plutusObjectConverter = new DefaultPlutusObjectConverter();

    public String createAndPostBatchTransaction(int batchSize) {
        List<Tuple<Utxo, VoteDatum>> utxoTuples = voteUtxoFinder.getUtxosWithVotes(batchSize);
        if (utxoTuples.size() == 0) {
            log.warn("No utxo found");
            return null;
        }

        String sender = operatorAccountProvider.getOperatorAddress();
        log.info("Sender Address: " + sender);

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

        return null;

        //Build and post contract txn
//        Output output = new Output()


        //Create txInputs and balance tx
//        TxBuilder txBuilder = txOutputBuilder
//                .buildInputs(InputBuilders.createFromSender(sender, sender))
//                .andThen(BalanceTxBuilders.balanceTx(sender));
//
//        Transaction transaction = TxBuilderContext.init(utxoSupplier, protocolParamsSupplier)
//                .buildAndSign(txBuilder, operatorAccountProvider.getTxSigner());
//
//        Result<String> result = transactionProcessor.submitTransaction(transaction.serialize());
//        if (!result.isSuccessful())
//            throw new RuntimeException("Transaction failed. " + result.getResponse());
//        else
//            System.out.println("Import Transaction Id : " + result.getValue());
//
//        transactionUtil.waitForTransaction(result);
//        return result.getValue();
    }


}
