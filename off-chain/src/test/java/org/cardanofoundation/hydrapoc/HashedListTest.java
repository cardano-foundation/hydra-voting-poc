package org.cardanofoundation.hydrapoc;

import com.bloxbean.cardano.client.plutus.api.PlutusObjectConverter;
import com.bloxbean.cardano.client.plutus.impl.DefaultPlutusObjectConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.cardanofoundation.hydrapoc.importvote.VoteDatum;
import org.cardanofoundation.list.HashedList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.bloxbean.cardano.client.util.HexUtil.encodeHexString;
import static org.cardanofoundation.util.Hashing.sha2_256;

public class HashedListTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final static PlutusObjectConverter PLUTUS_OBJECT_CONVERTER = new DefaultPlutusObjectConverter();

    private final String votesJson = """
            [ {
                    "voterKey" : "E9Y8mwRHjS/D4PlVP3kJSoMRHjrQPz93n1umpvN6xkc=",
                    "votingPower" : 5540767,
                    "challenge" : 258650178,
                    "proposal" : 2888851,
                    "choice" : 0
                    }, {
                    "voterKey" : "to6/Ff1uR99D+iwRwnNaTl41nPOvmvuTtfcy/5vvLB8=",
                    "votingPower" : 2604515,
                    "challenge" : 374145371,
                    "proposal" : 2675120,
                    "choice" : 0
                    }, {
                    "voterKey" : "Qbs5tHLTh1tud9ooQXguZe/7+FRHmCzRUpfrwoo0iAk=",
                    "votingPower" : 253840,
                    "challenge" : 72140352,
                    "proposal" : 3057554,
                    "choice" : 0
                    }, {
                    "voterKey" : "4ECxMuNvribHsNcnwuhZuFrla3FpsA4ATz0nzQ4/0/4=",
                    "votingPower" : 4654383,
                    "challenge" : 59549376,
                    "proposal" : 3637300,
                    "choice" : 1
                    }, {
                    "voterKey" : "GwJvm5sNFy38L6SkUFaf6M8QqDzithbrHc1feG6hmbA=",
                    "votingPower" : 3353282,
                    "challenge" : 214998680,
                    "proposal" : 4269173,
                    "choice" : 1
                    }, {
                    "voterKey" : "6I4lNR86I+vKHkyt92Cbagh/YdUlL4cK9d73V44btU4=",
                    "votingPower" : 4620080,
                    "challenge" : 116457209,
                    "proposal" : 617559,
                    "choice" : 2
                    }, {
                    "voterKey" : "E9Y8mwRHjS/D4PlVP3kJSoMRHjrQPz93n1umpvN6xkc=",
                    "votingPower" : 5540767,
                    "challenge" : 214998680,
                    "proposal" : 1123389,
                    "choice" : 0
                    }, {
                    "voterKey" : "bPbxSoMUXuRHwWXeW09Sn2a9VSKyaqjAOONS7QtqQbw=",
                    "votingPower" : 881560,
                    "challenge" : 258650178,
                    "proposal" : 442754,
                    "choice" : 2
                    }, {
                    "voterKey" : "to6/Ff1uR99D+iwRwnNaTl41nPOvmvuTtfcy/5vvLB8=",
                    "votingPower" : 2604515,
                    "challenge" : 72140352,
                    "proposal" : 5776693,
                    "choice" : 2
                    }, {
                    "voterKey" : "6I4lNR86I+vKHkyt92Cbagh/YdUlL4cK9d73V44btU4=",
                    "votingPower" : 4620080,
                    "challenge" : 59549376,
                    "proposal" : 1247152,
                    "choice" : 1
                    } ]
            """;

    @Test
    public void test_001() throws JsonProcessingException {
        val items = objectMapper.readValue(votesJson, new TypeReference<List<VoteDatum>>() {});

        val hashedList = HashedList.create(items, (v) -> sha2_256(PLUTUS_OBJECT_CONVERTER.toPlutusData(v).serializeToBytes()));

        System.out.println(encodeHexString(hashedList.hash()));
    }

}
