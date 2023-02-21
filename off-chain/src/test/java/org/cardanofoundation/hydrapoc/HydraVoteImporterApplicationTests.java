package org.cardanofoundation.hydrapoc;

import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.util.Tuple;
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

    @Test
    public void importVotes() throws Exception{
        Set<Vote> votes = randomVoteGenerator.getRandomVotes(20, 100);
        voteImporter.importVotes(votes);
    }

    @Test
    public void generateVotes() throws Exception {
        generateVotes.generateVotes(50, 300, "votes.json");
    }

    @Test
    public void importVotesCmd() throws Exception {
        List<String> txIds = generateVotes.importVotes(0, 90, "votes.json");
        System.out.println(txIds);
    }

    @Test
    public void getUtxosWithVotes() {
        List<Tuple<Utxo, VoteDatum>> utxoTuples = voteUtxoFinder.getUtxosWithVotes(20);
        System.out.println(utxoTuples);
    }

    @Test
    public void createAndPostBatch() throws Exception {
        voteBatcher.createAndPostBatchTransaction(20);
    }

}
