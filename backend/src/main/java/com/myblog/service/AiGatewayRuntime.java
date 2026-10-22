package com.myblog.service;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Isolates blocking provider streams from MVC, commonPool and business async jobs. */
@Component
public class AiGatewayRuntime implements AutoCloseable {
    private final ThreadPoolExecutor workers;
    private final ThreadPoolExecutor completions;
    private final ThreadPoolExecutor telemetry;
    private final Semaphore admissions;
    private final ScheduledThreadPoolExecutor timer;
    private final HttpClient client;
    private final long timeoutMs;
    private final long heartbeatMs;

    public AiGatewayRuntime(@Value("${ai.gateway.workers:2}") int concurrency,
                            @Value("${ai.gateway.queue-capacity:8}") int queueCapacity,
                            @Value("${ai.gateway.timeout-ms:120000}") long timeoutMs,
                            @Value("${ai.gateway.heartbeat-ms:10000}") long heartbeatMs) {
        if (concurrency < 1 || queueCapacity < 0 || timeoutMs < 1 || heartbeatMs < 1) {
            throw new IllegalArgumentException("Invalid AI Gateway capacity or time budget");
        }
        this.timeoutMs = timeoutMs;
        this.heartbeatMs = heartbeatMs;
        BlockingQueue<Runnable> queue = queueCapacity == 0
                ? new SynchronousQueue<>() : new ArrayBlockingQueue<>(queueCapacity);
        workers = new ThreadPoolExecutor(concurrency, concurrency, 30, TimeUnit.SECONDS,
                queue, threads("ai-gateway-"), new ThreadPoolExecutor.AbortPolicy());
        int capacity = Math.addExact(concurrency, queueCapacity);
        admissions = new Semaphore(capacity);
        // Socket completion may block. It must never occupy the deadline clock.
        // Admission remains held until completion, so this queue is also bounded.
        // Every admitted client may independently stall its socket. A client must not
        // occupy the only response worker needed to finish an unrelated queued request.
        completions = new ThreadPoolExecutor(capacity, capacity, 30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(capacity), threads("ai-gateway-response-"),
                new ThreadPoolExecutor.AbortPolicy());
        telemetry = new ThreadPoolExecutor(concurrency, concurrency, 30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(capacity), threads("ai-gateway-trace-"),
                new ThreadPoolExecutor.AbortPolicy());
        timer = new ScheduledThreadPoolExecutor(2, threads("ai-gateway-clock-"));
        timer.setRemoveOnCancelPolicy(true);
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    private static ThreadFactory threads(String prefix) {
        AtomicInteger sequence = new AtomicInteger();
        return task -> {
            Thread thread = new Thread(task, prefix + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    void execute(FutureTask<Void> task) {
        if (!admissions.tryAcquire()) throw new RejectedExecutionException("AI Gateway is full");
        try { workers.execute(task); }
        catch (RejectedExecutionException failure) { admissions.release(); throw failure; }
    }
    void complete(Runnable task) { completions.execute(task); }
    void record(Runnable task) { telemetry.execute(task); }
    void release() { admissions.release(); }
    void remove(FutureTask<Void> task) { workers.remove(task); }
    HttpClient client() { return client; }
    long timeoutMs() { return timeoutMs; }
    ScheduledFuture<?> deadline(Runnable task) { return timer.schedule(task, timeoutMs, TimeUnit.MILLISECONDS); }
    ScheduledFuture<?> heartbeat(Runnable task) {
        return timer.scheduleWithFixedDelay(task, heartbeatMs, heartbeatMs, TimeUnit.MILLISECONDS);
    }

    @Override
    @PreDestroy
    public void close() {
        for (Runnable task : workers.shutdownNow()) {
            if (task instanceof Future<?> future) future.cancel(true);
        }
        timer.shutdownNow();
        completions.shutdownNow();
        telemetry.shutdownNow();
    }
}
