package com.mulesoft.connectors.bedrock.internal.helper.payload;

import org.json.JSONObject;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.util.BedrockConstants;
import com.mulesoft.connectors.bedrock.internal.util.ModelIdentifier;

/**
 * Payload generator for Meta Llama models.
 */
public class LlamaPayloadGenerator extends BasePayloadGenerator {

  @Override
  public String generatePayload(String prompt, BedrockParameters parameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put(BedrockConstants.JsonKeys.PROMPT, prompt);
    jsonRequest.put(BedrockConstants.JsonKeys.TEMPERATURE, parameters.getTemperature());
    jsonRequest.put(BedrockConstants.JsonKeys.TOP_P.toLowerCase(), parameters.getTopP());
    jsonRequest.put(BedrockConstants.JsonKeys.MAX_GEN_LEN, parameters.getMaxTokenCount());

    return jsonRequest.toString();
  }

  @Override
  public boolean supports(String modelId) {
    return ModelIdentifier.matchesPattern(modelId, BedrockConstants.ModelPatterns.META_LLAMA);
  }
}
