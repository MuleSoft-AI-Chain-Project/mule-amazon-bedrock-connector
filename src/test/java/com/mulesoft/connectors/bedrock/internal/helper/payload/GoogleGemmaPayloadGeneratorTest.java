package com.mulesoft.connectors.bedrock.internal.helper.payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.api.params.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.util.BedrockConstants;

@DisplayName("GoogleGemmaPayloadGenerator")
class GoogleGemmaPayloadGeneratorTest {

  @Test
  @DisplayName("supports Google Gemma model IDs")
  void supportsGemma() {
    GoogleGemmaPayloadGenerator g = new GoogleGemmaPayloadGenerator();
    assertThat(g.supports("google.gemma-3-12b")).isTrue();
    assertThat(g.supports("google.gemma-2-27b")).isTrue();
    assertThat(g.supports("meta.llama3")).isFalse();
  }

  @Test
  @DisplayName("generatePayload produces messages with topP when set")
  void generatePayloadWithTopP() {
    BedrockParameters params = mock(BedrockParameters.class);
    when(params.getModelName()).thenReturn("google.gemma-3-12b");
    when(params.getTemperature()).thenReturn(0.6f);
    when(params.getMaxTokenCount()).thenReturn(256);
    when(params.getTopP()).thenReturn(0.9f);
    when(params.getTopK()).thenReturn(null);

    GoogleGemmaPayloadGenerator g = new GoogleGemmaPayloadGenerator();
    String payload = g.generatePayload("Hello Gemma", params);

    assertThat(payload).contains(BedrockConstants.JsonKeys.MESSAGES);
    assertThat(payload).contains("Hello Gemma");
    assertThat(payload).contains("max_tokens");
    assertThat(payload).contains("0.9");
  }

  @Test
  @DisplayName("generatePayload works when topP is null")
  void generatePayloadWithoutTopP() {
    BedrockParameters params = mock(BedrockParameters.class);
    when(params.getModelName()).thenReturn("google.gemma-2-27b");
    when(params.getTemperature()).thenReturn(0.5f);
    when(params.getMaxTokenCount()).thenReturn(100);
    when(params.getTopP()).thenReturn(null);
    when(params.getTopK()).thenReturn(null);

    GoogleGemmaPayloadGenerator g = new GoogleGemmaPayloadGenerator();
    String payload = g.generatePayload("Hi", params);

    assertThat(payload).isNotBlank();
    assertThat(payload).contains("Hi");
  }
}
