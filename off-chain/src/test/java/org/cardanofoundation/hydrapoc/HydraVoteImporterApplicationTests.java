package org.cardanofoundation.hydrapoc;

import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.client.util.Tuple;
import org.cardanofoundation.hydrapoc.batch.VoteBatchReducer;
import org.cardanofoundation.hydrapoc.batch.data.output.ResultBatchDatum;
import org.cardanofoundation.hydrapoc.batch.VoteBatcher;
import org.cardanofoundation.hydrapoc.batch.VoteUtxoFinder;
import org.cardanofoundation.hydrapoc.commands.Commands;
import org.cardanofoundation.hydrapoc.generator.RandomVoteGenerator;
import org.cardanofoundation.hydrapoc.importvote.VoteDatum;
import org.cardanofoundation.hydrapoc.importvote.VoteImporter;
import org.cardanofoundation.hydrapoc.model.Vote;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;

@SpringBootTest
class HydraVoteImporterApplicationTests {
    @Autowired
    private RandomVoteGenerator randomVoteGenerator;
    @Autowired
    private VoteImporter voteImporter;
    @Autowired
    private Commands generateVotes;
    @Autowired
    private VoteUtxoFinder voteUtxoFinder;
    @Autowired
    private VoteBatcher voteBatcher;
    @Autowired
    private VoteBatchReducer voteBatchReducer;

    @Test
    public void importVotes() throws Exception{
        Set<Vote> votes = randomVoteGenerator.getRandomVotes(20, 100);
        voteImporter.importVotes(votes);
    }

    @Test
    public void generateVotes() throws Exception {
        generateVotes.generateVotes(10, 30, "votes.json");
    }

    @Test
    public void importVotesCmd() throws Exception {
        List<String> txIds = generateVotes.importVotes(0, 10, "votes.json");
        System.out.println(txIds);
    }

    @Test
    public void getUtxosWithVotes() {
        List<Tuple<Utxo, VoteDatum>> utxoTuples = voteUtxoFinder.getUtxosWithVotes(20);
        System.out.println(utxoTuples);
    }

    @Test
    public void createAndPostBatch() throws Exception {
        voteBatcher.createAndPostBatchTransaction(10);
    }

    @Test
    public void reduceBatch() throws Exception {
        voteBatchReducer.postReduceBatchTransaction(5);
    }

    @Test
    public void readVoteResultDatum() {
        String resultDatum = "d8799fa2d8799f1a0184abe81a00047187ffd8799f001a009190fb00ffd8799f1a0fbb60ad1a0004320fffd8799f1a00754f980000ffff";
        ResultBatchDatum resultBatchDatum = ResultBatchDatum.deserialize(HexUtil.decodeHexString(resultDatum)).get();
        System.out.println(resultBatchDatum);
    }

    @Test
    public void getVoteBatches() {
        generateVotes.getVoteBatches(10);
    }

}
