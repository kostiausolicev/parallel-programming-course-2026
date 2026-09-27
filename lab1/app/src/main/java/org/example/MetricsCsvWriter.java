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

    @Override
    public void close() {
        try {
            throughputWriter.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to close CSV writers", e);
        }
    }
}
