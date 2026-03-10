package com.mulesoft.connectors.bedrock.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PipedOutputStream;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.bedrockagentruntime.model.PayloadPart;

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

  // ==================== Helpers ====================

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
