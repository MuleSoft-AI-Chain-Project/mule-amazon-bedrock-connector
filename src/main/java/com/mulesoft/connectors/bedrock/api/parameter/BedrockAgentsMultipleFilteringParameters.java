package com.mulesoft.connectors.bedrock.api.parameter;


import java.util.List;
import java.util.Objects;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.param.NullSafe;
import org.mule.sdk.api.annotation.param.Optional;
import org.mule.sdk.api.annotation.param.display.Placement;

/**
 * Parameter group for passing multiple knowledge-base configurations (per-KB settings).
 */
public class BedrockAgentsMultipleFilteringParameters {

  @Parameter
  @Optional
  @NullSafe
  @Placement
  private List<BedrockAgentsFilteringParameters.KnowledgeBaseConfig> knowledgeBases;

  public BedrockAgentsMultipleFilteringParameters() {
    // Default constructor intentionally empty.
    // Required for framework/deserialization/reflection-based instantiation.
  }

  public List<BedrockAgentsFilteringParameters.KnowledgeBaseConfig> getKnowledgeBases() {
    return knowledgeBases;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof BedrockAgentsMultipleFilteringParameters that))
      return false;
    return Objects.equals(knowledgeBases, that.knowledgeBases);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(knowledgeBases);
  }
}
