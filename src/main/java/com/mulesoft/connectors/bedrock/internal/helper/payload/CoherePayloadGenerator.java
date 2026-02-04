package com.mulesoft.connectors.bedrock.internal.helper.payload;

import org.json.JSONArray;
import org.json.JSONObject;
import com.mulesoft.connectors.bedrock.api.params.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.util.BedrockConstants;
import com.mulesoft.connectors.bedrock.internal.util.ModelIdentifier;

/**
 * Payload generator for Cohere Command models.
 */
public class CoherePayloadGenerator extends BasePayloadGenerator {

  @Override
  public String generatePayload(String prompt, BedrockParameters parameters) {
    // Build user message
    JSONObject userMessage = new JSONObject();
    userMessage.put(BedrockConstants.JsonKeys.ROLE, BedrockConstants.JsonKeys.USER);
    userMessage.put(BedrockConstants.JsonKeys.CONTENT, prompt);

    // Add to messages array
    JSONArray messages = new JSONArray();
    messages.put(userMessage);
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put(BedrockConstants.JsonKeys.MESSAGES, messages);
    jsonRequest.put(BedrockConstants.JsonKeys.TEMPERATURE, parameters.getTemperature());
    jsonRequest.put(BedrockConstants.JsonKeys.P, parameters.getTopP());
    if (parameters.getTopK() != null) {
      jsonRequest.put(BedrockConstants.JsonKeys.K, parameters.getTopK());
    }
    jsonRequest.put(BedrockConstants.JsonKeys.MAX_TOKENS_LIMIT.toLowerCase(), parameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  @Override
  public boolean supports(String modelId) {
    return ModelIdentifier.matchesPattern(modelId, BedrockConstants.ModelPatterns.COHERE_COMMAND);
  }
}
