package com.mulesoft.connectors.bedrock.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyLong;

import java.io.IOException;
import java.io.PipedOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.mulesoft.connectors.bedrock.api.parameter.BedrockAgentsFilteringParameters;
import com.mulesoft.connectors.bedrock.api.parameter.BedrockAgentsMultipleFilteringParameters;
import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import com.mulesoft.connectors.bedrock.internal.support.IntegrationTestParamHelper;
import com.mulesoft.connectors.bedrock.internal.util.StreamingRetryUtility;
import org.mule.runtime.extension.api.exception.ModuleException;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentResponseHandler;
import software.amazon.awssdk.services.bedrockagentruntime.model.PayloadPart;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrievedReference;

/**
 * Unit tests for AgentServiceImpl async streaming error paths, writer thread edge cases, and private helper methods that are
 * difficult to exercise via MUnit integration tests.
 */
@DisplayName("AgentServiceImpl")
class AgentServiceImplTest {

  private AgentServiceImpl service;
  private BedrockConnection mockConnection;

  @BeforeEach
  void setUp() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    mockConnection = mock(BedrockConnection.class);
    when(mockConnection.getConnectionTimeoutMs()).thenReturn(30000);
    when(mockConnection.getRegion()).thenReturn("us-east-1");
    service = new AgentServiceImpl(config, mockConnection);
  }

  // ==================== handleStreamChunk ====================

  @Nested
  @DisplayName("handleStreamChunk")
  class HandleStreamChunk {

    @Test
    @DisplayName("skips processing when client is disconnected (L1147-1149)")
    void clientDisconnected_skipsProcessing() throws Exception {
      BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>();
      AtomicBoolean clientDisconnected = new AtomicBoolean(true);
      AtomicBoolean chunksReceived = new AtomicBoolean(false);
      AtomicBoolean sessionStartSent = new AtomicBoolean(false);
      AtomicInteger chunkCount = new AtomicInteger(0);
      AtomicLong lastChunkTime = new AtomicLong(System.currentTimeMillis());
      AtomicLong timeToFirstChunk = new AtomicLong(-1);

      PayloadPart chunk = PayloadPart.builder()
          .bytes(SdkBytes.fromUtf8String("test chunk"))
          .build();

      invokeHandleStreamChunk(
                              "alias", "agentId", "prompt", "session1", "req1", "corr1", "user1",
                              chunksReceived, sessionStartSent, System.currentTimeMillis(),
                              writeQueue, chunk, chunkCount, lastChunkTime, timeToFirstChunk, clientDisconnected);

      assertThat(writeQueue).isEmpty();
      assertThat(chunksReceived.get()).isFalse();
      assertThat(chunkCount.get()).isEqualTo(0);
    }

    @Test
    @DisplayName("processes chunk and queues SSE event for normal flow")
    void normalChunk_queuesEvent() throws Exception {
      BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>();
      AtomicBoolean clientDisconnected = new AtomicBoolean(false);
      AtomicBoolean chunksReceived = new AtomicBoolean(false);
      AtomicBoolean sessionStartSent = new AtomicBoolean(false);
      AtomicInteger chunkCount = new AtomicInteger(0);
      AtomicLong lastChunkTime = new AtomicLong(System.currentTimeMillis());
      AtomicLong timeToFirstChunk = new AtomicLong(-1);

      PayloadPart chunk = PayloadPart.builder()
          .bytes(SdkBytes.fromUtf8String("Hello world"))
          .build();

      invokeHandleStreamChunk(
                              "alias", "agentId", "prompt", "session1", "req1", "corr1", "user1",
                              chunksReceived, sessionStartSent, System.currentTimeMillis(),
                              writeQueue, chunk, chunkCount, lastChunkTime, timeToFirstChunk, clientDisconnected);

      assertThat(chunksReceived.get()).isTrue();
      assertThat(sessionStartSent.get()).isTrue();
      assertThat(chunkCount.get()).isEqualTo(1);
      // Queue should contain session-start + chunk events
      assertThat(writeQueue.size()).isGreaterThanOrEqualTo(2);

      String allEvents = drainQueue(writeQueue);
      assertThat(allEvents).contains("session-start");
      assertThat(allEvents).contains("chunk");
      assertThat(allEvents).contains("Hello world");
    }

    @Test
    @DisplayName("logs slow chunk timing when timeSinceLastChunk > 1s (L1177-1178)")
    void slowChunkTiming_logs() throws Exception {
      BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>();
      AtomicBoolean clientDisconnected = new AtomicBoolean(false);
      AtomicBoolean chunksReceived = new AtomicBoolean(true); // Already received first chunk
      AtomicBoolean sessionStartSent = new AtomicBoolean(true);
      AtomicInteger chunkCount = new AtomicInteger(1);
      // Set lastChunkTime to 2 seconds ago to trigger timeSinceLastChunk > 1000
      AtomicLong lastChunkTime = new AtomicLong(System.currentTimeMillis() - 2000);
      AtomicLong timeToFirstChunk = new AtomicLong(100);

      PayloadPart chunk = PayloadPart.builder()
          .bytes(SdkBytes.fromUtf8String("delayed chunk"))
          .build();

      invokeHandleStreamChunk(
                              "alias", "agentId", "prompt", "session1", "req1", "corr1", "user1",
                              chunksReceived, sessionStartSent, System.currentTimeMillis() - 5000,
                              writeQueue, chunk, chunkCount, lastChunkTime, timeToFirstChunk, clientDisconnected);

      assertThat(chunkCount.get()).isEqualTo(2);
      String allEvents = drainQueue(writeQueue);
      assertThat(allEvents).contains("delayed chunk");
    }

    @Test
    @DisplayName("handles queue size > 25 without error (L1188-1190)")
    void queueOverflow_logsAndContinues() throws Exception {
      BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>();
      // Pre-fill queue with 26 items to trigger queueSize > 25 logging
      for (int i = 0; i < 26; i++) {
        writeQueue.offer("event-" + i);
      }
      AtomicBoolean clientDisconnected = new AtomicBoolean(false);
      AtomicBoolean chunksReceived = new AtomicBoolean(true);
      AtomicBoolean sessionStartSent = new AtomicBoolean(true);
      AtomicInteger chunkCount = new AtomicInteger(5);
      AtomicLong lastChunkTime = new AtomicLong(System.currentTimeMillis());
      AtomicLong timeToFirstChunk = new AtomicLong(100);

      PayloadPart chunk = PayloadPart.builder()
          .bytes(SdkBytes.fromUtf8String("overflow chunk"))
          .build();

      invokeHandleStreamChunk(
                              "alias", "agentId", "prompt", "session1", "req1", "corr1", "user1",
                              chunksReceived, sessionStartSent, System.currentTimeMillis(),
                              writeQueue, chunk, chunkCount, lastChunkTime, timeToFirstChunk, clientDisconnected);

      // Chunk should still be queued despite queue size > 25
      assertThat(writeQueue.size()).isGreaterThan(26);
      assertThat(chunkCount.get()).isEqualTo(6);
    }
  }

  // ==================== handleStreamComplete ====================

  @Nested
  @DisplayName("handleStreamComplete")
  class HandleStreamComplete {

    @Test
    @DisplayName("skips completion and queues sentinel when client disconnected (L1231-1235)")
    void clientDisconnected_queuesSentinelOnly() throws Exception {
      BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>();
      AtomicBoolean clientDisconnected = new AtomicBoolean(true);
      AtomicBoolean chunksReceived = new AtomicBoolean(true);
      AtomicInteger chunkCount = new AtomicInteger(5);
      AtomicLong timeToFirstChunk = new AtomicLong(200);

      invokeHandleStreamComplete(
                                 "session1", "agentId", "alias", System.currentTimeMillis() - 1000,
                                 "req1", "corr1", "user1",
                                 chunkCount, timeToFirstChunk, chunksReceived, writeQueue, clientDisconnected);

      assertThat(writeQueue.size()).isEqualTo(1);
      assertThat(writeQueue.poll()).isEqualTo("__END_OF_STREAM__");
    }

    @Test
    @DisplayName("queues completion event and sentinel for normal flow")
    void normalCompletion_queuesCompletionAndSentinel() throws Exception {
      BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>();
      AtomicBoolean clientDisconnected = new AtomicBoolean(false);
      AtomicBoolean chunksReceived = new AtomicBoolean(true);
      AtomicInteger chunkCount = new AtomicInteger(3);
      AtomicLong timeToFirstChunk = new AtomicLong(150);

      invokeHandleStreamComplete(
                                 "session1", "agentId", "alias", System.currentTimeMillis() - 2000,
                                 "req1", "corr1", "user1",
                                 chunkCount, timeToFirstChunk, chunksReceived, writeQueue, clientDisconnected);

      // Should have completion event + sentinel
      assertThat(writeQueue.size()).isEqualTo(2);
      String completionEvent = writeQueue.poll();
      assertThat(completionEvent).contains("session-complete");
      assertThat(completionEvent).contains("completed");
      assertThat(completionEvent).contains("req1");
      assertThat(completionEvent).contains("corr1");
      assertThat(completionEvent).contains("user1");
      assertThat(writeQueue.poll()).isEqualTo("__END_OF_STREAM__");
    }

    @Test
    @DisplayName("queues completion event with queue size > 25 (L1256-1258, L1282-1284)")
    void queueOverflow_logsAndQueuesNormally() throws Exception {
      BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>();
      // Pre-fill with 26 items
      for (int i = 0; i < 26; i++) {
        writeQueue.offer("event-" + i);
      }
      AtomicBoolean clientDisconnected = new AtomicBoolean(false);
      AtomicBoolean chunksReceived = new AtomicBoolean(true);
      AtomicInteger chunkCount = new AtomicInteger(3);
      AtomicLong timeToFirstChunk = new AtomicLong(150);

      invokeHandleStreamComplete(
                                 "session1", "agentId", "alias", System.currentTimeMillis() - 2000,
                                 "req1", "corr1", "user1",
                                 chunkCount, timeToFirstChunk, chunksReceived, writeQueue, clientDisconnected);

      // 26 pre-filled + 1 completion + 1 sentinel
      assertThat(writeQueue.size()).isEqualTo(28);
    }
  }

  // ==================== writeChunkErrorEvent ====================

  @Nested
  @DisplayName("writeChunkErrorEvent")
  class WriteChunkErrorEvent {

    @Test
    @DisplayName("queues chunk-error SSE event to write queue (L1217-1220)")
    void queuesErrorEvent() throws Exception {
      BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>();
      IOException error = new IOException("chunk write failed");

      Method m = AgentServiceImpl.class.getDeclaredMethod("writeChunkErrorEvent",
                                                          IOException.class, BlockingQueue.class);
      m.setAccessible(true);
      m.invoke(service, error, writeQueue);

      assertThat(writeQueue.size()).isEqualTo(1);
      String event = writeQueue.poll();
      assertThat(event).contains("chunk-error");
      assertThat(event).contains("chunk write failed");
    }
  }

  // ==================== writeSessionStartEvent ====================

  @Nested
  @DisplayName("writeSessionStartEvent")
  class WriteSessionStartEvent {

    @Test
    @DisplayName("queues session-start event with queue > 25 items (L1209-1211)")
    void queueOverflow_logsAndQueues() throws Exception {
      BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>();
      for (int i = 0; i < 26; i++) {
        writeQueue.offer("event-" + i);
      }

      Method m = AgentServiceImpl.class.getDeclaredMethod("writeSessionStartEvent",
                                                          String.class, String.class, String.class, String.class,
                                                          String.class, String.class, String.class,
                                                          long.class, BlockingQueue.class);
      m.setAccessible(true);
      m.invoke(service, "alias", "agentId", "prompt", "session1",
               "req1", "corr1", "user1", 100L, writeQueue);

      // 26 pre-filled + 1 session-start
      assertThat(writeQueue.size()).isEqualTo(27);
    }
  }

  // ==================== createChunkJson ====================

  @Nested
  @DisplayName("createChunkJson")
  class CreateChunkJson {

    @Test
    @DisplayName("adds error field when chunk.bytes() throws (L1332-1333)")
    void exceptionFromBytes_addsErrorToJson() throws Exception {
      PayloadPart chunk = mock(PayloadPart.class);
      when(chunk.bytes()).thenThrow(new RuntimeException("bytes decode failure"));

      Method m = AgentServiceImpl.class.getDeclaredMethod("createChunkJson", PayloadPart.class);
      m.setAccessible(true);
      JSONObject result = (JSONObject) m.invoke(service, chunk);

      assertThat(result.getString("type")).isEqualTo("chunk");
      assertThat(result.has("timestamp")).isTrue();
      assertThat(result.getString("error")).contains("bytes decode failure");
    }

    @Test
    @DisplayName("creates valid JSON for normal chunk")
    void normalChunk_createsValidJson() throws Exception {
      PayloadPart chunk = PayloadPart.builder()
          .bytes(SdkBytes.fromUtf8String("Hello world"))
          .build();

      Method m = AgentServiceImpl.class.getDeclaredMethod("createChunkJson", PayloadPart.class);
      m.setAccessible(true);
      JSONObject result = (JSONObject) m.invoke(service, chunk);

      assertThat(result.getString("type")).isEqualTo("chunk");
      assertThat(result.getString("text")).isEqualTo("Hello world");
      assertThat(result.has("timestamp")).isTrue();
    }

    @Test
    @DisplayName("handles chunk with null bytes")
    void nullBytes_noTextField() throws Exception {
      PayloadPart chunk = PayloadPart.builder().build();

      Method m = AgentServiceImpl.class.getDeclaredMethod("createChunkJson", PayloadPart.class);
      m.setAccessible(true);
      JSONObject result = (JSONObject) m.invoke(service, chunk);

      assertThat(result.getString("type")).isEqualTo("chunk");
      assertThat(result.has("text")).isFalse();
    }
  }

  // ==================== closeQuietly ====================

  @Nested
  @DisplayName("closeQuietly")
  class CloseQuietly {

    @Test
    @DisplayName("handles null stream without throwing (L943-944)")
    void nullStream_doesNotThrow() throws Exception {
      Method m = AgentServiceImpl.class.getDeclaredMethod("closeQuietly", PipedOutputStream.class);
      m.setAccessible(true);
      Object result = m.invoke(service, (PipedOutputStream) null);
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("handles already-closed stream without throwing (L945-947)")
    void alreadyClosedStream_doesNotThrow() throws Exception {
      Method m = AgentServiceImpl.class.getDeclaredMethod("closeQuietly", PipedOutputStream.class);
      m.setAccessible(true);

      PipedOutputStream os = new PipedOutputStream();
      os.close();
      Object result = m.invoke(service, os);
      assertThat(result).isNull();
    }
  }

  // ==================== toDuration ====================

  @Nested
  @DisplayName("toDuration")
  class ToDuration {

    @Test
    @DisplayName("defaults to seconds when TimeUnit is null (L466)")
    void nullUnit_defaultsToSeconds() throws Exception {
      Method m = AgentServiceImpl.class.getDeclaredMethod("toDuration", int.class, TimeUnit.class);
      m.setAccessible(true);
      Duration result = (Duration) m.invoke(null, 30, (TimeUnit) null);
      assertThat(result).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("converts milliseconds correctly when unit is provided")
    void withUnit_convertsCorrectly() throws Exception {
      Method m = AgentServiceImpl.class.getDeclaredMethod("toDuration", int.class, TimeUnit.class);
      m.setAccessible(true);
      Duration result = (Duration) m.invoke(null, 5000, TimeUnit.MILLISECONDS);
      assertThat(result).isEqualTo(Duration.ofMillis(5000));
    }
  }

  // ==================== createErrorJson ====================

  @Nested
  @DisplayName("createErrorJson")
  class CreateErrorJson {

    @Test
    @DisplayName("produces valid JSON with error message, type, and timestamp")
    void producesValidJson() throws Exception {
      Method m = AgentServiceImpl.class.getDeclaredMethod("createErrorJson", Throwable.class);
      m.setAccessible(true);
      JSONObject result = (JSONObject) m.invoke(null, new IOException("test IO error"));

      assertThat(result.getString("error")).isEqualTo("test IO error");
      assertThat(result.getString("type")).isEqualTo("IOException");
      assertThat(result.has("timestamp")).isTrue();
    }
  }

  // ==================== formatSSEEvent ====================

  @Nested
  @DisplayName("formatSSEEvent")
  class FormatSSEEvent {

    @Test
    @DisplayName("produces correctly formatted SSE with id, event, and data")
    void producesCorrectFormat() throws Exception {
      Method m = AgentServiceImpl.class.getDeclaredMethod("formatSSEEvent", String.class, String.class);
      m.setAccessible(true);
      String result = (String) m.invoke(service, "test-event", "{\"key\":\"value\"}");

      assertThat(result).contains("id:");
      assertThat(result).contains("event: test-event");
      assertThat(result).contains("data: {\"key\":\"value\"}");
    }
  }

  // ==================== handleStreamChunk - queue full paths ====================

  @Nested
  @DisplayName("handleStreamChunk - queue full")
  class HandleStreamChunkQueueFull {

    @Test
    @DisplayName("drops chunk when queue is full (L1079-1080)")
    void queueFull_dropsChunkEvent() throws Exception {
      // Use a queue with capacity 1, pre-fill it so offer() returns false
      BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>(1);
      writeQueue.offer("existing-event");

      AtomicBoolean clientDisconnected = new AtomicBoolean(false);
      AtomicBoolean chunksReceived = new AtomicBoolean(true);
      AtomicBoolean sessionStartSent = new AtomicBoolean(true);
      AtomicInteger chunkCount = new AtomicInteger(1);
      AtomicLong lastChunkTime = new AtomicLong(System.currentTimeMillis());
      AtomicLong timeToFirstChunk = new AtomicLong(100);

      PayloadPart chunk = PayloadPart.builder()
          .bytes(SdkBytes.fromUtf8String("dropped chunk"))
          .build();

      invokeHandleStreamChunk(
                              "alias", "agentId", "prompt", "session1", "req1", "corr1", "user1",
                              chunksReceived, sessionStartSent, System.currentTimeMillis(),
                              writeQueue, chunk, chunkCount, lastChunkTime, timeToFirstChunk, clientDisconnected);

      // Queue still has only the original event — chunk was dropped
      assertThat(writeQueue.size()).isEqualTo(1);
      assertThat(writeQueue.peek()).isEqualTo("existing-event");
      // But chunk count was still incremented
      assertThat(chunkCount.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("drops session-start when queue is full on first chunk (L1096)")
    void queueFull_dropsSessionStartEvent() throws Exception {
      BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>(1);
      writeQueue.offer("existing-event");

      AtomicBoolean clientDisconnected = new AtomicBoolean(false);
      AtomicBoolean chunksReceived = new AtomicBoolean(false);
      AtomicBoolean sessionStartSent = new AtomicBoolean(false);
      AtomicInteger chunkCount = new AtomicInteger(0);
      AtomicLong lastChunkTime = new AtomicLong(System.currentTimeMillis());
      AtomicLong timeToFirstChunk = new AtomicLong(-1);

      PayloadPart chunk = PayloadPart.builder()
          .bytes(SdkBytes.fromUtf8String("first chunk"))
          .build();

      invokeHandleStreamChunk(
                              "alias", "agentId", "prompt", "session1", "req1", "corr1", "user1",
                              chunksReceived, sessionStartSent, System.currentTimeMillis(),
                              writeQueue, chunk, chunkCount, lastChunkTime, timeToFirstChunk, clientDisconnected);

      // session-start couldn't be queued, chunk also couldn't be queued
      assertThat(writeQueue.size()).isEqualTo(1);
      assertThat(sessionStartSent.get()).isTrue();
      assertThat(chunksReceived.get()).isTrue();
    }
  }

  // ==================== writeChunkErrorEvent - queue full ====================

  @Nested
  @DisplayName("writeChunkErrorEvent - queue full")
  class WriteChunkErrorEventQueueFull {

    @Test
    @DisplayName("drops chunk-error event when queue is full (L1104)")
    void queueFull_dropsChunkErrorEvent() throws Exception {
      BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>(1);
      writeQueue.offer("existing-event");

      Method m = AgentServiceImpl.class.getDeclaredMethod("writeChunkErrorEvent",
                                                          IOException.class, BlockingQueue.class);
      m.setAccessible(true);
      m.invoke(service, new IOException("test error"), writeQueue);

      // Queue still has only the original event — error was dropped
      assertThat(writeQueue.size()).isEqualTo(1);
      assertThat(writeQueue.peek()).isEqualTo("existing-event");
    }
  }

  // ==================== handleStreamComplete - queue full paths ====================

  @Nested
  @DisplayName("handleStreamComplete - queue full")
  class HandleStreamCompleteQueueFull {

    @Test
    @DisplayName("drops completion event when queue is full (L1136)")
    void queueFull_dropsCompletionEvent() throws Exception {
      BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>(2);
      writeQueue.offer("event-1");
      writeQueue.offer("event-2");

      AtomicBoolean clientDisconnected = new AtomicBoolean(false);
      AtomicBoolean chunksReceived = new AtomicBoolean(true);
      AtomicInteger chunkCount = new AtomicInteger(3);
      AtomicLong timeToFirstChunk = new AtomicLong(150);

      invokeHandleStreamComplete(
                                 "session1", "agentId", "alias", System.currentTimeMillis() - 1000,
                                 "req1", "corr1", "user1",
                                 chunkCount, timeToFirstChunk, chunksReceived, writeQueue, clientDisconnected);

      // Completion event was dropped, but sentinel was force-queued (drains one item to make room)
      // Original: [event-1, event-2] -> completion dropped -> sentinel: poll event-1, offer sentinel
      // Result: [event-2, __END_OF_STREAM__]
      assertThat(writeQueue.size()).isEqualTo(2);
      String first = writeQueue.poll();
      String second = writeQueue.poll();
      assertThat(second).isEqualTo("__END_OF_STREAM__");
    }

    @Test
    @DisplayName("forces sentinel when queue is full by draining one item (L1151-1154)")
    void queueFull_forcesSentinel() throws Exception {
      // Queue capacity 1, pre-fill it
      BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>(1);
      writeQueue.offer("blocking-event");

      AtomicBoolean clientDisconnected = new AtomicBoolean(false);
      AtomicBoolean chunksReceived = new AtomicBoolean(true);
      AtomicInteger chunkCount = new AtomicInteger(3);
      AtomicLong timeToFirstChunk = new AtomicLong(150);

      invokeHandleStreamComplete(
                                 "session1", "agentId", "alias", System.currentTimeMillis() - 1000,
                                 "req1", "corr1", "user1",
                                 chunkCount, timeToFirstChunk, chunksReceived, writeQueue, clientDisconnected);

      // Sentinel must be present — it was force-queued after draining
      assertThat(writeQueue.size()).isEqualTo(1);
      assertThat(writeQueue.poll()).isEqualTo("__END_OF_STREAM__");
    }

    @Test
    @DisplayName("queues completion-error and sentinel when completion JSON creation fails (L1140-1144)")
    void completionJsonException_queuesErrorAndSentinel() throws Exception {
      // Use a queue with plenty of space — we want the error path, not queue-full path
      BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>(100);
      AtomicBoolean clientDisconnected = new AtomicBoolean(false);
      AtomicBoolean chunksReceived = new AtomicBoolean(true);
      // null chunkCount will cause NPE in createCompletionJson
      AtomicLong timeToFirstChunk = new AtomicLong(150);

      invokeHandleStreamComplete(
                                 "session1", "agentId", "alias", System.currentTimeMillis() - 1000,
                                 "req1", "corr1", "user1",
                                 null, timeToFirstChunk, chunksReceived, writeQueue, clientDisconnected);

      // Should have completion-error + sentinel
      assertThat(writeQueue.size()).isGreaterThanOrEqualTo(1);
      String allEvents = drainQueue(writeQueue);
      assertThat(allEvents).contains("__END_OF_STREAM__");
    }

    @Test
    @DisplayName("forces sentinel when queue full during client disconnect (L1121)")
    void clientDisconnected_queueFull_forcesSentinel() throws Exception {
      BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>(1);
      writeQueue.offer("blocking-event");

      AtomicBoolean clientDisconnected = new AtomicBoolean(true);
      AtomicBoolean chunksReceived = new AtomicBoolean(true);
      AtomicInteger chunkCount = new AtomicInteger(3);
      AtomicLong timeToFirstChunk = new AtomicLong(150);

      invokeHandleStreamComplete(
                                 "session1", "agentId", "alias", System.currentTimeMillis() - 1000,
                                 "req1", "corr1", "user1",
                                 chunkCount, timeToFirstChunk, chunksReceived, writeQueue, clientDisconnected);

      // Sentinel was force-queued (drained blocking-event)
      assertThat(writeQueue.size()).isEqualTo(1);
      assertThat(writeQueue.poll()).isEqualTo("__END_OF_STREAM__");
    }
  }

  // ==================== writeSessionStartEvent - queue full ====================

  @Nested
  @DisplayName("writeSessionStartEvent - queue full")
  class WriteSessionStartEventQueueFull {

    @Test
    @DisplayName("throws IOException when queue is full")
    void queueFull_throwsIOException() throws Exception {
      BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>(1);
      writeQueue.offer("existing-event");

      Method m = AgentServiceImpl.class.getDeclaredMethod("writeSessionStartEvent",
                                                          String.class, String.class, String.class, String.class,
                                                          String.class, String.class, String.class,
                                                          long.class, BlockingQueue.class);
      m.setAccessible(true);

      Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(
                                                                        () -> m.invoke(service, "alias", "agentId", "prompt",
                                                                                       "session1",
                                                                                       "req1", "corr1", "user1", 100L,
                                                                                       writeQueue));
      assertThat(thrown).isInstanceOf(InvocationTargetException.class);
      assertThat(thrown.getCause())
          .isInstanceOf(IOException.class)
          .hasMessageContaining("Failed to queue session-start event");

      // Queue still has only the original event
      assertThat(writeQueue.size()).isEqualTo(1);
      assertThat(writeQueue.peek()).isEqualTo("existing-event");
    }
  }

  // ==================== buildKnowledgeBaseConfigs ====================

  @Nested
  @DisplayName("buildKnowledgeBaseConfigs")
  class BuildKnowledgeBaseConfigs {

    @Test
    @DisplayName("returns null when legacyParams is null (L297)")
    void legacyParamsNull_returnsNull() throws Exception {
      Method m = AgentServiceImpl.class.getDeclaredMethod("buildKnowledgeBaseConfigs",
                                                          BedrockAgentsFilteringParameters.class,
                                                          BedrockAgentsMultipleFilteringParameters.class);
      m.setAccessible(true);
      Object result = m.invoke(null, (Object) null, (Object) null);
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("returns null when legacyParams has empty knowledgeBaseId")
    void emptyKnowledgeBaseId_returnsNull() throws Exception {
      Method m = AgentServiceImpl.class.getDeclaredMethod("buildKnowledgeBaseConfigs",
                                                          BedrockAgentsFilteringParameters.class,
                                                          BedrockAgentsMultipleFilteringParameters.class);
      m.setAccessible(true);

      BedrockAgentsFilteringParameters legacyParams = new BedrockAgentsFilteringParameters();
      IntegrationTestParamHelper.setField(legacyParams, "knowledgeBaseId", "");
      Object result = m.invoke(null, legacyParams, (Object) null);
      assertThat(result).isNull();
    }
  }

  // ==================== convertToSdkSearchType ====================

  @Nested
  @DisplayName("convertToSdkSearchType")
  class ConvertToSdkSearchType {

    @Test
    @DisplayName("returns null for null input (L674)")
    void nullInput_returnsNull() throws Exception {
      Method m = AgentServiceImpl.class.getDeclaredMethod("convertToSdkSearchType",
                                                          BedrockAgentsFilteringParameters.SearchType.class);
      m.setAccessible(true);
      Object result = m.invoke(null, (Object) null);
      assertThat(result).isNull();
    }
  }

  // ==================== definePromptTemplate error path ====================

  @Nested
  @DisplayName("definePromptTemplate")
  class DefinePromptTemplate {

    @Test
    @DisplayName("throws ModuleException when SdkClientException occurs (L185-186)")
    void sdkClientException_throwsModuleException() {
      when(mockConnection.answerPrompt(any()))
          .thenThrow(SdkClientException.builder().message("network error").build());

      com.mulesoft.connectors.bedrock.internal.parameter.BedrockParameters params =
          IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.5f, 50);

      org.assertj.core.api.Assertions.assertThatThrownBy(
                                                         () -> service.definePromptTemplate(
                                                                                            "Instructions: {{instructions}}. Data: {{dataset}}.",
                                                                                            "Answer briefly.", "Sample data",
                                                                                            params))
          .isInstanceOf(org.mule.runtime.extension.api.exception.ModuleException.class);
    }
  }

  // ==================== buildRetrievedReferenceJson ====================

  @Nested
  @DisplayName("buildRetrievedReferenceJson")
  class BuildRetrievedReferenceJson {

    @Test
    @DisplayName("builds JSON with content, location, and metadata (L411-421)")
    void fullReference_buildsCompleteJson() throws Exception {
      RetrievedReference ref = mock(RetrievedReference.class, RETURNS_DEEP_STUBS);
      when(ref.content().text()).thenReturn("reference text");
      when(ref.location().toString()).thenReturn("s3://bucket/key");
      Map<String, software.amazon.awssdk.core.document.Document> metadata = new HashMap<>();
      metadata.put("source", software.amazon.awssdk.core.document.Document.fromString("test-doc"));
      when(ref.metadata()).thenReturn(metadata);

      Method m = AgentServiceImpl.class.getDeclaredMethod("buildRetrievedReferenceJson",
                                                          RetrievedReference.class);
      m.setAccessible(true);
      JSONObject result = (JSONObject) m.invoke(service, ref);

      assertThat(result.getString("content")).isEqualTo("reference text");
      assertThat(result.getString("location")).isEqualTo("s3://bucket/key");
      assertThat(result.has("metadata")).isTrue();
    }
  }

  // ==================== buildVectorSearchConfiguration - reranking ====================

  @Nested
  @DisplayName("buildVectorSearchConfiguration - reranking")
  class RerankingConfig {

    @Test
    @DisplayName("configures reranking with additionalModelRequestFields (L622-624) and null selectionMode (L637)")
    void rerankingWithAdditionalFields_nullSelectionMode() throws Exception {
      BedrockAgentsFilteringParameters.RerankingConfiguration reranking =
          new BedrockAgentsFilteringParameters.RerankingConfiguration();
      reranking.setModelArn("arn:aws:bedrock:us-east-1:123:inference-profile/test");
      reranking.setRerankingType("BEDROCK");
      Map<String, String> additionalFields = new HashMap<>();
      additionalFields.put("topK", "5");
      reranking.setAdditionalModelRequestFields(additionalFields);
      // selectionMode is null -> covers L637 (early return in applyMetadataRerankingConfiguration)

      Method m = AgentServiceImpl.class.getDeclaredMethod("buildVectorSearchConfiguration",
                                                          Integer.class,
                                                          BedrockAgentsFilteringParameters.SearchType.class,
                                                          BedrockAgentsFilteringParameters.RetrievalMetadataFilterType.class,
                                                          Map.class,
                                                          BedrockAgentsFilteringParameters.RerankingConfiguration.class);
      m.setAccessible(true);
      Object result = m.invoke(null, null, null, null, null, reranking);

      assertThat(result).isNotNull();
    }
  }

  // ==================== closeQuietly - IOException ====================

  @Nested
  @DisplayName("closeQuietly - IOException")
  class CloseQuietlyIOException {

    @Test
    @DisplayName("catches IOException from close and logs without propagating (L885-886)")
    void closeThrowsIOException_doesNotPropagate() throws Exception {
      PipedOutputStream mockOs = mock(PipedOutputStream.class);
      doThrow(new IOException("close failed")).when(mockOs).close();

      Method m = AgentServiceImpl.class.getDeclaredMethod("closeQuietly", PipedOutputStream.class);
      m.setAccessible(true);
      // Should not throw - IOException is caught and logged
      Object result = m.invoke(service, mockOs);
      assertThat(result).isNull();
    }
  }

  // ==================== handleStreamComplete - completion error with queue full ====================

  @Nested
  @DisplayName("handleStreamComplete - completion error with queue full")
  class CompletionErrorQueueFull {

    @Test
    @DisplayName("drops completion-error event when queue is full (L1144)")
    void completionError_queueFull_dropsErrorEvent() throws Exception {
      BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>(1);
      writeQueue.offer("existing-event");

      AtomicBoolean clientDisconnected = new AtomicBoolean(false);
      AtomicBoolean chunksReceived = new AtomicBoolean(true);
      // null chunkCount triggers NPE in createCompletionJson -> enters catch block
      AtomicLong timeToFirstChunk = new AtomicLong(150);

      invokeHandleStreamComplete(
                                 "session1", "agentId", "alias", System.currentTimeMillis() - 1000,
                                 "req1", "corr1", "user1",
                                 null, timeToFirstChunk, chunksReceived, writeQueue, clientDisconnected);

      // Completion-error dropped (queue full, L1144), sentinel force-queued (drains existing)
      assertThat(writeQueue.size()).isEqualTo(1);
      assertThat(writeQueue.poll()).isEqualTo("__END_OF_STREAM__");
    }
  }

  // ==================== streamBedrockResponse - client disconnect paths ====================

  @Nested
  @DisplayName("streamBedrockResponse - client disconnect")
  class StreamBedrockResponseClientDisconnect {

    @Test
    @DisplayName("logs when Bedrock completes but client already disconnected (L993)")
    void completedFuture_clientDisconnected() throws Exception {
      when(mockConnection.invokeAgent(
                                      any(InvokeAgentRequest.class), any(InvokeAgentResponseHandler.class), anyLong()))
          .thenReturn(CompletableFuture.completedFuture(null));

      BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>();
      AtomicBoolean clientDisconnected = new AtomicBoolean(true);

      invokeStreamBedrockResponse(writeQueue, clientDisconnected);
      // Method returns normally; L993 (debug log for client disconnected) was hit
    }

    @Test
    @DisplayName("suppresses error and force-queues sentinel when client disconnected (L1006-1012)")
    void failedFuture_clientDisconnected_queuesSentinel() throws Exception {
      when(mockConnection.invokeAgent(
                                      any(InvokeAgentRequest.class), any(InvokeAgentResponseHandler.class), anyLong()))
          .thenReturn(CompletableFuture.failedFuture(new RuntimeException("bedrock error")));

      BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>(1);
      writeQueue.offer("existing-event"); // fill queue to trigger L1008-1010 (force sentinel)
      AtomicBoolean clientDisconnected = new AtomicBoolean(true);

      invokeStreamBedrockResponse(writeQueue, clientDisconnected);

      // Error suppressed, sentinel force-queued (drained existing)
      assertThat(writeQueue.size()).isEqualTo(1);
      assertThat(writeQueue.poll()).isEqualTo("__END_OF_STREAM__");
    }
  }

  // ==================== streamBedrockResponseWithRetry - exception re-throw paths ====================

  @Nested
  @DisplayName("streamBedrockResponseWithRetry - exception paths")
  class StreamBedrockResponseWithRetryExceptions {

    @Test
    @DisplayName("wraps IOException in ModuleException with enhanced message")
    void ioException_wrappedInModuleException() throws Exception {
      when(mockConnection.invokeAgent(
                                      any(InvokeAgentRequest.class), any(InvokeAgentResponseHandler.class), anyLong()))
          .thenAnswer(invocation -> {
            throw new IOException("network error");
          });

      try {
        invokeStreamBedrockResponseWithRetry();
        org.assertj.core.api.Assertions.fail("Expected ModuleException");
      } catch (InvocationTargetException e) {
        assertThat(e.getCause()).isInstanceOf(ModuleException.class);
        assertThat(e.getCause().getMessage()).contains("network error");
        assertThat(e.getCause().getCause()).isInstanceOf(IOException.class);
      }
    }

    @Test
    @DisplayName("wraps InterruptedException in ModuleException with enhanced message")
    void interruptedException_wrappedInModuleException() throws Exception {
      when(mockConnection.invokeAgent(
                                      any(InvokeAgentRequest.class), any(InvokeAgentResponseHandler.class), anyLong()))
          .thenAnswer(invocation -> {
            throw new InterruptedException("interrupted");
          });

      try {
        invokeStreamBedrockResponseWithRetry();
        org.assertj.core.api.Assertions.fail("Expected ModuleException");
      } catch (InvocationTargetException e) {
        assertThat(e.getCause()).isInstanceOf(ModuleException.class);
        assertThat(e.getCause().getMessage()).contains("interrupted");
        assertThat(e.getCause().getCause()).isInstanceOf(InterruptedException.class);
      }
    }

    @Test
    @DisplayName("wraps RuntimeException in ModuleException with null message handling")
    void runtimeException_wrappedInModuleException() throws Exception {
      when(mockConnection.invokeAgent(
                                      any(InvokeAgentRequest.class), any(InvokeAgentResponseHandler.class), anyLong()))
          .thenThrow(new RuntimeException());

      try {
        invokeStreamBedrockResponseWithRetry();
        org.assertj.core.api.Assertions.fail("Expected ModuleException");
      } catch (InvocationTargetException e) {
        assertThat(e.getCause()).isInstanceOf(ModuleException.class);
        assertThat(e.getCause().getCause()).isInstanceOf(RuntimeException.class);
      }
    }
  }

  // ==================== streamBedrockResponse - sentinel retry failure ====================

  @Nested
  @DisplayName("streamBedrockResponse - sentinel retry failure on client disconnect")
  class StreamBedrockResponseSentinelRetryFailure {

    @Test
    @DisplayName("throws ModuleException when sentinel cannot be queued after drain (L1014)")
    @SuppressWarnings("unchecked")
    void sentinelRetryFailure_clientDisconnected() throws Exception {
      when(mockConnection.invokeAgent(
                                      any(InvokeAgentRequest.class), any(InvokeAgentResponseHandler.class), anyLong()))
          .thenReturn(CompletableFuture.failedFuture(new RuntimeException("bedrock error")));

      BlockingQueue<String> mockQueue = mock(BlockingQueue.class);
      when(mockQueue.offer(any())).thenReturn(false);
      when(mockQueue.poll()).thenReturn("drained-event");
      AtomicBoolean clientDisconnected = new AtomicBoolean(true);

      Throwable thrown =
          org.assertj.core.api.Assertions.catchThrowable(() -> invokeStreamBedrockResponse(mockQueue, clientDisconnected));
      assertThat(thrown).isInstanceOf(InvocationTargetException.class);
      assertThat(thrown.getCause())
          .isInstanceOf(ModuleException.class)
          .hasMessageContaining("Failed to queue end-of-stream sentinel");
    }
  }

  // ==================== handleStreamComplete - sentinel retry failure ====================

  @Nested
  @DisplayName("handleStreamComplete - sentinel retry failure")
  class HandleStreamCompleteSentinelRetryFailure {

    @Test
    @DisplayName("logs error when sentinel cannot be queued - client disconnected (L1129)")
    @SuppressWarnings("unchecked")
    void sentinelRetryFailure_clientDisconnected() throws Exception {
      BlockingQueue<String> mockQueue = mock(BlockingQueue.class);
      when(mockQueue.offer(any())).thenReturn(false);
      when(mockQueue.poll()).thenReturn("drained-event");
      AtomicBoolean clientDisconnected = new AtomicBoolean(true);

      invokeHandleStreamComplete(
                                 "session1", "agentId", "alias", System.currentTimeMillis() - 1000,
                                 "req1", "corr1", "user1",
                                 new AtomicInteger(5), new AtomicLong(150),
                                 new AtomicBoolean(true), mockQueue, clientDisconnected);
      // L1129 logger.error hit: mock queue always returns false for offer
    }

    @Test
    @DisplayName("logs error when sentinel cannot be queued - finally block (L1162)")
    @SuppressWarnings("unchecked")
    void sentinelRetryFailure_finallyBlock() throws Exception {
      BlockingQueue<String> mockQueue = mock(BlockingQueue.class);
      when(mockQueue.offer(any())).thenReturn(false);
      when(mockQueue.poll()).thenReturn("drained-event");
      AtomicBoolean clientDisconnected = new AtomicBoolean(false);

      invokeHandleStreamComplete(
                                 "session1", "agentId", "alias", System.currentTimeMillis() - 1000,
                                 "req1", "corr1", "user1",
                                 new AtomicInteger(5), new AtomicLong(150),
                                 new AtomicBoolean(true), mockQueue, clientDisconnected);
      // L1162 logger.error hit: mock queue always returns false for offer in finally block
    }
  }

  // ==================== submitWithCallerRunsOnRejection ====================

  @Nested
  @DisplayName("submitWithCallerRunsOnRejection")
  class SubmitWithCallerRunsOnRejection {

    @Test
    @DisplayName("submits task to scheduler when capacity is available")
    void happyPath_submitsToScheduler() throws Exception {
      org.mule.runtime.api.scheduler.Scheduler scheduler = mock(org.mule.runtime.api.scheduler.Scheduler.class);
      Future<?> mockFuture = mock(Future.class);
      doAnswer(invocation -> mockFuture).when(scheduler).submit(any(Runnable.class));

      AtomicBoolean taskRan = new AtomicBoolean(false);
      Future<?> future = invokeSubmit(() -> taskRan.set(true), scheduler);

      assertThat(future).isSameAs(mockFuture);
    }

    @Test
    @DisplayName("runs task on caller thread when scheduler rejects (backpressure)")
    void rejection_runsOnCallerThread() throws Exception {
      org.mule.runtime.api.scheduler.Scheduler scheduler = mock(org.mule.runtime.api.scheduler.Scheduler.class);
      when(scheduler.submit(any(Runnable.class))).thenThrow(new RejectedExecutionException("pool full"));

      Thread callerThread = Thread.currentThread();
      AtomicBoolean taskRan = new AtomicBoolean(false);
      AtomicBoolean ranOnCallerThread = new AtomicBoolean(false);

      Future<?> future = invokeSubmit(() -> {
        taskRan.set(true);
        ranOnCallerThread.set(Thread.currentThread() == callerThread);
      }, scheduler);

      assertThat(taskRan.get()).isTrue();
      assertThat(ranOnCallerThread.get()).isTrue();
      assertThat(future.isDone()).isTrue();
    }

    @Test
    @DisplayName("propagates exception when rejected task throws")
    void rejection_taskThrows_propagatesException() {
      org.mule.runtime.api.scheduler.Scheduler scheduler = mock(org.mule.runtime.api.scheduler.Scheduler.class);
      when(scheduler.submit(any(Runnable.class))).thenThrow(new RejectedExecutionException("pool full"));

      RuntimeException taskException = new RuntimeException("task failed");
      try {
        invokeSubmit(() -> {
          throw taskException;
        }, scheduler);
        assertThat(true).as("Expected InvocationTargetException").isFalse();
      } catch (Exception e) {
        assertThat(e).isInstanceOf(java.lang.reflect.InvocationTargetException.class);
        assertThat(e.getCause()).isSameAs(taskException);
      }
    }

    private Future<?> invokeSubmit(Runnable task,
                                   org.mule.runtime.api.scheduler.Scheduler scheduler)
        throws Exception {
      Method m = AgentServiceImpl.class.getDeclaredMethod("submitWithCallerRunsOnRejection",
                                                          Runnable.class, org.mule.runtime.api.scheduler.Scheduler.class);
      m.setAccessible(true);
      return (Future<?>) m.invoke(null, task, scheduler);
    }
  }

  // ==================== Helpers ====================

  private void invokeStreamBedrockResponse(BlockingQueue<String> writeQueue,
                                           AtomicBoolean clientDisconnected)
      throws Exception {
    Method m = AgentServiceImpl.class.getDeclaredMethod("streamBedrockResponse",
                                                        String.class, String.class, String.class,
                                                        boolean.class, boolean.class,
                                                        String.class, boolean.class, Integer.class,
                                                        java.util.List.class,
                                                        PipedOutputStream.class, AtomicBoolean.class,
                                                        AtomicBoolean.class, String.class,
                                                        String.class, String.class,
                                                        Integer.class, TimeUnit.class,
                                                        BlockingQueue.class, AtomicBoolean.class);
    m.setAccessible(true);
    m.invoke(service,
             "alias", "agentId", "prompt",
             false, false,
             "session1", false, (Integer) null,
             null,
             (PipedOutputStream) null, new AtomicBoolean(false),
             new AtomicBoolean(false), "req1",
             "corr1", "user1",
             (Integer) null, (TimeUnit) null,
             writeQueue, clientDisconnected);
  }

  private void invokeStreamBedrockResponseWithRetry() throws Exception {
    StreamingRetryUtility.RetryConfig retryConfig =
        new StreamingRetryUtility.RetryConfig(0, 1000L, true);

    Method m = AgentServiceImpl.class.getDeclaredMethod("streamBedrockResponseWithRetry",
                                                        String.class, String.class, String.class,
                                                        boolean.class, boolean.class,
                                                        String.class, boolean.class, Integer.class,
                                                        java.util.List.class,
                                                        PipedOutputStream.class, StreamingRetryUtility.RetryConfig.class,
                                                        AtomicBoolean.class, AtomicBoolean.class,
                                                        String.class, String.class, String.class,
                                                        Integer.class, TimeUnit.class,
                                                        BlockingQueue.class, AtomicBoolean.class);
    m.setAccessible(true);
    m.invoke(service,
             "alias", "agentId", "prompt",
             false, false,
             "session1", false, (Integer) null,
             null,
             (PipedOutputStream) null, retryConfig,
             new AtomicBoolean(false), new AtomicBoolean(false),
             "req1", "corr1", "user1",
             (Integer) null, (TimeUnit) null,
             new LinkedBlockingQueue<>(), new AtomicBoolean(false));
  }

  private void invokeHandleStreamChunk(
                                       String agentAliasId, String agentId, String prompt, String effectiveSessionId,
                                       String requestId, String correlationId, String userId,
                                       AtomicBoolean chunksReceived, AtomicBoolean sessionStartSent, long startTime,
                                       BlockingQueue<String> writeQueue, PayloadPart chunk,
                                       AtomicInteger chunkCount, AtomicLong lastChunkTime, AtomicLong timeToFirstChunk,
                                       AtomicBoolean clientDisconnected)
      throws Exception {
    Method m = AgentServiceImpl.class.getDeclaredMethod("handleStreamChunk",
                                                        String.class, String.class, String.class, String.class,
                                                        String.class, String.class, String.class,
                                                        AtomicBoolean.class, AtomicBoolean.class, long.class,
                                                        BlockingQueue.class, PayloadPart.class,
                                                        AtomicInteger.class, AtomicLong.class, AtomicLong.class,
                                                        AtomicBoolean.class);
    m.setAccessible(true);
    m.invoke(service, agentAliasId, agentId, prompt, effectiveSessionId,
             requestId, correlationId, userId,
             chunksReceived, sessionStartSent, startTime,
             writeQueue, chunk, chunkCount, lastChunkTime, timeToFirstChunk, clientDisconnected);
  }

  private void invokeHandleStreamComplete(
                                          String effectiveSessionId, String agentId, String agentAliasId, long startTime,
                                          String requestId, String correlationId, String userId,
                                          AtomicInteger chunkCount, AtomicLong timeToFirstChunk,
                                          AtomicBoolean chunksReceived, BlockingQueue<String> writeQueue,
                                          AtomicBoolean clientDisconnected)
      throws Exception {
    Method m = AgentServiceImpl.class.getDeclaredMethod("handleStreamComplete",
                                                        String.class, String.class, String.class, long.class,
                                                        String.class, String.class, String.class,
                                                        AtomicInteger.class, AtomicLong.class,
                                                        AtomicBoolean.class, BlockingQueue.class,
                                                        AtomicBoolean.class);
    m.setAccessible(true);
    m.invoke(service, effectiveSessionId, agentId, agentAliasId, startTime,
             requestId, correlationId, userId,
             chunkCount, timeToFirstChunk, chunksReceived, writeQueue, clientDisconnected);
  }

  private static String drainQueue(BlockingQueue<String> queue) {
    StringBuilder sb = new StringBuilder();
    String item;
    while ((item = queue.poll()) != null) {
      sb.append(item);
    }
    return sb.toString();
  }
}
