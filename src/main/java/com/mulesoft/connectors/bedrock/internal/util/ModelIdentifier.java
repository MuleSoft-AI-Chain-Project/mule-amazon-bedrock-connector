package com.mulesoft.connectors.bedrock.internal.util;

import com.mulesoft.connectors.bedrock.internal.error.BedrockErrorType;
import org.mule.runtime.extension.api.exception.ModuleException;

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

    // If the model ID already carries a cross-region geo prefix, the caller is passing a
    // pre-qualified inference profile ID and should be routed through the ARN path unchanged.
    if (hasGeoPrefix(modelId)) {
      return true;
    }

    return modelId.contains(BedrockConstants.ModelPatterns.AMAZON_NOVA_PREMIER) ||
        modelId.contains(BedrockConstants.ModelPatterns.ANTHROPIC_CLAUDE_3) ||
        modelId.contains(BedrockConstants.ModelPatterns.ANTHROPIC_CLAUDE_4_OPUS) ||
        modelId.contains(BedrockConstants.ModelPatterns.ANTHROPIC_CLAUDE_4_SONNET) ||
        modelId.contains(BedrockConstants.ModelPatterns.ANTHROPIC_CLAUDE_4_HAIKU) ||
        modelId.contains(BedrockConstants.ModelPatterns.MISTRAL_PIXTRAL) ||
        modelId.contains(BedrockConstants.ModelPatterns.META_LLAMA_4) ||
        modelId.contains(BedrockConstants.ModelPatterns.META_LLAMA_3_3) ||
        modelId.contains(BedrockConstants.ModelPatterns.META_LLAMA_3_2) ||
        modelId.contains(BedrockConstants.ModelPatterns.META_LLAMA_3_1);
  }

  /**
   * Returns true if the model ID already starts with a cross-region geo prefix (e.g. "us.", "eu.", "global.").
   *
   * @param modelId the model identifier
   * @return true if the ID is already geo-qualified
   */
  public static boolean hasGeoPrefix(String modelId) {
    if (modelId == null || modelId.isBlank()) {
      return false;
    }
    return modelId.startsWith(BedrockConstants.GeoPrefix.US)
        || modelId.startsWith(BedrockConstants.GeoPrefix.EU)
        || modelId.startsWith(BedrockConstants.GeoPrefix.APAC)
        || modelId.startsWith(BedrockConstants.GeoPrefix.JP)
        || modelId.startsWith(BedrockConstants.GeoPrefix.AU)
        || modelId.startsWith(BedrockConstants.GeoPrefix.GLOBAL);
  }

  /**
   * Resolves the geographic prefix for a cross-region inference profile based on the region and model.
   *
   * <p>
   * Claude Opus 4.7 is only available via the {@code global.} profile regardless of region. Otherwise the prefix is derived from
   * the AWS region family: {@code us-*} -> {@code us.}, {@code eu-*} -> {@code eu.}, {@code ap-*} -> {@code apac.}, {@code ca-*}
   * -> {@code us.} (North-American routing), else {@code us.} as a safe default.
   *
   * @param region the AWS region (e.g. "us-east-1")
   * @param modelId the base model identifier (without geo prefix)
   * @return the geo prefix to inject into the ARN
   */
  public static String resolveGeoPrefix(String region, String modelId) {
    if (modelId != null && modelId.contains(BedrockConstants.ModelPatterns.ANTHROPIC_CLAUDE_OPUS_4_7)) {
      return BedrockConstants.GeoPrefix.GLOBAL;
    }
    if (region == null || region.isBlank()) {
      return BedrockConstants.GeoPrefix.US;
    }
    String r = region.toLowerCase();
    if (r.startsWith("eu-")) {
      return BedrockConstants.GeoPrefix.EU;
    }
    if (r.startsWith("ap-northeast-1") || r.startsWith("ap-northeast-3")) {
      return BedrockConstants.GeoPrefix.JP;
    }
    if (r.startsWith("ap-southeast-2") || r.startsWith("ap-southeast-4")) {
      return BedrockConstants.GeoPrefix.AU;
    }
    if (r.startsWith("ap-")) {
      return BedrockConstants.GeoPrefix.APAC;
    }
    return BedrockConstants.GeoPrefix.US;
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
    // If the caller passed a pre-qualified model ID (e.g. "global.anthropic.claude-opus-4-7-..."),
    // use it verbatim. Otherwise inject the geo prefix resolved from region + model.
    String geoPrefix = hasGeoPrefix(modelId)
        ? BedrockConstants.GeoPrefix.NONE
        : resolveGeoPrefix(region, modelId);
    return String.format(BedrockConstants.INFERENCE_PROFILE_ARN_TEMPLATE, region, accountId, geoPrefix, modelId);
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
   * Returns the account ID when required (e.g. for inference profile ARN). Throws if null or blank.
   *
   * @param accountId the AWS account ID from parameters (may be null or blank)
   * @return the account ID
   * @throws ModuleException with VALIDATION_ERROR if accountId is null or blank
   */
  public static String requireAccountId(String accountId) {
    if (accountId == null || accountId.isBlank()) {
      throw new ModuleException("AWS account ID is required for this operation but was not provided",
                                BedrockErrorType.VALIDATION_ERROR);
    }
    return accountId;
  }
}
