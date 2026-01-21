package org.mule.extension.bedrock.internal.helper.payload;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.bedrock.api.params.BedrockParameters;
import org.mule.extension.bedrock.internal.util.BedrockConstants;
import org.mule.extension.bedrock.internal.util.ModelIdentifier;

/**
 * Payload generator for Amazon Nova models.
 */
public class AmazonNovaPayloadGenerator extends BasePayloadGenerator {

  @Override
  public String generatePayload(String prompt, BedrockParameters parameters) {
    // Create text object
    JSONObject textObject = new JSONObject();
    textObject.put(BedrockConstants.JsonKeys.TEXT, prompt);

    // Create content array
    JSONArray contentArray = new JSONArray();
    contentArray.put(textObject);

    // Create user message
    JSONObject userMessage = new JSONObject();
    userMessage.put(BedrockConstants.JsonKeys.ROLE, BedrockConstants.JsonKeys.USER);
    userMessage.put(BedrockConstants.JsonKeys.CONTENT, contentArray);

    // Create messages array
    JSONArray messagesArray = new JSONArray();
    messagesArray.put(userMessage);

    // Create inference config
    JSONObject inferenceConfig = new JSONObject();
    inferenceConfig.put(BedrockConstants.JsonKeys.MAX_NEW_TOKENS, parameters.getMaxTokenCount());
    inferenceConfig.put(BedrockConstants.JsonKeys.TEMPERATURE, parameters.getTemperature());
    inferenceConfig.put(BedrockConstants.JsonKeys.TOP_P.toLowerCase(), parameters.getTopP());
    if (parameters.getTopK() != null) {
      inferenceConfig.put(BedrockConstants.JsonKeys.TOP_K.toLowerCase(), parameters.getTopK());
    }

    // Create root object
    JSONObject rootObject = new JSONObject();
    rootObject.put(BedrockConstants.JsonKeys.MESSAGES, messagesArray);
    rootObject.put(BedrockConstants.JsonKeys.INFERENCE_CONFIG, inferenceConfig);

    return rootObject.toString();
  }

  @Override
  public boolean supports(String modelId) {
    return ModelIdentifier.matchesPattern(modelId, BedrockConstants.ModelPatterns.AMAZON_NOVA);
  }
}
