package org.cardanofoundation.hydrapoc.generator;

public class RandomUtil {

    public static int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }
}
