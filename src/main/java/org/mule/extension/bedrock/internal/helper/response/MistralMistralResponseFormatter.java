package org.mule.extension.bedrock.internal.helper.response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.bedrock.internal.util.BedrockConstants;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

/**
 * Response formatter for Mistral Mistral models.
 */
public class MistralMistralResponseFormatter extends BaseResponseFormatter {

  @Override
  public String format(InvokeModelResponse response) {
    // Convert the raw JSON string from the response body
    String rawJson = response.body().asUtf8String();

    // Parse raw JSON
    JSONObject original = new JSONObject(rawJson);
    JSONArray outputs = original.getJSONArray("outputs");
    JSONObject output0 = outputs.getJSONObject(0);

    String text = output0.getString("text").trim();
    String stopReason = output0.optString("stop_reason", "stop");
    String guardrail = original.optString("amazon-bedrock-guardrailAction", "NONE");

    // Create assistant message block
    JSONArray contentArray = createContentArray(text);

    JSONObject message = createMessageObject("assistant", contentArray);

    JSONObject output = new JSONObject();
    output.put(BedrockConstants.JsonKeys.MESSAGE, message);

    // Create usage block with nulls
    JSONObject usage = new JSONObject();
    usage.put(BedrockConstants.JsonKeys.INPUT_TOKENS, JSONObject.NULL);
    usage.put(BedrockConstants.JsonKeys.OUTPUT_TOKENS, JSONObject.NULL);
    usage.put(BedrockConstants.JsonKeys.TOTAL_TOKENS, JSONObject.NULL);

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
