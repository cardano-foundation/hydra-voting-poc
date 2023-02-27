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
import org.cardanofoundation.hydrapoc.batch.VoteBatchReducer;
import org.cardanofoundation.hydrapoc.batch.VoteBatcher;
import org.cardanofoundation.hydrapoc.batch.VoteUtxoFinder;
import org.cardanofoundation.hydrapoc.batch.data.output.ResultBatchDatum;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
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

    //Run the following tests in sequence to import / create batch / reduce batch step

    //1. Generate 20 votes
    @Test
    public void generateVotes() throws Exception {
        command.generateVotes(3, 20, "votes.json");
    }

    //2. Import 20 votes from votes.json to script address
    @Test
    public void importVotesFromFile() throws Exception {
        List<Vote> votes = randomVoteGenerator.getVotes(0, 5, "votes.json");
        voteImporter.importVotes(votes);
    }

    //3. Create a batch of 5 votes --> 1 batch
    //Run this test multiple times to create multiple batches
    @Test
    public void createAndPostBatch() throws Exception {
        voteBatcher.createAndPostBatchTransaction(5);
    }

    //4. Reduce batch of 5 to 1
    //Run this test multiple times to reduce batches to 1 batch
    @Test
    public void reduceBatch() throws Exception {
        voteBatchReducer.postReduceBatchTransaction(5);
    }

    @Test
    public void getVoteBatches() {
        command.getVoteBatches(10);
    }
    //End


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
        String compileCode = "5902a30100003232323232323232323232222533300632323232533300a3370e00290000991919299980699b873232300100330010012253330140011480004c8cdc02400466006006002602e00290050a5114a0646464646464646600c00246602a64a66602a66e1c005200013232323232323232323253330243027002149854cc085241364c6973742f5475706c652f436f6e73747220636f6e7461696e73206d6f7265206974656d73207468616e2069742065787065637465640016375a604a002604a0046eb4c08c004c08c008dd6981080098108011bad301f001301f002375c603a002601e0042a6602e92012b436f6e73747220696e64657820646964206e6f74206d6174636820616e7920747970652076617269616e740016301737540020026600600246464a66602a66e1c0052004132323374a90001980e800a5eb80c074004c03c0084cdd2a400497ae0301737540020026600800246601660186601660180029001240086eb0cc024c028cc024c02802d200048000c0040048894ccc05800852f5c0264646464a66602a66e1c00520021333007007003005132323301d001333009009005007301d001300f002301737540026006004603400660300046002002444a666028004297ae01323233017300300233300500500100330180033016002375c6024002600800c2940c030dd5002980119299980499b8748008c030dd500088008a99805a4812a4173736572746564206f6e20696e636f727265637420636f6e7374727563746f722076617269616e742e00163300130020034800888c8ccc0040052000003222333300c3370e008004026466600800866e0000d200230150010012300b37540022930b180080091129998048010a4c26600a600260160046660060066018004002ae695cdab9c5573aaae7955cfaba05742ae881";

        ByteString bs = new ByteString(HexUtil.decodeHexString(compileCode));
        String cborHex = HexUtil.encodeHexString(CborSerializationUtil.serialize(bs));
        System.out.println(cborHex);

        PlutusV2Script plutusV2Script = PlutusV2Script
                .builder()
                .cborHex(cborHex)
                .build();

        String address = AddressProvider.getEntAddress(plutusV2Script, Networks.testnet()).toBech32();
        System.out.println(address);
        assertThat(address).isEqualTo("addr_test1wpj74hek9qt8uscvdr25vhxqs2vfsty97ga3a3cp8dytxjqkuk596");
    }

}
