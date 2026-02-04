package com.mulesoft.connectors.bedrock.internal.helper.payload;

import org.json.JSONObject;
import com.mulesoft.connectors.bedrock.api.params.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.util.BedrockConstants;
import com.mulesoft.connectors.bedrock.internal.util.ModelIdentifier;

/**
 * Payload generator for Amazon Titan text models.
 */
public class AmazonTitanPayloadGenerator extends BasePayloadGenerator {

  @Override
  public String generatePayload(String prompt, BedrockParameters parameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put(BedrockConstants.JsonKeys.INPUT_TEXT, prompt);

    JSONObject textGenerationConfig = createFullInferenceConfig(parameters);
    jsonRequest.put(BedrockConstants.JsonKeys.TEXT_GENERATION_CONFIG, textGenerationConfig);

    return jsonRequest.toString();
  }

  @Override
  public boolean supports(String modelId) {
    return ModelIdentifier.matchesPattern(modelId, BedrockConstants.ModelPatterns.AMAZON_TITAN_TEXT);
  }
}
