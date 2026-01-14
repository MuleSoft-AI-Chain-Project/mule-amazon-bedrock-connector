package org.mule.extension.bedrock.internal.helper;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.bedrock.api.params.BedrockParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

public class PromptPayloadHelper {

  private static final Logger logger = LoggerFactory.getLogger(PromptPayloadHelper.class);

  public static String identifyPayload(String prompt, BedrockParameters bedrockParameters) {
    if (bedrockParameters.getModelName().contains("amazon.titan-text")) {
      return getAmazonTitanText(prompt, bedrockParameters);
    } else if (bedrockParameters.getModelName().contains("amazon.nova")) {
      logger.info("Generating payload for nova");
      return getAmazonNovaText(prompt, bedrockParameters);
    } else if (bedrockParameters.getModelName().contains("anthropic.claude")) {
      return getAnthropicClaudeText(prompt, bedrockParameters);
    } else if (bedrockParameters.getModelName().contains("ai21.jamba")) {
      return getAI21Text(prompt, bedrockParameters);
    } else if (bedrockParameters.getModelName().contains("mistral")) {
      return getMistralAIText(prompt, bedrockParameters);
    } else if (bedrockParameters.getModelName().contains("cohere.command")) {
      return getCohereText(prompt, bedrockParameters);
    } else if (bedrockParameters.getModelName().contains("meta.llama")) {
      return getLlamaText(prompt, bedrockParameters);
    } else if (bedrockParameters.getModelName().contains("stability.stable")) {
      return getStabilityTitanText(prompt);
    } else {
      return "Unsupported model";
    }

  }


  public static InvokeModelRequest createInvokeRequest(BedrockParameters bedrockParameters,
                                                       String nativeRequest) {

    String modelId = bedrockParameters.getModelName();
    logger.debug("modelId: {}", modelId);

    String accountId = (bedrockParameters.getAwsAccountId() != null && !bedrockParameters.getAwsAccountId().isBlank())
        ? bedrockParameters.getAwsAccountId()
        : "076261412953";
    logger.debug("accountId: {}", accountId);

    String region = bedrockParameters.getRegion();

    // for Anthropic Claude 3-x, mistral.pxtral, meta.llama3, prep the model id
    // using the following format
    // arn:aws:bedrock:us-east-1:076261412953:inference-profile/us.anthropic.claude-3-5-sonnet-20241022-v2:0
    // arn:aws:bedrock:us-east-1:076261412953:inference-profile/us.mistral.pixtral-large-2502-v1:0
    // arn:aws:bedrock:us-east-1:076261412953:inference-profile/us.meta.llama3-3-70b-instruct-v1:0

    if (modelId.contains("amazon.nova-premier") ||
        modelId.contains("anthropic.claude-3") ||
        modelId.contains("mistral.pixtral") ||
        modelId.contains("meta.llama4") ||
        modelId.contains("meta.llama3-3") ||
        modelId.contains("meta.llama3-2") ||
        modelId.contains("meta.llama3-1")) {

      modelId = "arn:aws:bedrock:" + region + ":" + accountId + ":inference-profile/us." + modelId;
    }

    String guardrailIdentifier = bedrockParameters.getGuardrailIdentifier();
    String guardrailVersion = bedrockParameters.getGuardrailVersion();

    InvokeModelRequest request;
    if (guardrailIdentifier != null && !guardrailIdentifier.isEmpty() &&
        guardrailVersion != null && !guardrailVersion.isEmpty()) {

      // Both values are present ? specify guardrail
      request = InvokeModelRequest.builder()
          .modelId(modelId)
          .body(SdkBytes.fromUtf8String(nativeRequest))
          .contentType("application/json")
          .guardrailIdentifier(guardrailIdentifier)
          .guardrailVersion(guardrailVersion)
          .build();

    } else {
      request = InvokeModelRequest.builder()
          .body(SdkBytes.fromUtf8String(nativeRequest))
          .modelId(modelId)
          .build();
    }

    return request;
  }

