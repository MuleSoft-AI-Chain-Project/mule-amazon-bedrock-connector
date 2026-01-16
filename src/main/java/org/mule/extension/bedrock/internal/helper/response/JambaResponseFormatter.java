package org.mule.extension.bedrock.internal.helper.response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.bedrock.internal.util.BedrockConstants;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

/**
 * Response formatter for AI21 Jamba models.
 */
public class JambaResponseFormatter extends BaseResponseFormatter {

  @Override
  public String format(InvokeModelResponse response) {
    // Convert raw response body to string
    String rawJson = response.body().asUtf8String();

    // Parse original response
    JSONObject original = new JSONObject(rawJson);

    // Extract content and metadata
    JSONObject choice = original.getJSONArray("choices").getJSONObject(0);
    JSONObject message = choice.getJSONObject("message");
    String role = message.getString("role");
    String text = message.getString("content").trim();
    String stopReason = choice.optString("finish_reason", "stop");

    int inputTokens = original.getJSONObject("usage").getInt("prompt_tokens");
    int outputTokens = original.getJSONObject("usage").getInt("completion_tokens");
    int totalTokens = original.getJSONObject("usage").getInt("total_tokens");
    String guardrail = original.optString("amazon-bedrock-guardrailAction", "NONE");

    // Create new JSON format
    JSONArray contentArray = createContentArray(text);

    JSONObject newMessage = createMessageObject(role, contentArray);

    JSONObject output = new JSONObject();
    output.put(BedrockConstants.JsonKeys.MESSAGE, newMessage);

    JSONObject usage = createUsageObject(inputTokens, outputTokens, totalTokens);

    JSONObject finalResult = new JSONObject();
    finalResult.put(BedrockConstants.JsonKeys.OUTPUT, output);
    finalResult.put(BedrockConstants.JsonKeys.STOP_REASON, normalizeStopReason(stopReason));
    finalResult.put(BedrockConstants.JsonKeys.USAGE, usage);
    finalResult.put(BedrockConstants.JsonKeys.GUARDRAIL_ACTION, guardrail);

    // Output the normalized JSON
    logger.info(finalResult.toString(2));

    return finalResult.toString();
  }
}
