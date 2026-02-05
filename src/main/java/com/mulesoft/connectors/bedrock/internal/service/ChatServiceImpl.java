package com.mulesoft.connectors.bedrock.internal.service;

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
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import com.mulesoft.connectors.bedrock.internal.error.ErrorHandler;
import com.mulesoft.connectors.bedrock.internal.helper.PromptPayloadHelper;
import com.mulesoft.connectors.bedrock.internal.helper.request.ConverseStreamRequestBuilder;
import com.mulesoft.connectors.bedrock.internal.metadata.provider.ModelProvider;
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
  private static final String TEMPERATURE = "temperature";
  private static final String TOP_P = "top_p";
  private static final String TOP_K = "top_k";
  private static final String SESSION_START = "session-start";
  private static final String ERROR_KEY = "error";
  private static final String ERROR_WRITING_EVENT_LOG = "Error writing error event: {}";

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
    textGenerationConfig.put(TEMPERATURE, awsBedrockParameters.getTemperature());
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
    inferenceConfig.put(TEMPERATURE, awsBedrockParameters.getTemperature());
    inferenceConfig.put(TOP_P, awsBedrockParameters.getTopP());
    inferenceConfig.put(TOP_K, awsBedrockParameters.getTopK());

    // Combine everything into the root JSON object
    JSONObject rootObject = new JSONObject();
    rootObject.put("messages", messagesArray);
    rootObject.put("inferenceConfig", inferenceConfig);

    return rootObject.toString();
  }

  private static String getAnthropicClaudeText(String prompt, BedrockParameters awsBedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put(PROMPT, "\n\nHuman:" + prompt + "\n\nAssistant:");
    jsonRequest.put(TEMPERATURE, awsBedrockParameters.getTemperature());
    jsonRequest.put(TOP_P, awsBedrockParameters.getTopP());
    jsonRequest.put(TOP_K, awsBedrockParameters.getTopK());
    jsonRequest.put("max_tokens_to_sample", awsBedrockParameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  private static String getAI21Text(String prompt, BedrockParameters awsBedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put(PROMPT, prompt);
    jsonRequest.put(TEMPERATURE, awsBedrockParameters.getTemperature());
    jsonRequest.put("topP", awsBedrockParameters.getTopP());
    jsonRequest.put("maxTokens", awsBedrockParameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  private static String getMistralAIText(String prompt, BedrockParameters awsBedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put(PROMPT, "\n\nHuman:" + prompt + "\n\nAssistant:");
    jsonRequest.put(TEMPERATURE, awsBedrockParameters.getTemperature());
    jsonRequest.put(TOP_P, awsBedrockParameters.getTopP());
    jsonRequest.put(TOP_K, awsBedrockParameters.getTopK());
    jsonRequest.put("max_tokens", awsBedrockParameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  private static String getCohereText(String prompt, BedrockParameters awsBedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put(PROMPT, prompt);
    jsonRequest.put(TEMPERATURE, awsBedrockParameters.getTemperature());
    jsonRequest.put("p", awsBedrockParameters.getTopP());
    jsonRequest.put("k", awsBedrockParameters.getTopK());
    jsonRequest.put("max_tokens", awsBedrockParameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  private static String getLlamaText(String prompt, BedrockParameters awsBedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put(PROMPT, prompt);
    jsonRequest.put(TEMPERATURE, awsBedrockParameters.getTemperature());
    jsonRequest.put(TOP_P, awsBedrockParameters.getTopP());
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
    PipedOutputStream outputStream = null;
    try {
      outputStream = new PipedOutputStream();
      PipedInputStream inputStream = new PipedInputStream(outputStream);

      AtomicBoolean sessionStartSent = new AtomicBoolean(false);
      final PipedOutputStream finalOut = outputStream;
      CompletableFuture.runAsync(() -> {
        try {
          streamConverseResponse(prompt, bedrockParameters, finalOut, sessionStartSent);
        } catch (Exception e) {
          try {
            if (sessionStartSent.compareAndSet(false, true)) {
              String sseStart = formatSSEEvent(SESSION_START,
                                               createSessionStartJson(prompt, bedrockParameters.getModelName(),
                                                                      Instant.now().toString())
                                                   .toString());
              finalOut.write(sseStart.getBytes(StandardCharsets.UTF_8));
              finalOut.flush();
              logger.info(sseStart);
            }
            JSONObject errorJson = createErrorJson(e);
            String errorEvent = formatSSEEvent(ERROR_KEY, errorJson.toString());
            finalOut.write(errorEvent.getBytes(StandardCharsets.UTF_8));
            finalOut.flush();
            logger.error(errorEvent);
          } catch (IOException ioException) {
            logger.error(ERROR_WRITING_EVENT_LOG, ioException.getMessage());
          }
        } finally {
          try {
            finalOut.close();
          } catch (IOException ioException) {
            logger.debug("Error closing stream: {}", ioException.getMessage());
          } finally {
            closeQuietly(finalOut);
          }
        }
      });

      return inputStream;

    } catch (IOException e) {
      String errorEvent = formatSSEEvent(ERROR_KEY, createErrorJson(e).toString());
      logger.error(errorEvent);
      if (outputStream != null) {
        closeQuietly(outputStream);
      }
      return new ByteArrayInputStream(errorEvent.getBytes(StandardCharsets.UTF_8));
    }
  }

  private void closeQuietly(PipedOutputStream os) {
    try {
      if (os != null)
        os.close();
    } catch (IOException e) {
      logger.debug("Error closing stream", e);
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
          String text = deltaEvent.delta().text();
          if (text != null) {
            handleConverseContentDelta(prompt, bedrockParameters, outputStream, sessionStartSent, text);
          }
        })
        .onMessageStop(stopEvent -> {
          /* Completion is sent in onComplete */ })
        .onDefault(unknown -> logger.debug("Received event type: {}", unknown.getClass().getSimpleName()))
        .build();

    ConverseStreamResponseHandler handler = ConverseStreamResponseHandler.builder()
        .onResponse(response -> logger.debug("Streaming connection opened"))
        .onEventStream(publisher -> publisher.subscribe(event -> event.accept(visitor)))
        .onError(error -> handleConverseError(prompt, bedrockParameters, outputStream, sessionStartSent, error))
        .onComplete(() -> handleConverseComplete(prompt, bedrockParameters, outputStream, startTime))
        .build();

    getConnection().answerPromptStreaming(request, handler).get();
  }

  private void handleConverseContentDelta(String prompt, BedrockParameters bedrockParameters,
                                          PipedOutputStream outputStream, AtomicBoolean sessionStartSent, String text) {
    try {
      if (sessionStartSent.compareAndSet(false, true)) {
        writeConverseSessionStart(prompt, bedrockParameters.getModelName(), outputStream);
      }
      JSONObject chunkData = createChunkJson(text);
      String sseEvent = formatSSEEvent(CHUNK, chunkData.toString());
      outputStream.write(sseEvent.getBytes(StandardCharsets.UTF_8));
      outputStream.flush();
      logger.debug(sseEvent);
    } catch (IOException e) {
      writeConverseChunkError(e, outputStream);
    }
  }

  private void writeConverseSessionStart(String prompt, String modelName, PipedOutputStream outputStream)
      throws IOException {
    String sseStart = formatSSEEvent(SESSION_START,
                                     createSessionStartJson(prompt, modelName, Instant.now().toString()).toString());
    outputStream.write(sseStart.getBytes(StandardCharsets.UTF_8));
    outputStream.flush();
    logger.info(sseStart);
  }

  private void writeConverseChunkError(IOException e, PipedOutputStream outputStream) {
    try {
      String errorEvent = formatSSEEvent("chunk-error", createErrorJson(e).toString());
      outputStream.write(errorEvent.getBytes(StandardCharsets.UTF_8));
      outputStream.flush();
      logger.error(errorEvent);
    } catch (IOException ioException) {
      logger.error(ERROR_WRITING_EVENT_LOG, ioException.getMessage());
    }
  }

  private void handleConverseError(String prompt, BedrockParameters bedrockParameters,
                                   PipedOutputStream outputStream, AtomicBoolean sessionStartSent, Throwable error) {
    try {
      if (sessionStartSent.compareAndSet(false, true)) {
        writeConverseSessionStart(prompt, bedrockParameters.getModelName(), outputStream);
      }
      String errorEvent = formatSSEEvent(ERROR_KEY, createErrorJson(error).toString());
      outputStream.write(errorEvent.getBytes(StandardCharsets.UTF_8));
      outputStream.flush();
    } catch (IOException e) {
      logger.error(ERROR_WRITING_EVENT_LOG, e.getMessage());
    } finally {
      try {
        outputStream.close();
      } catch (IOException ignored) {
      }
    }
  }

  private void handleConverseComplete(String prompt, BedrockParameters bedrockParameters,
                                      PipedOutputStream outputStream, long startTime) {
    try {
      long duration = System.currentTimeMillis() - startTime;
      JSONObject completionData = createCompletionJson(prompt, bedrockParameters.getModelName(), duration);
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
    errorData.put(ERROR_KEY, error.getMessage());
    errorData.put("type", error.getClass().getSimpleName());
    errorData.put(TIMESTAMP, Instant.now().toString());
    return errorData;
  }

  private String formatSSEEvent(String eventType, String data) {
    int eventId = eventCounter.incrementAndGet();
    return String.format("id: %d%nevent: %s%ndata: %s%n%n", eventId, eventType, data);
  }
}
