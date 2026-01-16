package org.mule.extension.bedrock.internal.helper.response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.bedrock.internal.util.BedrockConstants;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

/**
 * Response formatter for Meta Llama models.
 */
public class LlamaResponseFormatter extends BaseResponseFormatter {

  @Override
  public String format(InvokeModelResponse response) {
    // Read the raw JSON string from response
    String rawJson = response.body().asUtf8String();

    // Parse original response
    JSONObject original = new JSONObject(rawJson);

    // Extract the content text
    String generationText = original.getString("generation");
    int inputTokens = original.getInt("prompt_token_count");
    int outputTokens = original.getInt("generation_token_count");
    String stopReason = original.optString("stop_reason", "stop");

    // Wrap generation text in content array
    JSONArray contentArray = createContentArray(generationText.trim());

    // Build message object
    JSONObject message = createMessageObject("assistant", contentArray);

    // Wrap message into output
    JSONObject output = new JSONObject();
    output.put(BedrockConstants.JsonKeys.MESSAGE, message);

    // Build usage block
    JSONObject usage = createUsageObject(inputTokens, outputTokens, inputTokens + outputTokens);

    // Final output structure
    String guardrailAction = original.optString("amazon-bedrock-guardrailAction", "NONE");

    return createFinalPayload(message, normalizeStopReason(stopReason), usage, guardrailAction);
  }
}
