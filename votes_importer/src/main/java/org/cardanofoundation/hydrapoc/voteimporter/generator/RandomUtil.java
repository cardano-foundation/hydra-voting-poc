package org.cardanofoundation.hydrapoc.voteimporter.generator;

public class RandomUtil {

    public static long getRandomNumber(long min, long max) {
        return (long) ((Math.random() * (max - min)) + min);
    }
}
