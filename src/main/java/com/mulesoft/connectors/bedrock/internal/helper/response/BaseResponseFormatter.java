package com.mulesoft.connectors.bedrock.internal.helper.response;

import org.json.JSONArray;
import org.json.JSONObject;
import com.mulesoft.connectors.bedrock.internal.util.BedrockConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for response formatters providing common utility methods.
 */
public abstract class BaseResponseFormatter implements ResponseFormatter {

  private static final Logger logger = LoggerFactory.getLogger(BaseResponseFormatter.class);

  /**
   * Creates a standardized usage JSON object.
   *
   * @param inputTokens input token count
   * @param outputTokens output token count
   * @param totalTokens total token count
   * @return JSONObject with usage information
   */
  protected JSONObject createUsageObject(int inputTokens, int outputTokens, int totalTokens) {
    JSONObject usage = new JSONObject();
    usage.put(BedrockConstants.JsonKeys.INPUT_TOKENS, inputTokens);
    usage.put(BedrockConstants.JsonKeys.OUTPUT_TOKENS, outputTokens);
    usage.put(BedrockConstants.JsonKeys.TOTAL_TOKENS, totalTokens);
    return usage;
  }

  /**
   * Creates a standardized content array with text.
   *
   * @param text the text content
   * @return JSONArray containing the text object
   */
  protected JSONArray createContentArray(String text) {
    JSONObject textObj = new JSONObject();
    textObj.put(BedrockConstants.JsonKeys.TEXT, text);
    return new JSONArray().put(textObj);
  }

  /**
   * Creates a standardized message object.
   *
   * @param role the message role
   * @param content the content array
   * @return JSONObject representing the message
   */
  protected JSONObject createMessageObject(String role, JSONArray content) {
    JSONObject message = new JSONObject();
    message.put(BedrockConstants.JsonKeys.ROLE, role);
    message.put(BedrockConstants.JsonKeys.CONTENT, content);
    return message;
  }

  /**
   * Creates the final standardized response payload.
   *
   * @param message the message object
   * @param stopReason the stop reason
   * @param usage the usage object
   * @param guardrailAction the guardrail action
   * @return formatted JSON string
   */
  protected String createFinalPayload(JSONObject message, String stopReason, JSONObject usage, String guardrailAction) {
    JSONObject output = new JSONObject();
    output.put(BedrockConstants.JsonKeys.MESSAGE, message);

    JSONObject finalPayload = new JSONObject();
    finalPayload.put(BedrockConstants.JsonKeys.OUTPUT, output);
    finalPayload.put(BedrockConstants.JsonKeys.STOP_REASON, stopReason);
    finalPayload.put(BedrockConstants.JsonKeys.USAGE, usage);
    finalPayload.put(BedrockConstants.JsonKeys.GUARDRAIL_ACTION, guardrailAction);

    String result = finalPayload.toString();
    logger.info(finalPayload.toString(2));
    return result;
  }

  /**
   * Normalizes stop reason from various formats to standard format.
   *
   * @param stopReason the original stop reason
   * @return normalized stop reason
   */
  protected String normalizeStopReason(String stopReason) {
    return "stop".equals(stopReason) ? "end_turn" : stopReason;
  }
}
