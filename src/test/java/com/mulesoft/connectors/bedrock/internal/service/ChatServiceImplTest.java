package com.mulesoft.connectors.bedrock.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import org.mule.runtime.extension.api.exception.ModuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParameters;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamResponseHandler;
import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import com.mulesoft.connectors.bedrock.internal.support.IntegrationTestParamHelper;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@DisplayName("ChatServiceImpl")
class ChatServiceImplTest {

  @Test
  @DisplayName("can be constructed with config and connection")
  void constructor() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    assertThat(service).isNotNull();
  }

  @Test
  @DisplayName("implements ChatService and extends BedrockServiceImpl")
  void typeHierarchy() {
    ChatServiceImpl service = new ChatServiceImpl(mock(BedrockConfiguration.class), mock(BedrockConnection.class));
    assertThat(service).isInstanceOf(ChatService.class);
    assertThat(service).isInstanceOf(BedrockServiceImpl.class);
  }

  @Test
  @DisplayName("answerPrompt throws ModuleException when connection throws SdkClientException")
  void answerPromptThrowsWhenSdkClientException() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    when(connection.answerPrompt(any(InvokeModelRequest.class)))
        .thenThrow(software.amazon.awssdk.core.exception.SdkClientException.builder().message("network error").build());

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.5f, 50);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.answerPrompt("Hi", params))
        .isInstanceOf(ModuleException.class);
  }

  @Test
  @DisplayName("answerPrompt returns formatted response when connection returns success")
  void answerPromptReturnsFormattedResponse() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    InvokeModelResponse response = InvokeModelResponse.builder()
        .body(SdkBytes.fromUtf8String("{\"output\":{\"text\":\"Hello from Nova\"}}"))
        .build();
    when(connection.answerPrompt(any(InvokeModelRequest.class))).thenReturn(response);

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.5f, 50);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    String result = service.answerPrompt("Say hello.", params);

    assertThat(result).isNotBlank();
    assertThat(result).contains("output");
    assertThat(result).contains("Hello from Nova");
  }

  @Test
  @DisplayName("answerPrompt with anthropic model returns formatted response")
  void answerPromptAnthropic() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    String body =
        "{\"content\":[{\"text\":\"Hi from Claude\"}],\"usage\":{\"input_tokens\":1,\"output_tokens\":2},\"role\":\"assistant\"}";
    when(connection.answerPrompt(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder().body(SdkBytes.fromUtf8String(body)).build());

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("anthropic.claude-v2", 0.5f, 100);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    String result = service.answerPrompt("Hi", params);
    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("answerPrompt with amazon titan model returns formatted response")
  void answerPromptAmazonTitan() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    when(connection.answerPrompt(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder()
            .body(SdkBytes.fromUtf8String("{\"results\":[{\"outputText\":\"Hi\"}]}"))
            .build());

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.titan-text-express-v1", 0.5f, 100);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    String result = service.answerPrompt("Hi", params);
    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("answerPrompt with ai21 model returns formatted response")
  void answerPromptAi21() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    when(connection.answerPrompt(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder()
            .body(SdkBytes.fromUtf8String("{\"completions\":[{\"data\":{\"text\":\"Hi\"}}]}"))
            .build());

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("ai21.j2-mid-v1", 0.5f, 100);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    String result = service.answerPrompt("Hi", params);
    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("answerPrompt with mistral model returns formatted response")
  void answerPromptMistral() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    when(connection.answerPrompt(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder()
            .body(SdkBytes.fromUtf8String("{\"outputs\":[{\"text\":\"Hi\"}]}"))
            .build());

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("mistral.mistral-7b-instruct-v0:2", 0.5f, 100);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    String result = service.answerPrompt("Hi", params);
    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("answerPrompt with cohere model returns formatted response")
  void answerPromptCohere() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    when(connection.answerPrompt(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder()
            .body(SdkBytes.fromUtf8String("{\"text\":\"Hi\"}"))
            .build());

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("cohere.command-r-v1:0", 0.5f, 100);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    String result = service.answerPrompt("Hi", params);
    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("answerPrompt with meta llama model returns formatted response")
  void answerPromptMetaLlama() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    String body = "{\"generation\":\"Hi\",\"prompt_token_count\":1,\"generation_token_count\":2}";
    when(connection.answerPrompt(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder().body(SdkBytes.fromUtf8String(body)).build());

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("meta.llama3-70b-instruct-v1:0", 0.5f, 100);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    String result = service.answerPrompt("Hi", params);
    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("answerPromptStreaming returns InputStream when connection completes immediately")
  void answerPromptStreamingReturnsInputStream() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    when(connection.answerPromptStreaming(any(ConverseStreamRequest.class), any(ConverseStreamResponseHandler.class)))
        .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.5f, 50);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    InputStream stream = service.answerPromptStreaming("Hi", params);

    assertThat(stream).isNotNull();
  }

  @Test
  @DisplayName("private payload methods produce valid JSON when invoked via reflection")
  void privatePayloadMethodsProduceJson() throws Exception {
    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.titan-text-express-v1", 0.5f, 100);
    String prompt = "Hello";

    assertThat(invokePrivatePayloadMethod(ChatServiceImpl.class, "getAmazonTitanText", prompt, params))
        .contains("inputText").contains("Hello");
    assertThat(invokePrivatePayloadMethod(ChatServiceImpl.class, "getAmazonNovaText", prompt, params))
        .contains("messages").contains("inferenceConfig");
    assertThat(invokePrivatePayloadMethod(ChatServiceImpl.class, "getAnthropicClaudeText", prompt, params))
        .contains("prompt").contains("Human:");
    assertThat(invokePrivatePayloadMethod(ChatServiceImpl.class, "getAI21Text", prompt, params))
        .contains("prompt");
    assertThat(invokePrivatePayloadMethod(ChatServiceImpl.class, "getMistralAIText", prompt, params))
        .contains("prompt");
    assertThat(invokePrivatePayloadMethod(ChatServiceImpl.class, "getCohereText", prompt, params))
        .contains("prompt");
    assertThat(invokePrivatePayloadMethod(ChatServiceImpl.class, "getLlamaText", prompt, params))
        .contains("prompt");
    java.lang.reflect.Method stabilityMethod =
        ChatServiceImpl.class.getDeclaredMethod("getStabilityTitanText", String.class);
    stabilityMethod.setAccessible(true);
    assertThat((String) stabilityMethod.invoke(null, prompt)).contains("text_prompts");
  }

  private static String invokePrivatePayloadMethod(Class<?> clazz, String methodName, String prompt,
                                                   BedrockParameters params)
      throws Exception {
    java.lang.reflect.Method m = clazz.getDeclaredMethod(methodName, String.class, BedrockParameters.class);
    m.setAccessible(true);
    return (String) m.invoke(null, prompt, params);
  }

  @Test
  @DisplayName("answerPromptStreaming writes error event when streaming future fails")
  void answerPromptStreamingWritesErrorWhenFutureFails() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    java.util.concurrent.CompletableFuture<Void> failingFuture = new java.util.concurrent.CompletableFuture<>();
    failingFuture.completeExceptionally(new RuntimeException("stream failed"));
    when(connection.answerPromptStreaming(any(ConverseStreamRequest.class), any(ConverseStreamResponseHandler.class)))
        .thenReturn(failingFuture);

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.5f, 50);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    InputStream stream = service.answerPromptStreaming("Hi", params);

    assertThat(stream).isNotNull();
    byte[] buf = new byte[8192];
    int total = 0;
    long deadline = System.currentTimeMillis() + 5000;
    while (total < buf.length && System.currentTimeMillis() < deadline) {
      int n = stream.read(buf, total, buf.length - total);
      if (n <= 0)
        break;
      total += n;
      String soFar = new String(buf, 0, total, java.nio.charset.StandardCharsets.UTF_8);
      if (soFar.contains("event:") && soFar.contains("error"))
        break;
    }
    String content = new String(buf, 0, total, java.nio.charset.StandardCharsets.UTF_8);
    assertThat(content).contains("event:");
    assertThat(content).as("stream should contain error event when streaming fails").contains("error");
  }

  @Test
  @DisplayName("createChunkJson produces valid JSON with text type and timestamp")
  void createChunkJsonProducesValidJson() throws Exception {
    java.lang.reflect.Method m = ChatServiceImpl.class.getDeclaredMethod("createChunkJson", String.class);
    m.setAccessible(true);
    org.json.JSONObject result = (org.json.JSONObject) m.invoke(null, "Hello world");

    assertThat(result.getString("type")).isEqualTo("chunk");
    assertThat(result.getString("text")).isEqualTo("Hello world");
    assertThat(result.has("timestamp")).isTrue();
  }

  @Test
  @DisplayName("createCompletionJson produces valid JSON with prompt modelId status duration and timestamp")
  void createCompletionJsonProducesValidJson() throws Exception {
    java.lang.reflect.Method m =
        ChatServiceImpl.class.getDeclaredMethod("createCompletionJson", String.class, String.class, long.class);
    m.setAccessible(true);
    org.json.JSONObject result = (org.json.JSONObject) m.invoke(null, "test prompt", "amazon.nova-lite-v1:0", 1500L);

    assertThat(result.getString("prompt")).isEqualTo("test prompt");
    assertThat(result.getString("modelId")).isEqualTo("amazon.nova-lite-v1:0");
    assertThat(result.getString("status")).isEqualTo("completed");
    assertThat(result.getLong("total_duration_ms")).isEqualTo(1500L);
    assertThat(result.has("timestamp")).isTrue();
  }

  @Test
  @DisplayName("createSessionStartJson produces valid JSON with prompt modelId timestamp and status")
  void createSessionStartJsonProducesValidJson() throws Exception {
    java.lang.reflect.Method m = ChatServiceImpl.class.getDeclaredMethod("createSessionStartJson", String.class,
                                                                         String.class, String.class);
    m.setAccessible(true);
    org.json.JSONObject result = (org.json.JSONObject) m.invoke(null, "test prompt", "amazon.nova-lite-v1:0",
                                                                "2026-02-05T10:00:00Z");

    assertThat(result.getString("prompt")).isEqualTo("test prompt");
    assertThat(result.getString("modelId")).isEqualTo("amazon.nova-lite-v1:0");
    assertThat(result.getString("timestamp")).isEqualTo("2026-02-05T10:00:00Z");
    assertThat(result.getString("status")).isEqualTo("started");
  }

  @Test
  @DisplayName("createErrorJson produces valid JSON with error type and timestamp")
  void createErrorJsonProducesValidJson() throws Exception {
    java.lang.reflect.Method m = ChatServiceImpl.class.getDeclaredMethod("createErrorJson", Throwable.class);
    m.setAccessible(true);
    org.json.JSONObject result = (org.json.JSONObject) m.invoke(null, new RuntimeException("test error"));

    assertThat(result.getString("error")).isEqualTo("test error");
    assertThat(result.getString("type")).isEqualTo("RuntimeException");
    assertThat(result.has("timestamp")).isTrue();
  }

  @Test
  @DisplayName("formatSSEEvent produces correctly formatted SSE event")
  void formatSSEEventProducesCorrectFormat() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);

    java.lang.reflect.Method m = ChatServiceImpl.class.getDeclaredMethod("formatSSEEvent", String.class, String.class);
    m.setAccessible(true);
    String result = (String) m.invoke(service, "test-event", "{\"key\":\"value\"}");

    assertThat(result).contains("id:");
    assertThat(result).contains("event: test-event");
    assertThat(result).contains("data: {\"key\":\"value\"}");
  }

  @Test
  @DisplayName("answerPrompt with stability model returns formatted response")
  void answerPromptStability() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    when(connection.answerPrompt(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder()
            .body(SdkBytes.fromUtf8String("{\"artifacts\":[{\"base64\":\"dGVzdA==\"}]}"))
            .build());

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("stability.stable-diffusion-xl-v1", 0.5f, 100);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    String result = service.answerPrompt("Hi", params);
    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("closeQuietly handles null stream gracefully")
  void closeQuietlyHandlesNullStream() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);

    java.lang.reflect.Method m = ChatServiceImpl.class.getDeclaredMethod("closeQuietly", java.io.PipedOutputStream.class);
    m.setAccessible(true);
    // Should not throw when passing null
    Object result = m.invoke(service, (java.io.PipedOutputStream) null);
    assertThat(result).isNull(); // void method returns null
  }

  @Test
  @DisplayName("handleConverseContentDelta writes session start and chunk")
  void handleConverseContentDeltaWritesSessionStartAndChunk() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);

    java.lang.reflect.Method m = ChatServiceImpl.class.getDeclaredMethod("handleConverseContentDelta",
                                                                         String.class, BedrockParameters.class,
                                                                         java.io.PipedOutputStream.class,
                                                                         java.util.concurrent.atomic.AtomicBoolean.class,
                                                                         String.class);
    m.setAccessible(true);

    java.io.PipedOutputStream outputStream = new java.io.PipedOutputStream();
    java.io.PipedInputStream inputStream = new java.io.PipedInputStream(outputStream);
    java.util.concurrent.atomic.AtomicBoolean sessionStartSent = new java.util.concurrent.atomic.AtomicBoolean(false);
    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.5f, 50);

    m.invoke(service, "test prompt", params, outputStream, sessionStartSent, "Hello chunk");
    outputStream.close();

    byte[] buf = new byte[4096];
    int total = inputStream.read(buf);
    String content = new String(buf, 0, total, java.nio.charset.StandardCharsets.UTF_8);

    assertThat(content).contains("session-start");
    assertThat(content).contains("chunk");
    assertThat(sessionStartSent.get()).isTrue();
    inputStream.close();
  }

  @Test
  @DisplayName("handleConverseContentDelta skips session start if already sent")
  void handleConverseContentDeltaSkipsSessionStartIfAlreadySent() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);

    java.lang.reflect.Method m = ChatServiceImpl.class.getDeclaredMethod("handleConverseContentDelta",
                                                                         String.class, BedrockParameters.class,
                                                                         java.io.PipedOutputStream.class,
                                                                         java.util.concurrent.atomic.AtomicBoolean.class,
                                                                         String.class);
    m.setAccessible(true);

    java.io.PipedOutputStream outputStream = new java.io.PipedOutputStream();
    java.io.PipedInputStream inputStream = new java.io.PipedInputStream(outputStream);
    java.util.concurrent.atomic.AtomicBoolean sessionStartSent = new java.util.concurrent.atomic.AtomicBoolean(true);
    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.5f, 50);

    m.invoke(service, "test prompt", params, outputStream, sessionStartSent, "Second chunk");
    outputStream.close();

    byte[] buf = new byte[4096];
    int total = inputStream.read(buf);
    String content = new String(buf, 0, total, java.nio.charset.StandardCharsets.UTF_8);

    assertThat(content).doesNotContain("session-start");
    assertThat(content).contains("chunk");
    inputStream.close();
  }

  @Test
  @DisplayName("handleConverseError writes session start and error")
  void handleConverseErrorWritesSessionStartAndError() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);

    java.lang.reflect.Method m = ChatServiceImpl.class.getDeclaredMethod("handleConverseError",
                                                                         String.class, BedrockParameters.class,
                                                                         java.io.PipedOutputStream.class,
                                                                         java.util.concurrent.atomic.AtomicBoolean.class,
                                                                         Throwable.class);
    m.setAccessible(true);

    java.io.PipedOutputStream outputStream = new java.io.PipedOutputStream();
    java.io.PipedInputStream inputStream = new java.io.PipedInputStream(outputStream);
    java.util.concurrent.atomic.AtomicBoolean sessionStartSent = new java.util.concurrent.atomic.AtomicBoolean(false);
    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.5f, 50);

    m.invoke(service, "test prompt", params, outputStream, sessionStartSent, new RuntimeException("test error"));

    byte[] buf = new byte[4096];
    int total = inputStream.read(buf);
    String content = new String(buf, 0, total, java.nio.charset.StandardCharsets.UTF_8);

    assertThat(content).contains("session-start");
    assertThat(content).contains("error");
    assertThat(content).contains("test error");
    assertThat(sessionStartSent.get()).isTrue();
    inputStream.close();
  }

  @Test
  @DisplayName("handleConverseError skips session start if already sent")
  void handleConverseErrorSkipsSessionStartIfAlreadySent() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);

    java.lang.reflect.Method m = ChatServiceImpl.class.getDeclaredMethod("handleConverseError",
                                                                         String.class, BedrockParameters.class,
                                                                         java.io.PipedOutputStream.class,
                                                                         java.util.concurrent.atomic.AtomicBoolean.class,
                                                                         Throwable.class);
    m.setAccessible(true);

    java.io.PipedOutputStream outputStream = new java.io.PipedOutputStream();
    java.io.PipedInputStream inputStream = new java.io.PipedInputStream(outputStream);
    java.util.concurrent.atomic.AtomicBoolean sessionStartSent = new java.util.concurrent.atomic.AtomicBoolean(true);
    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.5f, 50);

    m.invoke(service, "test prompt", params, outputStream, sessionStartSent, new RuntimeException("test error"));

    byte[] buf = new byte[4096];
    int total = inputStream.read(buf);
    String content = new String(buf, 0, total, java.nio.charset.StandardCharsets.UTF_8);

    assertThat(content).doesNotContain("session-start");
    assertThat(content).contains("error");
    inputStream.close();
  }

  @Test
  @DisplayName("handleConverseComplete writes completion event")
  void handleConverseCompleteWritesCompletionEvent() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);

    java.lang.reflect.Method m = ChatServiceImpl.class.getDeclaredMethod("handleConverseComplete",
                                                                         String.class, BedrockParameters.class,
                                                                         java.io.PipedOutputStream.class,
                                                                         long.class);
    m.setAccessible(true);

    java.io.PipedOutputStream outputStream = new java.io.PipedOutputStream();
    java.io.PipedInputStream inputStream = new java.io.PipedInputStream(outputStream);
    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.5f, 50);

    m.invoke(service, "test prompt", params, outputStream, System.currentTimeMillis() - 1000);

    byte[] buf = new byte[4096];
    int total = inputStream.read(buf);
    String content = new String(buf, 0, total, java.nio.charset.StandardCharsets.UTF_8);

    assertThat(content).contains("session-complete");
    assertThat(content).contains("completed");
    assertThat(content).contains("total_duration_ms");
    inputStream.close();
  }

  @Test
  @DisplayName("writeConverseSessionStart writes session start event")
  void writeConverseSessionStartWritesEvent() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);

    java.lang.reflect.Method m = ChatServiceImpl.class.getDeclaredMethod("writeConverseSessionStart",
                                                                         String.class, String.class,
                                                                         java.io.PipedOutputStream.class);
    m.setAccessible(true);

    java.io.PipedOutputStream outputStream = new java.io.PipedOutputStream();
    java.io.PipedInputStream inputStream = new java.io.PipedInputStream(outputStream);

    m.invoke(service, "test prompt", "amazon.nova-lite-v1:0", outputStream);
    outputStream.close();

    byte[] buf = new byte[4096];
    int total = inputStream.read(buf);
    String content = new String(buf, 0, total, java.nio.charset.StandardCharsets.UTF_8);

    assertThat(content).contains("session-start");
    assertThat(content).contains("test prompt");
    assertThat(content).contains("amazon.nova-lite-v1:0");
    inputStream.close();
  }

  @Test
  @DisplayName("writeConverseChunkError writes chunk error event")
  void writeConverseChunkErrorWritesEvent() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);

    java.lang.reflect.Method m = ChatServiceImpl.class.getDeclaredMethod("writeConverseChunkError",
                                                                         java.io.IOException.class,
                                                                         java.io.PipedOutputStream.class);
    m.setAccessible(true);

    java.io.PipedOutputStream outputStream = new java.io.PipedOutputStream();
    java.io.PipedInputStream inputStream = new java.io.PipedInputStream(outputStream);

    m.invoke(service, new java.io.IOException("chunk write error"), outputStream);
    outputStream.close();

    byte[] buf = new byte[4096];
    int total = inputStream.read(buf);
    String content = new String(buf, 0, total, java.nio.charset.StandardCharsets.UTF_8);

    assertThat(content).contains("chunk-error");
    assertThat(content).contains("chunk write error");
    inputStream.close();
  }

  @Test
  @DisplayName("getAmazonTitanText generates correct payload")
  void getAmazonTitanTextPayload() throws Exception {
    java.lang.reflect.Method m = ChatServiceImpl.class.getDeclaredMethod("getAmazonTitanText",
                                                                         String.class, BedrockParameters.class);
    m.setAccessible(true);

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.titan-text-lite-v1", 0.5f, 100);

    String result = (String) m.invoke(null, "Hello there", params);

    assertThat(result).contains("inputText");
    assertThat(result).contains("Hello there");
    assertThat(result).contains("textGenerationConfig");
    assertThat(result).contains("temperature");
  }

  @Test
  @DisplayName("getAmazonNovaText generates correct payload")
  void getAmazonNovaTextPayload() throws Exception {
    java.lang.reflect.Method m = ChatServiceImpl.class.getDeclaredMethod("getAmazonNovaText",
                                                                         String.class, BedrockParameters.class);
    m.setAccessible(true);

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.7f, 200);

    String result = (String) m.invoke(null, "Test prompt", params);

    assertThat(result).contains("messages");
    assertThat(result).contains("user");
    assertThat(result).contains("Test prompt");
    assertThat(result).contains("inferenceConfig");
  }

  @Test
  @DisplayName("getAnthropicClaudeText generates correct payload")
  void getAnthropicClaudeTextPayload() throws Exception {
    java.lang.reflect.Method m = ChatServiceImpl.class.getDeclaredMethod("getAnthropicClaudeText",
                                                                         String.class, BedrockParameters.class);
    m.setAccessible(true);

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("anthropic.claude-v2", 0.6f, 150);

    String result = (String) m.invoke(null, "Explain AI", params);

    assertThat(result).contains("prompt");
    assertThat(result).contains("Human:");
    assertThat(result).contains("Assistant:");
    assertThat(result).contains("Explain AI");
    assertThat(result).contains("max_tokens_to_sample");
  }

  @Test
  @DisplayName("getAI21Text generates correct payload")
  void getAI21TextPayload() throws Exception {
    java.lang.reflect.Method m = ChatServiceImpl.class.getDeclaredMethod("getAI21Text",
                                                                         String.class, BedrockParameters.class);
    m.setAccessible(true);

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("ai21.j2-mid-v1", 0.8f, 250);

    String result = (String) m.invoke(null, "Write code", params);

    assertThat(result).contains("prompt");
    assertThat(result).contains("Write code");
    assertThat(result).contains("maxTokens");
    assertThat(result).contains("temperature");
  }

  @Test
  @DisplayName("getMistralAIText generates correct payload")
  void getMistralAITextPayload() throws Exception {
    java.lang.reflect.Method m = ChatServiceImpl.class.getDeclaredMethod("getMistralAIText",
                                                                         String.class, BedrockParameters.class);
    m.setAccessible(true);

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("mistral.mistral-7b-instruct-v0:2", 0.4f, 120);

    String result = (String) m.invoke(null, "Summarize", params);

    assertThat(result).contains("prompt");
    assertThat(result).contains("Human:");
    assertThat(result).contains("Assistant:");
    assertThat(result).contains("Summarize");
    assertThat(result).contains("max_tokens");
  }

  @Test
  @DisplayName("getCohereText generates correct payload")
  void getCohereTextPayload() throws Exception {
    java.lang.reflect.Method m = ChatServiceImpl.class.getDeclaredMethod("getCohereText",
                                                                         String.class, BedrockParameters.class);
    m.setAccessible(true);

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("cohere.command-text-v14", 0.3f, 180);

    String result = (String) m.invoke(null, "Translate text", params);

    assertThat(result).contains("prompt");
    assertThat(result).contains("Translate text");
    assertThat(result).contains("max_tokens");
    assertThat(result).contains("temperature");
  }

  @Test
  @DisplayName("getLlamaText generates correct payload")
  void getLlamaTextPayload() throws Exception {
    java.lang.reflect.Method m = ChatServiceImpl.class.getDeclaredMethod("getLlamaText",
                                                                         String.class, BedrockParameters.class);
    m.setAccessible(true);

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("meta.llama2-13b-chat-v1", 0.9f, 300);

    String result = (String) m.invoke(null, "Generate story", params);

    assertThat(result).contains("prompt");
    assertThat(result).contains("Generate story");
    assertThat(result).contains("max_gen_len");
    assertThat(result).contains("temperature");
  }

  @Test
  @DisplayName("getStabilityTitanText generates correct payload")
  void getStabilityTitanTextPayload() throws Exception {
    java.lang.reflect.Method m = ChatServiceImpl.class.getDeclaredMethod("getStabilityTitanText", String.class);
    m.setAccessible(true);

    String result = (String) m.invoke(null, "Create image");

    assertThat(result).contains("text_prompts");
    assertThat(result).contains("Create image");
  }

  @Test
  @DisplayName("answerPrompt handles SdkClientException")
  void answerPromptSdkClientException() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    when(connection.answerPrompt(any())).thenThrow(
                                                   software.amazon.awssdk.core.exception.SdkClientException
                                                       .create("Simulated SDK error"));

    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.titan-text-lite-v1", 0.5f, 100);

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.answerPrompt("test prompt", params))
        .isInstanceOf(org.mule.runtime.extension.api.exception.ModuleException.class);
  }

  @Test
  @DisplayName("closeQuietly handles IOException gracefully")
  void closeQuietlyHandlesException() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);

    java.lang.reflect.Method m = ChatServiceImpl.class.getDeclaredMethod("closeQuietly",
                                                                         java.io.PipedOutputStream.class);
    m.setAccessible(true);

    // Test with null - should not throw
    Object result1 = m.invoke(service, (Object) null);
    assertThat(result1).isNull();

    // Test with already closed stream - should not throw
    java.io.PipedOutputStream os = new java.io.PipedOutputStream();
    os.close();
    Object result2 = m.invoke(service, os);
    assertThat(result2).isNull();
  }
}
