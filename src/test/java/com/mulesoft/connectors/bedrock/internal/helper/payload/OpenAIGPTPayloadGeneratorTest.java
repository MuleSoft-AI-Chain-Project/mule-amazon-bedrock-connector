package com.mulesoft.connectors.bedrock.internal.helper.payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.api.params.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.util.BedrockConstants;

@DisplayName("OpenAIGPTPayloadGenerator")
class OpenAIGPTPayloadGeneratorTest {

  @Test
  @DisplayName("supports OpenAI GPT model IDs")
  void supportsOpenAI() {
    OpenAIGPTPayloadGenerator g = new OpenAIGPTPayloadGenerator();
    assertThat(g.supports("openai.gpt-4o")).isTrue();
    assertThat(g.supports("openai.gpt-4o-mini")).isTrue();
    assertThat(g.supports("anthropic.claude-3")).isFalse();
  }

  @Nested
  @DisplayName("generatePayload")
  class GeneratePayload {

    @Test
    @DisplayName("produces messages with role and content")
    void withTopP() {
      BedrockParameters params = mock(BedrockParameters.class);
      when(params.getModelName()).thenReturn("openai.gpt-4o");
      when(params.getTemperature()).thenReturn(0.7f);
      when(params.getMaxTokenCount()).thenReturn(200);
      when(params.getTopP()).thenReturn(0.95f);
      when(params.getTopK()).thenReturn(null);

      OpenAIGPTPayloadGenerator g = new OpenAIGPTPayloadGenerator();
      String payload = g.generatePayload("Hello GPT", params);

      assertThat(payload).contains(BedrockConstants.JsonKeys.MESSAGES);
      assertThat(payload).contains(BedrockConstants.JsonKeys.ROLE);
      assertThat(payload).contains(BedrockConstants.JsonKeys.CONTENT);
      assertThat(payload).contains("Hello GPT");
      assertThat(payload).contains("max_tokens");
      assertThat(payload).contains("0.95");
    }

    @Test
    @DisplayName("works when topP is null")
    void withoutTopP() {
      BedrockParameters params = mock(BedrockParameters.class);
      when(params.getModelName()).thenReturn("openai.gpt-4o");
      when(params.getTemperature()).thenReturn(0.5f);
      when(params.getMaxTokenCount()).thenReturn(100);
      when(params.getTopP()).thenReturn(null);
      when(params.getTopK()).thenReturn(null);

      OpenAIGPTPayloadGenerator g = new OpenAIGPTPayloadGenerator();
      String payload = g.generatePayload("Hi", params);

      assertThat(payload).contains("Hi");
      assertThat(payload).contains("messages");
    }
  }
}
