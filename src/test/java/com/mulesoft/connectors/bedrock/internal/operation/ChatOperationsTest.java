package com.mulesoft.connectors.bedrock.internal.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.mule.runtime.extension.api.exception.ModuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import com.mulesoft.connectors.bedrock.internal.support.IntegrationTestParamHelper;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@DisplayName("ChatOperations")
class ChatOperationsTest {

  @Test
  @DisplayName("extends BedrockOperation and uses ChatService")
  void extendsBedrockOperation() {
    ChatOperations ops = new ChatOperations();
    assertThat(ops).isNotNull();
    assertThat(ops).isInstanceOf(BedrockOperation.class);
  }

  @Test
  @DisplayName("can be instantiated with no-arg constructor")
  void instantiation() {
    assertThat(new ChatOperations()).isNotNull();
  }

  @Test
  @DisplayName("chatAnswerPrompt returns InputStream with response content")
  void chatAnswerPromptReturnsInputStream() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    when(connection.answerPrompt(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder()
            .body(SdkBytes.fromUtf8String("{\"output\":{\"text\":\"Hello\"}}"))
            .build());
    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.5f, 50);

    ChatOperations ops = new ChatOperations();
    InputStream result = ops.chatAnswerPrompt(config, connection, params, "Hi");

    assertThat(result).isNotNull();
    String content = new Scanner(result, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
    assertThat(content).contains("output");
    assertThat(content).contains("Hello");
  }

  @Test
  @DisplayName("chatAnswerPrompt throws ModuleException when connection throws SdkClientException")
  void chatAnswerPromptThrowsWhenSdkClientException() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    when(connection.answerPrompt(any(InvokeModelRequest.class)))
        .thenThrow(software.amazon.awssdk.core.exception.SdkClientException.builder().message("network error").build());
    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.5f, 50);

    ChatOperations ops = new ChatOperations();
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> ops.chatAnswerPrompt(config, connection, params, "Hi"))
        .isInstanceOf(ModuleException.class);
  }

  @Test
  @DisplayName("chatAnswerPromptStreaming returns InputStream")
  void chatAnswerPromptStreamingReturnsInputStream() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.3f, 20);

    ChatOperations ops = new ChatOperations();
    InputStream result = ops.chatAnswerPromptStreaming(config, connection, params, "Stream me");

    assertThat(result).isNotNull();
  }
}
