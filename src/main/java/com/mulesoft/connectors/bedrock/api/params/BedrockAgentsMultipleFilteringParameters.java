package com.mulesoft.connectors.bedrock.api.params;


import java.util.List;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.param.Optional;

/**
 * Parameter group for passing multiple knowledge-base configurations (per-KB settings).
 */
public class BedrockAgentsMultipleFilteringParameters {

  @Parameter
  @Optional
  private List<BedrockAgentsFilteringParameters.KnowledgeBaseConfig> knowledgeBases;

  public BedrockAgentsMultipleFilteringParameters() {}

  public List<BedrockAgentsFilteringParameters.KnowledgeBaseConfig> getKnowledgeBases() {
    return knowledgeBases;
  }

}
