package com.mulesoft.connectors.bedrock.internal.helper.payload;

import org.json.JSONObject;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.util.BedrockConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default payload generator for unsupported models. Creates a generic payload based on model type (text, vision, moderation,
 * image).
 */
public class DefaultPayloadGenerator extends BasePayloadGenerator {

  private static final Logger logger = LoggerFactory.getLogger(DefaultPayloadGenerator.class);
  private final ModelType modelType;

  public DefaultPayloadGenerator(ModelType modelType) {
    this.modelType = modelType;
  }

  @Override
  public String generatePayload(String prompt, BedrockParameters parameters) {
    logger.warn("Using default payload generator for model type: {}", modelType);

    switch (modelType) {
      case TEXT:
        return generateDefaultTextPayload(prompt, parameters);
      case VISION:
        return generateDefaultVisionPayload(prompt, parameters);
      case MODERATION:
        return generateDefaultModerationPayload(prompt, parameters);
      case IMAGE:
        return generateDefaultImagePayload(prompt, parameters);
      default:
        return generateDefaultTextPayload(prompt, parameters);
    }
  }

  @Override
  public boolean supports(String modelId) {
    // Default generator supports all models (fallback)
    return true;
  }

  /**
   * Generates a default text payload with common structure.
   */
  private String generateDefaultTextPayload(String prompt, BedrockParameters parameters) {
    JSONObject jsonRequest = new JSONObject();

    // Try to use a common text payload structure
    // Most models support either "prompt" or "inputText" or "messages"
    if (isMessageBasedModel(parameters.getModelName())) {
      return generateMessageBasedPayload(prompt, parameters);
    } else {
      // Simple prompt-based payload
      jsonRequest.put(BedrockConstants.JsonKeys.PROMPT, prompt);
      jsonRequest.put(BedrockConstants.JsonKeys.TEMPERATURE, parameters.getTemperature());
      jsonRequest.put(BedrockConstants.JsonKeys.MAX_TOKENS.toLowerCase(), parameters.getMaxTokenCount());
      if (parameters.getTopP() != null) {
        jsonRequest.put(BedrockConstants.JsonKeys.TOP_P.toLowerCase(), parameters.getTopP());
      }
    }

    return jsonRequest.toString();
  }

  /**
   * Generates a default vision payload (multimodal models).
   */
  private String generateDefaultVisionPayload(String prompt, BedrockParameters parameters) {
    // Vision models typically use message-based format with content blocks
    JSONObject userMessage = new JSONObject();
    userMessage.put(BedrockConstants.JsonKeys.ROLE, BedrockConstants.JsonKeys.USER);

    // For vision models, content can be text or image
    // Default to text content for now
    org.json.JSONArray contentArray = new org.json.JSONArray();
    JSONObject textContent = new JSONObject();
    textContent.put(BedrockConstants.JsonKeys.TEXT, prompt);
    contentArray.put(textContent);
    userMessage.put(BedrockConstants.JsonKeys.CONTENT, contentArray);

    org.json.JSONArray messages = new org.json.JSONArray();
    messages.put(userMessage);

    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put(BedrockConstants.JsonKeys.MESSAGES, messages);
    jsonRequest.put(BedrockConstants.JsonKeys.TEMPERATURE, parameters.getTemperature());
    jsonRequest.put(BedrockConstants.JsonKeys.MAX_TOKENS.toLowerCase(), parameters.getMaxTokenCount());
    if (parameters.getTopP() != null) {
      jsonRequest.put(BedrockConstants.JsonKeys.TOP_P.toLowerCase(), parameters.getTopP());
    }

    return jsonRequest.toString();
  }

  /**
   * Generates a default moderation payload.
   */
  private String generateDefaultModerationPayload(String prompt, BedrockParameters parameters) {
    // Moderation models typically use inputText format
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put(BedrockConstants.JsonKeys.INPUT_TEXT, prompt);
    return jsonRequest.toString();
  }

  /**
   * Generates a default image payload.
   */
  private String generateDefaultImagePayload(String prompt, BedrockParameters parameters) {
    JSONObject jsonRequest = new JSONObject();
    JSONObject textPrompts = new JSONObject();
    textPrompts.put(BedrockConstants.JsonKeys.TEXT, prompt);
    jsonRequest.put(BedrockConstants.JsonKeys.TEXT_PROMPTS, textPrompts);
    return jsonRequest.toString();
  }

  /**
   * Generates a message-based payload (for chat models).
   */
  private String generateMessageBasedPayload(String prompt, BedrockParameters parameters) {
    JSONObject userMessage = new JSONObject();
    userMessage.put(BedrockConstants.JsonKeys.ROLE, BedrockConstants.JsonKeys.USER);
    userMessage.put(BedrockConstants.JsonKeys.CONTENT, prompt);

    org.json.JSONArray messages = new org.json.JSONArray();
    messages.put(userMessage);

    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put(BedrockConstants.JsonKeys.MESSAGES, messages);
    jsonRequest.put(BedrockConstants.JsonKeys.TEMPERATURE, parameters.getTemperature());
    jsonRequest.put(BedrockConstants.JsonKeys.MAX_TOKENS.toLowerCase(), parameters.getMaxTokenCount());
    if (parameters.getTopP() != null) {
      jsonRequest.put(BedrockConstants.JsonKeys.TOP_P.toLowerCase(), parameters.getTopP());
    }

    return jsonRequest.toString();
  }

  /**
   * Checks if model likely uses message-based structure.
   */
  private boolean isMessageBasedModel(String modelName) {
    if (modelName == null) {
      return false;
    }
    String lower = modelName.toLowerCase();
    return lower.contains("claude") ||
        lower.contains("nova") ||
        lower.contains("jamba") ||
        lower.contains("instruct");
  }
}
