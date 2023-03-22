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
  "voterKey" : "GwJvm5sNFy38L6SkUFaf6M8QqDzithbrHc1feG6hmbA=",
  "votingPower" : 3353282,
  "challenge" : 449181773,
  "proposal" : 2365581,
  "choice" : 0
}, {
  "voterKey" : "QZBkg0M2NoFcz0E1eqLNGs94/xEVn3KppbCBn7IjQUw=",
  "votingPower" : 5215698,
  "challenge" : 449181773,
  "proposal" : 1932430,
  "choice" : 0
}, {
  "voterKey" : "4ECxMuNvribHsNcnwuhZuFrla3FpsA4ATz0nzQ4/0/4=",
  "votingPower" : 4654383,
  "challenge" : 303167468,
  "proposal" : 4988558,
  "choice" : 2
}, {
  "voterKey" : "Qbs5tHLTh1tud9ooQXguZe/7+FRHmCzRUpfrwoo0iAk=",
  "votingPower" : 253840,
  "challenge" : 116457209,
  "proposal" : 2297573,
  "choice" : 2
}, {
  "voterKey" : "Qbs5tHLTh1tud9ooQXguZe/7+FRHmCzRUpfrwoo0iAk=",
  "votingPower" : 253840,
  "challenge" : 131249474,
  "proposal" : 588916,
  "choice" : 2
}, {
  "voterKey" : "Qbs5tHLTh1tud9ooQXguZe/7+FRHmCzRUpfrwoo0iAk=",
  "votingPower" : 253840,
  "challenge" : 303167468,
  "proposal" : 3301549,
  "choice" : 1
}, {
  "voterKey" : "GwJvm5sNFy38L6SkUFaf6M8QqDzithbrHc1feG6hmbA=",
  "votingPower" : 3353282,
  "challenge" : 374145371,
  "proposal" : 5342861,
  "choice" : 0
}, {
  "voterKey" : "4ECxMuNvribHsNcnwuhZuFrla3FpsA4ATz0nzQ4/0/4=",
  "votingPower" : 4654383,
  "challenge" : 214998680,
  "proposal" : 226531,
  "choice" : 0
}, {
  "voterKey" : "Qbs5tHLTh1tud9ooQXguZe/7+FRHmCzRUpfrwoo0iAk=",
  "votingPower" : 253840,
  "challenge" : 51052969,
  "proposal" : 3531179,
  "choice" : 2
}, {
  "voterKey" : "6I4lNR86I+vKHkyt92Cbagh/YdUlL4cK9d73V44btU4=",
  "votingPower" : 4620080,
  "challenge" : 282723914,
  "proposal" : 5558632,
  "choice" : 1
} ]
            """;

    @Test
    public void test_001() throws JsonProcessingException {
        val items = objectMapper.readValue(votesJson, new TypeReference<List<VoteDatum>>() {});

        items.stream().forEach(voteDatum -> {
            System.out.println(encodeHexString(voteDatum.getVoterKey()));
        });

        for (int i = 0; i < items.size(); i++) {
            val voteDatum = items.get(i);
            System.out.println(String.format("""
             let v%d = Vote(
                            #"%s",
                            %d,
                            %d,
                            %d,
                            %d,
                            )
                    """, i, encodeHexString(voteDatum.getVoterKey()), voteDatum.getVotingPower(), voteDatum.getChallenge(), voteDatum.getProposal(), voteDatum.getChoice()));
        }

        System.out.println("-----");


        val hashedList = HashedList.create(items, (v) -> sha2_256(PLUTUS_OBJECT_CONVERTER.toPlutusData(v).serializeToBytes()));

        System.out.println(encodeHexString(hashedList.hash()));
    }

}
