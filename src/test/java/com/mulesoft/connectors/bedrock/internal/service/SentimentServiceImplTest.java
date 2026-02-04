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

@DisplayName("SentimentServiceImpl")
class SentimentServiceImplTest {

  @Test
  @DisplayName("can be constructed with config and connection")
  void constructor() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    SentimentServiceImpl service = new SentimentServiceImpl(config, connection);
    assertThat(service).isNotNull();
  }

  @Test
  @DisplayName("implements SentimentService and extends BedrockServiceImpl")
  void typeHierarchy() {
    SentimentServiceImpl service = new SentimentServiceImpl(mock(BedrockConfiguration.class), mock(BedrockConnection.class));
    assertThat(service).isInstanceOf(SentimentService.class);
    assertThat(service).isInstanceOf(BedrockServiceImpl.class);
  }

  @Test
  @DisplayName("extractSentiments throws BedrockException when connection throws SdkClientException")
  void extractSentimentsThrowsWhenSdkClientException() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    when(connection.answerPrompt(any(InvokeModelRequest.class)))
        .thenThrow(software.amazon.awssdk.core.exception.SdkClientException.builder().message("client error").build());

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.3f, 50);
    SentimentServiceImpl service = new SentimentServiceImpl(config, connection);
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.extractSentiments("Hello", params))
        .isInstanceOf(BedrockException.class);
  }

  @Test
  @DisplayName("extractSentiments returns formatted response when connection returns success")
  void extractSentimentsReturnsFormattedResponse() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    String responseBody = "{\"output\":{\"text\":\"{\\\"Sentiment\\\":\\\"POSITIVE\\\",\\\"IsPositive\\\":true}\"}}";
    when(connection.answerPrompt(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder().body(SdkBytes.fromUtf8String(responseBody)).build());

    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.3f, 50);
    SentimentServiceImpl service = new SentimentServiceImpl(config, connection);
    String result = service.extractSentiments("I love this product!", params);

    assertThat(result).isNotBlank();
    assertThat(result).contains("output");
  }
}
