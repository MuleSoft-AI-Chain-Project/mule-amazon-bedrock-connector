package org.mule.extension.bedrock.internal.helper.payload;

import org.json.JSONObject;
import org.mule.extension.bedrock.api.params.BedrockParameters;
import org.mule.extension.bedrock.internal.util.BedrockConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for payload generators providing common utility methods. Implements Template Method pattern for shared JSON building
 * logic.
 */
public abstract class BasePayloadGenerator implements PayloadGenerator {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Creates a JSON object with common inference parameters.
   *
   * @param parameters the bedrock parameters
   * @return JSONObject with inference config
   */
  protected JSONObject createInferenceConfig(BedrockParameters parameters) {
    JSONObject config = new JSONObject();
    config.put(BedrockConstants.JsonKeys.TEMPERATURE, parameters.getTemperature());
    config.put(BedrockConstants.JsonKeys.MAX_TOKENS.toLowerCase(), parameters.getMaxTokenCount());
    return config;
  }

  /**
   * Creates a JSON object with temperature and topP parameters.
   *
   * @param parameters the bedrock parameters
   * @return JSONObject with temperature and topP
   */
  protected JSONObject createTemperatureAndTopP(BedrockParameters parameters) {
    JSONObject config = new JSONObject();
    config.put(BedrockConstants.JsonKeys.TEMPERATURE, parameters.getTemperature());
    config.put(BedrockConstants.JsonKeys.TOP_P.toLowerCase(), parameters.getTopP());
    return config;
  }

  /**
   * Creates a JSON object with all common parameters (temperature, topP, topK, maxTokens).
   *
   * @param parameters the bedrock parameters
   * @return JSONObject with all parameters
   */
  protected JSONObject createFullInferenceConfig(BedrockParameters parameters) {
    JSONObject config = new JSONObject();
    config.put(BedrockConstants.JsonKeys.TEMPERATURE, parameters.getTemperature());
    config.put(BedrockConstants.JsonKeys.TOP_P.toLowerCase(), parameters.getTopP());
    if (parameters.getTopK() != null) {
      config.put(BedrockConstants.JsonKeys.TOP_K.toLowerCase(), parameters.getTopK());
    }
    config.put(BedrockConstants.JsonKeys.MAX_TOKENS.toLowerCase(), parameters.getMaxTokenCount());
    return config;
  }
}
