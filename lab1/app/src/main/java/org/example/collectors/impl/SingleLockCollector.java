package org.example.collectors.impl;

import org.example.Snapshot;
import org.example.collectors.CollectorUtils;
import org.example.collectors.MetricsCollector;

import java.util.Arrays;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class SingleLockCollector implements MetricsCollector {
    private final long[] buckets = new long[256];
    private long sum = 0;
    private long count = 0;
    private long min = Integer.MAX_VALUE;
    private long max = Integer.MIN_VALUE;

    @Override
    public synchronized void record(long value) {
        int bucket = min((int) value / 4, 255);
        buckets[bucket]++;
        sum += value;
        count++;
        min = min(value, min);
        max = max(value, max);
    }

    @Override
    public synchronized Snapshot snapshot() {
        long[] bucketsSnapshot = Arrays.copyOf(buckets, 256);
        long min = this.min;
        long max = this.max;
        long count = this.count;
        long sum = this.sum;
        long p50 = CollectorUtils.calcPercent(bucketsSnapshot, count, 0.50);
        long p99 = CollectorUtils.calcPercent(bucketsSnapshot, count, 0.99);
        return new Snapshot(bucketsSnapshot, count, sum, min, max, p50, p99);
    }
}
