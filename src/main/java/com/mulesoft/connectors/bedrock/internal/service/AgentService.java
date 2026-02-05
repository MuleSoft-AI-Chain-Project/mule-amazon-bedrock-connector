package com.mulesoft.connectors.bedrock.internal.service;

import java.io.InputStream;
import org.mule.connectors.commons.template.service.ConnectorService;
import com.mulesoft.connectors.bedrock.api.parameter.BedrockAgentsFilteringParameters;
import com.mulesoft.connectors.bedrock.api.parameter.BedrockAgentsMultipleFilteringParameters;
import com.mulesoft.connectors.bedrock.api.parameter.BedrockAgentsResponseLoggingParameters;
import com.mulesoft.connectors.bedrock.api.parameter.BedrockAgentsResponseParameters;
import com.mulesoft.connectors.bedrock.api.parameter.BedrockAgentsSessionParameters;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParameters;

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
