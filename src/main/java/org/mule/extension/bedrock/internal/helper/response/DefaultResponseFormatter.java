package org.mule.extension.bedrock.internal.helper.response;

import org.json.JSONObject;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

/**
 * Default response formatter for models that don't require special formatting. Returns the raw response as-is (e.g., Amazon Nova
 * models).
 */
public class DefaultResponseFormatter extends BaseResponseFormatter {

  @Override
  public String format(InvokeModelResponse response) {
    // Amazon Nova models & the rest
    // Default case: pretty-print the raw response
    JSONObject responseBody = new JSONObject(response.body().asUtf8String());
    return responseBody.toString();
  }
}
