package com.myblog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.controller.AiController;
import com.myblog.dto.AiChatRequest;
import com.myblog.dto.AiTraceResponse;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real loopback provider; no model key, Redis, database or paid requests required. */
class AiGatewayServiceTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final BlogContentToolService content = mock(BlogContentToolService.class);
    private final AiTraceService traces = mock(AiTraceService.class);
    private final AtomicInteger requests = new AtomicInteger();
    private final CountDownLatch providerStarted = new CountDownLatch(1);
    private final CountDownLatch providerClosed = new CountDownLatch(1);
    private HttpServer server;
    private ExecutorService providerThreads;
    private AiGatewayRuntime runtime;
    private AiGatewayService service;

    private void setup(long timeout, HttpHandler handler) throws Exception {
        setup(timeout, handler, 1, 1);
    }

    private void setup(long timeout, HttpHandler handler, int workers, int queueCapacity) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        providerThreads = Executors.newCachedThreadPool();
        server.setExecutor(providerThreads);
        server.createContext("/chat/completions", exchange -> {
            requests.incrementAndGet();
            handler.handle(exchange);
        });
        server.start();
        runtime = new AiGatewayRuntime(workers, queueCapacity, timeout, 25);
        service = spy(new AiGatewayService(mapper, content, traces, runtime));
        ReflectionTestUtils.setField(service, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
        ReflectionTestUtils.setField(service, "apiKey", "fake-local-test-key");
        ReflectionTestUtils.setField(service, "model", "fake-test-model");
        when(content.retrieve(anyString(), any(), eq(3)))
                .thenReturn(new BlogContentToolService.RetrievalResult("context", List.of()));
    }

    private HttpHandler hangingBody() {
        return exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (var body = exchange.getResponseBody()) {
                body.write("data: {\"choices\":[{\"delta\":{\"content\":\"partial\"}}]}\n\n".getBytes(StandardCharsets.UTF_8));
                body.flush();
                providerStarted.countDown();
                while (!Thread.currentThread().isInterrupted()) {
                    body.write(": provider-heartbeat\n\n".getBytes(StandardCharsets.UTF_8));
                    body.flush();
                    Thread.sleep(20);
                }
            } catch (IOException closed) {
                providerClosed.countDown();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally { exchange.close(); }
        };
    }

    private RecordingEmitter open() {
        return open(new RecordingEmitter());
    }

    private RecordingEmitter open(RecordingEmitter emitter) {
        doReturn(emitter).when(service).createEmitter();
        AiChatRequest request = new AiChatRequest();
        request.setQuery("test");
        service.stream(request);
        return emitter;
    }

    @AfterEach
    void close() {
        if (runtime != null) runtime.close();
        if (server != null) server.stop(0);
        if (providerThreads != null) providerThreads.shutdownNow();
    }

    @Test
    void successAndRacingCallbacksRecordOneTerminal() throws Exception {
        setup(3000, exchange -> {
            byte[] response = ("data: {\"choices\":[{\"delta\":{\"content\":\"ok\"},\"finish_reason\":\"stop\"}]}\n\n"
                    + "data: [DONE]\n\n").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        RecordingEmitter emitter = open();
        assertTrue(emitter.completed.await(4, TimeUnit.SECONDS));
        emitter.timeout.run();
        emitter.error.accept(new IOException("closed"));
        emitter.completion.run();
        ArgumentCaptor<AiTraceResponse> recorded = ArgumentCaptor.forClass(AiTraceResponse.class);
        verify(traces, timeout(3000).times(1)).record(recorded.capture());
        assertEquals("success", recorded.getValue().getStatus());
        assertEquals(1, emitter.payloads.stream().filter("[DONE]"::equals).count());
        assertEquals(1, emitter.completionCount.get());
        assertTrue(recorded.getValue().getRequestId().startsWith("ai-"));
    }

    @Test
    void overloadIs503BeforeRetrievalAndQueuedCancellationReleasesCapacity() throws Exception {
        setup(5000, hangingBody());
        RecordingEmitter active = open();
        assertTrue(providerStarted.await(3, TimeUnit.SECONDS));
        RecordingEmitter queued = open();
        RecordingEmitter rejected = new RecordingEmitter();
        doReturn(rejected).when(service).createEmitter();
        AiChatRequest request = new AiChatRequest(); request.setQuery("overload");
        var exception = assertThrows(AiGatewayService.OverloadedException.class, () -> service.stream(request));
        var response = new AiController(service).overloaded(exception);
        assertEquals(503, response.getStatusCode().value());
        assertEquals("AI_GATEWAY_BUSY", response.getBody().code());
        assertEquals("5", response.getHeaders().getFirst("Retry-After"));
        verify(content, times(1)).retrieve(anyString(), any(), eq(3));
        assertEquals(1, requests.get());
        queued.error.accept(new IOException("queued client left"));
        assertTrue(queued.completed.await(2, TimeUnit.SECONDS));
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () -> {
            var slots = (Semaphore) ReflectionTestUtils.getField(runtime, "admissions");
            while (slots.availablePermits() == 0) Thread.sleep(5);
        });
        RecordingEmitter replacement = open();
        replacement.completion.run();
        active.error.accept(new IOException("active client left"));
        assertTrue(providerClosed.await(4, TimeUnit.SECONDS), "cancel must close the real provider body");
        assertEquals(1, requests.get(), "cancelled queued work must never invoke provider");
        assertEquals(0, rejected.payloads.size(), "rejected response must not open SSE");
    }

    @Test
    void deadlineClosesHangingBodyAndRecordsTimeoutOnce() throws Exception {
        setup(800, hangingBody());
        RecordingEmitter emitter = open();
        assertTrue(providerStarted.await(3, TimeUnit.SECONDS));
        assertTrue(emitter.completed.await(4, TimeUnit.SECONDS));
        assertTrue(providerClosed.await(4, TimeUnit.SECONDS));
        emitter.error.accept(new IOException("late close"));
        ArgumentCaptor<AiTraceResponse> recorded = ArgumentCaptor.forClass(AiTraceResponse.class);
        verify(traces, timeout(3000).times(1)).record(recorded.capture());
        assertEquals("timeout", recorded.getValue().getStatus());
        assertTrue(emitter.payloads.stream().anyMatch(value -> value instanceof AiGatewayService.AiError error
                && error.code().equals("AI_TIMEOUT")));
    }

    @Test
    void heartbeatDetectsSilentClientDisconnectAndCancelsProvider() throws Exception {
        setup(5000, hangingBody());
        RecordingEmitter emitter = open();
        assertTrue(providerStarted.await(3, TimeUnit.SECONDS));
        emitter.disconnectOnHeartbeat = true;
        assertTrue(emitter.completed.await(4, TimeUnit.SECONDS));
        assertTrue(providerClosed.await(4, TimeUnit.SECONDS));
        ArgumentCaptor<AiTraceResponse> recorded = ArgumentCaptor.forClass(AiTraceResponse.class);
        verify(traces, timeout(3000).times(1)).record(recorded.capture());
        assertEquals("cancelled", recorded.getValue().getStatus());
    }

    @Test
    void cancellationBeforeProviderHeadersInterruptsSend() throws Exception {
        setup(5000, exchange -> {
            providerStarted.countDown();
            try { Thread.sleep(4000); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            exchange.close();
        });
        RecordingEmitter emitter = open();
        assertTrue(providerStarted.await(3, TimeUnit.SECONDS));
        emitter.error.accept(new IOException("stop"));
        assertTrue(emitter.completed.await(1, TimeUnit.SECONDS));
        ArgumentCaptor<AiTraceResponse> recorded = ArgumentCaptor.forClass(AiTraceResponse.class);
        verify(traces, timeout(3000).times(1)).record(recorded.capture());
        assertEquals("cancelled", recorded.getValue().getStatus());
    }

    @Test
    void providerErrorBodyIsNotExposedInEventsOrTrace() throws Exception {
        setup(3000, exchange -> {
            byte[] body = "sk-sensitive Bearer secret-password internal-host full-system-prompt".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(401, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        RecordingEmitter emitter = open();
        assertTrue(emitter.completed.await(4, TimeUnit.SECONDS));
        ArgumentCaptor<AiTraceResponse> recorded = ArgumentCaptor.forClass(AiTraceResponse.class);
        verify(traces, timeout(3000).times(1)).record(recorded.capture());
        String serialized = mapper.writeValueAsString(emitter.payloads) + mapper.writeValueAsString(recorded.getValue());
        assertFalse(serialized.contains("sk-sensitive"));
        assertFalse(serialized.contains("secret-password"));
        assertFalse(serialized.contains("internal-host"));
        assertFalse(serialized.contains("full-system-prompt"));
        assertTrue(serialized.contains("AI_PROVIDER_UNAVAILABLE"));
        assertTrue(serialized.contains(recorded.getValue().getRequestId()));
    }

    @Test
    void providerErrorEventIsSanitizedAndPrematureEofIsNotSuccess() throws Exception {
        setup(3000, exchange -> {
            byte[] body = "data: {\"error\":{\"message\":\"sk-sensitive\"}}\n\n".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body); exchange.close();
        });
        RecordingEmitter emitter = open();
        assertTrue(emitter.completed.await(4, TimeUnit.SECONDS));
        assertFalse(mapper.writeValueAsString(emitter.payloads).contains("sk-sensitive"));
        assertTrue(emitter.payloads.stream().anyMatch(value -> value instanceof AiGatewayService.AiError));
        assertFalse(emitter.payloads.contains("[DONE]"));
    }

    @Test
    void blockedClientSendCannotPreventDeadlineFromClosingUpstreamOrExpiringQueuedWork() throws Exception {
        setup(800, hangingBody());
        RecordingEmitter blocked = new RecordingEmitter();
        blocked.blockContent = true;
        try {
            open(blocked);
            assertTrue(blocked.writeBlocked.await(3, TimeUnit.SECONDS));
            RecordingEmitter queued = open();
            assertTrue(providerClosed.await(3, TimeUnit.SECONDS), "upstream must close while emitter send is still blocked");
            assertEquals(1, blocked.releaseWrite.getCount(), "the test client is deliberately still blocked");
            assertTrue(queued.completed.await(3, TimeUnit.SECONDS), "a blocked client must not occupy the deadline clock");
            verify(traces, timeout(3000).times(2)).record(any(AiTraceResponse.class));
            assertEquals(1, requests.get());
        } finally {
            blocked.releaseWrite.countDown();
        }
        assertTrue(blocked.completed.await(3, TimeUnit.SECONDS));
        assertEquals(1, blocked.payloads.stream().filter(value -> value instanceof AiGatewayService.AiError).count());
    }

    @Test
    void blockedHeartbeatsNeverOccupyEitherDeadlineThread() throws Exception {
        setup(800, hangingBody(), 2, 2);
        RecordingEmitter first = new RecordingEmitter();
        RecordingEmitter second = new RecordingEmitter();
        first.blockHeartbeat = true;
        second.blockHeartbeat = true;
        try {
            open(first);
            open(second);
            assertTrue(first.writeBlocked.await(3, TimeUnit.SECONDS));
            assertTrue(second.writeBlocked.await(3, TimeUnit.SECONDS));
            verify(traces, timeout(3000).times(2)).record(argThat(trace -> "timeout".equals(trace.getStatus())));
            assertTrue(providerClosed.await(3, TimeUnit.SECONDS));
            assertEquals(1, first.releaseWrite.getCount());
            assertEquals(1, second.releaseWrite.getCount());
            assertEquals(2, requests.get());
        } finally {
            first.releaseWrite.countDown();
            second.releaseWrite.countDown();
        }
        assertTrue(first.completed.await(3, TimeUnit.SECONDS));
        assertTrue(second.completed.await(3, TimeUnit.SECONDS));
    }

    @Test
    void blockedTracePersistenceDoesNotDelayDeadlinesOrClientCompletion() throws Exception {
        setup(800, hangingBody());
        CountDownLatch traceEntered = new CountDownLatch(1);
        CountDownLatch releaseTrace = new CountDownLatch(1);
        doAnswer(invocation -> {
            traceEntered.countDown();
            releaseTrace.await();
            return null;
        }).when(traces).record(any(AiTraceResponse.class));
        try {
            RecordingEmitter active = open();
            assertTrue(providerStarted.await(3, TimeUnit.SECONDS));
            RecordingEmitter queued = open();
            assertTrue(traceEntered.await(3, TimeUnit.SECONDS));
            assertTrue(active.completed.await(3, TimeUnit.SECONDS));
            assertTrue(queued.completed.await(3, TimeUnit.SECONDS));
            assertTrue(providerClosed.await(3, TimeUnit.SECONDS));
            assertEquals(1, releaseTrace.getCount(), "trace storage is deliberately still blocked");
        } finally {
            releaseTrace.countDown();
        }
        verify(traces, timeout(3000).times(2)).record(any(AiTraceResponse.class));
    }

    @Test
    void successfulProviderChunksOnlyForwardTrustedFields() throws Exception {
        setup(3000, exchange -> {
            String data = "data: {\"debug\":\"sk-private\",\"internal_url\":\"http://private\","
                    + "\"choices\":[{\"delta\":{\"content\":\"hello\",\"provider_secret\":\"Bearer private\"},"
                    + "\"finish_reason\":\"stop\",\"private_metadata\":\"sk-private\"}],"
                    + "\"usage\":{\"total_tokens\":2,\"credentials\":\"sk-private\"}}\n\ndata: [DONE]\n\n";
            byte[] body = data.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body); exchange.close();
        });
        RecordingEmitter emitter = open();
        assertTrue(emitter.completed.await(4, TimeUnit.SECONDS));
        String result = mapper.writeValueAsString(emitter.payloads);
        assertFalse(result.contains("sk-private"));
        assertFalse(result.contains("Bearer private"));
        assertFalse(result.contains("http://private"));
        assertTrue(result.contains("hello"));
        String chunk = emitter.payloads.stream().filter(value -> value instanceof String text
                && text.startsWith("{\"choices\"" )).map(Object::toString).findFirst().orElseThrow();
        assertEquals(2, mapper.readTree(chunk).path("usage").path("total_tokens").asInt());
    }

    @Test
    void providerEofWithoutDoneRecordsIncompleteInsteadOfSuccess() throws Exception {
        setup(3000, exchange -> {
            byte[] body = "data: {\"choices\":[{\"delta\":{\"content\":\"partial\"}}]}\n\n".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body); exchange.close();
        });
        RecordingEmitter emitter = open();
        assertTrue(emitter.completed.await(4, TimeUnit.SECONDS));
        assertFalse(emitter.payloads.contains("[DONE]"));
        assertTrue(emitter.payloads.stream().anyMatch(value -> value instanceof AiGatewayService.AiError error
                && error.code().equals("AI_STREAM_INCOMPLETE")));
    }

    static class RecordingEmitter extends SseEmitter {
        final List<Object> payloads = new CopyOnWriteArrayList<>();
        final CountDownLatch completed = new CountDownLatch(1);
        final AtomicInteger completionCount = new AtomicInteger();
        volatile boolean disconnectOnHeartbeat;
        volatile boolean blockContent;
        volatile boolean blockHeartbeat;
        final CountDownLatch writeBlocked = new CountDownLatch(1);
        final CountDownLatch releaseWrite = new CountDownLatch(1);
        Runnable timeout;
        Runnable completion;
        Consumer<Throwable> error;
        @Override public void onTimeout(Runnable callback) { timeout = callback; }
        @Override public void onCompletion(Runnable callback) { completion = callback; }
        @Override public void onError(Consumer<Throwable> callback) { error = callback; }
        @Override public void send(SseEventBuilder event) throws IOException {
            for (var item : event.build()) {
                if (disconnectOnHeartbeat && item.getData().toString().contains("keepalive")) throw new IOException("client closed");
                if (blockContent && item.getData() instanceof String text && text.contains("\"content\":")
                        || blockHeartbeat && item.getData().toString().contains("keepalive")) {
                    writeBlocked.countDown();
                    boolean interrupted = false;
                    while (releaseWrite.getCount() > 0) {
                        try { releaseWrite.await(); }
                        catch (InterruptedException ex) { interrupted = true; }
                    }
                    if (interrupted) Thread.currentThread().interrupt();
                }
                payloads.add(item.getData());
            }
        }
        @Override public void complete() {
            completionCount.incrementAndGet();
            if (completion != null) completion.run();
            completed.countDown();
        }
    }
}
