package com.mulesoft.connectors.bedrock.internal.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
import java.util.function.LongFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.BedrockClientBuilder;
import software.amazon.awssdk.services.bedrock.model.GetFoundationModelRequest;
import software.amazon.awssdk.services.bedrock.model.GetFoundationModelResponse;
import software.amazon.awssdk.services.bedrock.model.ListFoundationModelsResponse;
import software.amazon.awssdk.services.bedrockagent.BedrockAgentClient;
import software.amazon.awssdk.services.bedrockagent.BedrockAgentClientBuilder;
import software.amazon.awssdk.services.bedrockagent.model.GetAgentRequest;
import software.amazon.awssdk.services.bedrockagent.model.GetAgentResponse;
import org.mule.runtime.extension.api.exception.ModuleException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.bedrockagent.model.ListAgentsRequest;
import software.amazon.awssdk.services.bedrockagent.model.ListAgentsResponse;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClient;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClientBuilder;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClientBuilder;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.IamClientBuilder;

@DisplayName("BedrockConnection")
class BedrockConnectionTest {

  private BedrockRuntimeClient mockRuntimeClient;
  private BedrockClient mockBedrockClient;
  private BedrockAgentClient mockAgentClient;
  private BedrockAgentRuntimeClient mockAgentRuntimeClient;
  private IamClient mockIamClient;

  @BeforeEach
  void setUp() {
    mockRuntimeClient = mock(BedrockRuntimeClient.class);
    mockBedrockClient = mock(BedrockClient.class);
    mockAgentClient = mock(BedrockAgentClient.class);
    mockAgentRuntimeClient = mock(BedrockAgentRuntimeClient.class);
    mockIamClient = mock(IamClient.class);
  }

  private BedrockConnection createConnection() {
    BedrockRuntimeClientBuilder runtimeBuilder = mock(BedrockRuntimeClientBuilder.class);
    when(runtimeBuilder.build()).thenReturn(mockRuntimeClient);

    BedrockClientBuilder bedrockBuilder = mock(BedrockClientBuilder.class);
    when(bedrockBuilder.build()).thenReturn(mockBedrockClient);

    BedrockAgentClientBuilder agentBuilder = mock(BedrockAgentClientBuilder.class);
    when(agentBuilder.build()).thenReturn(mockAgentClient);

    BedrockAgentRuntimeClientBuilder agentRuntimeBuilder = mock(BedrockAgentRuntimeClientBuilder.class);
    when(agentRuntimeBuilder.build()).thenReturn(mockAgentRuntimeClient);

    IamClientBuilder iamBuilder = mock(IamClientBuilder.class);
    when(iamBuilder.build()).thenReturn(mockIamClient);

    LongFunction<BedrockAgentRuntimeAsyncClient> agentAsyncFactory = mock(LongFunction.class);
    when(agentAsyncFactory.apply(anyLong())).thenReturn(mock(BedrockAgentRuntimeAsyncClient.class));

    LongFunction<BedrockRuntimeAsyncClient> runtimeAsyncFactory = mock(LongFunction.class);
    when(runtimeAsyncFactory.apply(anyLong())).thenReturn(mock(BedrockRuntimeAsyncClient.class));

    return new BedrockConnection("us-east-1", runtimeBuilder, bedrockBuilder,
                                 agentBuilder, agentRuntimeBuilder, iamBuilder,
                                 60_000, agentAsyncFactory, runtimeAsyncFactory);
  }

  @Test
  @DisplayName("getRegion returns configured region")
  void getRegion() {
    BedrockConnection conn = createConnection();
    assertThat(conn.getRegion()).isEqualTo("us-east-1");
  }

  @Test
  @DisplayName("getConnectionTimeoutMs returns configured timeout")
  void getConnectionTimeoutMs() {
    BedrockConnection conn = createConnection();
    assertThat(conn.getConnectionTimeoutMs()).isEqualTo(60_000);
  }

  @Test
  @DisplayName("getBedrockRuntimeClient returns client from builder")
  void getBedrockRuntimeClient() {
    BedrockConnection conn = createConnection();
    assertThat(conn.getBedrockRuntimeClient()).isSameAs(mockRuntimeClient);
  }

  @Test
  @DisplayName("getBedrockClient returns client from builder")
  void getBedrockClient() {
    BedrockConnection conn = createConnection();
    assertThat(conn.getBedrockClient()).isSameAs(mockBedrockClient);
  }

  @Test
  @DisplayName("getBedrockAgentClient returns client from builder")
  void getBedrockAgentClient() {
    BedrockConnection conn = createConnection();
    assertThat(conn.getBedrockAgentClient()).isSameAs(mockAgentClient);
  }

  @Test
  @DisplayName("getIamClient returns client from builder")
  void getIamClient() {
    BedrockConnection conn = createConnection();
    assertThat(conn.getIamClient()).isSameAs(mockIamClient);
  }

