package com.mulesoft.connectors.bedrock.internal.helper.payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.util.BedrockConstants;

@DisplayName("LlamaPayloadGenerator")
class LlamaPayloadGeneratorTest {

  @Test
  @DisplayName("supports Meta Llama model IDs")
  void supportsLlama() {
    LlamaPayloadGenerator g = new LlamaPayloadGenerator();
    assertThat(g.supports("meta.llama3-70b-instruct")).isTrue();
    assertThat(g.supports("meta.llama3-1-8b")).isTrue();
    assertThat(g.supports("anthropic.claude-3")).isFalse();
  }

  @Test
  @DisplayName("generatePayload produces prompt and max_gen_len")
  void generatePayload() {
    BedrockParameters params = mock(BedrockParameters.class);
    when(params.getModelName()).thenReturn("meta.llama3-70b-instruct");
    when(params.getTemperature()).thenReturn(0.8f);
    when(params.getMaxTokenCount()).thenReturn(512);
    when(params.getTopP()).thenReturn(0.9f);
    when(params.getTopK()).thenReturn(null);

    LlamaPayloadGenerator g = new LlamaPayloadGenerator();
    String payload = g.generatePayload("Hello Llama", params);

    assertThat(payload).contains(BedrockConstants.JsonKeys.PROMPT);
    assertThat(payload).contains("Hello Llama");
    assertThat(payload).contains(BedrockConstants.JsonKeys.MAX_GEN_LEN);
    assertThat(payload).contains(BedrockConstants.JsonKeys.TEMPERATURE);
    assertThat(payload).contains("topp");
    assertThat(payload).contains("0.9");
  }
}
