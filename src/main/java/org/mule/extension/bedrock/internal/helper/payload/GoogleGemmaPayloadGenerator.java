package org.mule.extension.bedrock.internal.helper.payload;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.bedrock.api.params.BedrockParameters;
import org.mule.extension.bedrock.internal.util.BedrockConstants;
import org.mule.extension.bedrock.internal.util.ModelIdentifier;

/**
 * Payload generator for Google Gemma 3 models. Supports Gemma 3 models available on Amazon Bedrock Marketplace.
 */
public class GoogleGemmaPayloadGenerator extends BasePayloadGenerator {

  @Override
  public String generatePayload(String prompt, BedrockParameters parameters) {
    // Build user message
    JSONObject userMessage = new JSONObject();
    userMessage.put(BedrockConstants.JsonKeys.ROLE, BedrockConstants.JsonKeys.USER);
    userMessage.put(BedrockConstants.JsonKeys.CONTENT, prompt);

    // Add to messages array
    JSONArray messages = new JSONArray();
    messages.put(userMessage);

    // Construct request body
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put(BedrockConstants.JsonKeys.MESSAGES, messages);
    jsonRequest.put(BedrockConstants.JsonKeys.TEMPERATURE, parameters.getTemperature());
    if (parameters.getTopP() != null) {
      jsonRequest.put(BedrockConstants.JsonKeys.TOP_P.toLowerCase(), parameters.getTopP());
    }
    jsonRequest.put(BedrockConstants.JsonKeys.MAX_TOKENS_LIMIT.toLowerCase(), parameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  @Override
  public boolean supports(String modelId) {
    return ModelIdentifier.matchesPattern(modelId, BedrockConstants.ModelPatterns.GOOGLE_GEMMA);
  }
}
