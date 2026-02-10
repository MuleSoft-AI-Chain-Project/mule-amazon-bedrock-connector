package com.mulesoft.connectors.bedrock.internal.helper.payload;

import org.json.JSONObject;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.util.BedrockConstants;
import com.mulesoft.connectors.bedrock.internal.util.ModelIdentifier;

/**
 * Payload generator for Stability AI models.
 */
public class StabilityPayloadGenerator extends BasePayloadGenerator {

  @Override
  public String generatePayload(String prompt, BedrockParameters parameters) {
    JSONObject jsonRequest = new JSONObject();
    JSONObject textGenerationConfig = new JSONObject();
    textGenerationConfig.put(BedrockConstants.JsonKeys.TEXT, prompt);

    jsonRequest.put(BedrockConstants.JsonKeys.TEXT_PROMPTS, textGenerationConfig);

    return jsonRequest.toString();
  }

  @Override
  public boolean supports(String modelId) {
    return ModelIdentifier.matchesPattern(modelId, BedrockConstants.ModelPatterns.STABILITY_STABLE);
  }
}
