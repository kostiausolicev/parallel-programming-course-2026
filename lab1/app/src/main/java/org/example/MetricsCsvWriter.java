package org.example;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

public final class MetricsCsvWriter implements AutoCloseable {
    private final BufferedWriter throughputWriter;
    private final String collectorName;

    public MetricsCsvWriter(Path outputDir, String collectorName) {
        this.collectorName = collectorName;
        try {
            Files.createDirectories(outputDir);
            throughputWriter = Files.newBufferedWriter(
                    outputDir.resolve(collectorName + "_throughput.csv"),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            throughputWriter.write("collector,threads,ops_per_sec\n");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create CSV writers in " + outputDir, e);
        }
    }

    public void writeThroughput(int threads, double opsPerSec) {
        try {
            throughputWriter.write(String.format(
                    Locale.US,
                    "%s,%d,%.6f%n",
                    collectorName,
                    threads,
                    opsPerSec
            ));
            throughputWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write throughput CSV", e);
        }
    }

    public static void writeConsistency(
            Path outputDir,
            String collectorName,
            int threads,
            long totalSnapshots,
            long sumLessSnapshots,
            long sumGreaterSnapshots,
            long expectedRecordCalls,
            long actualRecordCount
    ) {
        long brokenSnapshots = sumLessSnapshots + sumGreaterSnapshots;
        double brokenPercent = totalSnapshots == 0
                ? 0.0
                : brokenSnapshots * 100.0 / totalSnapshots;
        long countDelta = actualRecordCount - expectedRecordCalls;

        try {
            Files.createDirectories(outputDir);
            try (BufferedWriter writer = Files.newBufferedWriter(
                    outputDir.resolve(collectorName + "_consistency.csv"),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            )) {
                writer.write("collector,threads,total_snapshots,broken_snapshots,broken_percent,sum_less_count,sum_greater_count,expected_record_calls,actual_record_count,count_delta\n");
                writer.write(String.format(
                        Locale.US,
                        "%s,%d,%d,%d,%.6f,%d,%d,%d,%d,%d%n",
                        collectorName,
                        threads,
                        totalSnapshots,
                        brokenSnapshots,
                        brokenPercent,
                        sumLessSnapshots,
                        sumGreaterSnapshots,
                        expectedRecordCalls,
                        actualRecordCount,
                        countDelta
                ));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write consistency CSV in " + outputDir, e);
        }
    }

    @Override
    public void close() {
        try {
            throughputWriter.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to close CSV writers", e);
        }
    }
}
