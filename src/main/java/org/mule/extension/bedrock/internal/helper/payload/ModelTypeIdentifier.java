package org.mule.extension.bedrock.internal.helper.payload;

/**
 * Utility class to identify model type based on model ID.
 */
public final class ModelTypeIdentifier {

  private ModelTypeIdentifier() {
    // Utility class - prevent instantiation
  }

  /**
   * Identifies the model type based on model ID patterns.
   *
   * @param modelId the model identifier
   * @return the identified model type
   */
  public static ModelType identifyType(String modelId) {
    if (modelId == null || modelId.isBlank()) {
      return ModelType.TEXT; // Default to TEXT if unknown
    }

    String lowerModelId = modelId.toLowerCase();

    // Check for moderation models
    if (lowerModelId.contains("moderation") || lowerModelId.contains("moderate")) {
      return ModelType.MODERATION;
    }

    // Check for vision models (multimodal, vision-capable)
    if (lowerModelId.contains("vision") ||
        lowerModelId.contains("multimodal") ||
        lowerModelId.contains("pixtral") || // Mistral Pixtral is vision-capable
        lowerModelId.contains("claude-3") || // Claude 3 has vision
        lowerModelId.contains("claude-4")) {
      return ModelType.VISION;
    }

    // Check for image generation models
    if (lowerModelId.contains("stable-diffusion") ||
        lowerModelId.contains("image") ||
        lowerModelId.contains("titan-image") ||
        lowerModelId.contains("diffusion")) {
      return ModelType.IMAGE;
    }

    // Default to text for known text model patterns
    if (lowerModelId.contains("titan-text") ||
        lowerModelId.contains("nova") ||
        lowerModelId.contains("claude") ||
        lowerModelId.contains("jamba") ||
        lowerModelId.contains("mistral") ||
        lowerModelId.contains("cohere") ||
        lowerModelId.contains("llama") ||
        lowerModelId.contains("command") ||
        lowerModelId.contains("embed")) { // Embedding models also use text-like payloads
      return ModelType.TEXT;
    }

    // If we can't determine, assume text (most common)
    return ModelType.TEXT;
  }
}
