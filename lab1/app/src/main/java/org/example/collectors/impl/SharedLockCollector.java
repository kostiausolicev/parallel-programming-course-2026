package org.example.collectors.impl;

import org.example.Snapshot;
import org.example.collectors.CollectorUtils;
import org.example.collectors.MetricsCollector;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.min;

public class SharedLockCollector implements MetricsCollector {
    private final long[] buckets = new long[256];
    private final AtomicLong sum = new AtomicLong(0);
    private final AtomicLong count = new AtomicLong(0);
    private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong max = new AtomicLong(Long.MIN_VALUE);

    private final Object[] locks = new Object[16];

    @Override
    public void record(long value) {
        int bucketI = min((int) value / 4, 255);
        int lockI = bucketI % 16;
        Object lock = locks[lockI];
        synchronized (lock) {
            buckets[bucketI]++;
        }
        sum.addAndGet(value);
        count.addAndGet(1);
        long old;
        do {
            old = min.get();
            if (value >= old) break;
        } while (!min.compareAndSet(old, value));
        do {
            old = max.get();
            if (value <= old) break;
        } while (!max.compareAndSet(old, value));
    }

    @Override
    public Snapshot snapshot() {
        long[] bucketsSnapshot = copyBucketsAtomicity();
        long min = this.min.get();
        long max = this.max.get();
        long count = this.count.get();
        long sum = this.sum.get();
        long p50 = CollectorUtils.calcPercent(bucketsSnapshot, count, 0.50);
        long p99 = CollectorUtils.calcPercent(bucketsSnapshot, count, 0.99);
        return new Snapshot(bucketsSnapshot, count, sum, min, max, p50, p99);
    }

    private long[] copyBucketsAtomicity() {
        long[] bucketsSnapshot = new long[buckets.length];
        for (int l = 0; l < locks.length; l++) {
            Object lock = locks[l];
            synchronized (lock) {
                for (int b = l; b < buckets.length; b += locks.length) {
                    bucketsSnapshot[b] = buckets[b];
                }
            }
        }
        return bucketsSnapshot;
    }
}
