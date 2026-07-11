package com.wimoor.ozon.ops.performance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.wimoor.ozon.ops.pojo.dto.OzonApiLogRecordCommand;
import com.wimoor.ozon.ops.pojo.dto.OzonOperationAuditRecordCommand;
import com.wimoor.ozon.ops.service.IOzonOpsService;
import com.wimoor.ozonops.OzonOpsTestApplication;

/**
 * Phase 7 - 性能测试
 *
 * 测试内容：
 * - 异步记录不阻塞主流程
 * - 批量插入性能
 * - 高并发场景
 */
@SpringBootTest(classes = OzonOpsTestApplication.class)
@ActiveProfiles("test")
class LoggingPerformanceTests {

    @Autowired
    private IOzonOpsService opsService;

    @Test
    void asyncLoggingDoesNotBlockMainFlow() throws InterruptedException {
        // 1. 记录开始时间
        long startTime = System.currentTimeMillis();

        // 2. 执行100次异步日志记录
        int iterations = 100;
        for (int i = 0; i < iterations; i++) {
            OzonApiLogRecordCommand command = new OzonApiLogRecordCommand(
                    "auth-perf-1",
                    "shop-perf-1",
                    "PRODUCT",
                    "LIST",
                    "/v1/product/list",
                    "POST",
                    "PRODUCT",
                    "product-" + i,
                    "{}",
                    "{}",
                    "SUCCESS",
                    null,
                    50L,
                    "perf-tester"
            );
            // 异步记录（不阻塞）
            CompletableFuture.runAsync(() -> opsService.recordApiLog(command));
        }

        // 3. 记录结束时间
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 4. 验证执行时间（异步应该很快完成）
        // 100次异步调用应该在1秒内完成（不等待实际写入）
        assertTrue(duration < 1000,
                "Async logging took " + duration + "ms, expected < 1000ms");

        // 等待异步任务完成
        Thread.sleep(2000);
    }

    @Test
    void batchInsertPerformance() {
        // 1. 准备批量数据
        int batchSize = 500;
        List<OzonApiLogRecordCommand> commands = new ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            commands.add(new OzonApiLogRecordCommand(
                    "auth-batch-1",
                    "shop-batch-1",
                    "STOCK",
                    "UPDATE",
                    "/v1/stock/update",
                    "POST",
                    "STOCK",
                    "stock-" + i,
                    "{}",
                    "{}",
                    "SUCCESS",
                    null,
                    80L,
                    "batch-tester"
            ));
        }

        // 2. 记录开始时间
        long startTime = System.currentTimeMillis();

        // 3. 批量插入
        commands.forEach(cmd -> opsService.recordApiLog(cmd));

        // 4. 记录结束时间
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 5. 验证性能（500条记录应在5秒内完成）
        assertTrue(duration < 5000,
                "Batch insert of " + batchSize + " records took " + duration + "ms, expected < 5000ms");

