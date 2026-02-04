package com.mulesoft.connectors.bedrock.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mulesoft.connectors.bedrock.internal.error.exception.BedrockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.api.params.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import com.mulesoft.connectors.bedrock.internal.support.IntegrationTestParamHelper;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@DisplayName("ChatServiceImpl")
class ChatServiceImplTest {

  @Test
  @DisplayName("can be constructed with config and connection")
  void constructor() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    assertThat(service).isNotNull();
  }

  @Test
  @DisplayName("implements ChatService and extends BedrockServiceImpl")
  void typeHierarchy() {
    ChatServiceImpl service = new ChatServiceImpl(mock(BedrockConfiguration.class), mock(BedrockConnection.class));
    assertThat(service).isInstanceOf(ChatService.class);
    assertThat(service).isInstanceOf(BedrockServiceImpl.class);
  }

  @Test
  @DisplayName("answerPrompt throws BedrockException when connection throws SdkClientException")
  void answerPromptThrowsWhenSdkClientException() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    when(connection.answerPrompt(any(InvokeModelRequest.class)))
        .thenThrow(software.amazon.awssdk.core.exception.SdkClientException.builder().message("network error").build());

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.5f, 50);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.answerPrompt("Hi", params))
        .isInstanceOf(BedrockException.class);
  }

  @Test
  @DisplayName("answerPrompt returns formatted response when connection returns success")
  void answerPromptReturnsFormattedResponse() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    InvokeModelResponse response = InvokeModelResponse.builder()
        .body(SdkBytes.fromUtf8String("{\"output\":{\"text\":\"Hello from Nova\"}}"))
        .build();
    when(connection.answerPrompt(any(InvokeModelRequest.class))).thenReturn(response);

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.5f, 50);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    String result = service.answerPrompt("Say hello.", params);

    assertThat(result).isNotBlank();
    assertThat(result).contains("output");
    assertThat(result).contains("Hello from Nova");
  }

  @Test
  @DisplayName("answerPrompt with anthropic model returns formatted response")
  void answerPromptAnthropic() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    String body =
        "{\"content\":[{\"text\":\"Hi from Claude\"}],\"usage\":{\"input_tokens\":1,\"output_tokens\":2},\"role\":\"assistant\"}";
    when(connection.answerPrompt(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder().body(SdkBytes.fromUtf8String(body)).build());

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("anthropic.claude-v2", 0.5f, 100);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    String result = service.answerPrompt("Hi", params);
    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("answerPrompt with amazon titan model returns formatted response")
  void answerPromptAmazonTitan() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    when(connection.answerPrompt(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder()
            .body(SdkBytes.fromUtf8String("{\"results\":[{\"outputText\":\"Hi\"}]}"))
            .build());

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.titan-text-express-v1", 0.5f, 100);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    String result = service.answerPrompt("Hi", params);
    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("answerPrompt with ai21 model returns formatted response")
  void answerPromptAi21() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    when(connection.answerPrompt(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder()
            .body(SdkBytes.fromUtf8String("{\"completions\":[{\"data\":{\"text\":\"Hi\"}}]}"))
            .build());

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("ai21.j2-mid-v1", 0.5f, 100);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    String result = service.answerPrompt("Hi", params);
    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("answerPrompt with mistral model returns formatted response")
  void answerPromptMistral() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    when(connection.answerPrompt(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder()
            .body(SdkBytes.fromUtf8String("{\"outputs\":[{\"text\":\"Hi\"}]}"))
            .build());

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("mistral.mistral-7b-instruct-v0:2", 0.5f, 100);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    String result = service.answerPrompt("Hi", params);
    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("answerPrompt with cohere model returns formatted response")
  void answerPromptCohere() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    when(connection.answerPrompt(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder()
            .body(SdkBytes.fromUtf8String("{\"text\":\"Hi\"}"))
            .build());

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("cohere.command-r-v1:0", 0.5f, 100);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    String result = service.answerPrompt("Hi", params);
    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("answerPrompt with meta llama model returns formatted response")
  void answerPromptMetaLlama() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    String body = "{\"generation\":\"Hi\",\"prompt_token_count\":1,\"generation_token_count\":2}";
    when(connection.answerPrompt(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder().body(SdkBytes.fromUtf8String(body)).build());

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("meta.llama3-70b-instruct-v1:0", 0.5f, 100);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    String result = service.answerPrompt("Hi", params);
    assertThat(result).isNotBlank();
  }
}
