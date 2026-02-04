package com.mulesoft.connectors.bedrock.api.params;

import com.mulesoft.connectors.bedrock.api.enums.TimeUnitEnum;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

/**
 * Parameter group for response-related settings including retry configuration. This group appears in the "Response" tab of agent
 * operations.
 */
public class BedrockAgentsResponseParameters {

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional
  @Summary("Request timeout (overrides connector-level timeout if provided)")
  @Placement(tab = "Response")
  private Integer requestTimeout;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional
  @Summary("Unit of time for the request timeout")
  @Placement(tab = "Response")
  private TimeUnitEnum requestTimeoutUnit;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional(defaultValue = "false")
  @Summary("Enable retry for streaming operations on timeout failures")
  @Placement(tab = "Response")
  private boolean enableRetry;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional(defaultValue = "3")
  @Summary("Maximum number of retry attempts (0 = no retries)")
  @Placement(tab = "Response")
  private Integer maxRetries;

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional(defaultValue = "1000")
  @Summary("Base backoff delay in milliseconds for exponential backoff")
  @Placement(tab = "Response")
  private Long retryBackoffMs;

  public Integer getRequestTimeout() {
    return requestTimeout;
  }

  public TimeUnitEnum getRequestTimeoutUnit() {
    return requestTimeoutUnit;
  }

  public boolean getEnableRetry() {
    return enableRetry;
  }

  public Integer getMaxRetries() {
    return maxRetries != null ? maxRetries : 3;
  }

  public Long getRetryBackoffMs() {
    return retryBackoffMs != null ? retryBackoffMs : 1000L;
  }

}
