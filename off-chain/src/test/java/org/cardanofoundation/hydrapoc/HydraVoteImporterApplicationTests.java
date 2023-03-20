package org.cardanofoundation.hydrapoc;

import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.ByteString;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.transaction.spec.PlutusV2Script;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.client.util.Tuple;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
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

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Slf4j
class HydraVoteImporterApplicationTests {

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

    @Test
    public void fullMonthy01() throws Exception {
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
        command.generateVotes(10, 110, "votes.json");
        var allVotes = randomVoteGenerator.getAllVotes("votes.json");
        System.out.println("Generated unique votes count:" + allVotes.size());
    }

    //2. Import 20 votes from votes.json to script address
    @Test
    public void importVotesFromFile() throws Exception {
        Thread.sleep(1000); //so that all previous messages are consumed from hydra
        var allVotes = randomVoteGenerator.getAllVotes("votes.json");

        var batchSize = 10;

        log.info("Starting import of votes, count:" + allVotes.size());
        var partitions = Lists.partition(allVotes, batchSize);
        for (var votesPart : partitions) {
            if (votesPart.size() == batchSize) {
                Thread.sleep(1000);
                voteImporter.importVotes(votesPart);
            } else {
                log.info("ignoring the rest.., size:" + votesPart.size());
            }
        }
        log.info("Votes imported into smart contract.");
    }

    //3. Create a batch of 3 votes --> 1 result
    //Run this test multiple times to create multiple batches
    @Test
    public void createAndPostBatch() throws Exception {
        Thread.sleep(5000);
        var allVotes = randomVoteGenerator.getAllVotes("votes.json");
        var batchSize = 10;
        var partitions = Lists.partition(allVotes, batchSize);

        log.info("Counting votes, count:" + allVotes.size());

        for (var votesPart : partitions) {
            if (votesPart.size() == batchSize) {
                Thread.sleep(1000);
                voteBatcher.createAndPostBatchTransaction(batchSize);
            }
        }

        log.info("Counting votes completed.");
    }

    // 4. Reduce batch of 20 to 1
    // Run this test multiple times to reduce batches to 1 batch
    @Test
    public void reduceBatch() throws Exception {
        Thread.sleep(1000);

        var batchSize = 5;

        var allVotes = randomVoteGenerator.getAllVotes("votes.json");
        var size = Double.valueOf(Math.ceil((double) allVotes.size() / batchSize)).intValue();

        log.info("Reducing votes results, size:" + size);

        voteBatchReducer.postReduceBatchTransaction(5, 0);
        voteBatchReducer.postReduceBatchTransaction(5, 0);
        voteBatchReducer.postReduceBatchTransaction(5, 0);
        voteBatchReducer.postReduceBatchTransaction(5, 0);

        voteBatchReducer.postReduceBatchTransaction(4, 1);

//        for (int i = 0; i < size; i++) {
//            Thread.sleep(100);
//            voteBatchReducer.postReduceBatchTransaction(batchSize, 0);
//        }
//
//        Thread.sleep(100);
//        voteBatchReducer.postReduceBatchTransaction(3, 1);
//
//        Thread.sleep(100);
//        voteBatchReducer.postReduceBatchTransaction(3, 1);
//
//        Thread.sleep(100);
//        voteBatchReducer.postReduceBatchTransaction(2, 2);
//
//        log.info("Reducing votes results completed.");
    }

