package org.example;

import java.util.Random;

public class SeqUtils {
    private static final int SIZE = 1 << 20;
    private static final int MAX_VALUE = 1023;
    private static final double EXPONENT = 1.15;
    private static final long SEED = 42L;

    private static final double[] CUMULATIVE = new double[MAX_VALUE];
    private static final long[] VALUES = new long[SIZE];

    static {
        double sum = 0.0;
        for (int k = 1; k <= MAX_VALUE; k++) {
            sum += 1.0 / Math.pow(k, EXPONENT);
        }

        double acc = 0.0;
        for (int k = 1; k <= MAX_VALUE; k++) {
            acc += (1.0 / Math.pow(k, EXPONENT)) / sum;
            CUMULATIVE[k - 1] = acc;
        }

        // Устраняем погрешность округления.
        CUMULATIVE[MAX_VALUE - 1] = 1.0;

        Random random = new Random(SEED);
        for (int i = 0; i < SIZE; i++) {
            VALUES[i] = (long) sample(random);
        }
    }

    public static long[] generateSequence() {
        return VALUES;
    }

    private static int sample(Random random) {
        double u = random.nextDouble();

        int left = 0;
        int right = CUMULATIVE.length - 1;

        while (left < right) {
            int mid = left + (right - left) / 2;

            if (u < CUMULATIVE[mid]) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }

        return left + 1;
    }
}
