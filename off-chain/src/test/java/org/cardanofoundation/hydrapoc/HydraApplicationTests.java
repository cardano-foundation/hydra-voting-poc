package org.cardanofoundation.hydrapoc;

import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.client.util.Tuple;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cardanofoundation.hydra.client.HydraClientOptions;
import org.cardanofoundation.hydra.client.HydraQueryEventListener;
import org.cardanofoundation.hydra.client.HydraStateEventListener;
import org.cardanofoundation.hydra.client.HydraWSClient;
import org.cardanofoundation.hydra.core.model.HydraState;
import org.cardanofoundation.hydra.core.model.UTXO;
import org.cardanofoundation.hydra.core.model.query.response.GreetingsResponse;
import org.cardanofoundation.hydra.core.model.query.response.HeadIsInitializingResponse;
import org.cardanofoundation.hydra.core.model.query.response.Response;
import org.cardanofoundation.hydrapoc.batch.VoteBatchReducer;
import org.cardanofoundation.hydrapoc.batch.VoteBatcher;
import org.cardanofoundation.hydrapoc.batch.VoteUtxoFinder;
import org.cardanofoundation.hydrapoc.batch.data.output.ResultBatchDatum;
import org.cardanofoundation.hydrapoc.commands.Commands;
import org.cardanofoundation.hydrapoc.generator.RandomVoteGenerator;
import org.cardanofoundation.hydrapoc.hydra.util.FuelTransaction;
import org.cardanofoundation.hydrapoc.importvote.VoteDatum;
import org.cardanofoundation.hydrapoc.importvote.VoteImporter;
import org.cardanofoundation.hydrapoc.model.Vote;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;

@SpringBootTest
@Slf4j
class HydraApplicationTests {

    @Autowired
    private RandomVoteGenerator randomVoteGenerator;

    @Autowired
    private VoteImporter voteImporter;

    @Autowired
    private Commands command;

    @Autowired
    private VoteUtxoFinder voteUtxoFinder;

    @Autowired
    private VoteBatcher voteBatcher;

    @Autowired
    private VoteBatchReducer voteBatchReducer;

    // docker-compose exec cardano-node cardano-cli query utxo --testnet-magic 42 --address addr_test1vru2drx33ev6dt8gfq245r5k0tmy7ngqe79va69de9dxkrg09c7d3
    @Test
    public void preInit() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(3);

        HydraStateEventListener hydraOpenHandler = (prevState, newState) -> {
            if (newState == HydraState.Open) {
                log.info("Hydra is open seen.");
                countDownLatch.countDown();
            }
        };

        val hc1 = new HydraWSClient(HydraClientOptions.createDefault("ws://dev.cf-hydra-voting-poc.metadata.dev.cf-deployments.org:4001"));
        hc1.addHydraStateEventListener(hydraOpenHandler);
        hc1.addHydraQueryEventListener(new HydraQueryEventListener.Stub() {
            @Override
            public void onResponse(Response response) {
                if (response instanceof GreetingsResponse gr) {
                    log.info("Sending init..., head status:{}", gr.getHeadStatus());
                    hc1.init();
                }
                if (response instanceof HeadIsInitializingResponse r) {
                    log.info("Head is initializing, committing utxo..., head_id:{}", r.getHeadId());
                    val utxo = new UTXO();
                    utxo.setAddress("addr_test1vru2drx33ev6dt8gfq245r5k0tmy7ngqe79va69de9dxkrg09c7d3");
                    utxo.setValue(Map.of("lovelace", BigInteger.valueOf(1000000000L)));

                    log.info("Committing utxo: {}", utxo);
                    hc1.commit("d2e63f56ab88c3ca31d3bb32b3bfa959b0bd522929932d45fe500f2e2bcf4463#0", utxo);
                }
            }
        });
        val hc2 = new HydraWSClient(HydraClientOptions.createDefault("ws://dev.cf-hydra-voting-poc.metadata.dev.cf-deployments.org:4002"));
        hc2.addHydraStateEventListener(hydraOpenHandler);
        hc2.addHydraQueryEventListener(new HydraQueryEventListener.Stub() {
            @Override
            public void onResponse(Response response) {
                if (response instanceof HeadIsInitializingResponse) {
                    hc2.commit();
                }
            }
        });
        val hc3 = new HydraWSClient(HydraClientOptions.createDefault("ws://dev.cf-hydra-voting-poc.metadata.dev.cf-deployments.org:4003"));
        hc3.addHydraStateEventListener(hydraOpenHandler);
        hc3.addHydraQueryEventListener(new HydraQueryEventListener.Stub() {
            @Override
            public void onResponse(Response response) {
                if (response instanceof HeadIsInitializingResponse) {
                    hc3.commit();
                }
            }
        });

