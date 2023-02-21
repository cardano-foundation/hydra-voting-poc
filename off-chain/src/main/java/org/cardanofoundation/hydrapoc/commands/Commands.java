package org.cardanofoundation.hydrapoc.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydrapoc.batch.VoteBatcher;
import org.cardanofoundation.hydrapoc.generator.RandomVoteGenerator;
import org.cardanofoundation.hydrapoc.model.Vote;
import org.cardanofoundation.hydrapoc.importvote.VoteImporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.ArrayList;
import java.util.List;

@ShellComponent
@RequiredArgsConstructor
@Slf4j
public class Commands {

    private final RandomVoteGenerator randomVoteGenerator;
    private final VoteImporter voteImporter;
    private final VoteBatcher voteBatcher;

    @Value("${import.interval:1000}")
    private long importInterval;

    @ShellMethod(value = "Generate random votes")
    public void generateVotes(@ShellOption int nVoters,
                              @ShellOption int nVotes,
                              @ShellOption String outFile) throws Exception {
        randomVoteGenerator.generate(nVoters, nVotes, outFile);
    }

    @ShellMethod(value = "Import votes")
    public List<String> importVotes(@ShellOption(defaultValue = "0") int startIndex, @ShellOption int batchSize, @ShellOption String voteFile) throws Exception {
        int index = startIndex;
        List<Vote> votes = randomVoteGenerator.getVotes(index, batchSize, voteFile);
        List<String> txIds = new ArrayList<>();
        while (votes.size() > 0) {
            Thread.sleep(importInterval);
            log.info("Import from index : " + index);
            String txHash = voteImporter.importVotes(votes);
            txIds.add(txHash);
            log.info("Tx Hash: " + txHash);

            index += votes.size();
            votes = randomVoteGenerator.getVotes(index, batchSize, voteFile);
        }

        return txIds;
    }

    @ShellMethod(value = "Create Batch")
    public void createBatch(@ShellOption int batchSize) throws Exception {
        voteBatcher.createAndPostBatchTransaction(batchSize);
    }
}
