package com.mulesoft.connectors.bedrock.internal.helper.response;

import org.json.JSONArray;
import org.json.JSONObject;
import com.mulesoft.connectors.bedrock.internal.util.BedrockConstants;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Response formatter for Amazon Titan models.
 */
public class TitanResponseFormatter extends BaseResponseFormatter {

  private static final Logger logger = LoggerFactory.getLogger(TitanResponseFormatter.class);

  @Override
  public String format(InvokeModelResponse response) {
    // Convert raw response body to string
    String rawJson = response.body().asUtf8String();

    // Parse raw JSON
    JSONObject original = new JSONObject(rawJson);
    JSONArray results = original.getJSONArray("results");
    JSONObject result0 = results.getJSONObject(0);

    // Extract values
    String text = result0.getString("outputText").trim();
    String stopReason = result0.optString("completionReason", "").equalsIgnoreCase("FINISH") ? "end_turn"
        : "unknown";
    String guardrail = original.optString("amazon-bedrock-guardrailAction", "NONE");

    int inputTokens = original.optInt("inputTextTokenCount", 0);
    int outputTokens = result0.optInt("tokenCount", 0);
    int totalTokens = inputTokens + outputTokens;

    // Construct message content
    JSONArray contentArray = createContentArray(text);

    JSONObject message = createMessageObject("assistant", contentArray);

    JSONObject output = new JSONObject();
    output.put(BedrockConstants.JsonKeys.MESSAGE, message);

    JSONObject usage = createUsageObject(inputTokens, outputTokens, totalTokens);

    // Final result
    JSONObject finalResult = new JSONObject();
    finalResult.put(BedrockConstants.JsonKeys.OUTPUT, output);
    finalResult.put(BedrockConstants.JsonKeys.STOP_REASON, stopReason);
    finalResult.put(BedrockConstants.JsonKeys.USAGE, usage);
    finalResult.put(BedrockConstants.JsonKeys.GUARDRAIL_ACTION, guardrail);

    // Print the result
    logger.info(finalResult.toString(2));
    return finalResult.toString();
  }
}
