package org.example;

import org.example.collectors.CollectorUtils;
import org.example.collectors.MetricsCollector;
import org.example.collectors.impl.*;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class App {

    private static final Path DEFAULT_METRICS_DIR = Path.of("build", "metrics");
    private static final List<Integer> threadsCount = List.of(1, 2, 4, 8, 10);

    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Usage: App <singleThread|singleLock|emptySingleLock|sharedLock|threadLocalSharedLock|twoBuffers|2buff|inconsistencyShared|inconsistencyThreadLocalShared|inconsistencyTwoBuffers|inconsistencyTwoBuffersNoCheck> [metricsOutputDir]");
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
            case "threadLocalSharedLock": {
                runThreadLocalSharedLockCollector(metricsOutputDir);
                break;
            }
            case "inconsistencyShared": {
                runInconsistencyTest("shared_lock", new SharedLockCollector(), metricsOutputDir);
                break;
            }
            case "inconsistencyThreadLocalShared": {
                runInconsistencyTest("thread_local_shared_lock", new ThreadLocalSharedLockCollector(), metricsOutputDir);
                break;
            }
            case "inconsistencyTwoBuffers": {
                runInconsistencyTest("two_buffers", new TwoBuffersMetricsCollector(), metricsOutputDir);
                break;
            }
            case "inconsistencyTwoBuffersNoCheck": {
                runInconsistencyTest("two_buffers_no_check", new TwoBuffersNoCheckMetricsCollector(), metricsOutputDir);
                break;
            }
            case "twoBuffers": {
                runTwoBuffersCollector(metricsOutputDir);
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

    private static void runThreadLocalSharedLockCollector(Path metricsOutputDir) {
        try (var csv = new MetricsCsvWriter(metricsOutputDir, "thread_local_shared_lock")) {
            var q = SeqUtils.generateSequence();
            for (int threads : threadsCount) {
                var singleThread = new ThreadLocalSharedLockCollector();
                var opSec = CollectorUtils.measurePoint(threads, q, singleThread);
                var snapshot = singleThread.snapshot();
                csv.writeThroughput(threads, opSec);
                System.out.printf("thread local shared lock (%d threads): %f op/sec\n", threads, opSec);
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

    private static void runTwoBuffersCollector(Path metricsOutputDir) {
        try (var csv = new MetricsCsvWriter(metricsOutputDir, "two_buffers")) {
            var q = SeqUtils.generateSequence();
            for (int threads : threadsCount) {
                var collector = new TwoBuffersMetricsCollector();
                var opSec = CollectorUtils.measurePoint(threads, q, collector);
                var snapshot = collector.snapshot();
                csv.writeThroughput(threads, opSec);
                System.out.printf("two buffers (%d threads): %f op/sec\n", threads, opSec);
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

    private static void runInconsistencyTest(String collectorName, MetricsCollector collector, Path metricsOutputDir) {
        final int threadsCount = 4;
        final int snapshotsCount = 10_000;
        CountDownLatch activeThread = new CountDownLatch(threadsCount);
        AtomicBoolean stop = new AtomicBoolean(false);
        long[] counts = new long[threadsCount];
        long[] q = SeqUtils.generateSequence();
        int[] indexes = new int[threadsCount];
        for (int i = 0; i < threadsCount; i++) {
            final int threadId = i;
            indexes[i] = threadId;
            new Thread(() -> {
                int localCount = 0;
                while (!stop.get()) {
                    collector.record(q[indexes[threadId]]);
                    indexes[threadId] = (indexes[threadId] + 10000) % q.length;
                    localCount++;
                }
                counts[threadId] = localCount;
                activeThread.countDown();
            }).start();
        }

        long sumLess = 0;
        long sumGreate = 0;

        for (int i = 0; i < snapshotsCount; i++) {
            Snapshot s = collector.snapshot();
            long count1 = Arrays.stream(s.buckets()).sum();
            long count2 = s.count();
            if (count1 < count2) {
                sumLess++;
            } else if (count1 > count2) {
                sumGreate++;
            }
        }
        stop.set(true);
        try {
            activeThread.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        long localRecordCall = Arrays.stream(counts).sum();
        long actualRecordCount = collector.snapshot().count();
        long brokenSnapshots = sumGreate + sumLess;
        double brokenPercent = brokenSnapshots * 100.0 / snapshotsCount;
        MetricsCsvWriter.writeConsistency(
                metricsOutputDir,
                collectorName,
                threadsCount,
                snapshotsCount,
                sumLess,
                sumGreate,
                localRecordCall,
                actualRecordCount
        );

        System.out.printf("expected count of method \"record\" calls: %d\n", localRecordCall);
        System.out.printf("fact count of method \"record\" calls: %d\n", actualRecordCount);
        System.out.println("===");
        System.out.printf("count of less snapshots: %d\n", sumLess);
        System.out.printf("count of greate snapshots: %d\n", sumGreate);
        System.out.printf("total count of broken snapshots: %d\n", brokenSnapshots);
        System.out.printf(Locale.US, "broken snapshots percent: %.6f%%%n", brokenPercent);
        System.out.printf("total count of snapshots: %d\n", snapshotsCount);
        System.out.printf("metrics file: %s\n", metricsOutputDir.resolve(collectorName + "_consistency.csv"));
    }
}