  public static String formatBedrockResponse(BedrockParameters bedrockParameters,
                                             InvokeModelResponse response) {

    String modelId = bedrockParameters.getModelName();

    String responseStr;

    String modelGroup;

    // Normalize model type using contains
    if (modelId.contains("claude")) {
      modelGroup = "claude";
    } else if (modelId.contains("mistral.pixtral")) {
      modelGroup = "mistral.pixtral";
    } else if (modelId.contains("mistral.mistral")) {
      modelGroup = "mistral.mistral";
    } else if (modelId.contains("jamba")) {
      modelGroup = "jamba";
    } else if (modelId.contains("llama")) {
      modelGroup = "llama";
    } else if (modelId.contains("titan")) {
      modelGroup = "titan";
    } else {
      modelGroup = "default";
    }

    JSONObject responseBody;

    // convert the response for all models to match Nova response..
    // ex:
    // {
    // "output": {
    // "message": {
    // "role": "assistant",
    // "content": [
    // {
    // "text": "Penang is renowned for its rich cultural heritage, delicious food,
    // and historical landmarks, ..."
    // }
    // ]
    // }
    // },
    // "stopReason": "end_turn",
    // "usage": {
    // "inputTokens": 14,
    // "outputTokens": 636,
    // "totalTokens": 650
    // },
    // "amazon-bedrock-guardrailAction": "NONE"
    // }

    // Switch on model group
    switch (modelGroup) {
      case "claude":
        return formatClaudeResponse(response);
      case "mistral.pixtral":
        return formatMistralPixtralResponse(response);
      case "mistral.mistral":
        return formatMistralMistralResponse(response);
      case "jamba":
        return formatJambaResponse(response);
      case "llama":
        return formatLlamaResponse(response);
      case "titan":
        // Amazon Titan
        return formatTitanResponse(response);
      default:
        // Amazon Nova models & the rest
        // Default case: pretty-print the raw response
        responseBody = new JSONObject(response.body().asUtf8String());
        responseStr = responseBody.toString();
        break;
    }

    return responseStr;

  }

  private static String getAmazonTitanText(String prompt, BedrockParameters bedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("inputText", prompt);

    JSONObject textGenerationConfig = new JSONObject();
    textGenerationConfig.put("temperature", bedrockParameters.getTemperature());
    textGenerationConfig.put("topP", bedrockParameters.getTopP());
    textGenerationConfig.put("maxTokenCount", bedrockParameters.getMaxTokenCount());

    jsonRequest.put("textGenerationConfig", textGenerationConfig);

    return jsonRequest.toString();
  }

  private static String getAmazonNovaText(String prompt, BedrockParameters bedrockParameters) {

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
    inferenceConfig.put("max_new_tokens", bedrockParameters.getMaxTokenCount());
    inferenceConfig.put("temperature", bedrockParameters.getTemperature());
    inferenceConfig.put("top_p", bedrockParameters.getTopP());
    inferenceConfig.put("top_k", bedrockParameters.getTopK());

    // Combine everything into the root JSON object
    JSONObject rootObject = new JSONObject();
    rootObject.put("messages", messagesArray);
    rootObject.put("inferenceConfig", inferenceConfig);

    return rootObject.toString();

  }

