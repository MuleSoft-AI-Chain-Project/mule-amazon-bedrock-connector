package org.mule.extension.bedrock.internal.helper.response;

import org.json.JSONArray;
import org.json.JSONObject;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

/**
 * Response formatter for Claude models.
 */
public class ClaudeResponseFormatter extends BaseResponseFormatter {

  @Override
  public String format(InvokeModelResponse response) {
    // Read the raw JSON string from response
    String rawJson = response.body().asUtf8String();

    // Parse the original JSON
    JSONObject original = new JSONObject(rawJson);

    // Extract the content text
    JSONArray contentArray = original.getJSONArray("content");
    JSONObject firstContentObj = contentArray.getJSONObject(0);
    String originalText = firstContentObj.getString("text");

    // Get token usage
    JSONObject usageOriginal = original.getJSONObject("usage");

    // Map existing keys to normalized token usage
    int inputTokens = usageOriginal.getInt("input_tokens");
    int outputTokens = usageOriginal.getInt("output_tokens");
    int totalTokens = inputTokens + outputTokens;

    // Build normalized usage block
    JSONObject usage = createUsageObject(inputTokens, outputTokens, totalTokens);

    // Build the new content array
    JSONArray newContentArray = createContentArray(originalText);

    // Build the message
    JSONObject message = createMessageObject(original.getString("role"), newContentArray);

    // Create final payload
    String stopReason = original.optString("stop_reason", "end_turn");
    String guardrailAction = original.optString("amazon-bedrock-guardrailAction", "NONE");

    return createFinalPayload(message, stopReason, usage, guardrailAction);
  }
}
