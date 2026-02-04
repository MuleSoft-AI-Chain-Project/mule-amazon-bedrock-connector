package com.mulesoft.connectors.bedrock.internal.service;

import java.io.InputStream;
import org.mule.connectors.commons.template.service.ConnectorService;
import com.mulesoft.connectors.bedrock.api.params.BedrockAgentsFilteringParameters;
import com.mulesoft.connectors.bedrock.api.params.BedrockAgentsMultipleFilteringParameters;
import com.mulesoft.connectors.bedrock.api.params.BedrockAgentsResponseLoggingParameters;
import com.mulesoft.connectors.bedrock.api.params.BedrockAgentsResponseParameters;
import com.mulesoft.connectors.bedrock.api.params.BedrockAgentsSessionParameters;
import com.mulesoft.connectors.bedrock.api.params.BedrockParameters;

public interface AgentService extends ConnectorService {

  String definePromptTemplate(String promptTemplate, String instructions, String dataset,
                              BedrockParameters bedrockParameters);

  String listAgents();

  String getAgentById(String agentId);

  String chatWithAgent(String agentId, String agentAliasId,
                       String prompt,
                       boolean enableTrace, boolean latencyOptimized,
                       BedrockAgentsSessionParameters bedrockSessionParameters,
                       BedrockAgentsFilteringParameters bedrockAgentsFilteringParameters,
                       BedrockAgentsMultipleFilteringParameters bedrockAgentsMultipleFilteringParameters,
                       BedrockAgentsResponseParameters bedrockAgentsResponseParameters,
                       BedrockAgentsResponseLoggingParameters bedrockAgentsResponseLoggingParameters);

  InputStream chatWithAgentSSEStream(String agentId, String agentAliasId,
                                     String prompt,
                                     boolean enableTrace, boolean latencyOptimized,
                                     BedrockAgentsSessionParameters bedrockSessionParameters,
                                     BedrockAgentsFilteringParameters bedrockAgentsFilteringParameters,
                                     BedrockAgentsMultipleFilteringParameters bedrockAgentsMultipleFilteringParameters,
                                     BedrockAgentsResponseParameters bedrockAgentsResponseParameters,
                                     BedrockAgentsResponseLoggingParameters bedrockAgentsResponseLoggingParameters);

}
