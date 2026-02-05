package com.mulesoft.connectors.bedrock.internal.helper.response;

import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

/**
 * Strategy interface for formatting model responses. Each model type implements this interface to provide its specific formatting
 * logic.
 */
public interface ResponseFormatter {

  /**
   * Formats the response from a model invocation into a standardized JSON format.
   *
   * @param response the raw response from the model
   * @return formatted JSON string matching the Nova response format
   */
  String format(InvokeModelResponse response);
}
