package com.mulesoft.connectors.bedrock.api.params;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

/**
 * Parameter group for response logging fields. This group appears as a "Response Logging" section within the "Response" tab in
 * agent operations.
 */
public class BedrockAgentsResponseLoggingParameters {

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional
  @Summary("Request ID mapped from REQUEST-ID header to include in session-start event")
  @Placement(tab = "Response")
  private String requestId;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional
  @Summary("Correlation ID mapped from CORRELATION-ID header to include in session-start event")
  @Placement(tab = "Response")
  private String correlationId;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional
  @Summary("User ID mapped from USER-ID header to include in session-start event")
  @Placement(tab = "Response")
  private String userId;

  public String getRequestId() {
    return requestId;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public String getUserId() {
    return userId;
  }
}
