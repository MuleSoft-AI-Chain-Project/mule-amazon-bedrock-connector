package com.mulesoft.connectors.bedrock.internal.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import com.mulesoft.connectors.bedrock.internal.support.IntegrationTestParamHelper;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@DisplayName("SentimentOperations")
class SentimentOperationsTest {

  @Test
  @DisplayName("extends BedrockOperation and uses SentimentService")
  void extendsBedrockOperation() {
    SentimentOperations ops = new SentimentOperations();
    assertThat(ops).isNotNull();
    assertThat(ops).isInstanceOf(BedrockOperation.class);
  }

  @Test
  @DisplayName("can be instantiated with no-arg constructor")
  void instantiation() {
    assertThat(new SentimentOperations()).isNotNull();
  }

  @Test
  @DisplayName("sentimentAnalysis returns InputStream with response")
  void sentimentAnalysisReturnsInputStream() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    when(connection.answerPrompt(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder()
            .body(SdkBytes.fromUtf8String("{\"output\":{\"text\":\"POSITIVE\"}}"))
            .build());
    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.3f, 50);

    SentimentOperations ops = new SentimentOperations();
    InputStream result = ops.sentimentAnalysis(config, connection, params, "I love it!");

    assertThat(result).isNotNull();
    String content = new Scanner(result, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
    assertThat(content).contains("output");
  }
}
