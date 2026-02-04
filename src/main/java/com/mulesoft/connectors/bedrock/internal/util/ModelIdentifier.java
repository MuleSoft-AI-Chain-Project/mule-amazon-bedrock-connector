package com.mulesoft.connectors.bedrock.internal.util;

import java.util.Optional;
import com.mulesoft.connectors.bedrock.internal.metadata.provider.ModelProvider;

/**
 * Factory class for model identification using Strategy pattern. Centralizes model ID pattern matching logic.
 */
public final class ModelIdentifier {

  private ModelIdentifier() {
    // Utility class - prevent instantiation
  }

  /**
   * Identifies if a model requires inference profile ARN format. Uses Strategy pattern to check against known model patterns.
   *
   * @param modelId the model identifier
   * @return true if model requires inference profile ARN format
   */
  public static boolean requiresInferenceProfileArn(String modelId) {
    if (modelId == null || modelId.isBlank()) {
      return false;
    }

    return modelId.contains(BedrockConstants.ModelPatterns.AMAZON_NOVA_PREMIER) ||
        modelId.contains(BedrockConstants.ModelPatterns.ANTHROPIC_CLAUDE_3) ||
        modelId.contains(BedrockConstants.ModelPatterns.MISTRAL_PIXTRAL) ||
        modelId.contains(BedrockConstants.ModelPatterns.META_LLAMA_4) ||
        modelId.contains(BedrockConstants.ModelPatterns.META_LLAMA_3_3) ||
        modelId.contains(BedrockConstants.ModelPatterns.META_LLAMA_3_2) ||
        modelId.contains(BedrockConstants.ModelPatterns.META_LLAMA_3_1);
  }

  /**
   * Builds inference profile ARN for models that require it.
   *
   * @param region the AWS region
   * @param accountId the AWS account ID
   * @param modelId the original model ID
   * @return the inference profile ARN
   */
  public static String buildInferenceProfileArn(String region, String accountId, String modelId) {
    return String.format(BedrockConstants.INFERENCE_PROFILE_ARN_TEMPLATE, region, accountId, modelId);
  }

  /**
   * Identifies the model provider from model ID. Uses existing ModelProvider enum with enhanced matching.
   *
   * @param modelId the model identifier
   * @return Optional ModelProvider if identified
   */
  public static Optional<ModelProvider> identifyModelProvider(String modelId) {
    if (modelId == null || modelId.isBlank()) {
      return Optional.empty();
    }
    return ModelProvider.fromModelId(modelId);
  }

  /**
   * Determines the response format group for a model. Used for response formatting strategy selection.
   *
   * @param modelId the model identifier
   * @return the response group identifier
   */
  public static String getResponseGroup(String modelId) {
    if (modelId == null || modelId.isBlank()) {
      return BedrockConstants.ResponseGroups.DEFAULT;
    }

    String lowerModelId = modelId.toLowerCase();

    if (lowerModelId.contains("claude")) {
      return BedrockConstants.ResponseGroups.CLAUDE;
    } else if (lowerModelId.contains(BedrockConstants.ModelPatterns.MISTRAL_PIXTRAL)) {
      return BedrockConstants.ResponseGroups.MISTRAL_PIXTRAL;
    } else if (lowerModelId.contains(BedrockConstants.ModelPatterns.MISTRAL_MISTRAL)) {
      return BedrockConstants.ResponseGroups.MISTRAL_MISTRAL;
    } else if (lowerModelId.contains("jamba")) {
      return BedrockConstants.ResponseGroups.JAMBA;
    } else if (lowerModelId.contains("llama")) {
      return BedrockConstants.ResponseGroups.LLAMA;
    } else if (lowerModelId.contains("titan")) {
      return BedrockConstants.ResponseGroups.TITAN;
    }

    return BedrockConstants.ResponseGroups.DEFAULT;
  }

  /**
   * Checks if a model ID matches a specific pattern.
   *
   * @param modelId the model identifier
   * @param pattern the pattern to match
   * @return true if model ID contains the pattern
   */
  public static boolean matchesPattern(String modelId, String pattern) {
    return modelId != null && !modelId.isBlank() && modelId.contains(pattern);
  }

  /**
   * Gets the default account ID if none is provided.
   *
   * @param accountId the provided account ID (may be null or blank)
   * @return the account ID to use (default if provided is null/blank)
   */
  public static String getAccountIdOrDefault(String accountId) {
    return (accountId != null && !accountId.isBlank())
        ? accountId
        : BedrockConstants.DEFAULT_AWS_ACCOUNT_ID;
  }
}
