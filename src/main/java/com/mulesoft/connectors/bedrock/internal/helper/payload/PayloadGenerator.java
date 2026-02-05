package com.mulesoft.connectors.bedrock.internal.helper.payload;

import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParameters;

/**
 * Strategy interface for generating model-specific payloads. Each model type implements this interface to provide its specific
 * payload generation logic.
 */
public interface PayloadGenerator {

  /**
   * Generates a JSON payload string for the given prompt and parameters.
   *
   * @param prompt the user prompt
   * @param parameters the bedrock parameters
   * @return JSON payload as string
   */
  String generatePayload(String prompt, BedrockParameters parameters);

  /**
   * Checks if this generator supports the given model.
   *
   * @param modelId the model identifier
   * @return true if this generator supports the model
   */
  boolean supports(String modelId);
}
