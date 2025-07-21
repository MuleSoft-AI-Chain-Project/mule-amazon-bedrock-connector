package org.mule.extension.mulechain.helpers;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.mule.extension.mulechain.internal.AwsbedrockConfiguration;
import org.mule.extension.mulechain.internal.AwsbedrockParameters;
import org.mule.extension.mulechain.internal.ModelProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

public class AwsbedrockChatMemoryHelper {

  @FunctionalInterface
  private interface PayloadGenerator extends BiFunction<String, AwsbedrockParameters, String> {
  }

  private static final Map<ModelProvider, PayloadGenerator> payloadGeneratorMap = new EnumMap<>(ModelProvider.class);

  static {
    payloadGeneratorMap.put(ModelProvider.AMAZON, AwsbedrockChatMemoryHelper::getAmazonTitanText);
    payloadGeneratorMap.put(ModelProvider.AMAZON_NOVA, AwsbedrockChatMemoryHelper::getAmazonNovaText);
    payloadGeneratorMap.put(ModelProvider.ANTHROPIC, AwsbedrockChatMemoryHelper::getAnthropicClaudeText);
    payloadGeneratorMap.put(ModelProvider.AI21, AwsbedrockChatMemoryHelper::getAI21Text);
    payloadGeneratorMap.put(ModelProvider.MISTRAL, AwsbedrockChatMemoryHelper::getMistralAIText);
    payloadGeneratorMap.put(ModelProvider.COHERE, AwsbedrockChatMemoryHelper::getCohereText);
    payloadGeneratorMap.put(ModelProvider.META, AwsbedrockChatMemoryHelper::getLlamaText);
    payloadGeneratorMap.put(ModelProvider.STABILITY, (prompt, params) -> getStabilityTitanText(prompt));
  }

  private static BedrockRuntimeClient createClient(AwsBasicCredentials awsCreds, Region region, Boolean useFipsMode) {
    return BedrockRuntimeClient.builder()
        .credentialsProvider(StaticCredentialsProvider.create(awsCreds)).fipsEnabled(useFipsMode)
        .region(region)
        .build();
  }

  private static BedrockRuntimeClient createClientSession(AwsSessionCredentials awsCreds, Region region,
      Boolean useFipsMode) {
    return BedrockRuntimeClient.builder()
        .credentialsProvider(StaticCredentialsProvider.create(awsCreds)).fipsEnabled(useFipsMode)
        .region(region)
        .build();
  }

  private static InvokeModelRequest createInvokeRequest(String modelId, String nativeRequest) {
    return InvokeModelRequest.builder()
        .body(SdkBytes.fromUtf8String(nativeRequest))
        .modelId(modelId)
        .build();

  }

  private static String getAmazonTitanText(String prompt, AwsbedrockParameters awsBedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("inputText", prompt);

    JSONObject textGenerationConfig = new JSONObject();
    textGenerationConfig.put("temperature", awsBedrockParameters.getTemperature());
    textGenerationConfig.put("topP", awsBedrockParameters.getTopP());
    textGenerationConfig.put("maxTokenCount", awsBedrockParameters.getMaxTokenCount());

    jsonRequest.put("textGenerationConfig", textGenerationConfig);

    return jsonRequest.toString();
  }

