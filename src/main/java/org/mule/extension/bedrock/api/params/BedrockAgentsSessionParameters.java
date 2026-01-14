package org.mule.extension.bedrock.api.params;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.param.Optional;

public class BedrockAgentsSessionParameters {

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
