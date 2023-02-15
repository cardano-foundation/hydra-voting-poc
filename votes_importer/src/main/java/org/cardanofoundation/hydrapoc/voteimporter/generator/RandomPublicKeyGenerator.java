package org.cardanofoundation.hydrapoc.voteimporter.generator;

import com.bloxbean.cardano.client.crypto.KeyGenUtil;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.util.HexUtil;
import org.cardanofoundation.hydrapoc.voteimporter.model.Voter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class RandomPublicKeyGenerator {

    public List<Voter> getRandomPublicKey(int n) throws Exception {
        List<Voter> publicKeys = new ArrayList<>();
        IntStream.range(0, n).forEach(
                value -> {
                    try {
                        String pubKey = HexUtil.encodeHexString(KeyGenUtil.generateKey().getVkey().getBytes());
                        publicKeys.add(new Voter(pubKey, RandomUtil.getRandomNumber(500, 10000000)));
                    } catch (CborSerializationException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        return publicKeys;
    }
}
