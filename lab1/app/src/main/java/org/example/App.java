package org.example;

import org.example.collectors.CollectorUtils;
import org.example.collectors.impl.EmptyLockCollector;
import org.example.collectors.impl.SharedLockCollector;
import org.example.collectors.impl.SingleLockCollector;
import org.example.collectors.impl.SingleThreadCollector;

import java.nio.file.Path;
import java.util.List;

public class App {

    private static final Path DEFAULT_METRICS_DIR = Path.of("build", "metrics");
    private static final List<Integer> threadsCount = List.of(1, 2, 4, 8, 10);

    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Usage: App <singleThread|singleLock|emptySingleLock|sharedLock|2buff> [metricsOutputDir]");
        }

        var collectorType = args[0];
        Path metricsOutputDir = args.length > 1 ? Path.of(args[1]) : DEFAULT_METRICS_DIR;

        switch (collectorType) {
            case "singleThread": {
                runSingleThreadCollector(metricsOutputDir);
                break;
            }
            case "singleLock": {
                runSingleLockCollector(metricsOutputDir);
                break;
            }
            case "emptySingleLock": {
                runEmptySingleLockCollector(metricsOutputDir);
                break;
            }
            case "sharedLock": {
                runSharedLockCollector(metricsOutputDir);
                break;
            }
            case "2buff": {
                runSingleLockCollector(metricsOutputDir); // todo
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown collector type: " + collectorType);
        }
    }

    private static void runSingleThreadCollector(Path metricsOutputDir) {
        var q = SeqUtils.generateSequence();
        var singleThread = new SingleThreadCollector();
        var opSec = CollectorUtils.measurePoint(1, q, singleThread);
        var snapshot = singleThread.snapshot();
        try (var csv = new MetricsCsvWriter(metricsOutputDir, "single_thread")) {
            csv.writeThroughput(1, opSec);
        }
        System.out.printf("single thread: %f op/sec\n", opSec);
        System.out.printf("sum: %d\n\tcount: %d\n\tmin: %d\n\tmax: %d\n",
                snapshot.sum(),
                snapshot.count(),
                snapshot.min(),
                snapshot.max()
        );
        System.out.println("=====");
    }

    private static void runSingleLockCollector(Path metricsOutputDir) {
        try (var csv = new MetricsCsvWriter(metricsOutputDir, "single_lock")) {
            var q = SeqUtils.generateSequence();
            for (int threads : threadsCount) {
                var singleThread = new SingleLockCollector();
                var opSec = CollectorUtils.measurePoint(threads, q, singleThread);
                var snapshot = singleThread.snapshot();
                csv.writeThroughput(threads, opSec);
                System.out.printf("single lock (%d threads): %f op/sec\n", threads, opSec);
                System.out.printf("\tsum: %d\n\tcount: %d\n\tmin: %d\n\tmax: %d\n",
                        snapshot.sum(),
                        snapshot.count(),
                        snapshot.min(),
                        snapshot.max()
                );
                System.out.println("=====");
            }
        }
    }

    private static void runEmptySingleLockCollector(Path metricsOutputDir) {
        try (var csv = new MetricsCsvWriter(metricsOutputDir, "empty_single_lock")) {
            var q = SeqUtils.generateSequence();
            for (int threads : threadsCount) {
                var singleThread = new EmptyLockCollector();
                var opSec = CollectorUtils.measurePoint(threads, q, singleThread);
                csv.writeThroughput(threads, opSec);
                System.out.printf("empty lock (%d threads): %f op/sec\n", threads, opSec);
                System.out.println("=====");
            }
        }
    }

    private static void runSharedLockCollector(Path metricsOutputDir) {
        try (var csv = new MetricsCsvWriter(metricsOutputDir, "shared_lock")) {
            var q = SeqUtils.generateSequence();
            for (int threads : threadsCount) {
                var singleThread = new SharedLockCollector();
                var opSec = CollectorUtils.measurePoint(threads, q, singleThread);
                var snapshot = singleThread.snapshot();
                csv.writeThroughput(threads, opSec);
                System.out.printf("shared lock (%d threads): %f op/sec\n", threads, opSec);
                System.out.printf("\tsum: %d\n\tcount: %d\n\tmin: %d\n\tmax: %d\n",
                        snapshot.sum(),
                        snapshot.count(),
                        snapshot.min(),
                        snapshot.max()
                );
                System.out.println("=====");
            }
        }
    }
}