    @Test
    public void reduceBatch2() throws Exception {
        Thread.sleep(5000);

        var batchSize = 3;

        log.info("Reducing votes results, batch size:" + batchSize);

        voteBatchReducer.postReduceBatchTransaction(batchSize, 0);

        log.info("Reducing votes results completed.");
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

    @Test
    public void contractCompileCodeTest() throws CborException, CborSerializationException {
        String compileCode = "5908a10100003232323232323232323232222533300632323232533300a3370e0029000099191919192999807991919911919802001119191980c19299980c19b87001480004c8c94ccc07cc0880084cc06ccc0600048c8c8cc078c94ccc078cdc3800a4000264646464a66604e60540042930a998122481364c6973742f5475706c652f436f6e73747220636f6e7461696e73206d6f7265206974656d73207468616e2069742065787065637465640016375a605000260500046eb4c098004c06000c54cc0812412b436f6e73747220696e64657820646964206e6f74206d6174636820616e7920747970652076617269616e740016302037540046603c64a66603c66e1c005200013232323232325333029302c002149854cc099241364c6973742f5475706c652f436f6e73747220636f6e7461696e73206d6f7265206974656d73207468616e2069742065787065637465640016375a605400260540046eb4c0a0004c0a0008dd69813000980c0010a9981024812b436f6e73747220696e64657820646964206e6f74206d6174636820616e7920747970652076617269616e74001630203754002931810001180f000a4c2a66038921364c6973742f5475706c652f436f6e73747220636f6e7461696e73206d6f7265206974656d73207468616e20697420657870656374656400163756604000260240042a660349212b436f6e73747220696e64657820646964206e6f74206d6174636820616e7920747970652076617269616e740016301a375400266ebc004010c028004cc038c03c00520043374a90001980c9ba60024bd700019bac3300a300b3300a300b00c4800120043001001222533301700214a026464a666028600600429444ccc01401400400cc06c00cc0640084cc89288010020a503232323232323232323232323232330020014bd6f7b63000718008009112999810801080089919191919199991111999808002001801000999804004002003199991111999806802001801000a5eb7bdb180cdd2a40006604e6ea0008cc09cdd4000a5eb80cdd2a40006604e6ea0dd69980d180d801a40046604e6ea0dd69980d180d801a40086604e6ea0dd69980d180d801a400c97ae000b222323374a900019815800a5eb80cdd2a4000660546ea0cdc01bad3301d301e00248000dd69980e980f000a4000660546ea0cdc01bad3301d301e00248008dd69980e980f000a4004660546ea0cdc01bad3301d301e00248010dd69980e980f000a400897ae000b375a66032603400890031bad33018301900348010c8c8c8c8c8cdd2a400066052008660526ea000ccc0a4dd4001198149ba80014bd701919809000a40006602400890001919808800a40006602200690011919808000a400066020004900219ba548000cc094dd41bad33018301900148010cc094dd41bad330183019001480192f5c0004604a0066046004600200244444a66604200826604466ec000c0092f5bded8c0264646464a66604066ebccc01401c004cdd2a400097ae01330263376000e00c0102a66604066ebc01c0044cc098cdd800380300189981319bb0001002333330090090030070060053022003302200230250053023004223322533301a33720004002266e9520004bd700a99980d19b8f00200113374a900125eb804cdd2a400897ae037660046ecc004c004004888894ccc078010400c4c8c8c8c8ccccc02402400cccccc02801c004008018014018014c07c00cc07c008c088014c080010c0040048888894ccc0740144cc078cdd8002001a5eb7bdb1804c8c8c8c94ccc070cdd79980280400099ba5480012f5c026604466ec002001c02454ccc070cdd780400089919299980f19b87001480004c8c8cc098cdd80060008039813000980c001080298101baa0013330060080070021330223376000200466666601401400601000e00c00a603c006603c004604200c603e00a4464a66602866e1c0052002100213232001375a6038002601c006602c6ea800888c8cc88c94ccc058cdc3800a4004266e9520024bd700991919ba548000cc078dd41802000a5eb80dd6980f0009808001980c1baa0020012375a6601a601c008900119801a5eb84101000081010100810102001299980999b87375a66018601a0069004000899b8700200114a06002002444a66602c004266e9520024bd700991929998099801801099ba548000cc064dd400125eb804ccc01401400400cc06800cdd6980c001191919191919191980300091980b19299980b19b87001480004c8c8c8c8c8c8c8c8c8c94ccc094c0a000852615330224901364c6973742f5475706c652f436f6e73747220636f6e7461696e73206d6f7265206974656d73207468616e2069742065787065637465640016375a604c002604c0046eb4c090004c090008dd6981100098110011bad30200013020002375c603c00260200042a6603092012b436f6e73747220696e64657820646964206e6f74206d6174636820616e7920747970652076617269616e74001630183754002002660060024601000266008002466018601a66018601a0029001240086eb0cc028c02ccc028c02c031200048000c0040048894ccc05c00852f5c0264646464a66602c66e1c00520021333007007003005132323301e001333009009005007301e0013010002301837540026006004603600660320046002002444a66602a004297ae01323233018300300233300500500100330190033017002232533300e3370e00290020991919ba548000cc0580052f5c0602c0026010004266e9520024bd7018081baa001375c6024002600800c2940c030dd5002980119299980499b8748008c030dd500088008a99805a492a4173736572746564206f6e20696e636f727265637420636f6e7374727563746f722076617269616e742e00163300130020034800888c8ccc0040052000003222333300c3370e008004026466600800866e0000d200230150010012300b37540022930b180080091129998048010a4c26600a600260160046660060066018004002ae695cdab9c5573aaae7955cfaba05742ae881";

        ByteString bs = new ByteString(HexUtil.decodeHexString(compileCode));
        String cborHex = HexUtil.encodeHexString(CborSerializationUtil.serialize(bs));
        System.out.println(cborHex);

        PlutusV2Script plutusV2Script = PlutusV2Script
                .builder()
                .cborHex(cborHex)
                .build();

        String address = AddressProvider.getEntAddress(plutusV2Script, Networks.testnet()).toBech32();
        System.out.println(address);
        assertThat(address).isEqualTo("addr_test1wqc0qxcdrrk63df4zurraavkq6pglpg0ur73zdn6398xuwsrt3mje");
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
