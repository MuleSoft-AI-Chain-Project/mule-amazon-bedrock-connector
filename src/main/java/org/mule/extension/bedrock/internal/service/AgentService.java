package org.mule.extension.bedrock.internal.service;

import java.io.InputStream;
import org.mule.connectors.commons.template.service.ConnectorService;
import org.mule.extension.bedrock.api.params.BedrockAgentsFilteringParameters;
import org.mule.extension.bedrock.api.params.BedrockAgentsParameters;
import org.mule.extension.bedrock.api.params.BedrockAgentsSessionParameters;
import org.mule.extension.bedrock.api.params.BedrockParameters;

public interface AgentService extends ConnectorService {

  public String definePromptTemplate(String promptTemplate, String instructions, String dataset,
                                     BedrockParameters bedrockParameters);

  public String listAgents(BedrockAgentsParameters bedrockAgentsParameters);

  public String getAgentById(String agentId, BedrockAgentsParameters bedrockAgentsParameters);

  public String chatWithAgent(String agentId, String agentAliasId,
                              String prompt,
                              boolean enableTrace, boolean latencyOptimized,
                              BedrockAgentsSessionParameters bedrockSessionParameters,
                              BedrockAgentsFilteringParameters bedrockAgentsFilteringParameters,
                              BedrockAgentsParameters bedrockAgentsParameters);

  public InputStream chatWithAgentSSEStream(String agentId, String agentAliasId,
                                            String prompt,
                                            boolean enableTrace, boolean latencyOptimized,
                                            BedrockAgentsSessionParameters bedrockSessionParameters,
                                            BedrockAgentsFilteringParameters bedrockAgentsFilteringParameters,
                                            BedrockAgentsParameters bedrockAgentsParameters);

}
