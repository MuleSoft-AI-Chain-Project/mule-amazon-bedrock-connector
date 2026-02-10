package com.mulesoft.connectors.bedrock.internal.helper.payload;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating appropriate PayloadGenerator instances based on model ID. Implements Strategy pattern with fallback to
 * default generator.
 */
public final class PayloadGeneratorFactory {

  private static final Logger logger = LoggerFactory.getLogger(PayloadGeneratorFactory.class);

  // List of all specific generators (order matters - more specific first)
  private static final List<PayloadGenerator> generators = new ArrayList<>();

  static {
    // Register all specific generators
    generators.add(new AmazonNovaPayloadGenerator());
    generators.add(new AnthropicClaudePayloadGenerator());
    generators.add(new AI21JambaPayloadGenerator());
    generators.add(new MistralPayloadGenerator());
    generators.add(new CoherePayloadGenerator());
    generators.add(new LlamaPayloadGenerator());
    generators.add(new StabilityPayloadGenerator());
    generators.add(new GoogleGemmaPayloadGenerator());
    generators.add(new OpenAIGPTPayloadGenerator());
  }

  private PayloadGeneratorFactory() {
    // Utility class - prevent instantiation
  }

  /**
   * Returns the appropriate PayloadGenerator for a given model ID. Falls back to DefaultPayloadGenerator if no specific generator
   * is found.
   *
   * @param modelId The ID of the model
   * @return A PayloadGenerator instance
   */
  public static PayloadGenerator getGenerator(String modelId) {
    if (modelId == null || modelId.isBlank()) {
      logger.warn("Model ID is null or blank, using default generator");
      return createDefaultGenerator(modelId);
    }

    // Try to find a specific generator
    for (PayloadGenerator generator : generators) {
      if (generator.supports(modelId)) {
        logger.debug("Found specific generator for model: {}", modelId);
        return generator;
      }
    }

    // No specific generator found - use default based on model type
    logger.info("No specific generator found for model: {}, using default generator", modelId);
    return createDefaultGenerator(modelId);
  }

  /**
   * Creates a default generator based on model type.
   */
  private static PayloadGenerator createDefaultGenerator(String modelId) {
    ModelType modelType = ModelTypeIdentifier.identifyType(modelId);
    return new DefaultPayloadGenerator(modelType);
  }
}
