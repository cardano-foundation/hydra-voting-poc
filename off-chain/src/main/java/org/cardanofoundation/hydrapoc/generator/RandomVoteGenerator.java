package org.cardanofoundation.hydrapoc.generator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydrapoc.model.ChallengeProposal;
import org.cardanofoundation.hydrapoc.model.Choice;
import org.cardanofoundation.hydrapoc.model.Vote;
import org.cardanofoundation.hydrapoc.model.Voter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@Slf4j
@Component
public class RandomVoteGenerator {
    @Value("${proposals.per.challenge:100}")
    private int nRandomProposal;

    @Value("${total.challenges:20}")
    private int nChallenges;

    RandomPublicKeyGenerator randomPublicKeyGenerator = new RandomPublicKeyGenerator();
    ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generate random votes
     * @param noOfVoters
     * @param noOfVotes
     * @param fileName
     * @throws Exception
     */
    public void generate(int noOfVoters, int noOfVotes, String fileName) throws Exception {
        Set<Vote> votes = getRandomVotes(noOfVoters, noOfVotes);

        FileOutputStream fout = new FileOutputStream(new File(fileName));
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(fout, votes);

        log.info("Random vote generation - Done");
    }

    public Set<Vote> getRandomVotes(int noOfVoters, int noOfVotes) throws Exception {
        //Generate random public keys
        List<Voter> voters = randomPublicKeyGenerator.getRandomPublicKey(noOfVoters);
        log.info("Random voter generation {} - Done", voters.size());

        //Generate random challenges + proposal
        Set<Long> challengeIds = new HashSet<>();
        for (int i =0; i<nChallenges; i++)
            challengeIds.add(RandomUtil.getRandomNumber(12000000, 450000000));
        log.info("Random challenge generation {} - Done", challengeIds.size());


        //100 random proposals for every challenge
        List<ChallengeProposal> challengeProposals = new ArrayList<>();
        for (Long challengeId: challengeIds) {
            for (int i=0; i<nRandomProposal; i++) {
                ChallengeProposal challengeProposal = new ChallengeProposal(challengeId, RandomUtil.getRandomNumber(100000, 6000000));
                challengeProposals.add(challengeProposal);
            }
        }
        log.info("Random proposal generation {} - Done", challengeProposals.size());

        //Generate random votes
        Set<Vote> votes = new HashSet<>();
        for (int i = 0; i< noOfVotes; i++) {
            int randomVoterIndex = (int) RandomUtil.getRandomNumber(0, voters.size());
            int randomChallengeIndex = (int) RandomUtil.getRandomNumber(0, challengeProposals.size());

            Voter voter = voters.get(randomVoterIndex);
            ChallengeProposal challengeProposal = challengeProposals.get(randomChallengeIndex);

            Vote vote = new Vote(voter.pubKey(), voter.votingPower(), challengeProposal.challenge(),
                    challengeProposal.proposal(), getRandomChoice());
            votes.add(vote);
        }
        log.info("Random vote generation - Done");
        return votes;
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

    /**
     * Read votes from a vote json file
     * @param startIndex start index in vote list
     * @param batchSize no of votes to read
     * @param voteFile vote file
     * @return List of votes
     */
    public List<Vote> getVotes(int startIndex, int batchSize, String voteFile) throws Exception {
        log.info("Start Index: " + startIndex);
        log.info("End Index: " + (startIndex + batchSize));
        log.info("Reading votes ...");
        List<Vote> votes = getAllVotes(voteFile);

        if (startIndex >= votes.size()) {
            return Collections.EMPTY_LIST;
        } else {
            if (startIndex + batchSize >= votes.size())
                return votes.subList(startIndex, votes.size());
            else
                return votes.subList(startIndex, startIndex + batchSize);

        }
    }

    public List<Vote> getAllVotes(String voteFile) throws IOException {
        return objectMapper.readValue(new File(voteFile), new TypeReference<List<Vote>>() {});
    }

    public static void main(String[] args) throws Exception {
        new RandomVoteGenerator().generate(89000, 300000, "votes.json");
    }
}
