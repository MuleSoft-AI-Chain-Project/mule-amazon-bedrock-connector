package com.mulesoft.connectors.bedrock.internal.helper.response;

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

    // Extract the content text. Claude 4 extended thinking may emit non-text blocks
    // (e.g. {"type":"thinking", ...}) before the final answer, so scan for the first text block.
    JSONArray contentArray = original.getJSONArray("content");
    String originalText = extractFirstTextBlock(contentArray);

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

  private static String extractFirstTextBlock(JSONArray contentArray) {
    for (int i = 0; i < contentArray.length(); i++) {
      JSONObject block = contentArray.getJSONObject(i);
      String type = block.optString("type", "text");
      if ("text".equals(type) && block.has("text")) {
        return block.getString("text");
      }
    }
    // Fallback: no explicit text block found, return first block's text if present.
    JSONObject first = contentArray.getJSONObject(0);
    return first.optString("text", "");
  }
}
