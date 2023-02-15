package org.cardanofoundation.hydrapoc.voteimporter.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydrapoc.voteimporter.model.ChallengeProposal;
import org.cardanofoundation.hydrapoc.voteimporter.model.Choice;
import org.cardanofoundation.hydrapoc.voteimporter.model.Vote;
import org.cardanofoundation.hydrapoc.voteimporter.model.Voter;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class RandomVoteGenerator {
    RandomPublicKeyGenerator randomPublicKeyGenerator = new RandomPublicKeyGenerator();
    ObjectMapper objectMapper = new ObjectMapper();

    public void generate(int noOfVoters, int noOfVotes, String fileName) throws Exception {
        //Generate random public keys
        List<Voter> voters = randomPublicKeyGenerator.getRandomPublicKey(noOfVoters);
        log.info("Random voter generation {} - Done", voters.size());

        //Generate random challenges + proposal
        Set<Long> challengeIds = new HashSet<>();
        for (int i =0; i<20; i++)
            challengeIds.add(RandomUtil.getRandomNumber(12000000, 450000000));
        log.info("Random challenge generation {} - Done", challengeIds.size());


        //100 random proposals for every challenge
        List<ChallengeProposal> challengeProposals = new ArrayList<>();
        for (Long challengeId: challengeIds) {
            for (int i=0; i<100; i++) {
                ChallengeProposal challengeProposal = new ChallengeProposal(challengeId, RandomUtil.getRandomNumber(100000, 6000000));
                challengeProposals.add(challengeProposal);
            }
        }
        log.info("Random proposal generation {} - Done", challengeProposals.size());

        //Generate random votes
        Set<Vote> votes = new HashSet<>();
        for (int i=0; i<noOfVotes; i++) {
            int randomVoterIndex = (int) RandomUtil.getRandomNumber(0, voters.size());
            int randomChallengeIndex = (int) RandomUtil.getRandomNumber(0, challengeProposals.size());

            Voter voter = voters.get(randomVoterIndex);
            ChallengeProposal challengeProposal = challengeProposals.get(randomChallengeIndex);
            Vote vote = new Vote(voter.pubKey(), voter.votingPower(), challengeProposal.challenge(),
                    challengeProposal.proposal(), getRandomChoice());
            votes.add(vote);
        }
        log.info("Random vote generation - Done");

        FileOutputStream fout = new FileOutputStream(new File(fileName));
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(fout, votes);

        log.info("Random vote generation - Done");
    }

    private Choice getRandomChoice() {
        int randomChoice = (int) RandomUtil.getRandomNumber(0, 3);
        if (randomChoice == 0)
            return Choice.ABSTAIN;
        else if (randomChoice == 1)
            return Choice.NAY;
        else if (randomChoice == 2)
            return Choice.YAY;
        else
            throw new RuntimeException("Invalid choice : " + randomChoice);
    }

    public static void main(String[] args) throws Exception {
        new RandomVoteGenerator().generate(89000, 300000, "votes.json");
    }
}
