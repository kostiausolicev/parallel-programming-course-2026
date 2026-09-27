package org.example.collectors;

import org.example.Snapshot;

public interface MetricsCollector {
    void record(long value);

    Snapshot snapshot();
}