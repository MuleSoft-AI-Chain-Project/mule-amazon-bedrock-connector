package org.mule.extension.bedrock.internal.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.bedrock.api.params.BedrockParameters;
import org.mule.extension.bedrock.internal.config.BedrockConfiguration;
import org.mule.extension.bedrock.internal.connection.BedrockConnection;
import org.mule.extension.bedrock.internal.error.ErrorHandler;
import org.mule.extension.bedrock.internal.helper.PromptPayloadHelper;
import org.mule.extension.bedrock.internal.helper.request.ConverseStreamRequestBuilder;
import org.mule.extension.bedrock.internal.metadata.provider.ModelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamResponseHandler;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;


public class ChatServiceImpl extends BedrockServiceImpl implements ChatService {

  private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);
  private static final AtomicInteger eventCounter = new AtomicInteger(0);
  private static final String TIMESTAMP = "timestamp";
  private static final String PROMPT = "prompt";
  private static final String TEXT = "text";
  private static final String CHUNK = "chunk";
  private static final String TYPE = "type";

  @FunctionalInterface
  private interface PayloadGenerator extends BiFunction<String, BedrockParameters, String> {
  }

  private static final Map<ModelProvider, PayloadGenerator> payloadGeneratorMap = new EnumMap<>(ModelProvider.class);

  static {
    payloadGeneratorMap.put(ModelProvider.AMAZON, ChatServiceImpl::getAmazonTitanText);
    payloadGeneratorMap.put(ModelProvider.AMAZON_NOVA, ChatServiceImpl::getAmazonNovaText);
    payloadGeneratorMap.put(ModelProvider.ANTHROPIC, ChatServiceImpl::getAnthropicClaudeText);
    payloadGeneratorMap.put(ModelProvider.AI21, ChatServiceImpl::getAI21Text);
    payloadGeneratorMap.put(ModelProvider.MISTRAL, ChatServiceImpl::getMistralAIText);
    payloadGeneratorMap.put(ModelProvider.COHERE, ChatServiceImpl::getCohereText);
    payloadGeneratorMap.put(ModelProvider.META, ChatServiceImpl::getLlamaText);
    payloadGeneratorMap.put(ModelProvider.STABILITY, (prompt, params) -> getStabilityTitanText(prompt));
  }

  public ChatServiceImpl(BedrockConfiguration config, BedrockConnection bedrockConnection) {
    super(config, bedrockConnection);
  }

  private static String getAmazonTitanText(String prompt, BedrockParameters awsBedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("inputText", prompt);

    JSONObject textGenerationConfig = new JSONObject();
    textGenerationConfig.put("temperature", awsBedrockParameters.getTemperature());
    textGenerationConfig.put("topP", awsBedrockParameters.getTopP());
    textGenerationConfig.put("maxTokenCount", awsBedrockParameters.getMaxTokenCount());

    jsonRequest.put("textGenerationConfig", textGenerationConfig);

    return jsonRequest.toString();
  }

  private static String getAmazonNovaText(String prompt, BedrockParameters awsBedrockParameters) {
    JSONObject textObject = new JSONObject();
    textObject.put("text", prompt);

    // Create the "content" array containing the "text" object
    JSONArray contentArray = new JSONArray();
    contentArray.put(textObject);

    // Create the "messages" array and add the "user" message
    JSONObject userMessage = new JSONObject();
    userMessage.put("role", "user");
    userMessage.put("content", contentArray);

    JSONArray messagesArray = new JSONArray();
    messagesArray.put(userMessage);

    // Create the "inferenceConfig" object with optional parameters
    JSONObject inferenceConfig = new JSONObject();
    inferenceConfig.put("max_new_tokens", awsBedrockParameters.getMaxTokenCount());
    inferenceConfig.put("temperature", awsBedrockParameters.getTemperature());
    inferenceConfig.put("top_p", awsBedrockParameters.getTopP());
    inferenceConfig.put("top_k", awsBedrockParameters.getTopK());

    // Combine everything into the root JSON object
    JSONObject rootObject = new JSONObject();
    rootObject.put("messages", messagesArray);
    rootObject.put("inferenceConfig", inferenceConfig);

    return rootObject.toString();
  }

  private static String getAnthropicClaudeText(String prompt, BedrockParameters awsBedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("prompt", "\n\nHuman:" + prompt + "\n\nAssistant:");
    jsonRequest.put("temperature", awsBedrockParameters.getTemperature());
    jsonRequest.put("top_p", awsBedrockParameters.getTopP());
    jsonRequest.put("top_k", awsBedrockParameters.getTopK());
    jsonRequest.put("max_tokens_to_sample", awsBedrockParameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  private static String getAI21Text(String prompt, BedrockParameters awsBedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("prompt", prompt);
    jsonRequest.put("temperature", awsBedrockParameters.getTemperature());
    jsonRequest.put("topP", awsBedrockParameters.getTopP());
    jsonRequest.put("maxTokens", awsBedrockParameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  private static String getMistralAIText(String prompt, BedrockParameters awsBedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("prompt", "\n\nHuman:" + prompt + "\n\nAssistant:");
    jsonRequest.put("temperature", awsBedrockParameters.getTemperature());
    jsonRequest.put("top_p", awsBedrockParameters.getTopP());
    jsonRequest.put("top_k", awsBedrockParameters.getTopK());
    jsonRequest.put("max_tokens", awsBedrockParameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  private static String getCohereText(String prompt, BedrockParameters awsBedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("prompt", prompt);
    jsonRequest.put("temperature", awsBedrockParameters.getTemperature());
    jsonRequest.put("p", awsBedrockParameters.getTopP());
    jsonRequest.put("k", awsBedrockParameters.getTopK());
    jsonRequest.put("max_tokens", awsBedrockParameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  private static String getLlamaText(String prompt, BedrockParameters awsBedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("prompt", prompt);
    jsonRequest.put("temperature", awsBedrockParameters.getTemperature());
    jsonRequest.put("top_p", awsBedrockParameters.getTopP());
    jsonRequest.put("max_gen_len", awsBedrockParameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  private static String getStabilityTitanText(String prompt) {
    JSONObject jsonRequest = new JSONObject();
    JSONObject textGenerationConfig = new JSONObject();
    textGenerationConfig.put("text", prompt);

    jsonRequest.put("text_prompts", textGenerationConfig);

    return jsonRequest.toString();
  }

  @Override
  public String answerPrompt(String prompt, BedrockParameters bedrockParameters) {
    try {
      String nativeRequest = PromptPayloadHelper.identifyPayload(prompt, bedrockParameters);
      logger.info("Native request: {}", nativeRequest);
      String region = getConnection().getRegion();
      InvokeModelRequest invokeModelRequest = PromptPayloadHelper.createInvokeRequest(bedrockParameters, region, nativeRequest);
      InvokeModelResponse invokeModelResponse = getConnection().answerPrompt(invokeModelRequest);
      return PromptPayloadHelper.formatBedrockResponse(bedrockParameters, invokeModelResponse);
    } catch (SdkClientException e) {
      throw ErrorHandler.handleSdkClientException(e, bedrockParameters.getModelName());
    }
  }

  @Override
  public InputStream answerPromptStreaming(String prompt, BedrockParameters bedrockParameters) {
    return invokeConverseSSEStream(prompt, bedrockParameters);
  }

  /**
   * Invokes Converse API streaming and returns SSE-formatted InputStream. Same pattern as chatWithAgentSSEStream: pipe + runAsync
   * task that blocks on future.get(), SSE events (session-start, chunk, session-complete, error).
   */
  private InputStream invokeConverseSSEStream(String prompt, BedrockParameters bedrockParameters) {
    try {
      PipedOutputStream outputStream = new PipedOutputStream();
      PipedInputStream inputStream = new PipedInputStream(outputStream);

      AtomicBoolean sessionStartSent = new AtomicBoolean(false);

      CompletableFuture.runAsync(() -> {
        try {
          streamConverseResponse(prompt, bedrockParameters, outputStream, sessionStartSent);
        } catch (Exception e) {
          try {
            if (sessionStartSent.compareAndSet(false, true)) {
              String sseStart = formatSSEEvent("session-start",
                                               createSessionStartJson(prompt, bedrockParameters.getModelName(),
                                                                      Instant.now().toString())
                                                   .toString());
              outputStream.write(sseStart.getBytes(StandardCharsets.UTF_8));
              outputStream.flush();
              logger.info(sseStart);
            }
            JSONObject errorJson = createErrorJson(e);
            String errorEvent = formatSSEEvent("error", errorJson.toString());
            outputStream.write(errorEvent.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            outputStream.close();
            logger.error(errorEvent);
          } catch (IOException ioException) {
            logger.error("Error writing error event: {}", ioException.getMessage());
          }
        }
      });

      return inputStream;

    } catch (IOException e) {
      String errorEvent = formatSSEEvent("error", createErrorJson(e).toString());
      logger.error(errorEvent);
      return new ByteArrayInputStream(errorEvent.getBytes(StandardCharsets.UTF_8));
    }
  }

  /**
   * Builds ConverseStreamRequest and handler that write SSE events to the pipe, then blocks on future.get() until stream
   * completes (same as streamBedrockResponse in AgentServiceImpl).
   */
  private void streamConverseResponse(String prompt, BedrockParameters bedrockParameters,
                                      PipedOutputStream outputStream, AtomicBoolean sessionStartSent)
      throws ExecutionException, InterruptedException, IOException {

    long startTime = System.currentTimeMillis();
    String region = getConnection().getRegion();
    ConverseStreamRequest request = ConverseStreamRequestBuilder.create(bedrockParameters, region, prompt).build();

    ConverseStreamResponseHandler.Visitor visitor = ConverseStreamResponseHandler.Visitor.builder()
        .onContentBlockDelta(deltaEvent -> {
          try {
            String text = deltaEvent.delta().text();
            if (text == null) {
              return;
            }
            if (sessionStartSent.compareAndSet(false, true)) {
              String sseStart = formatSSEEvent("session-start",
                                               createSessionStartJson(prompt, bedrockParameters.getModelName(),
                                                                      Instant.now().toString())
                                                   .toString());
              outputStream.write(sseStart.getBytes(StandardCharsets.UTF_8));
              outputStream.flush();
              logger.info(sseStart);
            }
            JSONObject chunkData = createChunkJson(text);
            String sseEvent = formatSSEEvent("chunk", chunkData.toString());
            outputStream.write(sseEvent.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            logger.debug(sseEvent);
          } catch (IOException e) {
            try {
              String errorEvent = formatSSEEvent("chunk-error", createErrorJson(e).toString());
              outputStream.write(errorEvent.getBytes(StandardCharsets.UTF_8));
              outputStream.flush();
              logger.error(errorEvent);
            } catch (IOException ioException) {
              logger.error("Error writing error event: {}", ioException.getMessage());
            }
          }
        })
        .onMessageStop(stopEvent -> {
          // Completion is sent in onComplete
        })
        .onDefault(unknown -> {
          logger.debug("Received event type: {}", unknown.getClass().getSimpleName());
        })
        .build();

    ConverseStreamResponseHandler handler = ConverseStreamResponseHandler.builder()
        .onResponse(response -> logger.debug("Streaming connection opened"))
        .onEventStream(publisher -> publisher.subscribe(event -> event.accept(visitor)))
        .onError(error -> {
          try {
            if (sessionStartSent.compareAndSet(false, true)) {
              String sseStart = formatSSEEvent("session-start",
                                               createSessionStartJson(prompt, bedrockParameters.getModelName(),
                                                                      Instant.now().toString())
                                                   .toString());
              outputStream.write(sseStart.getBytes(StandardCharsets.UTF_8));
              outputStream.flush();
            }
            String errorEvent = formatSSEEvent("error", createErrorJson(error).toString());
            outputStream.write(errorEvent.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
          } catch (IOException e) {
            logger.error("Error writing error event: {}", e.getMessage());
          } finally {
            try {
              outputStream.close();
            } catch (IOException ignored) {
            }
          }
        })
        .onComplete(() -> {
          try {
            long duration = System.currentTimeMillis() - startTime;
            JSONObject completionData =
                createCompletionJson(prompt, bedrockParameters.getModelName(), duration);
            String completionEvent = formatSSEEvent("session-complete", completionData.toString());
            outputStream.write(completionEvent.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            logger.info(completionEvent);
          } catch (IOException e) {
            try {
              String errorEvent = formatSSEEvent("completion-error", createErrorJson(e).toString());
              outputStream.write(errorEvent.getBytes(StandardCharsets.UTF_8));
              outputStream.flush();
            } catch (IOException ioException) {
              logger.error("Error writing completion error event: {}", ioException.getMessage());
            }
          } finally {
            try {
              outputStream.close();
            } catch (IOException ioException) {
              logger.error("Error closing stream: {}", ioException.getMessage());
            }
          }
        })
        .build();

    CompletableFuture<Void> invocationFuture = getConnection().answerPromptStreaming(request, handler);
    invocationFuture.get();
  }

  private static JSONObject createSessionStartJson(String prompt, String modelId, String timestamp) {
    JSONObject startData = new JSONObject();
    startData.put(PROMPT, prompt);
    startData.put("modelId", modelId);
    startData.put(TIMESTAMP, timestamp);
    startData.put("status", "started");
    return startData;
  }

  private static JSONObject createChunkJson(String text) {
    JSONObject chunkData = new JSONObject();
    chunkData.put(TYPE, CHUNK);
    chunkData.put(TIMESTAMP, Instant.now().toString());
    chunkData.put(TEXT, text);
    return chunkData;
  }

  private static JSONObject createCompletionJson(String prompt, String modelId, long durationMs) {
    JSONObject completionData = new JSONObject();
    completionData.put(PROMPT, prompt);
    completionData.put("modelId", modelId);
    completionData.put("status", "completed");
    completionData.put("total_duration_ms", durationMs);
    completionData.put(TIMESTAMP, Instant.now().toString());
    return completionData;
  }

  private static JSONObject createErrorJson(Throwable error) {
    JSONObject errorData = new JSONObject();
    errorData.put("error", error.getMessage());
    errorData.put("type", error.getClass().getSimpleName());
    errorData.put(TIMESTAMP, Instant.now().toString());
    return errorData;
  }

  private String formatSSEEvent(String eventType, String data) {
    int eventId = eventCounter.incrementAndGet();
    return String.format("id: %d%nevent: %s%ndata: %s%n%n", eventId, eventType, data);
  }
}