  @Test
  @DisplayName("listFoundationalModels delegates to bedrock client")
  void listFoundationalModels() {
    when(mockBedrockClient.listFoundationModels(any(Consumer.class)))
        .thenReturn(ListFoundationModelsResponse.builder().build());
    BedrockConnection conn = createConnection();
    assertThat(conn.listFoundationalModels()).isNotNull();
  }

  @Test
  @DisplayName("getFoundationModel delegates to bedrock client")
  void getFoundationModel() {
    when(mockBedrockClient.getFoundationModel(any(GetFoundationModelRequest.class)))
        .thenReturn(GetFoundationModelResponse.builder().build());
    BedrockConnection conn = createConnection();
    assertThat(conn.getFoundationModel(GetFoundationModelRequest.builder().modelIdentifier("amazon.nova-lite-v1:0").build()))
        .isNotNull();
  }

  @Test
  @DisplayName("answerPrompt delegates to runtime client")
  void answerPrompt() {
    when(mockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder().build());
    BedrockConnection conn = createConnection();
    assertThat(conn.answerPrompt(InvokeModelRequest.builder().modelId("amazon.nova-lite-v1:0").build())).isNotNull();
  }

  @Test
  @DisplayName("invokeModel delegates to runtime client")
  void invokeModel() {
    when(mockRuntimeClient.invokeModel(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder().build());
    BedrockConnection conn = createConnection();
    assertThat(conn.invokeModel(InvokeModelRequest.builder().modelId("amazon.titan-embed-text-v1").build())).isNotNull();
  }

  @Test
  @DisplayName("listAgents delegates to agent client")
  void listAgents() {
    when(mockAgentClient.listAgents(any(ListAgentsRequest.class)))
        .thenReturn(ListAgentsResponse.builder().build());
    BedrockConnection conn = createConnection();
    assertThat(conn.listAgents(ListAgentsRequest.builder().build())).isNotNull();
  }

  @Test
  @DisplayName("getAgent delegates to agent client")
  void getAgent() {
    when(mockAgentClient.getAgent(any(GetAgentRequest.class)))
        .thenReturn(GetAgentResponse.builder().build());
    BedrockConnection conn = createConnection();
    assertThat(conn.getAgent(GetAgentRequest.builder().agentId("AGENT1").build())).isNotNull();
  }

  @Test
  @DisplayName("getOrCreateBedrockRuntimeAsyncClient returns client from factory")
  void getOrCreateBedrockRuntimeAsyncClient() {
    BedrockConnection conn = createConnection();
    assertThat(conn.getOrCreateBedrockRuntimeAsyncClient(60_000)).isNotNull();
    assertThat(conn.getOrCreateBedrockRuntimeAsyncClient(60_000)).isSameAs(conn.getOrCreateBedrockRuntimeAsyncClient(60_000));
  }

  @Test
  @DisplayName("getOrCreateBedrockAgentRuntimeAsyncClient returns client from factory and caches by timeout")
  void getOrCreateBedrockAgentRuntimeAsyncClient() {
    BedrockConnection conn = createConnection();
    assertThat(conn.getOrCreateBedrockAgentRuntimeAsyncClient(60_000)).isNotNull();
    assertThat(conn.getOrCreateBedrockAgentRuntimeAsyncClient(60_000))
        .isSameAs(conn.getOrCreateBedrockAgentRuntimeAsyncClient(60_000));
  }

  @Test
  @DisplayName("getBedrockRuntimeAsyncClient returns client")
  void getBedrockRuntimeAsyncClient() {
    BedrockConnection conn = createConnection();
    assertThat(conn.getBedrockRuntimeAsyncClient()).isNotNull();
  }

  @Test
  @DisplayName("validate throws ModuleException when listFoundationModels throws SdkClientException with unable to load credentials")
  void validateThrowsWhenUnableToLoadCredentials() {
    when(mockBedrockClient.listFoundationModels(any(Consumer.class)))
        .thenThrow(SdkClientException.builder().message("Unable to load credentials from").build());
    BedrockConnection conn = createConnection();
    assertThatThrownBy(conn::validate).isInstanceOf(ModuleException.class).hasMessageContaining("Invalid credentials");
  }

  @Test
  @DisplayName("validate throws when listFoundationModels throws AWS BedrockException with 403")
  void validateThrowsWhen403() {
    when(mockBedrockClient.listFoundationModels(any(Consumer.class)))
        .thenThrow(software.amazon.awssdk.services.bedrock.model.BedrockException.builder().statusCode(403).build());
    BedrockConnection conn = createConnection();
    assertThatThrownBy(conn::validate).isInstanceOf(ModuleException.class).hasMessageContaining("Invalid credentials");
  }

  @Test
  @DisplayName("validate succeeds when listFoundationModels returns")
  void validateSucceeds() {
    when(mockBedrockClient.listFoundationModels(any(Consumer.class))).thenReturn(ListFoundationModelsResponse.builder().build());
    BedrockConnection conn = createConnection();
    conn.validate();
  }
}
