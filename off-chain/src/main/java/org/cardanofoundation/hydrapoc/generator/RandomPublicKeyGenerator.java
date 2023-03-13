package org.cardanofoundation.hydrapoc.generator;

import com.bloxbean.cardano.client.crypto.KeyGenUtil;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import org.cardanofoundation.hydrapoc.model.Voter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class RandomPublicKeyGenerator {

    public List<Voter> getRandomPublicKey(int n) throws Exception {
        List<Voter> publicKeys = new ArrayList<>();
        IntStream.range(0, n).forEach(
                value -> {
                    try {
                        byte[] pubKey = KeyGenUtil.generateKey().getVkey().getBytes();
                        publicKeys.add(new Voter(pubKey, RandomUtil.getRandomNumber(500, 10000000)));
                    } catch (CborSerializationException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        return publicKeys;
    }
}
