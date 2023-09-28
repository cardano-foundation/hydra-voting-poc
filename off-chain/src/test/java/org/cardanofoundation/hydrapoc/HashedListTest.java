package org.cardanofoundation.hydrapoc;

import com.bloxbean.cardano.client.plutus.api.PlutusObjectConverter;
import com.bloxbean.cardano.client.plutus.impl.DefaultPlutusObjectConverter;
import com.bloxbean.cardano.client.util.HexUtil;
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
  "voterKey" : "aPt032SvG7RVntWx93dRWtmyWZI/oWGPNUmSusEv0Uo=",
  "votingPower" : 9631517,
  "challenge" : 192691745,
  "proposal" : 1426925,
  "choice" : 1
}, {
  "voterKey" : "glTMowNHBiR1sos2adoqXkuTwWTtgdU0HPIp9rbwkWg=",
  "votingPower" : 1418415,
  "challenge" : 422503982,
  "proposal" : 5026403,
  "choice" : 2
}, {
  "voterKey" : "xhbbs1twrHs9Nl1GdiJcX52bYgHLsUEklpHz+BRI/zw=",
  "votingPower" : 7859940,
  "challenge" : 223135486,
  "proposal" : 2376972,
  "choice" : 2
} ]
            """;

    @Test
    public void test_001() throws JsonProcessingException {
        val items = objectMapper.readValue(votesJson, new TypeReference<List<VoteDatum>>() {});

        items.forEach(voteDatum -> {
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


        val hashedList = HashedList.create(items, (v) -> sha2_256(HexUtil.decodeHexString(PLUTUS_OBJECT_CONVERTER.toPlutusData(v).serializeToHex())));

        System.out.println(encodeHexString(hashedList.hash()));
    }

}
