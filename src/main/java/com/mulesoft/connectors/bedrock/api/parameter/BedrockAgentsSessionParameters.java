package com.mulesoft.connectors.bedrock.api.parameter;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.param.Optional;
import org.mule.sdk.api.annotation.param.display.Placement;

import java.util.Objects;

public class BedrockAgentsSessionParameters {

  @Parameter
  @Optional
  @Placement
  private String sessionId;

  @Parameter
  @Optional
  @Placement
  private boolean excludePreviousThinkingSteps;

  @Parameter
  @Optional
  @Placement
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

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof BedrockAgentsSessionParameters that))
      return false;
    return excludePreviousThinkingSteps == that.excludePreviousThinkingSteps && Objects.equals(sessionId, that.sessionId)
        && Objects.equals(previousConversationTurnsToInclude, that.previousConversationTurnsToInclude);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sessionId, excludePreviousThinkingSteps, previousConversationTurnsToInclude);
  }
}
