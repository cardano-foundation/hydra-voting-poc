package org.cardanofoundation.hydrapoc.util;

import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.util.Tuple;
import org.cardanofoundation.hydrapoc.model.Vote;

import java.util.Comparator;
import java.util.function.Function;

public class MoreComparators {

    public static <T> Comparator<Tuple<Utxo, T>> createTxHashAndTransactionIndexComparator() {
        return Comparator.comparing((Function<Tuple<Utxo, T>, String>) t -> t._1.getTxHash())
                .thenComparing(t -> t._1.getOutputIndex());
    }

    public static <T> Comparator<Vote> createVoteComparator() {
        return Comparator.comparingInt(Vote::getChallenge).thenComparingInt(Vote::getProposal);
    }

}
