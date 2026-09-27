package org.example.collectors.impl;

import org.example.Snapshot;
import org.example.collectors.CollectorUtils;
import org.example.collectors.MetricsCollector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

class ThreadState {
    final AtomicLongArray buckets = new AtomicLongArray(256);
    final AtomicLong sum = new AtomicLong(0);
    final AtomicLong count = new AtomicLong(0);
    final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
    final AtomicLong max = new AtomicLong(Long.MIN_VALUE);
}

public class ThreadLocalSharedLockCollector implements MetricsCollector {
    private final List<ThreadState> allStates = new ArrayList<>();
    private final Object threadMonitor = new Object();
    private final ThreadLocal<ThreadState> localState = ThreadLocal.withInitial(() -> {
        ThreadState state = new ThreadState();
        synchronized (threadMonitor) {
            allStates.add(state);
        }
        return state;
    });

    @Override
    public void record(long value) {
        ThreadState s = localState.get();
        int b = (int) Math.min(value / 4, 255);

        s.buckets.setRelease(b, s.buckets.getPlain(b) + 1);
        s.count.setRelease(s.count.getPlain() + 1);
        s.sum.setRelease(s.sum.getPlain() + value);

        // min и max обновляются обычным сравнением без CAS:
        if (value < s.min.getPlain()) s.min.setRelease(value);
        if (value > s.max.getPlain()) s.max.setRelease(value);
    }

    @Override
    public Snapshot snapshot() {
        List<ThreadState> copyOfStates;
        synchronized (threadMonitor) {
            copyOfStates = List.copyOf(allStates);
        }
        long[] out = new long[256];
        long count = 0, sum = 0, min = Long.MAX_VALUE, max = 0;

        for (ThreadState s : copyOfStates) {
            for (int i = 0; i < 256; i++) out[i] += s.buckets.get(i);
            count += s.count.get();

            sum += s.sum.get();
            min = Math.min(min, s.min.get());
            max = Math.max(max, s.max.get());
        }

        long p50 = CollectorUtils.calcPercent(out, count, 0.50);
        long p99 = CollectorUtils.calcPercent(out, count, 0.99);
        return new Snapshot(out, count, sum, min, max, p50, p99);
    }
}
