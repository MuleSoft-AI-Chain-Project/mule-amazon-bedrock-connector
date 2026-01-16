package org.mule.extension.bedrock.internal.helper.payload;

import org.json.JSONObject;
import org.mule.extension.bedrock.api.params.BedrockParameters;
import org.mule.extension.bedrock.internal.util.BedrockConstants;
import org.mule.extension.bedrock.internal.util.ModelIdentifier;

/**
 * Payload generator for Cohere Command models.
 */
public class CoherePayloadGenerator extends BasePayloadGenerator {

  @Override
  public String generatePayload(String prompt, BedrockParameters parameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put(BedrockConstants.JsonKeys.PROMPT, prompt);
    jsonRequest.put(BedrockConstants.JsonKeys.TEMPERATURE, parameters.getTemperature());
    jsonRequest.put(BedrockConstants.JsonKeys.P, parameters.getTopP());
    if (parameters.getTopK() != null) {
      jsonRequest.put(BedrockConstants.JsonKeys.K, parameters.getTopK());
    }
    jsonRequest.put(BedrockConstants.JsonKeys.MAX_TOKENS.toLowerCase(), parameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  @Override
  public boolean supports(String modelId) {
    return ModelIdentifier.matchesPattern(modelId, BedrockConstants.ModelPatterns.COHERE_COMMAND);
  }
}
