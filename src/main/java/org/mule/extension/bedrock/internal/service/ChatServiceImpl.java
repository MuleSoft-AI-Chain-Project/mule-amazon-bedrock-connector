package org.mule.extension.bedrock.internal.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.bedrock.api.params.BedrockParameters;
import org.mule.extension.bedrock.internal.config.BedrockConfiguration;
import org.mule.extension.bedrock.internal.connection.BedrockConnection;
import org.mule.extension.bedrock.internal.error.ErrorHandler;
import org.mule.extension.bedrock.internal.helper.BedrockChatMemory;
import org.mule.extension.bedrock.internal.helper.PromptPayloadHelper;
import org.mule.extension.bedrock.internal.helper.request.ConverseStreamRequestBuilder;
import org.mule.extension.bedrock.internal.helper.streaming.StreamResponseHandlerFactory;
import org.mule.extension.bedrock.internal.metadata.provider.ModelProvider;
import org.mule.extension.bedrock.internal.util.BedrockConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamResponseHandler;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;


public class ChatServiceImpl extends BedrockServiceImpl implements ChatService {

  private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);

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
      InvokeModelRequest invokeModelRequest = PromptPayloadHelper.createInvokeRequest(bedrockParameters, nativeRequest);
      InvokeModelResponse invokeModelResponse = getConnection().answerPrompt(invokeModelRequest);
      return PromptPayloadHelper.formatBedrockResponse(bedrockParameters, invokeModelResponse);
    } catch (SdkClientException e) {
      throw ErrorHandler.handleSdkClientException(e, bedrockParameters.getModelName());
    }
  }

  @Override
  public String answerPromptMemory(String prompt, String memoryPath, String memoryName,
                                   Integer keepLastMessages, BedrockParameters bedrockParameters) {


    BedrockChatMemory chatMemory = initializeChatMemory(memoryPath, memoryName);
    // Get keepLastMessages
    List<String> keepLastMessagesList = getKeepLastMessage(chatMemory, keepLastMessages);
    keepLastMessagesList.add(prompt);
    String memoryPrompt = formatMemoryPrompt(keepLastMessagesList);

    String nativeRequest = identifyPayload(memoryPrompt, bedrockParameters);

    addMessageToMemory(chatMemory, prompt);

    // Encode and send the request to the Bedrock Runtime
    InvokeModelRequest request = InvokeModelRequest.builder()
        .body(SdkBytes.fromUtf8String(nativeRequest))
        .modelId(bedrockParameters.getModelName())
        .build();

    logger.debug("Native request: {}", nativeRequest);

    InvokeModelResponse response = getConnection().invokeModel(request);

    // Decode the response body.
    JSONObject responseBody = new JSONObject(response.body().asUtf8String());

    return responseBody.toString();

  }



  private static List<String> getKeepLastMessage(BedrockChatMemory chatMemory, Integer keepLastMessages) {

    // Retrieve all messages in ascending order of messageId
    List<String> messagesAsc = chatMemory.getAllMessagesByMessageIdAsc();

    // Keep only the last index messages
    if (messagesAsc.size() > keepLastMessages) {
      messagesAsc = messagesAsc.subList(messagesAsc.size() - keepLastMessages, messagesAsc.size());
    }

    return messagesAsc;

  }

  private static String formatMemoryPrompt(List<String> messages) {
    StringBuilder formattedPrompt = new StringBuilder();
    for (String message : messages) {
      formattedPrompt.append(message).append("\n");
    }
    return formattedPrompt.toString().trim();
  }

  private static String identifyPayload(String prompt, BedrockParameters awsBedrockParameters) {
    return ModelProvider.fromModelId(awsBedrockParameters.getModelName())
        .map(provider -> payloadGeneratorMap.get(provider).apply(prompt, awsBedrockParameters))
        .orElse("Unsupported model");
  }

  private static BedrockChatMemory initializeChatMemory(String memoryPath, String memoryName) {
    return new BedrockChatMemory(memoryPath, memoryName);
  }

  private static void addMessageToMemory(BedrockChatMemory chatMemory, String prompt) {
    if (!isQuestion(prompt)) {
      chatMemory.addMessage(chatMemory.getMessageCount() + 1, prompt);
    }
  }

  private static boolean isQuestion(String message) {
    if (message == null || message.isBlank()) {
      return false;
    }

    String trimmedMessage = message.trim();

    // Check if the message ends with a question mark
    if (trimmedMessage.endsWith(BedrockConstants.QUESTION_MARK)) {
      return true;
    }

    // Check if the message starts with a question word (case insensitive)
    String lowerCaseMessage = trimmedMessage.toLowerCase();
    for (String questionWord : BedrockConstants.QUESTION_WORDS) {
      if (lowerCaseMessage.startsWith(questionWord + BedrockConstants.SPACE)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public InputStream answerPromptStreaming(String prompt, BedrockParameters bedrockParameters) {
    try {
      PipedOutputStream outputStream = new PipedOutputStream();
      PipedInputStream inputStream = new PipedInputStream(outputStream, BedrockConstants.STREAM_BUFFER_SIZE);

      // Start the streaming process asynchronously to avoid blocking
      CompletableFuture.runAsync(() -> {
        try {
          startConverseStream(prompt, bedrockParameters, outputStream);
        } catch (Exception e) {
          // Handle errors by writing to stream and closing it
          handleStreamingError(outputStream, e);
        }
      });

      return inputStream;

    } catch (IOException ex) {
      logger.error("Failed to create streaming pipes", ex);
      throw new RuntimeException(BedrockConstants.ErrorMessages.STREAMING_PIPE_CREATION_FAILED, ex);
    }
  }

  private void startConverseStream(String prompt, BedrockParameters bedrockParameters, PipedOutputStream outputStream) {
    // Track if stream is already closed to prevent double-closing
    AtomicBoolean streamClosed = new AtomicBoolean(false);

    // Use Builder pattern to construct request
    ConverseStreamRequest request = ConverseStreamRequestBuilder.create(bedrockParameters, prompt).build();
    ConverseStreamResponseHandler handler = StreamResponseHandlerFactory.createHandler(outputStream, streamClosed);

    // Start the async streaming - don't block here
    CompletableFuture<Void> future = getConnection().answerPromptStreaming(request, handler);

    // Handle any exceptions from the future itself
    future.exceptionally(throwable -> {
      logger.error("Exception in streaming future", throwable);
      if (streamClosed.compareAndSet(false, true)) {
        handleStreamingError(outputStream, throwable);
      }
      return null;
    });

    // Note: We don't call future.join() here - let it run asynchronously
    // The stream will be closed by the handlers when appropriate
  }

  /**
   * Handles errors during streaming by writing error information to the stream and then closing it gracefully.
   */
  private void handleStreamingError(PipedOutputStream outputStream, Throwable error) {
    AtomicBoolean streamClosed = new AtomicBoolean(false);
    StreamResponseHandlerFactory.handleStreamError(error, outputStream, streamClosed);
  }
}
