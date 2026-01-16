package org.mule.extension.bedrock.internal.helper.response;

import java.util.HashMap;
import java.util.Map;
import org.mule.extension.bedrock.internal.util.BedrockConstants;
import org.mule.extension.bedrock.internal.util.ModelIdentifier;

/**
 * Factory class for creating appropriate ResponseFormatter instances. Uses Strategy pattern to select the correct formatter based
 * on model type.
 */
public final class ResponseFormatterFactory {

  private static final Map<String, ResponseFormatter> formatterMap = new HashMap<>();

  static {
    formatterMap.put(BedrockConstants.ResponseGroups.CLAUDE, new ClaudeResponseFormatter());
    formatterMap.put(BedrockConstants.ResponseGroups.MISTRAL_PIXTRAL, new MistralPixtralResponseFormatter());
    formatterMap.put(BedrockConstants.ResponseGroups.MISTRAL_MISTRAL, new MistralMistralResponseFormatter());
    formatterMap.put(BedrockConstants.ResponseGroups.JAMBA, new JambaResponseFormatter());
    formatterMap.put(BedrockConstants.ResponseGroups.LLAMA, new LlamaResponseFormatter());
    formatterMap.put(BedrockConstants.ResponseGroups.TITAN, new TitanResponseFormatter());
    formatterMap.put(BedrockConstants.ResponseGroups.DEFAULT, new DefaultResponseFormatter());
  }

  private ResponseFormatterFactory() {
    // Utility class - prevent instantiation
  }

  /**
   * Gets the appropriate formatter for the given model ID.
   *
   * @param modelId the model identifier
   * @return ResponseFormatter instance for the model
   */
  public static ResponseFormatter getFormatter(String modelId) {
    String responseGroup = ModelIdentifier.getResponseGroup(modelId);
    return formatterMap.getOrDefault(responseGroup, formatterMap.get(BedrockConstants.ResponseGroups.DEFAULT));
  }
}
