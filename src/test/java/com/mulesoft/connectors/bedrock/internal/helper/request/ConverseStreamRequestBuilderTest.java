package com.mulesoft.connectors.bedrock.internal.helper.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.api.params.BedrockParameters;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest;

@DisplayName("ConverseStreamRequestBuilder")
class ConverseStreamRequestBuilderTest {

  private static BedrockParameters minimalParams(String modelName) {
    BedrockParameters p = mock(BedrockParameters.class);
    when(p.getModelName()).thenReturn(modelName);
    when(p.getAwsAccountId()).thenReturn(null);
    when(p.getTemperature()).thenReturn(0.7f);
    when(p.getMaxTokenCount()).thenReturn(100);
    when(p.getTopP()).thenReturn(null);
    when(p.getTopK()).thenReturn(null);
    when(p.getGuardrailIdentifier()).thenReturn(null);
    when(p.getGuardrailVersion()).thenReturn(null);
    return p;
  }

  @Test
  @DisplayName("create returns builder instance")
  void createReturnsBuilder() {
    BedrockParameters params = minimalParams("amazon.nova-lite-v1:0");
    ConverseStreamRequestBuilder builder = ConverseStreamRequestBuilder.create(params, "us-east-1", "Hello");
    assertThat(builder).isNotNull();
  }

  @Nested
  @DisplayName("build")
  class Build {

    @Test
    @DisplayName("builds request with modelId and prompt")
    void buildsRequest() {
      BedrockParameters params = minimalParams("amazon.nova-lite-v1:0");
      ConverseStreamRequest request = ConverseStreamRequestBuilder.create(params, "us-east-1", "Hello")
          .build();
      assertThat(request).isNotNull();
      assertThat(request.modelId()).isEqualTo("amazon.nova-lite-v1:0");
      assertThat(request.messages()).hasSize(1);
    }

    @Test
    @DisplayName("builds request with guardrail when identifier and version set")
    void buildsRequestWithGuardrail() {
      BedrockParameters params = minimalParams("amazon.nova-lite-v1:0");
      when(params.getGuardrailIdentifier()).thenReturn("guard-id");
      when(params.getGuardrailVersion()).thenReturn("1");

      ConverseStreamRequest request = ConverseStreamRequestBuilder.create(params, "us-east-1", "Hi")
          .build();

      assertThat(request).isNotNull();
      assertThat(request.guardrailConfig()).isNotNull();
      assertThat(request.guardrailConfig().guardrailIdentifier()).isEqualTo("guard-id");
      assertThat(request.guardrailConfig().guardrailVersion()).isEqualTo("1");
    }

    @Test
    @DisplayName("builds request with inference profile ARN for Claude model")
    void buildsRequestWithInferenceProfileArn() {
      BedrockParameters params = minimalParams("anthropic.claude-3-5-sonnet-v1");
      when(params.getAwsAccountId()).thenReturn("123456789012");

      ConverseStreamRequest request = ConverseStreamRequestBuilder.create(params, "us-east-1", "Hi")
          .build();

      assertThat(request).isNotNull();
      assertThat(request.modelId()).contains("arn:aws:bedrock");
      assertThat(request.modelId()).contains("anthropic.claude");
    }
  }
}
