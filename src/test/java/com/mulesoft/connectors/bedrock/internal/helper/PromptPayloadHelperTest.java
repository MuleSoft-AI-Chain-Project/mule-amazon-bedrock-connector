package com.mulesoft.connectors.bedrock.internal.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParameters;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@DisplayName("PromptPayloadHelper")
class PromptPayloadHelperTest {

  @Nested
  @DisplayName("identifyPayload")
  class IdentifyPayload {

    @Test
    @DisplayName("returns JSON payload for prompt and parameters")
    void returnsPayload() {
      BedrockParameters params = mock(BedrockParameters.class);
      when(params.getModelName()).thenReturn("amazon.nova-lite-v1:0");
      String payload = PromptPayloadHelper.identifyPayload("Hello", params);
      assertThat(payload).isNotBlank();
      assertThat(payload).contains("Hello");
    }
  }

  @Nested
  @DisplayName("createInvokeRequest")
  class CreateInvokeRequest {

    @Test
    @DisplayName("builds request with modelId and body")
    void buildsRequest() {
      BedrockParameters params = mock(BedrockParameters.class);
      when(params.getModelName()).thenReturn("amazon.titan-text-express-v1");
      when(params.getAwsAccountId()).thenReturn(null);
      String nativeRequest = "{\"inputText\":\"test\"}";
      InvokeModelRequest request = PromptPayloadHelper.createInvokeRequest(
                                                                           params, "us-east-1", nativeRequest);
      assertThat(request).isNotNull();
      assertThat(request.modelId()).isEqualTo("amazon.titan-text-express-v1");
      assertThat(request.body()).isEqualTo(SdkBytes.fromUtf8String(nativeRequest));
    }

    @Test
    @DisplayName("builds request with inference profile ARN for Claude 3 model")
    void buildsRequestWithInferenceProfileArn() {
      BedrockParameters params = mock(BedrockParameters.class);
      when(params.getModelName()).thenReturn("anthropic.claude-3-5-sonnet-v1");
      when(params.getAwsAccountId()).thenReturn("123456789012");
      String nativeRequest = "{}";
      InvokeModelRequest request = PromptPayloadHelper.createInvokeRequest(
                                                                           params, "us-east-1", nativeRequest);
      assertThat(request).isNotNull();
      assertThat(request.modelId()).contains("arn:aws:bedrock");
      assertThat(request.modelId()).contains("anthropic.claude");
    }

    @Test
    @DisplayName("builds request with guardrail when identifier and version set")
    void buildsRequestWithGuardrail() {
      BedrockParameters params = mock(BedrockParameters.class);
      when(params.getModelName()).thenReturn("amazon.nova-lite-v1:0");
      when(params.getAwsAccountId()).thenReturn(null);
      when(params.getGuardrailIdentifier()).thenReturn("guardrail-id");
      when(params.getGuardrailVersion()).thenReturn("1");
      InvokeModelRequest request = PromptPayloadHelper.createInvokeRequest(
                                                                           params, "us-east-1", "{}");
      assertThat(request.guardrailIdentifier()).isEqualTo("guardrail-id");
      assertThat(request.guardrailVersion()).isEqualTo("1");
    }
  }

  @Nested
  @DisplayName("formatBedrockResponse")
  class FormatBedrockResponse {

    @Test
    @DisplayName("formats response using model-specific formatter")
    void formatsResponse() {
      BedrockParameters params = mock(BedrockParameters.class);
      when(params.getModelName()).thenReturn("amazon.nova-lite-v1:0");
      String body = "{\"output\":{\"text\":\"Hi\"}}";
      InvokeModelResponse response = InvokeModelResponse.builder()
          .body(SdkBytes.fromUtf8String(body))
          .build();
      String result = PromptPayloadHelper.formatBedrockResponse(params, response);
      assertThat(result).isNotNull();
      assertThat(result).contains("output");
    }
  }

  @Nested
  @DisplayName("definePromptTemplate")
  class DefinePromptTemplate {

    @Test
    @DisplayName("replaces placeholders with instructions and dataset")
    void replacesPlaceholders() {
      String template = "Template: {{instructions}} and {{dataset}}";
      String result = PromptPayloadHelper.definePromptTemplate(template, "Do it", "Data1");
      assertThat(result).contains("Do it");
      assertThat(result).contains("Data1");
      assertThat(result).doesNotContain("{{instructions}}");
      assertThat(result).doesNotContain("{{dataset}}");
    }

    @Test
    @DisplayName("appends Instructions and Dataset sections")
    void appendsSections() {
      String result = PromptPayloadHelper.definePromptTemplate("", "inst", "data");
      assertThat(result).contains("Instructions:");
      assertThat(result).contains("Dataset:");
      assertThat(result).contains("inst");
      assertThat(result).contains("data");
    }
  }
}
