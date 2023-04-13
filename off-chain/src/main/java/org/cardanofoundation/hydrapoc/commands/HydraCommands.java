package org.cardanofoundation.hydrapoc.commands;

import com.bloxbean.cardano.client.util.HexUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydrapoc.hydra.HydraClient;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

@ShellComponent
@RequiredArgsConstructor
@Slf4j
public class HydraCommands {

    private boolean isHydraConnected = false;

    private HydraClient hydraClient;

    @ShellMethod()
    public void connect() throws URISyntaxException, InterruptedException {
//        this.hydraClient = new HydraClient()
//                .connectFrom();

        // actually the most ideal would be to set this flag when we have received the last message
        this.isHydraConnected = true;
    }

    // last seq
    private void storeLastSeq(int seq) throws IOException {
        var ioDir = System.getenv("java.io.tmpdir");
        log.info("tmp dir:{}", ioDir);

        byte[] bytes = ByteBuffer.allocate(4).putInt(seq).array();

        Files.write(Path.of(ioDir, "hydra-seq.dat"), HexUtil.encodeHexString(bytes).getBytes());
    }

//    @She
//    public void importVotes() {
//        var allVotes = randomVoteGenerator.getAllVotes("votes.json");
//
//        var batchSize = 2;
//
//        log.info("Starting import of votes, count:" + allVotes.size());
//        var partitions = Lists.partition(allVotes, batchSize);
//
//        for (var votesPart : partitions) {
//            if (votesPart.size() == batchSize) {
//                //Thread.sleep(1000);
//                voteImporter.importVotes(votesPart);
//            }
//        }
//
//        log.info("Votes imported into smart contract.");
//    }

}
