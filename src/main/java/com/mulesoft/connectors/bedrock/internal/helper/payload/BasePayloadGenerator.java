package com.mulesoft.connectors.bedrock.internal.helper.payload;

import org.json.JSONObject;
import com.mulesoft.connectors.bedrock.api.params.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.util.BedrockConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for payload generators providing common utility methods. Implements Template Method pattern for shared JSON building
 * logic.
 */
public abstract class BasePayloadGenerator implements PayloadGenerator {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Creates a JSON object with all common parameters (temperature, topP, topK, maxTokens).
   *
   * @param parameters the bedrock parameters
   * @return JSONObject with all parameters
   */
  protected JSONObject createFullInferenceConfig(BedrockParameters parameters) {
    JSONObject config = new JSONObject();
    config.put(BedrockConstants.JsonKeys.TEMPERATURE, parameters.getTemperature());
    if (parameters.getTopP() != null) {
      config.put(BedrockConstants.JsonKeys.TOP_P.toLowerCase(), parameters.getTopP());
    }
    if (parameters.getTopK() != null) {
      config.put(BedrockConstants.JsonKeys.TOP_K.toLowerCase(), parameters.getTopK());
    }
    config.put(BedrockConstants.JsonKeys.MAX_TOKENS.toLowerCase(), parameters.getMaxTokenCount());
    return config;
  }
}
