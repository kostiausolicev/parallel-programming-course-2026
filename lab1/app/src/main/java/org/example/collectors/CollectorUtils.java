package org.example.collectors;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class CollectorUtils {

    public static double measurePoint(
            int threadsCount,
            long[] values,
            MetricsCollector collector
    ) {
        run(threadsCount, values, collector, 5);
        double[] results = new double[5];
        for (int i = 0; i < 5; i++) {
            results[i] = run(threadsCount, values, collector, 5);
        }
        return calcMedian(results);
    }

    public static long calcPercent(long[] buckets, long count, double percent) {
        double threshold = count * percent;
        long counted = 0;
        for (int i = 0; i < 256; i++) {
            counted += buckets[i];
            if (counted >= threshold) {
                return i * 4;
            }
        }
        return 1020;
    }

    private static double run(int threadsCount, long[] values, MetricsCollector collector, int seconds) {
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch waiter = new CountDownLatch(threadsCount);
        final AtomicBoolean stop = new AtomicBoolean(false);
        final long[] ops = new long[threadsCount];
        for (int t = 0; t < threadsCount; t++) {
            final int _t = t;
            var thread = new Thread(() -> {
                int localCount = 0;
                int i = _t * 1000;
                try {
                    latch.await();
                    while (!stop.get()) {
                        collector.record(values[i]);
                        localCount++;
                        i = (i + 1) % values.length;
                    }
                    ops[_t] += localCount;
                    waiter.countDown();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            thread.start();
        }
        try {
            long start = System.nanoTime();
            latch.countDown();
            Thread.sleep(seconds * 1000L);
            stop.set(true);
            long finish = System.nanoTime();
            waiter.await();
            double count = (double) Arrays.stream(ops).sum();
            long delay = finish - start;

            return count * 1_000_000_000.0 / delay;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private static double calcMedian(double[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Массив не должен быть null или пустым");
        }

        double[] sorted = array.clone();
        java.util.Arrays.sort(sorted);

        int middle = sorted.length / 2;

        return sorted.length % 2 == 0
                ? sorted[middle - 1] / 2.0 + sorted[middle] / 2.0
                : sorted[middle];
    }
}
