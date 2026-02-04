package org.mule.extension.bedrock.internal.helper.payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mule.extension.bedrock.api.params.BedrockParameters;
import org.mule.extension.bedrock.internal.util.BedrockConstants;

@DisplayName("MistralPayloadGenerator")
class MistralPayloadGeneratorTest {

  @Test
  @DisplayName("supports Mistral model IDs")
  void supportsMistral() {
    MistralPayloadGenerator g = new MistralPayloadGenerator();
    assertThat(g.supports("mistral.mistral-7b")).isTrue();
    assertThat(g.supports("mistral.mixtral-8x7b")).isTrue();
    assertThat(g.supports("mistral.pixtral-large")).isTrue();
    assertThat(g.supports("anthropic.claude-3")).isFalse();
  }

  @Nested
  @DisplayName("generatePayload")
  class GeneratePayload {

    @Test
    @DisplayName("Mistral models use prompt with Human/Assistant format")
    void mistralPromptFormat() {
      BedrockParameters params = mock(BedrockParameters.class);
      when(params.getModelName()).thenReturn("mistral.mistral-7b");
      when(params.getTemperature()).thenReturn(0.7f);
      when(params.getMaxTokenCount()).thenReturn(100);
      when(params.getTopP()).thenReturn(null);
      when(params.getTopK()).thenReturn(null);

      MistralPayloadGenerator g = new MistralPayloadGenerator();
      String payload = g.generatePayload("Hello", params);

      assertThat(payload).contains(BedrockConstants.JsonKeys.PROMPT);
      assertThat(payload).contains("Human:");
      assertThat(payload).contains("Assistant:");
      assertThat(payload).contains("Hello");
      assertThat(payload).contains(BedrockConstants.JsonKeys.TEMPERATURE);
    }

    @Test
    @DisplayName("Pixtral models use messages format")
    void pixtralMessagesFormat() {
      BedrockParameters params = mock(BedrockParameters.class);
      when(params.getModelName()).thenReturn("mistral.pixtral-large-2502-v1:0");
      when(params.getTemperature()).thenReturn(0.5f);
      when(params.getMaxTokenCount()).thenReturn(200);
      when(params.getTopP()).thenReturn(null);
      when(params.getTopK()).thenReturn(null);

      MistralPayloadGenerator g = new MistralPayloadGenerator();
      String payload = g.generatePayload("Describe image", params);

      assertThat(payload).contains(BedrockConstants.JsonKeys.MESSAGES);
      assertThat(payload).contains(BedrockConstants.JsonKeys.ROLE);
      assertThat(payload).contains(BedrockConstants.JsonKeys.CONTENT);
      assertThat(payload).contains("Describe image");
    }
  }
}
