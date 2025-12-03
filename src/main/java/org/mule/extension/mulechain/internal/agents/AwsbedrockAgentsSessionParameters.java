package org.mule.extension.mulechain.internal.agents;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.param.Optional;

public class AwsbedrockAgentsSessionParameters {

  @Parameter
  @Optional
  private String sessionId;

  @Parameter
  @Optional
  private boolean excludePreviousThinkingSteps;

  @Parameter
  @Optional
  private Integer previousConversationTurnsToInclude;

  public String getSessionId() {
    return sessionId;
  }

  public boolean getExcludePreviousThinkingSteps() {
    return excludePreviousThinkingSteps;
  }

  public Integer getPreviousConversationTurnsToInclude() {
    return previousConversationTurnsToInclude;
  }

}
