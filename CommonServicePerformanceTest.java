package com.service.impl;

import com.service.CommonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommonServicePerformanceTest {

    @Autowired
    private CommonService commonService;

    /**
     * 生成重复字符串（兼容Java 8）
     */
    private String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * 并发性能测试
     */
    @Test
    void testConcurrentPerformance() throws InterruptedException {
        System.out.println("\n🚀 并发性能测试");

        int threadCount = 10;      // 并发线程数
        int iterationsPerThread = 20;  // 每个线程执行次数
        int totalRequests = threadCount * iterationsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        ConcurrentLinkedQueue<Long> responseTimes = new ConcurrentLinkedQueue<>();

        // 准备测试数据 - 将params设为final
        final Map<String, Object> params = new HashMap<>();
        params.put("table", "meishixinxi");
        params.put("column", "meishifenlei");

        // 创建并发任务
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();  // 等待所有线程准备就绪

                    for (int j = 0; j < iterationsPerThread; j++) {
                        long startTime = System.nanoTime();

                        try {
                            commonService.selectGroup(params);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            System.err.println("线程 " + threadId + " 第 " + j + " 次执行失败: " + e.getMessage());
                        }

                        long endTime = System.nanoTime();
                        responseTimes.add(endTime - startTime);

                        // 模拟用户思考时间
                        try {
                            Thread.sleep(ThreadLocalRandom.current().nextInt(10, 50));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 开始测试
        System.out.println("🔄 启动 " + threadCount + " 个并发线程...");
        long testStartTime = System.currentTimeMillis();
        startLatch.countDown();  // 所有线程同时开始

        // 等待所有线程完成
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);

        long testEndTime = System.currentTimeMillis();
        long totalTestTime = testEndTime - testStartTime;

        // 关闭线程池
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 计算结果
        if (!completed) {
            System.out.println("⚠️  测试超时！");
        }

        // 计算统计数据
        long totalResponseTime = 0;
        long minResponseTime = Long.MAX_VALUE;
        long maxResponseTime = Long.MIN_VALUE;

        for (Long time : responseTimes) {
            totalResponseTime += time;
            minResponseTime = Math.min(minResponseTime, time);
            maxResponseTime = Math.max(maxResponseTime, time);
        }

        long avgResponseTime = responseTimes.isEmpty() ? 0 : totalResponseTime / responseTimes.size();
        double throughput = (double) successCount.get() / (totalTestTime / 1000.0);  // QPS

        System.out.println("\n📊 并发性能测试结果:");
        String line = repeatString("=", 80);
        System.out.println(line);
        System.out.println("总请求数: " + totalRequests);
        System.out.println("成功请求: " + successCount.get());
        System.out.println("失败请求: " + errorCount.get());
        System.out.println("成功率: " + String.format("%.2f%%",
                (double) successCount.get() / totalRequests * 100));
        System.out.println("测试总时间: " + totalTestTime + "ms");
        System.out.println("吞吐量(QPS): " + String.format("%.2f", throughput));
        System.out.println("平均响应时间: " + (avgResponseTime / 1_000_000.0) + "ms");
        System.out.println("最小响应时间: " + (minResponseTime / 1_000_000.0) + "ms");
        System.out.println("最大响应时间: " + (maxResponseTime / 1_000_000.0) + "ms");
        System.out.println(line);

        // 性能断言
        assertTrue(throughput > 10, "吞吐量应大于10 QPS，实际: " + throughput);
        assertTrue(avgResponseTime / 1_000_000.0 < 200,
                "平均响应时间应小于200ms，实际: " + (avgResponseTime / 1_000_000.0) + "ms");
        assertTrue(successCount.get() > totalRequests * 0.95,
                "成功率应大于95%，实际: " + ((double) successCount.get() / totalRequests * 100) + "%");
    }

    /**
     * 压力测试：逐渐增加并发数
     */
    @Test
    void testStressPerformance() throws InterruptedException {
        System.out.println("\n🚀 压力测试：逐步增加并发");

        int[] concurrencyLevels = {1, 5, 10, 20, 30};  // 并发级别
        int iterations = 50;  // 每个级别执行次数

        System.out.println("📈 压力测试结果:");
        String line = repeatString("=", 80);
        System.out.println(line);
        System.out.printf("%-12s %-12s %-12s %-12s %-12s%n",
                "并发数", "平均时间(ms)", "最小时间(ms)", "最大时间(ms)", "QPS");
        System.out.println(line);

        for (int concurrency : concurrencyLevels) {
            ExecutorService executor = Executors.newFixedThreadPool(concurrency);
            CountDownLatch latch = new CountDownLatch(concurrency);

            // 使用原子变量来避免同步块内的变量修改问题
            AtomicLong totalTime = new AtomicLong(0);
            AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
            AtomicLong maxTime = new AtomicLong(Long.MIN_VALUE);

            // 创建final参数
            final Map<String, Object> params = new HashMap<>();
            params.put("table", "meishixinxi");
            params.put("column", "meishifenlei");

            for (int i = 0; i < concurrency; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    for (int j = 0; j < iterations; j++) {
                        long startTime = System.nanoTime();
                        commonService.selectGroup(params);
                        long endTime = System.nanoTime();
                        long duration = endTime - startTime;

                        // 使用原子操作更新统计值
                        totalTime.addAndGet(duration);

                        // 更新最小值
                        long currentMin;
                        do {
                            currentMin = minTime.get();
                            if (duration >= currentMin) break;
                        } while (!minTime.compareAndSet(currentMin, duration));

                        // 更新最大值
                        long currentMax;
                        do {
                            currentMax = maxTime.get();
                            if (duration <= currentMax) break;
                        } while (!maxTime.compareAndSet(currentMax, duration));
                    }
                    latch.countDown();
                });
            }

            latch.await();
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            long avgTime = totalTime.get() / (concurrency * iterations);
            double qps = 1000.0 / (avgTime / 1_000_000.0);

            System.out.printf("%-12d %-12.3f %-12.3f %-12.3f %-12.1f%n",
                    concurrency,
                    avgTime / 1_000_000.0,
                    minTime.get() / 1_000_000.0,
                    maxTime.get() / 1_000_000.0,
                    qps);
        }

        System.out.println(line);
    }

    /**
     * 简单压力测试 - 循环调用
     */
    @Test
    void testSimpleStress() {
        System.out.println("\n🚀 简单压力测试");

        Map<String, Object> params = new HashMap<>();
        params.put("table", "meishixinxi");
        params.put("column", "meishifenlei");

        int iterations = 1000;
        long startTime = System.currentTimeMillis();

        int success = 0;
        int error = 0;

        for (int i = 0; i < iterations; i++) {
            try {
                commonService.selectGroup(params);
                success++;

                if (i % 100 == 0) {
                    System.out.println("  已执行 " + i + " 次请求");
                }
            } catch (Exception e) {
                error++;
                if (error <= 3) {  // 只打印前3个错误
                    System.err.println("  第 " + i + " 次请求失败: " + e.getMessage());
                }
            }
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        System.out.println("\n📊 简单压力测试结果:");
        System.out.println("总请求数: " + iterations);
        System.out.println("成功: " + success);
        System.out.println("失败: " + error);
        System.out.println("成功率: " + String.format("%.2f%%", (double) success / iterations * 100));
        System.out.println("总耗时: " + totalTime + "ms");
        System.out.println("平均耗时: " + (totalTime / (double) iterations) + "ms/请求");
        System.out.println("QPS: " + String.format("%.2f", iterations / (totalTime / 1000.0)));

        // 断言
        assertTrue(success > iterations * 0.95, "成功率应大于95%");
        assertTrue(totalTime < 30000, "总耗时应小于30秒");
    }

    /**
     * 内存使用测试
     */
    @Test
    void testMemoryUsage() {
        System.out.println("\n🚀 内存使用测试");

        // 记录初始内存
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // 建议垃圾回收
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        Map<String, Object> params = new HashMap<>();
        params.put("table", "meishixinxi");
        params.put("column", "meishifenlei");

        int iterations = 1000;

        System.out.println("  开始执行 " + iterations + " 次查询...");
        for (int i = 0; i < iterations; i++) {
            commonService.selectGroup(params);

            if (i % 200 == 0) {
                long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                long memoryIncrease = currentMemory - initialMemory;
                System.out.println("  第 " + i + " 次迭代，内存增加: " +
                        (memoryIncrease / 1024) + " KB");
            }
        }

        // 最终内存使用
        runtime.gc(); // 再次建议垃圾回收
        Thread.yield(); // 让出CPU时间片

        try {
            Thread.sleep(100); // 短暂等待，让GC有机会执行
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;

        System.out.println("\n📊 内存测试结果:");
        System.out.println("初始内存: " + (initialMemory / 1024) + " KB");
        System.out.println("最终内存: " + (finalMemory / 1024) + " KB");
        System.out.println("内存增加: " + (memoryIncrease / 1024) + " KB");
        System.out.println("内存增加: " + (memoryIncrease / 1024 / 1024.0) + " MB");

        // 断言：内存增长不应超过50MB
        assertTrue(memoryIncrease < 50 * 1024 * 1024,
                "内存增长不应超过50MB，实际: " + (memoryIncrease / 1024 / 1024.0) + " MB");
    }

    /**
     * 响应时间分布测试
     */
    @Test
    void testResponseTimeDistribution() {
        System.out.println("\n🚀 响应时间分布测试");

        Map<String, Object> params = new HashMap<>();
        params.put("table", "meishixinxi");
        params.put("column", "meishifenlei");

        int iterations = 500;
        int[] timeBuckets = new int[6]; // 0: <10ms, 1: 10-50ms, 2: 50-100ms, 3: 100-200ms, 4: 200-500ms, 5: >500ms

        System.out.println("  执行 " + iterations + " 次查询，统计响应时间分布...");

        // 预热
        for (int i = 0; i < 10; i++) {
            commonService.selectGroup(params);
        }

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            commonService.selectGroup(params);
            long endTime = System.nanoTime();

            long durationMs = (endTime - startTime) / 1_000_000;

            if (durationMs < 10) {
                timeBuckets[0]++;
            } else if (durationMs < 50) {
                timeBuckets[1]++;
            } else if (durationMs < 100) {
                timeBuckets[2]++;
            } else if (durationMs < 200) {
                timeBuckets[3]++;
            } else if (durationMs < 500) {
                timeBuckets[4]++;
            } else {
                timeBuckets[5]++;
            }

            if (i % 100 == 0) {
                System.out.println("  已完成 " + i + " 次查询");
            }
        }

        System.out.println("\n📊 响应时间分布:");
        System.out.println("  <10ms:    " + timeBuckets[0] + " (" +
                String.format("%.1f", timeBuckets[0] * 100.0 / iterations) + "%)");
        System.out.println("  10-50ms:  " + timeBuckets[1] + " (" +
                String.format("%.1f", timeBuckets[1] * 100.0 / iterations) + "%)");
        System.out.println("  50-100ms: " + timeBuckets[2] + " (" +
                String.format("%.1f", timeBuckets[2] * 100.0 / iterations) + "%)");
        System.out.println("  100-200ms:" + timeBuckets[3] + " (" +
                String.format("%.1f", timeBuckets[3] * 100.0 / iterations) + "%)");
        System.out.println("  200-500ms:" + timeBuckets[4] + " (" +
                String.format("%.1f", timeBuckets[4] * 100.0 / iterations) + "%)");
        System.out.println("  >500ms:   " + timeBuckets[5] + " (" +
                String.format("%.1f", timeBuckets[5] * 100.0 / iterations) + "%)");

        // 断言：90%的请求应在100ms内完成
        int fastRequests = timeBuckets[0] + timeBuckets[1] + timeBuckets[2];
        double percentage = fastRequests * 100.0 / iterations;
        assertTrue(percentage > 90, "90%的请求应在100ms内完成，实际: " +
                String.format("%.1f", percentage) + "%");
    }
}