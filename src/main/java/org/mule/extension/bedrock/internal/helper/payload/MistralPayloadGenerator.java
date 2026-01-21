package org.mule.extension.bedrock.internal.helper.payload;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.bedrock.api.params.BedrockParameters;
import org.mule.extension.bedrock.internal.util.BedrockConstants;
import org.mule.extension.bedrock.internal.util.ModelIdentifier;

/**
 * Payload generator for Mistral AI models.
 */
public class MistralPayloadGenerator extends BasePayloadGenerator {

  @Override
  public String generatePayload(String prompt, BedrockParameters parameters) {
    JSONObject jsonRequest = new JSONObject();

    if (ModelIdentifier.matchesPattern(parameters.getModelName(), BedrockConstants.ModelPatterns.MISTRAL_PIXTRAL)) {
      // Pixtral models use message-based format
      JSONObject userMessage = new JSONObject();
      userMessage.put(BedrockConstants.JsonKeys.ROLE, BedrockConstants.JsonKeys.USER);
      userMessage.put(BedrockConstants.JsonKeys.CONTENT, prompt);

      JSONArray messages = new JSONArray();
      messages.put(userMessage);

      jsonRequest.put(BedrockConstants.JsonKeys.MESSAGES, messages);
    } else {
      // Mistral Mistral models use prompt format
      jsonRequest.put(BedrockConstants.JsonKeys.PROMPT,
                      BedrockConstants.CLAUDE_HUMAN_PREFIX + prompt + BedrockConstants.CLAUDE_ASSISTANT_SUFFIX);
    }

    jsonRequest.put(BedrockConstants.JsonKeys.TEMPERATURE, parameters.getTemperature());
    jsonRequest.put(BedrockConstants.JsonKeys.MAX_TOKENS.toLowerCase(), parameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  @Override
  public boolean supports(String modelId) {
    return ModelIdentifier.matchesPattern(modelId, BedrockConstants.ModelPatterns.MISTRAL);
  }
}
