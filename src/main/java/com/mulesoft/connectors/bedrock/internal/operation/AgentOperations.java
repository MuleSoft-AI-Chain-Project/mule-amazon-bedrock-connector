package com.mulesoft.connectors.bedrock.internal.operation;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

import java.io.InputStream;
import com.mulesoft.connectors.bedrock.api.parameter.BedrockAgentsFilteringParameters;
import com.mulesoft.connectors.bedrock.api.parameter.BedrockAgentsMultipleFilteringParameters;
import com.mulesoft.connectors.bedrock.api.parameter.BedrockAgentsResponseLoggingParameters;
import com.mulesoft.connectors.bedrock.api.parameter.BedrockAgentsResponseParameters;
import com.mulesoft.connectors.bedrock.api.parameter.BedrockAgentsSessionParameters;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import com.mulesoft.connectors.bedrock.internal.error.provider.BedrockErrorsProvider;
import com.mulesoft.connectors.bedrock.internal.service.AgentService;
import com.mulesoft.connectors.bedrock.internal.service.AgentServiceImpl;
import com.mulesoft.connectors.bedrock.internal.util.BedrockModelFactory;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.execution.Execution;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;


public class AgentOperations extends BedrockOperation<AgentService> {

  public AgentOperations() {
    super(AgentServiceImpl::new);
  }