  private static String getAnthropicClaudeText(String prompt, BedrockParameters bedrockParameters) {

    // Build the user message
    JSONObject userMessage = new JSONObject();
    userMessage.put("role", "user");
    userMessage.put("content", prompt); // no need to prepend Human/Assistant here

    // Add to messages array
    JSONArray messages = new JSONArray();
    messages.put(userMessage);

    // Construct the request body for Claude 3.x
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("messages", messages);
    jsonRequest.put("anthropic_version", "bedrock-2023-05-31");
    jsonRequest.put("temperature", bedrockParameters.getTemperature());
    jsonRequest.put("top_p", bedrockParameters.getTopP());
    jsonRequest.put("max_tokens", bedrockParameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  private static String getAI21Text(String prompt, BedrockParameters bedrockParameters) {
    // Create message object
    JSONObject message = new JSONObject();
    message.put("role", "user");
    message.put("content", prompt);

    // Wrap it into messages array
    JSONArray messages = new JSONArray();
    messages.put(message);

    // Create body object
    JSONObject body = new JSONObject();
    body.put("messages", messages);
    body.put("max_tokens", bedrockParameters.getMaxTokenCount());
    body.put("top_p", bedrockParameters.getTopP());
    body.put("temperature", bedrockParameters.getTemperature());

    return body.toString();
  }

  private static String getMistralAIText(String prompt, BedrockParameters bedrockParameters) {
    JSONObject jsonRequest = new JSONObject();

    if (bedrockParameters.getModelName().contains("mistral.pixtral")) { // for mistral.pixtral
      // Create user message object
      JSONObject userMessage = new JSONObject();
      userMessage.put("role", "user");
      userMessage.put("content", prompt); // No need for "Human:" and "Assistant:"

      // Wrap in messages array
      JSONArray messages = new JSONArray();
      messages.put(userMessage);

      // Construct the full request body
      jsonRequest.put("messages", messages);
    } else {
      // default for mistral.mistral
      jsonRequest.put("prompt", "\n\nHuman:" + prompt + "\n\nAssistant:");
    }

    jsonRequest.put("temperature", bedrockParameters.getTemperature());
    jsonRequest.put("max_tokens", bedrockParameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  private static String getCohereText(String prompt, BedrockParameters bedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("prompt", prompt);
    jsonRequest.put("temperature", bedrockParameters.getTemperature());
    jsonRequest.put("p", bedrockParameters.getTopP());
    jsonRequest.put("k", bedrockParameters.getTopK());
    jsonRequest.put("max_tokens", bedrockParameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  private static String getLlamaText(String prompt, BedrockParameters bedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("prompt", prompt);
    jsonRequest.put("temperature", bedrockParameters.getTemperature());
    jsonRequest.put("top_p", bedrockParameters.getTopP());
    jsonRequest.put("max_gen_len", bedrockParameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  private static String getStabilityTitanText(String prompt) {
    JSONObject jsonRequest = new JSONObject();
    JSONObject textGenerationConfig = new JSONObject();
    textGenerationConfig.put("text", prompt);

    jsonRequest.put("text_prompts", textGenerationConfig);

    return jsonRequest.toString();
  }

  private static String formatClaudeResponse(InvokeModelResponse response) {

    // Step 1: Read the raw JSON string from response
    String rawJson = response.body().asUtf8String(); // your actual response object here

    // Step 2: Parse the original JSON
    JSONObject original = new JSONObject(rawJson);

    // Step 3: Extract the content text
    JSONArray contentArray = original.getJSONArray("content");
    JSONObject firstContentObj = contentArray.getJSONObject(0);
    String originalText = firstContentObj.getString("text");

    // get token usage
    JSONObject usageOriginal = original.getJSONObject("usage");

    // Map existing keys to normalized token usage
    int inputTokens = usageOriginal.getInt("input_tokens");
    int outputTokens = usageOriginal.getInt("output_tokens");
    int totalTokens = inputTokens + outputTokens;

    // Build normalized usage block
    JSONObject usage = new JSONObject();
    usage.put("inputTokens", inputTokens);
    usage.put("outputTokens", outputTokens);
    usage.put("totalTokens", totalTokens);

    // Step 4: Build the new content array
    JSONArray newContentArray = new JSONArray();
    JSONObject textObject = new JSONObject();
    textObject.put("text", originalText);
    newContentArray.put(textObject);

    // Step 5: Build the message
    JSONObject message = new JSONObject();
    message.put("role", original.getString("role"));
    message.put("content", newContentArray);

    // Step 6: Wrap it in the new output format
    JSONObject output = new JSONObject();
    output.put("message", message);

    JSONObject finalPayload = new JSONObject();
    finalPayload.put("output", output);
    finalPayload.put("stopReason", original.optString("stop_reason", "end_turn"));
    finalPayload.put("usage", usage);
    finalPayload.put("amazon-bedrock-guardrailAction",
                     original.optString("amazon-bedrock-guardrailAction", "NONE"));

    // Step 7: Print the result
    logger.info(finalPayload.toString(2));

    return finalPayload.toString();
  }

  private static String formatMistralPixtralResponse(InvokeModelResponse response) {

    // Step 1: Convert the raw JSON string from the response body
    String rawJson = response.body().asUtf8String();

    // Parse original JSON
    JSONObject original = new JSONObject(rawJson);
    JSONObject choice = original.getJSONArray("choices").getJSONObject(0);
    JSONObject messageObj = choice.getJSONObject("message");

    String content = messageObj.getString("content").trim();
    String role = messageObj.optString("role", "assistant");
    String stopReason = choice.optString("finish_reason", "stop");
    String guardrail = original.optString("amazon-bedrock-guardrailAction", "NONE");

    // Extract token usage if available
    JSONObject usageObj = original.optJSONObject("usage");
    Object inputTokens = (usageObj != null && usageObj.has("prompt_tokens")) ? usageObj.get("prompt_tokens")
        : JSONObject.NULL;
    Object outputTokens = (usageObj != null && usageObj.has("completion_tokens"))
        ? usageObj.get("completion_tokens")
        : JSONObject.NULL;
    Object totalTokens = (usageObj != null && usageObj.has("total_tokens")) ? usageObj.get("total_tokens")
        : JSONObject.NULL;

    // Build content array
    JSONObject textObj = new JSONObject();
    textObj.put("text", content);
    JSONArray contentArray = new JSONArray().put(textObj);

    // Build message
    JSONObject message = new JSONObject();
    message.put("role", role);
    message.put("content", contentArray);

    // Wrap into output object
    JSONObject output = new JSONObject();
    output.put("message", message);

    // Usage section
    JSONObject usage = new JSONObject();
    usage.put("inputTokens", inputTokens);
    usage.put("outputTokens", outputTokens);
    usage.put("totalTokens", totalTokens);

    // Assemble final response
    JSONObject finalPayload = new JSONObject();
    finalPayload.put("output", output);
    finalPayload.put("stopReason", stopReason.equals("stop") ? "end_turn" : stopReason);
    finalPayload.put("usage", usage);
    finalPayload.put("amazon-bedrock-guardrailAction", guardrail);

    // Print the final JSON
    logger.info(finalPayload.toString(2));

    return finalPayload.toString();

  }

  private static String formatMistralMistralResponse(InvokeModelResponse response) {

    // Step 1: Convert the raw JSON string from the response body
    String rawJson = response.body().asUtf8String();

    // Parse raw JSON
    JSONObject original = new JSONObject(rawJson);
    JSONArray outputs = original.getJSONArray("outputs");
    JSONObject output0 = outputs.getJSONObject(0);

    String text = output0.getString("text").trim();
    String stopReason = output0.optString("stop_reason", "stop");
    String guardrail = original.optString("amazon-bedrock-guardrailAction", "NONE");

    // Create assistant message block
    JSONObject textObj = new JSONObject();
    textObj.put("text", text);
    JSONArray contentArray = new JSONArray().put(textObj);

    JSONObject message = new JSONObject();
    message.put("role", "assistant");
    message.put("content", contentArray);

    JSONObject output = new JSONObject();
    output.put("message", message);

    // Create usage block with nulls (or you can skip this block)
    JSONObject usage = new JSONObject();
    usage.put("inputTokens", JSONObject.NULL);
    usage.put("outputTokens", JSONObject.NULL);
    usage.put("totalTokens", JSONObject.NULL);

    // Assemble final response
    JSONObject finalPayload = new JSONObject();
    finalPayload.put("output", output);
    finalPayload.put("stopReason", stopReason.equals("stop") ? "end_turn" : stopReason);
    finalPayload.put("usage", usage);
    finalPayload.put("amazon-bedrock-guardrailAction", guardrail);

    // Print the final JSON
    logger.info(finalPayload.toString(2));

    return finalPayload.toString();
  }

  private static String formatJambaResponse(InvokeModelResponse response) {
    // Step 1: Convert raw response body to string
    String rawJson = response.body().asUtf8String();

    // Parse original response
    JSONObject original = new JSONObject(rawJson);

    // Extract content and metadata
    JSONObject choice = original.getJSONArray("choices").getJSONObject(0);
    JSONObject message = choice.getJSONObject("message");
    String role = message.getString("role");
    String text = message.getString("content").trim();
    String stopReason = choice.optString("finish_reason", "stop");

    int inputTokens = original.getJSONObject("usage").getInt("prompt_tokens");
    int outputTokens = original.getJSONObject("usage").getInt("completion_tokens");
    int totalTokens = original.getJSONObject("usage").getInt("total_tokens");
    String guardrail = original.optString("amazon-bedrock-guardrailAction", "NONE");

    // Create new JSON format
    JSONObject textObj = new JSONObject();
    textObj.put("text", text);

    JSONArray contentArray = new JSONArray().put(textObj);

    JSONObject newMessage = new JSONObject();
    newMessage.put("role", role);
    newMessage.put("content", contentArray);

    JSONObject output = new JSONObject();
    output.put("message", newMessage);

    JSONObject usage = new JSONObject();
    usage.put("inputTokens", inputTokens);
    usage.put("outputTokens", outputTokens);
    usage.put("totalTokens", totalTokens);

    JSONObject finalResult = new JSONObject();
    finalResult.put("output", output);
    finalResult.put("stopReason", stopReason.equals("stop") ? "end_turn" : stopReason);
    finalResult.put("usage", usage);
    finalResult.put("amazon-bedrock-guardrailAction", guardrail);

    // Output the normalized JSON
    logger.info(finalResult.toString(2));

    return finalResult.toString();
  }

  private static String formatLlamaResponse(InvokeModelResponse response) {

    // Step 1: Read the raw JSON string from response
    String rawJson = response.body().asUtf8String(); // your actual response object here

    // Step 2: Parse original response
    JSONObject original = new JSONObject(rawJson);

    // Step 3: Extract the content text
    String generationText = original.getString("generation");
    int inputTokens = original.getInt("prompt_token_count");
    int outputTokens = original.getInt("generation_token_count");
    String stopReason = original.optString("stop_reason", "stop");

    // Wrap generation text in content array
    JSONObject textObj = new JSONObject();
    textObj.put("text", generationText.trim());

    JSONArray contentArray = new JSONArray().put(textObj);

    // Build message object
    JSONObject message = new JSONObject();
    message.put("role", "assistant");
    message.put("content", contentArray);

    // Wrap message into output
    JSONObject output = new JSONObject();
    output.put("message", message);

    // Build usage block
    JSONObject usage = new JSONObject();
    usage.put("inputTokens", inputTokens);
    usage.put("outputTokens", outputTokens);
    usage.put("totalTokens", inputTokens + outputTokens);

    // Final output structure
    JSONObject finalPayload = new JSONObject();
    finalPayload.put("output", output);
    finalPayload.put("stopReason", stopReason.equals("stop") ? "end_turn" : stopReason); // covert stop to end_turn
    finalPayload.put("usage", usage);
    finalPayload.put("amazon-bedrock-guardrailAction",
                     original.optString("amazon-bedrock-guardrailAction", "NONE"));

    // Print final JSON
    logger.info(finalPayload.toString(2));

    return finalPayload.toString();
  }

  private static String formatTitanResponse(InvokeModelResponse response) {
    // Step 1: Convert raw response body to string
    String rawJson = response.body().asUtf8String();

    // Parse raw JSON
    JSONObject original = new JSONObject(rawJson);
    JSONArray results = original.getJSONArray("results");
    JSONObject result0 = results.getJSONObject(0);

    // Extract values
    String text = result0.getString("outputText").trim();
    String stopReason = result0.optString("completionReason", "").equalsIgnoreCase("FINISH") ? "end_turn"
        : "unknown";
    String guardrail = original.optString("amazon-bedrock-guardrailAction", "NONE");

    int inputTokens = original.optInt("inputTextTokenCount", 0);
    int outputTokens = result0.optInt("tokenCount", 0);
    int totalTokens = inputTokens + outputTokens;

    // Construct message content
    JSONObject textObj = new JSONObject();
    textObj.put("text", text);

    JSONArray contentArray = new JSONArray().put(textObj);

    JSONObject message = new JSONObject();
    message.put("role", "assistant");
    message.put("content", contentArray);

    JSONObject output = new JSONObject();
    output.put("message", message);

    JSONObject usage = new JSONObject();
    usage.put("inputTokens", inputTokens);
    usage.put("outputTokens", outputTokens);
    usage.put("totalTokens", totalTokens);

    // Final result
    JSONObject finalResult = new JSONObject();
    finalResult.put("output", output);
    finalResult.put("stopReason", stopReason);
    finalResult.put("usage", usage);
    finalResult.put("amazon-bedrock-guardrailAction", guardrail);

    // Print the result
    logger.info(finalResult.toString(2));
    return finalResult.toString();
  }

  public static String definePromptTemplate(String promptTemplate, String instructions, String dataSet) {
    // Create the final template by concatenating strings with line separators
    String finalTemplate = promptTemplate
        + System.lineSeparator()
        + "Instructions: "
        + "{{instructions}}"
        + System.lineSeparator()
        + "Dataset: "
        + "{{dataset}}";

    // Create a map for the variables
    Map<String, String> variables = new HashMap<>();
    variables.put("instructions", instructions);
    variables.put("dataset", dataSet);

    // Replace the placeholders with actual values
    return processTemplate(finalTemplate, variables);
  }

  private static String processTemplate(String template, Map<String, String> variables) {
    for (Map.Entry<String, String> entry : variables.entrySet()) {
      template = template.replace("{{" + entry.getKey() + "}}", entry.getValue());
    }
    return template;
  }
}
