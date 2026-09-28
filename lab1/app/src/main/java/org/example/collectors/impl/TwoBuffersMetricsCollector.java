package org.example.collectors.impl;

import org.example.Snapshot;
import org.example.collectors.CollectorUtils;
import org.example.collectors.MetricsCollector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class Buffer {
    final long[] buckets = new long[256];
    long sum = 0;
    long count = 0;
    long min = Long.MAX_VALUE;
    long max = Long.MIN_VALUE;

    void reset() {
        Arrays.fill(buckets, 0);
        sum = 0;
        count = 0;
        min = Long.MAX_VALUE;
        max = Long.MIN_VALUE;
    }
}

class ThreadBuffer {
    final Buffer[] buffers = new Buffer[2];
    final AtomicInteger inside = new AtomicInteger(-1);

    ThreadBuffer() {
        buffers[0] = new Buffer();
        buffers[1] = new Buffer();
    }
}

public class TwoBuffersMetricsCollector implements MetricsCollector {
    private final Object snapshotMonitor = new Object();
    private final List<ThreadBuffer> allBuffers = new ArrayList<>();
    private final AtomicInteger activeBuffer = new AtomicInteger(0);
    private final ThreadLocal<ThreadBuffer> localState = ThreadLocal.withInitial(() -> {
        ThreadBuffer buf = new ThreadBuffer();
        synchronized (snapshotMonitor) {
            allBuffers.add(buf);
        }
        return buf;
    });

    // сводка
    private final long[] buckets = new long[256];
    private long sum = 0;
    private long count = 0;
    private long min = Long.MAX_VALUE;
    private long max = Long.MIN_VALUE;

    @Override
    public void record(long value) {
        ThreadBuffer localBuffers = localState.get();
        int currentBuffer;
        while (true) {
            currentBuffer = activeBuffer.get();
            localBuffers.inside.set(currentBuffer);
            if (activeBuffer.get() == currentBuffer) {
                break;
            }
            localBuffers.inside.setRelease(-1);
            Thread.onSpinWait();
        }

        Buffer lb = localBuffers.buffers[currentBuffer];
        int b = (int) Math.min(value / 4, 255);
        lb.buckets[b]++;

        lb.count++;
        lb.sum += value;
        lb.max = Math.max(lb.max, value);
        lb.min = Math.min(lb.min, value);

        localBuffers.inside.setRelease(-1);
    }

    @Override
    public Snapshot snapshot() {
        synchronized (snapshotMonitor) {
            int old = activeBuffer.get();
            activeBuffer.set(1 - old);

            for (ThreadBuffer tb : allBuffers) {
                while (tb.inside.get() == old) {
                    Thread.onSpinWait();
                }

                Buffer buf = tb.buffers[old];
                this.sum += buf.sum;
                this.count += buf.count;
                this.min = Math.min(this.min, buf.min);
                this.max = Math.max(this.max, buf.max);

                for (int bi = 0; bi < buf.buckets.length; bi++) {
                    this.buckets[bi] += buf.buckets[bi];
                }

                buf.reset();
            }

            long[] bucketsSnapshot = this.buckets.clone();
            long p50 = CollectorUtils.calcPercent(bucketsSnapshot, count, 0.50);
            long p99 = CollectorUtils.calcPercent(bucketsSnapshot, count, 0.99);
            return new Snapshot(bucketsSnapshot, count, sum, min, max, p50, p99);
        }
    }
}
