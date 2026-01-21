package org.mule.extension.bedrock.internal.operations;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

import java.io.InputStream;
import org.mule.extension.bedrock.api.params.BedrockAgentsFilteringParameters;
import org.mule.extension.bedrock.api.params.BedrockAgentsParameters;
import org.mule.extension.bedrock.api.params.BedrockAgentsSessionParameters;
import org.mule.extension.bedrock.api.params.BedrockParameters;
import org.mule.extension.bedrock.internal.config.BedrockConfiguration;
import org.mule.extension.bedrock.internal.connection.BedrockConnection;
import org.mule.extension.bedrock.internal.metadata.provider.BedrockErrorsProvider;
import org.mule.extension.bedrock.internal.service.AgentService;
import org.mule.extension.bedrock.internal.service.AgentServiceImpl;
import org.mule.extension.bedrock.internal.util.BedrockModelFactory;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.execution.Execution;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;


public class AgentOperations extends BedrockOperation<AgentService> {

  public AgentOperations() {
    super(AgentServiceImpl::new);
  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Execution(ExecutionType.BLOCKING)
  @Alias("AGENT-define-prompt-template")
  public InputStream definePromptTemplate(String template, String instructions, String dataset,
                                          @Config BedrockConfiguration configuration,
                                          @Connection BedrockConnection connection,
                                          @ParameterGroup(
                                              name = "Additional properties") BedrockParameters bedrockParameters) {
    return newExecutionBuilder(configuration, connection)
        .execute(AgentService::definePromptTemplate, BedrockModelFactory::createInputStream)
        .withParam(template)
        .withParam(instructions)
        .withParam(dataset)
        .withParam(bedrockParameters);
  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Execution(ExecutionType.BLOCKING)
  @Alias("AGENT-list")
  public InputStream listAgents(@Config BedrockConfiguration configuration,
                                @Connection BedrockConnection connection,
                                @ParameterGroup(
                                    name = "Additional properties") BedrockAgentsParameters bedrockAgentsParameters) {
    return newExecutionBuilder(configuration, connection)
        .execute(AgentService::listAgents, BedrockModelFactory::createInputStream)
        .withParam(bedrockAgentsParameters);

  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("AGENT-get-by-id")
  @Execution(ExecutionType.BLOCKING)
  public InputStream getAgentById(String agentId, @Config BedrockConfiguration configuration,
                                  @Connection BedrockConnection connection,
                                  @ParameterGroup(
                                      name = "Additional properties") BedrockAgentsParameters bedrockAgentsParameters) {
    return newExecutionBuilder(configuration, connection)
        .execute(AgentService::getAgentById, BedrockModelFactory::createInputStream)
        .withParam(agentId)
        .withParam(bedrockAgentsParameters);
  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("AGENT-get-by-name")
  @Execution(ExecutionType.BLOCKING)
  public InputStream getAgentByName(String agentName, @Config BedrockConfiguration configuration,
                                    @Connection BedrockConnection connection,
                                    @ParameterGroup(
                                        name = "Additional properties") BedrockAgentsParameters bedrockAgentsParameters) {
    return newExecutionBuilder(configuration, connection)
        .execute(AgentService::getAgentByAgentName, BedrockModelFactory::createInputStream)
        .withParam(agentName)
        .withParam(bedrockAgentsParameters);
  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("AGENT-delete-by-id")
  @Execution(ExecutionType.BLOCKING)
  public InputStream deleteAgentById(String agentId, @Config BedrockConfiguration configuration,
                                     @Connection BedrockConnection connection,
                                     @ParameterGroup(
                                         name = "Additional properties") BedrockAgentsParameters bedrockAgentsParameters) {
    return newExecutionBuilder(configuration, connection)
        .execute(AgentService::deleteAgentByAgentId, BedrockModelFactory::createInputStream)
        .withParam(agentId)
        .withParam(bedrockAgentsParameters);
  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("AGENT-create")
  @Execution(ExecutionType.BLOCKING)
  public InputStream createAgent(String agentName, String instructions, @Config BedrockConfiguration configuration,
                                 @Connection BedrockConnection connection,
                                 @ParameterGroup(
                                     name = "Additional properties") BedrockAgentsParameters bedrockAgentsParameters) {
    return newExecutionBuilder(configuration, connection)
        .execute(AgentService::createAgent, BedrockModelFactory::createInputStream)
        .withParam(agentName)
        .withParam(instructions)
        .withParam(bedrockAgentsParameters);
  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("AGENT-create-alias")
  @Execution(ExecutionType.BLOCKING)
  public InputStream createAgentAlias(String agentAlias, String agentId, @Config BedrockConfiguration configuration,
                                      @Connection BedrockConnection connection,
                                      @ParameterGroup(
                                          name = "Additional properties") BedrockAgentsParameters bedrockAgentsParameters) {
    return newExecutionBuilder(configuration, connection)
        .execute(AgentService::createAgentAlias, BedrockModelFactory::createInputStream)
        .withParam(agentAlias)
        .withParam(agentId)
        .withParam(bedrockAgentsParameters);
  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("AGENT-get-alias-by-agent-id")
  @Execution(ExecutionType.BLOCKING)
  public InputStream getAliasByAgentId(String agentId, @Config BedrockConfiguration configuration,
                                       @Connection BedrockConnection connection,
                                       @ParameterGroup(
                                           name = "Additional properties") BedrockAgentsParameters bedrockAgentsParameters) {
    return newExecutionBuilder(configuration, connection)
        .execute(AgentService::getAgentAliasById, BedrockModelFactory::createInputStream)
        .withParam(agentId)
        .withParam(bedrockAgentsParameters);
  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("AGENT-delete-agent-aliases")
  @Execution(ExecutionType.BLOCKING)
  public InputStream deleteAgentAlias(String agentId, String agentAliasName,
                                      @Config BedrockConfiguration configuration,
                                      @Connection BedrockConnection connection,
                                      @ParameterGroup(
                                          name = "Additional properties") BedrockAgentsParameters bedrockAgentsParameters) {
    return newExecutionBuilder(configuration, connection)
        .execute(AgentService::deleteAgentAlias, BedrockModelFactory::createInputStream)
        .withParam(agentId)
        .withParam(agentAliasName)
        .withParam(bedrockAgentsParameters);

  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("AGENT-chat")
  @Execution(ExecutionType.BLOCKING)
  public InputStream chatWithAgent(String agentId, String agentAliasId,
                                   String prompt,
                                   boolean enableTrace, boolean latencyOptimized,
                                   @Config BedrockConfiguration configuration,
                                   @Connection BedrockConnection connection,
                                   @ParameterGroup(
                                       name = "Session properties") BedrockAgentsSessionParameters bedrockAgentsSessionParameters,
                                   @ParameterGroup(
                                       name = "Knowledge Base Metadata Filtering") BedrockAgentsFilteringParameters awsBedrockAgentsFilteringParameters,
                                   @ParameterGroup(
                                       name = "Additional properties") BedrockAgentsParameters bedrockAgentsParameters) {
    return newExecutionBuilder(configuration, connection)
        .execute(AgentService::chatWithAgent, BedrockModelFactory::createInputStream)
        .withParam(agentId)
        .withParam(agentAliasId)
        .withParam(prompt)
        .withParam(enableTrace)
        .withParam(latencyOptimized)
        .withParam(bedrockAgentsSessionParameters)
        .withParam(awsBedrockAgentsFilteringParameters)
        .withParam(bedrockAgentsParameters);

  }

  @MediaType(value = "text/event-stream", strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("AGENT-chat-streaming-SSE")
  @Execution(ExecutionType.BLOCKING)
  @DisplayName("Agent chat streaming (SSE)")
  public InputStream chatWithAgentSSEStream(String agentId, String agentAliasId,
                                            String prompt,
                                            boolean enableTrace, boolean latencyOptimized,
                                            @Config BedrockConfiguration configuration,
                                            @Connection BedrockConnection connection,
                                            @ParameterGroup(
                                                name = "Session properties") BedrockAgentsSessionParameters bedrockAgentsSessionParameters,
                                            @ParameterGroup(
                                                name = "Knowledge Base Metadata Filtering") BedrockAgentsFilteringParameters awsBedrockAgentsFilteringParameters,
                                            @ParameterGroup(
                                                name = "Additional properties") BedrockAgentsParameters bedrockAgentsParameters) {
    return newExecutionBuilder(configuration, connection)
        .execute(AgentService::chatWithAgentSSEStream)
        .withParam(agentId)
        .withParam(agentAliasId)
        .withParam(prompt)
        .withParam(enableTrace)
        .withParam(latencyOptimized)
        .withParam(bedrockAgentsSessionParameters)
        .withParam(awsBedrockAgentsFilteringParameters)
        .withParam(bedrockAgentsParameters);

  }



}