  private static String getAmazonNovaText(String prompt, AwsbedrockParameters awsBedrockParameters) {
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

  private static String getStabilityTitanText(String prompt) {
    JSONObject jsonRequest = new JSONObject();
    JSONObject textGenerationConfig = new JSONObject();
    textGenerationConfig.put("text", prompt);

    jsonRequest.put("text_prompts", textGenerationConfig);

    return jsonRequest.toString();
  }

  private static String getAnthropicClaudeText(String prompt, AwsbedrockParameters awsBedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("prompt", "\n\nHuman:" + prompt + "\n\nAssistant:");
    jsonRequest.put("temperature", awsBedrockParameters.getTemperature());
    jsonRequest.put("top_p", awsBedrockParameters.getTopP());
    jsonRequest.put("top_k", awsBedrockParameters.getTopK());
    jsonRequest.put("max_tokens_to_sample", awsBedrockParameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  private static String getMistralAIText(String prompt, AwsbedrockParameters awsBedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("prompt", "\n\nHuman:" + prompt + "\n\nAssistant:");
    jsonRequest.put("temperature", awsBedrockParameters.getTemperature());
    jsonRequest.put("top_p", awsBedrockParameters.getTopP());
    jsonRequest.put("top_k", awsBedrockParameters.getTopK());
    jsonRequest.put("max_tokens", awsBedrockParameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  private static String getAI21Text(String prompt, AwsbedrockParameters awsBedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("prompt", prompt);
    jsonRequest.put("temperature", awsBedrockParameters.getTemperature());
    jsonRequest.put("topP", awsBedrockParameters.getTopP());
    jsonRequest.put("maxTokens", awsBedrockParameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  private static String getCohereText(String prompt, AwsbedrockParameters awsBedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("prompt", prompt);
    jsonRequest.put("temperature", awsBedrockParameters.getTemperature());
    jsonRequest.put("p", awsBedrockParameters.getTopP());
    jsonRequest.put("k", awsBedrockParameters.getTopK());
    jsonRequest.put("max_tokens", awsBedrockParameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  private static String getLlamaText(String prompt, AwsbedrockParameters awsBedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("prompt", prompt);
    jsonRequest.put("temperature", awsBedrockParameters.getTemperature());
    jsonRequest.put("top_p", awsBedrockParameters.getTopP());
    jsonRequest.put("max_gen_len", awsBedrockParameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  private static String identifyPayload(String prompt, AwsbedrockParameters awsBedrockParameters) {
    return ModelProvider.fromModelId(awsBedrockParameters.getModelName())
        .map(provider -> payloadGeneratorMap.get(provider).apply(prompt, awsBedrockParameters))
        .orElse("Unsupported model");
  }

  private static BedrockRuntimeClient InitiateClient(AwsbedrockConfiguration configuration,
      AwsbedrockParameters awsBedrockParameters) {
    // Initialize the AWS credentials
    // AwsBasicCredentials awsCreds =
    // AwsBasicCredentials.create(configuration.getAwsAccessKeyId(),
    // configuration.getAwsSecretAccessKey());
    // Create Bedrock Client
    // return createClient(awsCreds,
    // AwsbedrockPayloadHelper.getRegion(awsBedrockParameters.getRegion()));
    if (configuration.getAwsSessionToken() == null || configuration.getAwsSessionToken().isEmpty()) {
      AwsBasicCredentials awsCredsBasic = AwsBasicCredentials.create(configuration.getAwsAccessKeyId(),
          configuration.getAwsSecretAccessKey());
      return createClient(awsCredsBasic, AwsbedrockPayloadHelper.getRegion(awsBedrockParameters.getRegion()),
          configuration.getFipsModeEnabled());
    } else {
      AwsSessionCredentials awsCredsSession = AwsSessionCredentials.create(configuration.getAwsAccessKeyId(),
          configuration.getAwsSecretAccessKey(), configuration.getAwsSessionToken());
      return createClientSession(awsCredsSession, AwsbedrockPayloadHelper.getRegion(awsBedrockParameters.getRegion()),
          configuration.getFipsModeEnabled());
    }

  }

  private static AwsbedrockChatMemory intializeChatMemory(String memoryPath, String memoryName) {
    return new AwsbedrockChatMemory(memoryPath, memoryName);
  }

  private static List<String> getKeepLastMessage(AwsbedrockChatMemory chatMemory, Integer keepLastMessages) {

    // Retrieve all messages in ascending order of messageId
    List<String> messagesAsc = chatMemory.getAllMessagesByMessageIdAsc();

    // Keep only the last index messages
    if (messagesAsc.size() > keepLastMessages) {
      messagesAsc = messagesAsc.subList(messagesAsc.size() - keepLastMessages, messagesAsc.size());
    }

    return messagesAsc;

  }

  private static void addMessageToMemory(AwsbedrockChatMemory chatMemory, String prompt) {
    if (!isQuestion(prompt)) {
      chatMemory.addMessage(chatMemory.getMessageCount() + 1, prompt);
    }
  }

  private static boolean isQuestion(String message) {
    // Check if the message ends with a question mark
    if (message.trim().endsWith("?")) {
      return true;
    }
    // Check if the message starts with a question word (case insensitive)
    String[] questionWords = { "who", "what", "when", "where", "why", "how", "tell", "tell me", "do you", "what is",
        "can you", "could you", "would you", "is there", "are there", "will you", "won't you", "can't you",
        "couldn't you", "wouldn't you", "is it", "isn't it", "are they", "aren't they", "will they", "won't they",
        "can they", "can't they", "could they", "couldn't they", "would they", "wouldn't they" };
    String lowerCaseMessage = message.trim().toLowerCase();
    for (String questionWord : questionWords) {
      if (lowerCaseMessage.startsWith(questionWord + " ")) {
        return true;
      }
    }
    return false;
  }

  private static String formatMemoryPrompt(List<String> messages) {
    StringBuilder formattedPrompt = new StringBuilder();
    for (String message : messages) {
      formattedPrompt.append(message).append("\n");
    }
    return formattedPrompt.toString().trim();
  }

  public static String invokeModel(String prompt, String memoryPath, String memoryName, Integer keepLastMessages,
      AwsbedrockConfiguration configuration, AwsbedrockParameters awsBedrockParameters) {

    // Create Bedrock Client
    BedrockRuntimeClient client = InitiateClient(configuration, awsBedrockParameters);

    // Chatmemory initialization
    AwsbedrockChatMemory chatMemory = intializeChatMemory(memoryPath, memoryName);

    // Get keepLastMessages
    List<String> keepLastMessagesList = getKeepLastMessage(chatMemory, keepLastMessages);
    keepLastMessagesList.add(prompt);
    // String memoryPrompt = keepLastMessagesList.toString();
    String memoryPrompt = formatMemoryPrompt(keepLastMessagesList);

    String nativeRequest = identifyPayload(memoryPrompt, awsBedrockParameters);

    addMessageToMemory(chatMemory, prompt);

    try {
      // Encode and send the request to the Bedrock Runtime.
      InvokeModelRequest request = createInvokeRequest(awsBedrockParameters.getModelName(), nativeRequest);

      // logger.info("Native request: " + nativeRequest);

      InvokeModelResponse response = client.invokeModel(request);

      // Decode the response body.
      JSONObject responseBody = new JSONObject(response.body().asUtf8String());

      return responseBody.toString();

    } catch (SdkClientException e) {
      System.err.printf("ERROR: Can't invoke '%s'. Reason: %s", awsBedrockParameters.getModelName(), e.getMessage());
      throw new RuntimeException(e);
    }
  }

}