  /**
   * Defines a prompt template with instructions and dataset for use in agent operations.
   *
   * @param template The prompt template string with placeholders for instructions and dataset.
   * @param instructions Instructions to be inserted into the template.
   * @param dataset Dataset to be inserted into the template.
   * @param configuration Configuration for Bedrock connector.
   * @param connection Bedrock connection instance.
   * @param bedrockParameters Additional properties including model name, region, temperature, topP, topK, maxTokenCount, and
   *        optional guardrail identifier.
   * @return InputStream containing the JSON response with the processed prompt template.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Execution(ExecutionType.BLOCKING)
  @Alias("AGENT-define-prompt-template")
  @Summary("Define or update the agents prompt template")
  public InputStream definePromptTemplate(@Config BedrockConfiguration configuration,
                                          @Connection BedrockConnection connection,
                                          @ParameterGroup(
                                              name = "Additional properties") BedrockParameters bedrockParameters,
                                          String template, String instructions, String dataset) {
    return newExecutionBuilder(configuration, connection)
        .execute(AgentService::definePromptTemplate, BedrockModelFactory::createInputStream)
        .withParam(template)
        .withParam(instructions)
        .withParam(dataset)
        .withParam(bedrockParameters);
  }

  /**
   * Lists all Bedrock agents available in the AWS account.
   *
   * @param configuration Configuration for Bedrock connector.
   * @param connection Bedrock connection instance.
   * @return InputStream containing the JSON response with list of agents including agent IDs, names, and statuses.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Execution(ExecutionType.BLOCKING)
  @Alias("AGENT-list")
  @Summary("List all available agents")
  public InputStream listAgents(@Config BedrockConfiguration configuration,
                                @Connection BedrockConnection connection) {
    return newExecutionBuilder(configuration, connection)
        .execute(AgentService::listAgents, BedrockModelFactory::createInputStream);

  }

  /**
   * Retrieves details of a Bedrock agent by its unique identifier.
   *
   * @param agentId The unique identifier of the agent to retrieve.
   * @param configuration Configuration for Bedrock connector.
   * @param connection Bedrock connection instance.
   * @return InputStream containing the JSON response with agent details including name, instructions, knowledge base
   *         configuration, and status.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("AGENT-get-by-id")
  @Execution(ExecutionType.BLOCKING)
  @Summary("Retrieve agent details by agent ID")
  public InputStream getAgentById(@Config BedrockConfiguration configuration,
                                  @Connection BedrockConnection connection, String agentId) {
    return newExecutionBuilder(configuration, connection)
        .execute(AgentService::getAgentById, BedrockModelFactory::createInputStream)
        .withParam(agentId);
  }


  /**
   * Sends a chat message to a Bedrock agent and receives a response. The agent can use knowledge bases and action groups to
   * provide intelligent responses.
   *
   * @param agentId The unique identifier of the agent to chat with.
   * @param agentAliasId The alias ID of the agent version to use.
   * @param prompt The user's message or prompt to send to the agent.
   * @param enableTrace Whether to enable trace logging for debugging agent reasoning.
   * @param latencyOptimized Whether to optimize for lower latency at the cost of some functionality.
   * @param configuration Configuration for Bedrock connector.
   * @param connection Bedrock connection instance.
   * @param bedrockAgentsSessionParameters Session properties including session ID for maintaining conversation context.
   * @param bedrockAgentsFilteringParameters Knowledge base metadata filtering parameters for querying specific documents.
   * @return InputStream containing the JSON response with agent's reply, citations, and trace information (if enabled).
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("AGENT-chat")
  @Execution(ExecutionType.BLOCKING)
  @Summary("Invoke an agent for a chat-based interaction")
  public InputStream chatWithAgent(@Config BedrockConfiguration configuration,
                                   @Connection BedrockConnection connection,
                                   @ParameterGroup(
                                       name = "Session properties") BedrockAgentsSessionParameters bedrockAgentsSessionParameters,
                                   @ParameterGroup(
                                       name = "Knowledge Base Metadata Filtering (for single KB Id)") BedrockAgentsFilteringParameters bedrockAgentsFilteringParameters,
                                   @ParameterGroup(
                                       name = "Knowledge Base Metadata Filtering (for multiple KB Ids)") BedrockAgentsMultipleFilteringParameters bedrockAgentsMultipleFilteringParameters,
                                   @ParameterGroup(
                                       name = "Response") BedrockAgentsResponseParameters bedrockAgentsResponseParameters,
                                   @ParameterGroup(
                                       name = "Response Logging") BedrockAgentsResponseLoggingParameters bedrockAgentsResponseLoggingParameters,
                                   String agentId, String agentAliasId,
                                   String prompt,
                                   boolean enableTrace, boolean latencyOptimized) {
    return newExecutionBuilder(configuration, connection)
        .execute(AgentService::chatWithAgent, BedrockModelFactory::createInputStream)
        .withParam(agentId)
        .withParam(agentAliasId)
        .withParam(prompt)
        .withParam(enableTrace)
        .withParam(latencyOptimized)
        .withParam(bedrockAgentsSessionParameters)
        .withParam(bedrockAgentsFilteringParameters)
        .withParam(bedrockAgentsMultipleFilteringParameters)
        .withParam(bedrockAgentsResponseParameters)
        .withParam(bedrockAgentsResponseLoggingParameters);
  }

  /**
   * Sends a chat message to a Bedrock agent and receives a streaming response using Server-Sent Events (SSE). Returns responses
   * in real-time as the agent generates them.
   *
   * @param agentId The unique identifier of the agent to chat with.
   * @param agentAliasId The alias ID of the agent version to use.
   * @param prompt The user's message or prompt to send to the agent.
   * @param enableTrace Whether to enable trace logging for debugging agent reasoning.
   * @param latencyOptimized Whether to optimize for lower latency at the cost of some functionality.
   * @param configuration Configuration for Bedrock connector.
   * @param connection Bedrock connection instance.
   * @param bedrockAgentsSessionParameters Session properties including session ID for maintaining conversation context.
   * @param bedrockAgentsFilteringParameters Knowledge base metadata filtering parameters for querying specific documents.
   * @return InputStream containing Server-Sent Events (SSE) stream with real-time agent response chunks, citations, and trace
   *         information (if enabled).
   */
  @MediaType(value = "text/event-stream", strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("AGENT-chat-streaming-SSE")
  @Execution(ExecutionType.BLOCKING)
  @DisplayName("Agent chat streaming (SSE)")
  @Summary("Invoke an agent with streaming responses using Server-Sent Events")
  public InputStream chatWithAgentSSEStream(@Config BedrockConfiguration configuration,
                                            @Connection BedrockConnection connection,
                                            @ParameterGroup(
                                                name = "Session properties") BedrockAgentsSessionParameters bedrockAgentsSessionParameters,
                                            @ParameterGroup(
                                                name = "Knowledge Base Metadata Filtering (for single KB Id)") BedrockAgentsFilteringParameters bedrockAgentsFilteringParameters,
                                            @ParameterGroup(
                                                name = "Knowledge Base Metadata Filtering (for multiple KB Ids)") BedrockAgentsMultipleFilteringParameters bedrockAgentsMultipleFilteringParameters,
                                            @ParameterGroup(
                                                name = "Response") BedrockAgentsResponseParameters bedrockAgentsResponseParameters,
                                            @ParameterGroup(
                                                name = "Response Logging") BedrockAgentsResponseLoggingParameters bedrockAgentsResponseLoggingParameters,
                                            String agentId, String agentAliasId,
                                            String prompt,
                                            boolean enableTrace, boolean latencyOptimized) {
    return newExecutionBuilder(configuration, connection)
        .execute(AgentService::chatWithAgentSSEStream)
        .withParam(agentId)
        .withParam(agentAliasId)
        .withParam(prompt)
        .withParam(enableTrace)
        .withParam(latencyOptimized)
        .withParam(bedrockAgentsSessionParameters)
        .withParam(bedrockAgentsFilteringParameters)
        .withParam(bedrockAgentsMultipleFilteringParameters)
        .withParam(bedrockAgentsResponseParameters)
        .withParam(bedrockAgentsResponseLoggingParameters);

  }



}
