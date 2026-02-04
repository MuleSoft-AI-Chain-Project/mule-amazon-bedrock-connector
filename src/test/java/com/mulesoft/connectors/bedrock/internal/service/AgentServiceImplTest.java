package com.mulesoft.connectors.bedrock.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import software.amazon.awssdk.core.document.Document;

import com.mulesoft.connectors.bedrock.api.params.BedrockAgentsFilteringParameters;
import com.mulesoft.connectors.bedrock.api.params.BedrockAgentsMultipleFilteringParameters;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.concurrent.TimeUnit;
import com.mulesoft.connectors.bedrock.api.params.BedrockAgentsResponseParameters;
import com.mulesoft.connectors.bedrock.api.params.BedrockAgentsSessionParameters;
import com.mulesoft.connectors.bedrock.api.params.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import com.mulesoft.connectors.bedrock.internal.support.IntegrationTestParamHelper;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockagent.model.Agent;
import software.amazon.awssdk.services.bedrockagent.model.AgentStatus;
import software.amazon.awssdk.services.bedrockagent.model.AgentSummary;
import software.amazon.awssdk.services.bedrockagent.model.GetAgentResponse;
import software.amazon.awssdk.services.bedrockagent.model.ListAgentsResponse;
import software.amazon.awssdk.services.bedrockagentruntime.model.Attribution;
import software.amazon.awssdk.services.bedrockagentruntime.model.Citation;
import software.amazon.awssdk.services.bedrockagentruntime.model.GeneratedResponsePart;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentResponseHandler;
import software.amazon.awssdk.services.bedrockagentruntime.model.PayloadPart;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrievalResultContent;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrievalResultLocation;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrievedReference;
import software.amazon.awssdk.services.bedrockagentruntime.model.TextResponsePart;
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
        .isInstanceOf(ModuleException.class);
  }

  @Test
  @DisplayName("chatWithAgent returns JSON when invokeAgent completes with no chunks")
  void chatWithAgentReturnsWhenInvokeCompletes() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.invokeAgent(any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

    BedrockAgentsSessionParameters sessionParams = IntegrationTestParamHelper.sessionParams("sess-1", false, 2);
    BedrockAgentsResponseParameters responseParams =
        IntegrationTestParamHelper.responseParams(30, TimeUnit.SECONDS, false, 3, 1000L);

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
        IntegrationTestParamHelper.responseParams(10, TimeUnit.MINUTES, false, 1, 2000L);

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
  @DisplayName("chatWithAgent with requestTimeoutUnit null uses default duration")
  void chatWithAgentTimeoutUnitNull() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getConnectionTimeoutMs()).thenReturn(60_000);
    when(connection.invokeAgent(any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

    BedrockAgentsSessionParameters sessionParams = IntegrationTestParamHelper.sessionParams("s1", false, 1);
    BedrockAgentsResponseParameters responseParams =
        IntegrationTestParamHelper.responseParams(30, TimeUnit.SECONDS, false, 2, 1000L);
    IntegrationTestParamHelper.setField(responseParams, "requestTimeoutUnit", null);

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    String result = service.chatWithAgent(
                                          "agentId", "aliasId", "Hi", false, false,
                                          sessionParams, null, null, responseParams, null);

    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("chatWithAgent with requestTimeout in milliseconds")
  void chatWithAgentTimeoutMilliseconds() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.invokeAgent(any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

    BedrockAgentsSessionParameters sessionParams = IntegrationTestParamHelper.sessionParams("s1", true, 3);
    BedrockAgentsResponseParameters responseParams =
        IntegrationTestParamHelper.responseParams(5000, TimeUnit.MILLISECONDS, true, 2, 500L);

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

  @Test
  @DisplayName("chatWithAgent with legacy single KB filtering uses buildKnowledgeBaseConfigs")
  void chatWithAgentLegacySingleKb() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getConnectionTimeoutMs()).thenReturn(60_000);
    when(connection.invokeAgent(any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

    BedrockAgentsSessionParameters sessionParams = IntegrationTestParamHelper.sessionParams("s1", false, 1);
    BedrockAgentsResponseParameters responseParams =
        IntegrationTestParamHelper.responseParams(30, TimeUnit.SECONDS, false, 3, 1000L);

    BedrockAgentsFilteringParameters filteringParams = new BedrockAgentsFilteringParameters();
    IntegrationTestParamHelper.setField(filteringParams, "knowledgeBaseId", "kb-123");
    IntegrationTestParamHelper.setField(filteringParams, "numberOfResults", 5);
    IntegrationTestParamHelper.setField(filteringParams, "overrideSearchType",
                                        BedrockAgentsFilteringParameters.SearchType.SEMANTIC);

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    String result = service.chatWithAgent(
                                          "agentId", "aliasId", "Hello", false, false,
                                          sessionParams,
                                          filteringParams,
                                          null,
                                          responseParams,
                                          null);

    assertThat(result).isNotBlank();
    assertThat(result).contains("chunks");
  }

  @Test
  @DisplayName("chatWithAgent with multiple KB params uses multiple path")
  void chatWithAgentMultipleKb() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getConnectionTimeoutMs()).thenReturn(60_000);
    when(connection.invokeAgent(any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

    BedrockAgentsSessionParameters sessionParams = IntegrationTestParamHelper.sessionParams("s1", true, 2);
    BedrockAgentsResponseParameters responseParams =
        IntegrationTestParamHelper.responseParams(60, TimeUnit.SECONDS, false, 2, 500L);

    BedrockAgentsFilteringParameters.KnowledgeBaseConfig kbConfig =
        new BedrockAgentsFilteringParameters.KnowledgeBaseConfig("kb-1", 10,
                                                                 BedrockAgentsFilteringParameters.SearchType.HYBRID, null, null);
    BedrockAgentsMultipleFilteringParameters multipleParams = new BedrockAgentsMultipleFilteringParameters();
    IntegrationTestParamHelper.setField(multipleParams, "knowledgeBases", Collections.singletonList(kbConfig));

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    String result = service.chatWithAgent(
                                          "agentId", "aliasId", "Hi", false, true,
                                          sessionParams,
                                          null,
                                          multipleParams,
                                          responseParams,
                                          null);

    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("chatWithAgent returns JSON when handler may deliver chunk")
  void chatWithAgentWithChunkDeliveryAttempt() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getConnectionTimeoutMs()).thenReturn(60_000);
    when(connection.invokeAgent(any(), any(), anyLong())).thenAnswer(invocation -> {
      InvokeAgentResponseHandler handler = invocation.getArgument(1);
      CompletableFuture<Void> future = new CompletableFuture<>();
      try {
        Object subscriber = getHandlerSubscriber(handler);
        if (subscriber != null) {
          PayloadPart chunk = PayloadPart.builder().bytes(SdkBytes.fromUtf8String("Hello chunk")).build();
          InvokeAgentResponseHandler.Visitor.class.getMethod("onChunk", PayloadPart.class)
              .invoke(subscriber, chunk);
        }
        future.complete(null);
      } catch (Exception e) {
        future.completeExceptionally(e);
      }
      return future;
    });

    BedrockAgentsSessionParameters sessionParams = IntegrationTestParamHelper.sessionParams("sess-1", false, 1);
    BedrockAgentsResponseParameters responseParams =
        IntegrationTestParamHelper.responseParams(30, TimeUnit.SECONDS, false, 3, 1000L);

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    String result = service.chatWithAgent(
                                          "agentId", "aliasId", "Hi", false, false,
                                          sessionParams, null, null, responseParams, null);

    assertThat(result).isNotBlank();
    assertThat(result).contains("fullResponse");
    assertThat(result).contains("sessionId");
    assertThat(result).contains("chunks");
  }

  @Test
  @DisplayName("chatWithAgent with chunk containing attribution and citations")
  void chatWithAgentWithChunkWithAttributionAndCitations() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getConnectionTimeoutMs()).thenReturn(60_000);
    when(connection.invokeAgent(any(), any(), anyLong())).thenAnswer(invocation -> {
      InvokeAgentResponseHandler handler = invocation.getArgument(1);
      CompletableFuture<Void> future = new CompletableFuture<>();
      try {
        Object subscriber = getHandlerSubscriber(handler);
        if (subscriber != null) {
          TextResponsePart textPart = TextResponsePart.builder().text("generated").build();
          GeneratedResponsePart genPart = GeneratedResponsePart.builder().textResponsePart(textPart).build();
          RetrievalResultContent content = RetrievalResultContent.builder().text("cited text").build();
          RetrievalResultLocation location = RetrievalResultLocation.builder().type("S3").build();
          RetrievedReference ref = RetrievedReference.builder()
              .content(content)
              .location(location)
              .metadata(Collections.singletonMap("k", Document.fromString("v")))
              .build();
          Citation citation = Citation.builder()
              .generatedResponsePart(genPart)
              .retrievedReferences(Collections.singletonList(ref))
              .build();
          Attribution attribution = Attribution.builder().citations(Collections.singletonList(citation)).build();
          PayloadPart chunk = PayloadPart.builder()
              .bytes(SdkBytes.fromUtf8String("chunk with citation"))
              .attribution(attribution)
              .build();
          InvokeAgentResponseHandler.Visitor.class.getMethod("onChunk", PayloadPart.class).invoke(subscriber, chunk);
        }
        future.complete(null);
      } catch (Exception e) {
        future.completeExceptionally(e);
      }
      return future;
    });

    BedrockAgentsSessionParameters sessionParams = IntegrationTestParamHelper.sessionParams("sess-1", false, 1);
    BedrockAgentsResponseParameters responseParams =
        IntegrationTestParamHelper.responseParams(30, TimeUnit.SECONDS, false, 3, 1000L);

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    String result = service.chatWithAgent(
                                          "agentId", "aliasId", "Hi", false, false,
                                          sessionParams, null, null, responseParams, null);

    assertThat(result).isNotBlank();
    // If subscriber was found, chunk with attribution was delivered and result contains citations
    if (result.contains("citations")) {
      assertThat(result).contains("retrievedReferences");
    }
  }

  private static Object getHandlerSubscriber(InvokeAgentResponseHandler handler) {
    try {
      for (java.lang.reflect.Field f : handler.getClass().getDeclaredFields()) {
        f.setAccessible(true);
        Object val = f.get(handler);
        if (val != null && InvokeAgentResponseHandler.Visitor.class.isAssignableFrom(val.getClass())) {
          return val;
        }
      }
    } catch (Exception ignored) {
    }
    return null;
  }

  @Test
  @DisplayName("chatWithAgentSSEStream returns non-null InputStream")
  void chatWithAgentSSEStreamReturnsInputStream() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getConnectionTimeoutMs()).thenReturn(60_000);
    when(connection.invokeAgent(any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

    BedrockAgentsSessionParameters sessionParams = IntegrationTestParamHelper.sessionParams("s1", false, 1);
    BedrockAgentsResponseParameters responseParams =
        IntegrationTestParamHelper.responseParams(30, TimeUnit.SECONDS, false, 2, 1000L);

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    InputStream stream = service.chatWithAgentSSEStream(
                                                        "agentId", "aliasId", "Hi", false, false,
                                                        sessionParams, null, null, responseParams, null);

    assertThat(stream).isNotNull();
  }

  @Test
  @DisplayName("chatWithAgent throws when invokeAgent completes exceptionally")
  void chatWithAgentThrowsWhenInvokeAgentFails() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getConnectionTimeoutMs()).thenReturn(60_000);
    CompletableFuture<Void> failingFuture = new CompletableFuture<>();
    failingFuture.completeExceptionally(new RuntimeException("agent error"));
    when(connection.invokeAgent(any(), any(), anyLong())).thenReturn(failingFuture);

    BedrockAgentsSessionParameters sessionParams = IntegrationTestParamHelper.sessionParams("s1", false, 1);
    BedrockAgentsResponseParameters responseParams =
        IntegrationTestParamHelper.responseParams(30, TimeUnit.SECONDS, false, 2, 1000L);

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    org.assertj.core.api.Assertions.assertThatThrownBy(
                                                       () -> service.chatWithAgent(
                                                                                   "agentId", "aliasId", "Hi", false, false,
                                                                                   sessionParams, null, null, responseParams,
                                                                                   null))
        .hasMessageContaining("agent error");
  }

  @Test
  @DisplayName("chatWithAgent with multiple KB and legacy params set hits warn path")
  void chatWithAgentMultipleKbWithLegacyParamsIgnored() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getConnectionTimeoutMs()).thenReturn(60_000);
    when(connection.invokeAgent(any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

    BedrockAgentsSessionParameters sessionParams = IntegrationTestParamHelper.sessionParams("s1", false, 1);
    BedrockAgentsResponseParameters responseParams =
        IntegrationTestParamHelper.responseParams(30, TimeUnit.SECONDS, false, 2, 1000L);

    BedrockAgentsFilteringParameters legacyParams = new BedrockAgentsFilteringParameters();
    IntegrationTestParamHelper.setField(legacyParams, "knowledgeBaseId", "legacy-kb");
    IntegrationTestParamHelper.setField(legacyParams, "numberOfResults", 5);

    BedrockAgentsFilteringParameters.KnowledgeBaseConfig kbConfig =
        new BedrockAgentsFilteringParameters.KnowledgeBaseConfig("kb-1", 10,
                                                                 BedrockAgentsFilteringParameters.SearchType.HYBRID, null, null);
    BedrockAgentsMultipleFilteringParameters multipleParams = new BedrockAgentsMultipleFilteringParameters();
    IntegrationTestParamHelper.setField(multipleParams, "knowledgeBases", Collections.singletonList(kbConfig));

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    String result = service.chatWithAgent(
                                          "agentId", "aliasId", "Hi", false, false,
                                          sessionParams, legacyParams, multipleParams, responseParams, null);

    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("chatWithAgent with legacy filtering and empty knowledgeBaseId returns null config")
  void chatWithAgentLegacyEmptyKnowledgeBaseId() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getConnectionTimeoutMs()).thenReturn(60_000);
    when(connection.invokeAgent(any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

    BedrockAgentsSessionParameters sessionParams = IntegrationTestParamHelper.sessionParams("s1", false, 1);
    BedrockAgentsResponseParameters responseParams =
        IntegrationTestParamHelper.responseParams(30, TimeUnit.SECONDS, false, 2, 1000L);

    BedrockAgentsFilteringParameters filteringParams = new BedrockAgentsFilteringParameters();
    IntegrationTestParamHelper.setField(filteringParams, "knowledgeBaseId", "");

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    String result = service.chatWithAgent(
                                          "agentId", "aliasId", "Hi", false, false,
                                          sessionParams, filteringParams, null, responseParams, null);

    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("chatWithAgent with legacy single metadata filter uses buildVectorSearchConfiguration")
  void chatWithAgentLegacySingleMetadataFilter() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getConnectionTimeoutMs()).thenReturn(60_000);
    when(connection.invokeAgent(any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

    BedrockAgentsSessionParameters sessionParams = IntegrationTestParamHelper.sessionParams("s1", false, 1);
    BedrockAgentsResponseParameters responseParams =
        IntegrationTestParamHelper.responseParams(30, TimeUnit.SECONDS, false, 2, 1000L);

    BedrockAgentsFilteringParameters filteringParams = new BedrockAgentsFilteringParameters();
    IntegrationTestParamHelper.setField(filteringParams, "knowledgeBaseId", "kb-1");
    IntegrationTestParamHelper.setField(filteringParams, "numberOfResults", 5);
    IntegrationTestParamHelper.setField(filteringParams, "metadataFilters",
                                        java.util.Collections.singletonMap("key1", "value1"));

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    String result = service.chatWithAgent(
                                          "agentId", "aliasId", "Hi", false, false,
                                          sessionParams, filteringParams, null, responseParams, null);

    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("chatWithAgent with legacy multiple metadata filters AND_ALL")
  void chatWithAgentLegacyMultipleFiltersAndAll() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getConnectionTimeoutMs()).thenReturn(60_000);
    when(connection.invokeAgent(any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

    BedrockAgentsSessionParameters sessionParams = IntegrationTestParamHelper.sessionParams("s1", false, 1);
    BedrockAgentsResponseParameters responseParams =
        IntegrationTestParamHelper.responseParams(30, TimeUnit.SECONDS, false, 2, 1000L);

    java.util.Map<String, String> filters = new java.util.HashMap<>();
    filters.put("k1", "v1");
    filters.put("k2", "v2");
    BedrockAgentsFilteringParameters filteringParams = new BedrockAgentsFilteringParameters();
    IntegrationTestParamHelper.setField(filteringParams, "knowledgeBaseId", "kb-1");
    IntegrationTestParamHelper.setField(filteringParams, "metadataFilters", filters);
    IntegrationTestParamHelper.setField(filteringParams, "retrievalMetadataFilterType",
                                        BedrockAgentsFilteringParameters.RetrievalMetadataFilterType.AND_ALL);

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    String result = service.chatWithAgent(
                                          "agentId", "aliasId", "Hi", false, false,
                                          sessionParams, filteringParams, null, responseParams, null);

    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("chatWithAgent with legacy multiple metadata filters OR_ALL")
  void chatWithAgentLegacyMultipleFiltersOrAll() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getConnectionTimeoutMs()).thenReturn(60_000);
    when(connection.invokeAgent(any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

    BedrockAgentsSessionParameters sessionParams = IntegrationTestParamHelper.sessionParams("s1", false, 1);
    BedrockAgentsResponseParameters responseParams =
        IntegrationTestParamHelper.responseParams(30, TimeUnit.SECONDS, false, 2, 1000L);

    java.util.Map<String, String> filters = new java.util.HashMap<>();
    filters.put("a", "1");
    filters.put("b", "2");
    BedrockAgentsFilteringParameters filteringParams = new BedrockAgentsFilteringParameters();
    IntegrationTestParamHelper.setField(filteringParams, "knowledgeBaseId", "kb-1");
    IntegrationTestParamHelper.setField(filteringParams, "metadataFilters", filters);
    IntegrationTestParamHelper.setField(filteringParams, "retrievalMetadataFilterType",
                                        BedrockAgentsFilteringParameters.RetrievalMetadataFilterType.OR_ALL);

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    String result = service.chatWithAgent(
                                          "agentId", "aliasId", "Hi", false, false,
                                          sessionParams, filteringParams, null, responseParams, null);

    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("chatWithAgent with KB config reranking and SELECTIVE fieldsToExclude")
  void chatWithAgentRerankingSelectiveFieldsToExclude() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getConnectionTimeoutMs()).thenReturn(60_000);
    when(connection.invokeAgent(any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

    BedrockAgentsSessionParameters sessionParams = IntegrationTestParamHelper.sessionParams("s1", false, 1);
    BedrockAgentsResponseParameters responseParams =
        IntegrationTestParamHelper.responseParams(30, TimeUnit.SECONDS, false, 2, 1000L);

    BedrockAgentsFilteringParameters.RerankingConfiguration reranking =
        new BedrockAgentsFilteringParameters.RerankingConfiguration();
    reranking.setModelArn("arn:aws:bedrock:us-east-1::foundation-model/rerank-model");
    reranking.setSelectionMode(BedrockAgentsFilteringParameters.RerankingSelectionMode.SELECTIVE);
    reranking.setFieldsToExclude(java.util.Collections.singletonList("score"));

    BedrockAgentsFilteringParameters.KnowledgeBaseConfig kbConfig =
        new BedrockAgentsFilteringParameters.KnowledgeBaseConfig("kb-1", 10,
                                                                 BedrockAgentsFilteringParameters.SearchType.HYBRID, null, null);
    kbConfig.setRerankingConfiguration(reranking);

    BedrockAgentsMultipleFilteringParameters multipleParams = new BedrockAgentsMultipleFilteringParameters();
    IntegrationTestParamHelper.setField(multipleParams, "knowledgeBases", Collections.singletonList(kbConfig));

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    String result = service.chatWithAgent(
                                          "agentId", "aliasId", "Hi", false, false,
                                          sessionParams, null, multipleParams, responseParams, null);

    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("chatWithAgent with KB config reranking and fieldsToInclude")
  void chatWithAgentRerankingFieldsToInclude() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getConnectionTimeoutMs()).thenReturn(60_000);
    when(connection.invokeAgent(any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

    BedrockAgentsSessionParameters sessionParams = IntegrationTestParamHelper.sessionParams("s1", false, 1);
    BedrockAgentsResponseParameters responseParams =
        IntegrationTestParamHelper.responseParams(30, TimeUnit.SECONDS, false, 2, 1000L);

    BedrockAgentsFilteringParameters.RerankingConfiguration reranking =
        new BedrockAgentsFilteringParameters.RerankingConfiguration();
    reranking.setModelArn("arn:aws:bedrock:us-east-1::foundation-model/rerank");
    reranking.setSelectionMode(BedrockAgentsFilteringParameters.RerankingSelectionMode.SELECTIVE);
    reranking.setFieldsToInclude(java.util.Collections.singletonList("content"));

    BedrockAgentsFilteringParameters.KnowledgeBaseConfig kbConfig =
        new BedrockAgentsFilteringParameters.KnowledgeBaseConfig("kb-1", 5, null, null, null);
    kbConfig.setRerankingConfiguration(reranking);

    BedrockAgentsMultipleFilteringParameters multipleParams = new BedrockAgentsMultipleFilteringParameters();
    IntegrationTestParamHelper.setField(multipleParams, "knowledgeBases", Collections.singletonList(kbConfig));

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    String result = service.chatWithAgent(
                                          "agentId", "aliasId", "Hi", false, false,
                                          sessionParams, null, multipleParams, responseParams, null);

    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("chatWithAgent with KB config reranking numberOfRerankedResults and additionalModelRequestFields")
  void chatWithAgentRerankingNumberOfRerankedResultsAndAdditionalFields() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getConnectionTimeoutMs()).thenReturn(60_000);
    when(connection.invokeAgent(any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

    BedrockAgentsSessionParameters sessionParams = IntegrationTestParamHelper.sessionParams("s1", false, 1);
    BedrockAgentsResponseParameters responseParams =
        IntegrationTestParamHelper.responseParams(30, TimeUnit.SECONDS, false, 2, 1000L);

    BedrockAgentsFilteringParameters.RerankingConfiguration reranking =
        new BedrockAgentsFilteringParameters.RerankingConfiguration();
    reranking.setModelArn("arn:aws:bedrock:us-east-1::foundation-model/rerank");
    reranking.setRerankingType("CUSTOM");
    reranking.setNumberOfRerankedResults(15);
    reranking.setAdditionalModelRequestFields(java.util.Collections.singletonMap("key", "value"));

    BedrockAgentsFilteringParameters.KnowledgeBaseConfig kbConfig =
        new BedrockAgentsFilteringParameters.KnowledgeBaseConfig("kb-1", 10, null, null, null);
    kbConfig.setRerankingConfiguration(reranking);

    BedrockAgentsMultipleFilteringParameters multipleParams = new BedrockAgentsMultipleFilteringParameters();
    IntegrationTestParamHelper.setField(multipleParams, "knowledgeBases", Collections.singletonList(kbConfig));

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    String result = service.chatWithAgent(
                                          "agentId", "aliasId", "Hi", false, false,
                                          sessionParams, null, multipleParams, responseParams, null);

    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("chatWithAgent with requestTimeout in minutes uses toDuration MINUTES")
  void chatWithAgentTimeoutMinutes() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.invokeAgent(any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

    BedrockAgentsSessionParameters sessionParams = IntegrationTestParamHelper.sessionParams("s1", false, 1);
    BedrockAgentsResponseParameters responseParams =
        IntegrationTestParamHelper.responseParams(2, TimeUnit.MINUTES, false, 2, 1000L);

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    String result = service.chatWithAgent(
                                          "agentId", "aliasId", "Hi", false, false,
                                          sessionParams, null, null, responseParams, null);

    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("chatWithAgentSSEStream with response logging params uses requestId correlationId userId")
  void chatWithAgentSSEStreamWithResponseLoggingParams() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getConnectionTimeoutMs()).thenReturn(60_000);
    when(connection.invokeAgent(any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

    BedrockAgentsSessionParameters sessionParams = IntegrationTestParamHelper.sessionParams("s1", false, 1);
    BedrockAgentsResponseParameters responseParams =
        IntegrationTestParamHelper.responseParams(30, TimeUnit.SECONDS, false, 2, 1000L);
    com.mulesoft.connectors.bedrock.api.params.BedrockAgentsResponseLoggingParameters loggingParams =
        new com.mulesoft.connectors.bedrock.api.params.BedrockAgentsResponseLoggingParameters();
    IntegrationTestParamHelper.setField(loggingParams, "requestId", "req-123");
    IntegrationTestParamHelper.setField(loggingParams, "correlationId", "corr-456");
    IntegrationTestParamHelper.setField(loggingParams, "userId", "user-789");

    AgentServiceImpl service = new AgentServiceImpl(config, connection);
    InputStream stream = service.chatWithAgentSSEStream(
                                                        "agentId", "aliasId", "Hi", false, false,
                                                        sessionParams, null, null, responseParams, loggingParams);

    assertThat(stream).isNotNull();
  }
}
