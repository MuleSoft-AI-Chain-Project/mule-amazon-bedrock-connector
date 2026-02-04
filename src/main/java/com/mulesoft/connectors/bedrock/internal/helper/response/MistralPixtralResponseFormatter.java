package com.mulesoft.connectors.bedrock.internal.helper.response;

import org.json.JSONArray;
import org.json.JSONObject;
import com.mulesoft.connectors.bedrock.internal.util.BedrockConstants;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

/**
 * Response formatter for Mistral Pixtral models.
 */
public class MistralPixtralResponseFormatter extends BaseResponseFormatter {

  @Override
  public String format(InvokeModelResponse response) {
    // Convert the raw JSON string from the response body
    String rawJson = response.body().asUtf8String();

    // Parse original JSON
    JSONObject original = new JSONObject(rawJson);
    JSONObject choice = original.getJSONArray("choices").getJSONObject(0);
    JSONObject messageObj = choice.getJSONObject("message");

    String content = messageObj.getString("content").trim();
    String role = messageObj.optString("role", "assistant");
    String stopReason = choice.optString("finish_reason", "stop");
    String guardrail = original.optString("amazon-bedrock-guardrailAction", "NONE");

    // Extract token usage if available
    JSONObject usageObj = original.optJSONObject("usage");
    Object inputTokens = (usageObj != null && usageObj.has("prompt_tokens")) ? usageObj.get("prompt_tokens")
        : JSONObject.NULL;
    Object outputTokens = (usageObj != null && usageObj.has("completion_tokens"))
        ? usageObj.get("completion_tokens")
        : JSONObject.NULL;
    Object totalTokens = (usageObj != null && usageObj.has("total_tokens")) ? usageObj.get("total_tokens")
        : JSONObject.NULL;

    // Build content array
    JSONArray contentArray = createContentArray(content);

    // Build message
    JSONObject message = createMessageObject(role, contentArray);

    // Wrap into output object
    JSONObject output = new JSONObject();
    output.put(BedrockConstants.JsonKeys.MESSAGE, message);

    // Usage section
    JSONObject usage = new JSONObject();
    usage.put(BedrockConstants.JsonKeys.INPUT_TOKENS, inputTokens);
    usage.put(BedrockConstants.JsonKeys.OUTPUT_TOKENS, outputTokens);
    usage.put(BedrockConstants.JsonKeys.TOTAL_TOKENS, totalTokens);

    // Assemble final response
    JSONObject finalPayload = new JSONObject();
    finalPayload.put(BedrockConstants.JsonKeys.OUTPUT, output);
    finalPayload.put(BedrockConstants.JsonKeys.STOP_REASON, normalizeStopReason(stopReason));
    finalPayload.put(BedrockConstants.JsonKeys.USAGE, usage);
    finalPayload.put(BedrockConstants.JsonKeys.GUARDRAIL_ACTION, guardrail);

    // Print the final JSON
    logger.info(finalPayload.toString(2));

    return finalPayload.toString();
  }
}
