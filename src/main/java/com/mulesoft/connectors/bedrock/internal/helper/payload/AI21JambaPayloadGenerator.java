package com.mulesoft.connectors.bedrock.internal.helper.payload;

import org.json.JSONArray;
import org.json.JSONObject;
import com.mulesoft.connectors.bedrock.api.params.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.util.BedrockConstants;
import com.mulesoft.connectors.bedrock.internal.util.ModelIdentifier;

/**
 * Payload generator for AI21 Jamba models.
 */
public class AI21JambaPayloadGenerator extends BasePayloadGenerator {

  @Override
  public String generatePayload(String prompt, BedrockParameters parameters) {
    // Create message object
    JSONObject message = new JSONObject();
    message.put(BedrockConstants.JsonKeys.ROLE, BedrockConstants.JsonKeys.USER);
    message.put(BedrockConstants.JsonKeys.CONTENT, prompt);

    // Wrap in messages array
    JSONArray messages = new JSONArray();
    messages.put(message);

    // Create body object
    JSONObject body = new JSONObject();
    body.put(BedrockConstants.JsonKeys.MESSAGES, messages);
    body.put(BedrockConstants.JsonKeys.MAX_TOKENS.toLowerCase(), parameters.getMaxTokenCount());
    body.put(BedrockConstants.JsonKeys.TOP_P.toLowerCase(), parameters.getTopP());
    body.put(BedrockConstants.JsonKeys.TEMPERATURE, parameters.getTemperature());

    return body.toString();
  }

  @Override
  public boolean supports(String modelId) {
    return ModelIdentifier.matchesPattern(modelId, BedrockConstants.ModelPatterns.AI21_JAMBA);
  }
}
