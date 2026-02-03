package org.mule.extension.bedrock.internal.service;

import java.io.InputStream;
import org.mule.connectors.commons.template.service.ConnectorService;
import org.mule.extension.bedrock.api.params.BedrockAgentsFilteringParameters;
import org.mule.extension.bedrock.api.params.BedrockAgentsMultipleFilteringParameters;
import org.mule.extension.bedrock.api.params.BedrockAgentsResponseLoggingParameters;
import org.mule.extension.bedrock.api.params.BedrockAgentsResponseParameters;
import org.mule.extension.bedrock.api.params.BedrockAgentsSessionParameters;
import org.mule.extension.bedrock.api.params.BedrockParameters;

public interface AgentService extends ConnectorService {

  public String definePromptTemplate(String promptTemplate, String instructions, String dataset,
                                     BedrockParameters bedrockParameters);

  public String listAgents();

  public String getAgentById(String agentId);

  public String chatWithAgent(String agentId, String agentAliasId,
                              String prompt,
                              boolean enableTrace, boolean latencyOptimized,
                              BedrockAgentsSessionParameters bedrockSessionParameters,
                              BedrockAgentsFilteringParameters bedrockAgentsFilteringParameters,
                              BedrockAgentsMultipleFilteringParameters bedrockAgentsMultipleFilteringParameters,
                              BedrockAgentsResponseParameters bedrockAgentsResponseParameters,
                              BedrockAgentsResponseLoggingParameters bedrockAgentsResponseLoggingParameters);


  public InputStream chatWithAgentSSEStream(String agentId, String agentAliasId,
                                            String prompt,
                                            boolean enableTrace, boolean latencyOptimized,
                                            BedrockAgentsSessionParameters bedrockSessionParameters,
                                            BedrockAgentsFilteringParameters bedrockAgentsFilteringParameters,
                                            BedrockAgentsMultipleFilteringParameters bedrockAgentsMultipleFilteringParameters,
                                            BedrockAgentsResponseParameters bedrockAgentsResponseParameters,
                                            BedrockAgentsResponseLoggingParameters bedrockAgentsResponseLoggingParameters);

}