        System.out.println("Batch insert performance: " + batchSize + " records in " + duration + "ms");
        System.out.println("Throughput: " + (batchSize * 1000.0 / duration) + " records/sec");
    }

    @Test
    void highConcurrencyScenario() throws InterruptedException {
        // 1. 准备并发测试
        int threadCount = 20;
        int recordsPerThread = 50;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // 2. 记录开始时间
        long startTime = System.currentTimeMillis();

        // 3. 启动并发线程
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < recordsPerThread; i++) {
                        try {
                            OzonApiLogRecordCommand command = new OzonApiLogRecordCommand(
                                    "auth-concurrent-1",
                                    "shop-concurrent-1",
                                    "PRICE",
                                    "IMPORT",
                                    "/v1/price/import",
                                    "POST",
                                    "PRICE",
                                    "price-t" + threadId + "-" + i,
                                    "{}",
                                    "{}",
                                    "SUCCESS",
                                    null,
                                    100L,
                                    "concurrent-tester"
                            );
                            opsService.recordApiLog(command);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // 4. 等待所有线程完成
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // 5. 记录结束时间
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 6. 验证结果
        assertTrue(completed, "Concurrent test did not complete in time");

        int totalRecords = threadCount * recordsPerThread;
        System.out.println("Concurrent performance: " + totalRecords + " records in " + duration + "ms");
        System.out.println("Success: " + successCount.get() + ", Errors: " + errorCount.get());
        System.out.println("Throughput: " + (totalRecords * 1000.0 / duration) + " records/sec");

        // 验证成功率
        assertTrue(successCount.get() > totalRecords * 0.95,
                "Success rate too low: " + successCount.get() + "/" + totalRecords);
    }

    @Test
    void auditLoggingPerformance() {
        // 1. 准备数据
        int iterations = 300;
        List<OzonOperationAuditRecordCommand> commands = new ArrayList<>();

        for (int i = 0; i < iterations; i++) {
            commands.add(new OzonOperationAuditRecordCommand(
                    "auth-audit-perf-1",
                    "shop-audit-perf-1",
                    "PRODUCT_PUBLISH",
                    "PRODUCT",
                    "draft-" + i,
                    "Draft-" + i,
                    "{}",
                    "SUCCESS",
                    "Published",
                    "audit-perf-tester"
            ));
        }

        // 2. 记录开始时间
        long startTime = System.currentTimeMillis();

        // 3. 批量记录
        commands.forEach(cmd -> opsService.recordOperationAudit(cmd));

        // 4. 记录结束时间
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 5. 验证性能（300条记录应在3秒内完成）
        assertTrue(duration < 3000,
                "Audit logging of " + iterations + " records took " + duration + "ms, expected < 3000ms");

        System.out.println("Audit logging performance: " + iterations + " records in " + duration + "ms");
        System.out.println("Throughput: " + (iterations * 1000.0 / duration) + " records/sec");
    }

    @Test
    void mixedApiAndAuditLogging() throws InterruptedException {
        // 1. 混合API日志和审计日志
        int apiLogCount = 100;
        int auditLogCount = 100;
        CountDownLatch latch = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // 2. 记录开始时间
        long startTime = System.currentTimeMillis();

        // 3. 并发记录API日志
        executor.submit(() -> {
            try {
                for (int i = 0; i < apiLogCount; i++) {
                    opsService.recordApiLog(new OzonApiLogRecordCommand(
                            "auth-mixed-1",
                            "shop-mixed-1",
                            "FINANCE",
                            "SYNC",
                            "/v1/finance/sync",
                            "POST",
                            "FINANCE",
                            "finance-" + i,
                            "{}",
                            "{}",
                            "SUCCESS",
                            null,
                            120L,
                            "mixed-tester"
                    ));
                }
            } finally {
                latch.countDown();
            }
        });

        // 4. 记录审计日志
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < auditLogCount; i++) {
                        opsService.recordOperationAudit(new OzonOperationAuditRecordCommand(
                                "auth-mixed-1",
                                "shop-mixed-1",
                                "STOCK_UPDATE",
                                "STOCK",
                                "stock-" + i,
                                "Stock-" + i,
                                "{}",
                                "SUCCESS",
                                "Updated",
                                "mixed-tester"
                        ));
                    }
                } finally {
                    latch.countDown();
                }
            }
        });

        // 5. 等待完成
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // 6. 记录结束时间
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 7. 验证结果
        assertTrue(completed, "Mixed logging test did not complete in time");

        int totalRecords = apiLogCount + auditLogCount;
        System.out.println("Mixed logging performance: " + totalRecords + " records in " + duration + "ms");
        System.out.println("Throughput: " + (totalRecords * 1000.0 / duration) + " records/sec");
    }

    @Test
    void largePayloadHandling() {
        // 1. 创建大payload（超过截断阈值）
        String largePayload = buildRepeatedChar('x', 6000);

        // 2. 记录开始时间
        long startTime = System.currentTimeMillis();

        // 3. 记录日志
        OzonApiLogRecordCommand command = new OzonApiLogRecordCommand(
                "auth-large-1",
                "shop-large-1",
                "CHAT",
                "SEND",
                "/v1/chat/send",
                "POST",
                "CHAT",
                "chat-large-1",
                largePayload,
                largePayload,
                "SUCCESS",
                null,
                200L,
                "large-payload-tester"
        );

        opsService.recordApiLog(command);

        // 4. 记录结束时间
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 5. 验证处理时间（即使大payload也应快速完成）
        assertTrue(duration < 500,
                "Large payload logging took " + duration + "ms, expected < 500ms");

        System.out.println("Large payload handling: " + largePayload.length() + " chars in " + duration + "ms");
    }

    private String buildRepeatedChar(char ch, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(ch);
        }
        return builder.toString();
    }
}
