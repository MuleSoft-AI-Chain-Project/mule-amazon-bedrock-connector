package org.mule.extension.bedrock.internal.helper.payload;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.bedrock.api.params.BedrockParameters;
import org.mule.extension.bedrock.internal.util.BedrockConstants;
import org.mule.extension.bedrock.internal.util.ModelIdentifier;

/**
 * Payload generator for Anthropic Claude models.
 */
public class AnthropicClaudePayloadGenerator extends BasePayloadGenerator {

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
    jsonRequest.put("anthropic_version", "bedrock-2023-05-31");
    jsonRequest.put(BedrockConstants.JsonKeys.TEMPERATURE, parameters.getTemperature());
    if (parameters.getTopP() != null) {
      jsonRequest.put(BedrockConstants.JsonKeys.TOP_P.toLowerCase(), parameters.getTopP());
    }
    jsonRequest.put(BedrockConstants.JsonKeys.MAX_TOKENS.toLowerCase(), parameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  @Override
  public boolean supports(String modelId) {
    return ModelIdentifier.matchesPattern(modelId, BedrockConstants.ModelPatterns.ANTHROPIC_CLAUDE);
  }
}
