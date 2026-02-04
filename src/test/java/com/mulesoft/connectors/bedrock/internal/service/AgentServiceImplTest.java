package com.mulesoft.connectors.bedrock.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import com.mulesoft.connectors.bedrock.internal.error.exception.BedrockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.api.enums.TimeUnitEnum;
import com.mulesoft.connectors.bedrock.api.params.BedrockAgentsResponseParameters;
import com.mulesoft.connectors.bedrock.api.params.BedrockAgentsSessionParameters;
import com.mulesoft.connectors.bedrock.api.params.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import com.mulesoft.connectors.bedrock.internal.support.IntegrationTestParamHelper;
import software.amazon.awssdk.services.bedrockagent.model.Agent;
import software.amazon.awssdk.services.bedrockagent.model.AgentStatus;
import software.amazon.awssdk.services.bedrockagent.model.AgentSummary;
import software.amazon.awssdk.services.bedrockagent.model.GetAgentResponse;
import software.amazon.awssdk.services.bedrockagent.model.ListAgentsResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@DisplayName("AgentServiceImpl")
class AgentServiceImplTest {

  @Test
  @DisplayName("can be constructed with config and connection")
  void constructor() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    assertThat(service).isNotNull();
  }

  @Test
  @DisplayName("implements AgentService and extends BedrockServiceImpl")
  void typeHierarchy() {
    AgentServiceImpl service = new AgentServiceImpl(mock(BedrockConfiguration.class), mock(BedrockConnection.class));
    assertThat(service).isInstanceOf(AgentService.class);
    assertThat(service).isInstanceOf(BedrockServiceImpl.class);
  }

  @Test
  @DisplayName("listAgents returns JSON with agentNames when connection returns response")
  void listAgentsReturnsJson() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    AgentSummary summary = AgentSummary.builder().agentName("TestAgent").agentId("AGENT1").build();
    when(connection.listAgents(any()))
        .thenReturn(ListAgentsResponse.builder().agentSummaries(summary).build());

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    String result = service.listAgents();

    assertThat(result).isNotBlank();
    assertThat(result).contains("agentNames");
    assertThat(result).contains("TestAgent");
  }

  @Test
  @DisplayName("listAgents returns JSON with empty array when no agents")
  void listAgentsReturnsEmptyArray() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.listAgents(any())).thenReturn(ListAgentsResponse.builder().build());

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    String result = service.listAgents();

    assertThat(result).contains("agentNames");
    assertThat(result).contains("[]");
  }

  @Test
  @DisplayName("getAgentById returns JSON with agent details when connection returns response")
  void getAgentByIdReturnsJson() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    Agent agent = Agent.builder()
        .agentId("AGENT1")
        .agentName("TestAgent")
        .agentArn("arn:aws:bedrock:us-east-1:123:agent/AGENT1")
        .agentStatus(AgentStatus.PREPARED)
        .build();
    when(connection.getAgent(any())).thenReturn(GetAgentResponse.builder().agent(agent).build());

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    String result = service.getAgentById("AGENT1");

    assertThat(result).isNotBlank();
    assertThat(result).contains("AGENT1");
    assertThat(result).contains("TestAgent");
  }

  @Test
  @DisplayName("getAgentById returns full JSON when agent has all fields")
  void getAgentByIdReturnsFullJson() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    Agent agent = Agent.builder()
        .agentId("AGENT1")
        .agentName("TestAgent")
        .agentArn("arn:aws:bedrock:us-east-1:123:agent/AGENT1")
        .agentStatus(AgentStatus.PREPARED)
        .agentResourceRoleArn("roleArn")
        .clientToken("token")
        .description("desc")
        .foundationModel("amazon.nova-lite-v1")
        .idleSessionTTLInSeconds(600)
        .instruction("instruction")
        .build();
    when(connection.getAgent(any())).thenReturn(GetAgentResponse.builder().agent(agent).build());

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    String result = service.getAgentById("AGENT1");

    assertThat(result).contains("AGENT1");
    assertThat(result).contains("roleArn");
    assertThat(result).contains("desc");
  }

  @Test
  @DisplayName("definePromptTemplate returns formatted response when connection succeeds")
  void definePromptTemplateSuccess() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    when(connection.answerPrompt(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder()
            .body(software.amazon.awssdk.core.SdkBytes.fromUtf8String("{\"completion\":\"Hello\"}"))
            .build());

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.5f, 50);
    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    String result = service.definePromptTemplate("Hello {{instructions}}", "instr", "data", params);

    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("definePromptTemplate throws when connection throws SdkClientException")
  void definePromptTemplateThrowsOnClientError() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    when(connection.answerPrompt(any(InvokeModelRequest.class)))
        .thenThrow(software.amazon.awssdk.core.exception.SdkClientException.builder().message("network").build());

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.5f, 50);
    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    org.assertj.core.api.Assertions.assertThatThrownBy(
                                                       () -> service.definePromptTemplate("Hi", "instr", "data", params))
        .isInstanceOf(BedrockException.class);
  }

  @Test
  @DisplayName("chatWithAgent returns JSON when invokeAgent completes with no chunks")
  void chatWithAgentReturnsWhenInvokeCompletes() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.invokeAgent(any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

    BedrockAgentsSessionParameters sessionParams = IntegrationTestParamHelper.sessionParams("sess-1", false, 2);
    BedrockAgentsResponseParameters responseParams =
        IntegrationTestParamHelper.responseParams(30, TimeUnitEnum.SECONDS, false, 3, 1000L);

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    String result = service.chatWithAgent(
                                          "agentId", "aliasId", "Hello", false, false,
                                          sessionParams,
                                          null,
                                          null,
                                          responseParams,
                                          null);

    assertThat(result).isNotBlank();
    assertThat(result).contains("sessionId");
    assertThat(result).contains("agentId");
    assertThat(result).contains("chunks");
  }

  @Test
  @DisplayName("chatWithAgent with empty sessionId uses UUID")
  void chatWithAgentEmptySessionId() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.invokeAgent(any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

    BedrockAgentsSessionParameters sessionParams = IntegrationTestParamHelper.sessionParams("", false, null);
    BedrockAgentsResponseParameters responseParams =
        IntegrationTestParamHelper.responseParams(10, TimeUnitEnum.MINUTES, false, 1, 2000L);

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    String result = service.chatWithAgent(
                                          "aid", "alias", "Hi", false, false,
                                          sessionParams,
                                          null,
                                          null,
                                          responseParams,
                                          null);

    assertThat(result).contains("sessionId");
  }

  @Test
  @DisplayName("chatWithAgent with requestTimeout in milliseconds")
  void chatWithAgentTimeoutMilliseconds() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.invokeAgent(any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

    BedrockAgentsSessionParameters sessionParams = IntegrationTestParamHelper.sessionParams("s1", true, 3);
    BedrockAgentsResponseParameters responseParams =
        IntegrationTestParamHelper.responseParams(5000, TimeUnitEnum.MILLISECONDS, true, 2, 500L);

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    String result = service.chatWithAgent(
                                          "agentId", "aliasId", "Prompt", true, true,
                                          sessionParams,
                                          null,
                                          null,
                                          responseParams,
                                          null);

    assertThat(result).isNotBlank();
  }
}