        hc1.connect();
        hc2.connect();
        hc3.connect();

        countDownLatch.await(30, SECONDS);
    }

    @Test
    public void fullMonthy01() throws Exception {
        preInit();

        System.out.println("importing votes...");
        importVotesFromFile();
        System.out.println("creating and posting batches...");
        createAndPostBatch();
        System.out.println("reducing batches...");
        reduceBatch();
    }

    //1. Generate 150 votes
    @Test
    public void generateVotes() throws Exception {
        command.generateVotes(1000, 1100, "votes.json");
        var allVotes = randomVoteGenerator.getAllVotes("votes.json");
        System.out.println("Generated unique votes count:" + allVotes.size());
    }

    //2. Import 20 votes from votes.json to script address
    @Test
    public void importVotesFromFile() throws Exception {
        var allVotes = randomVoteGenerator.getAllVotes("votes.json");

        var batchSize = 5;

        log.info("Starting import of votes, count:" + allVotes.size());

        var partitions = Lists.partition(allVotes, batchSize);

        for (var votesPart : partitions) {
            if (votesPart.size() == batchSize) {
                voteImporter.importVotes(votesPart);
            }
        }

        log.info("Votes imported into smart contract.");
    }

    //3. Create a batch of 25 votes --> 1 result
    //Run this test multiple times to create multiple batches
    @Test
    public void createAndPostBatch() throws Exception {
        log.info("Batch creation...");

        var batchSize = 5;

        while (voteBatcher.createAndPostBatchTransaction(batchSize).isPresent()) {}

        log.info("Batches creation completed.");
    }

    @Test
    public void reduceBatch() throws Exception {
        log.info("Batch reduction...");

        var batchSize = 5;

        while (voteBatchReducer.postReduceBatchTransaction(batchSize).isPresent()) {}

        log.info("Reducing completed.");
    }

    //The following are additional tests for command line options and other methods
    @Test
    public void importVotes() throws Exception {
        Set<Vote> votes = randomVoteGenerator.getRandomVotes(20, 100);
        voteImporter.importVotes(votes);
    }

    @Test
    public void generateVotesCmd() throws Exception {
        command.generateVotes(10, 30, "votes-1.json");
    }

    @Test
    public void importVotesCmd() throws Exception {
        List<String> txIds = command.importVotes(0, 5, "votes-1.json");
        System.out.println(txIds);
    }

    @Test
    public void getUtxosWithVotes() {
        List<Tuple<Utxo, VoteDatum>> utxoTuples = voteUtxoFinder.getUtxosWithVotes(20);
        System.out.println(utxoTuples);
    }


    @Test
    public void readVoteResultDatum() {
        String resultDatum = "d8799fa2d8799f1a0184abe81a00047187ffd8799f001a009190fb00ffd8799f1a0fbb60ad1a0004320fffd8799f1a00754f980000ffff";
        ResultBatchDatum resultBatchDatum = ResultBatchDatum.deserialize(HexUtil.decodeHexString(resultDatum)).get();
        System.out.println(resultBatchDatum);
    }

    @Autowired
    FuelTransaction fuelTransaction;

    @Test
    public void fuel() throws Exception {
        List<String> addresses = List.of(
                "addr_test1vr2jvlvw62kv82x8gn0pewn6n5r82m6zxxn6c7vp04t9avs3wgpxv",
                "addr_test1vz4jpljkq88278xat56pcy240ey9ng9wza8qtdavg6f7vqs0z8903",
                "addr_test1vzh03tyuujtl4tfq4maduaxk0pvt893xy4g4l6cn4k7mtxs7rmsjz"
        );

        fuelTransaction.fuel(addresses);
    }

}
