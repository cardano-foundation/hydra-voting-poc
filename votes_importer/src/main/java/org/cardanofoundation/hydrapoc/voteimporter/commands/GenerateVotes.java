package org.cardanofoundation.hydrapoc.voteimporter.commands;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydrapoc.voteimporter.generator.RandomVoteGenerator;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
@Slf4j
public class GenerateVotes {

    @ShellMethod(value = "Generate random votes")
    public void generateVotes(@ShellOption int nVoters,
                              @ShellOption int nVotes,
                              @ShellOption String outFile) throws Exception {
        RandomVoteGenerator randomVoteGenerator = new RandomVoteGenerator();
        randomVoteGenerator.generate(nVoters, nVotes, outFile);
    }
}
