package org.example.collectors.impl;

import org.example.Snapshot;
import org.example.collectors.MetricsCollector;

public class EmptyLockCollector implements MetricsCollector {

    @Override
    public synchronized void record(long value) {
    }

    @Override
    public Snapshot snapshot() {
        return null;
    }
}

