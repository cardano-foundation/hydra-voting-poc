package org.cardanofoundation.hydrapoc;

import org.cardanofoundation.hydrapoc.commands.Commands;
import org.cardanofoundation.hydrapoc.generator.RandomVoteGenerator;
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


}
